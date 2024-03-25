import 'dart:async';

import 'package:flutter/services.dart';
import 'package:playback_capture/data/config/audioencoding.dart';

import 'data/playback_capture_result.dart';
import 'playback_capture_platform_interface.dart';

class PlaybackCapture {
  static const _permissionEventChannel = "permission_callback";
  static const _audioDataEventChannel = "audio_data_callback";

  static const _missingAudioRecordPermissionError = "-1";
  static const _audioRecordRequestDeniedError = "-2";
  static const _audioRecordPermissionGranted = "audio_record_permission_granted";
  static const _audioRecordPermissionNotGranted = "audio_record_permission_not_granted";
  static const _playbackCapturePermissionGranted = "playback_capture_permission_granted";
  static const _playbackCapturePermissionNotGranted = "playback_capture_permission_not_granted";
  StreamSubscription? _audioStream;

  Future<void> stopListening() async {
    if (_audioStream != null) {
      _audioStream!.cancel();
      await PlaybackCapturePlatform.instance.stopAudioListening();
      _audioStream = null;
    }
  }

  /// Starts Audio recording
  /// Specify your preferred [encoding] such as e.g. [AudioEncoding.pcm16] and define the [sampleRate] (defaults to `16000`)
  /// Additionally you can specify the [sampleReadSize]. With this you can adjust the latency and quality of your audio. A small number like `1024` decreases latency but may reduce quality, a bigger number increases latency but may increase quality.
  Future<PlaybackCaptureResult> listenAudio(
      {AudioEncoding encoding = AudioEncoding.pcm16, int sampleRate = 16000, int sampleReadSize = 1024 * 4, required Function(Uint8List data) audioDataCallback}) async {
    try {
      // Initialize our permission callback
      // This is critical because the user can deny this request every time
      const permissionEventChannel = EventChannel(_permissionEventChannel);
      Stream<dynamic> permissionStream = permissionEventChannel.receiveBroadcastStream();

      // Now start the capture process
      // The Android System will now show the user a popup about allowing this App to listen to system audio
      await PlaybackCapturePlatform.instance.listenAudio(encoding, sampleRate, sampleReadSize);

      // Check if the permissions are alright
      final permissionStatus = await permissionStream.first;
      if (permissionStatus == _playbackCapturePermissionNotGranted) {
        // User denied system-dialog for allowing playback capture
        return PlaybackCaptureResult.recordRequestDenied;
      }

      // Successfully initialized playback recording
      const audioEventChannel = EventChannel(_audioDataEventChannel);
      _audioStream = audioEventChannel.receiveBroadcastStream().listen((data) {
        audioDataCallback(data as Uint8List);
      });
      return PlaybackCaptureResult.recording;
    } on PlatformException catch (e) {
      if (e.code == _missingAudioRecordPermissionError) {
        return PlaybackCaptureResult.missingAudioRecordPermission;
      } else if (e.code == _audioRecordRequestDeniedError) {
        return PlaybackCaptureResult.recordRequestDenied;
      }
    }
    return PlaybackCaptureResult.missingAudioRecordPermission;
  }

  Future<bool> isAudioRecordingPermissionGranted() async {
    return await PlaybackCapturePlatform.instance.isAudioRecordingPermissionGranted();
  }
}
