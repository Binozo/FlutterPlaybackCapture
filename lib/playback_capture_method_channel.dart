import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'data/config/audioencoding.dart';
import 'playback_capture_platform_interface.dart';

/// An implementation of [PlaybackCapturePlatform] that uses method channels.
class MethodChannelPlaybackCapture extends PlaybackCapturePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('playback_capture');

  @override
  Future<void> listenAudio(AudioEncoding encoding, int sampleRate) async {
    await methodChannel.invokeMethod("startAudioListening", {
      "encoding": encoding.name,
      "sample_rate": sampleRate,
    });
  }

  @override
  Future<void> stopAudioListening() async {
    await methodChannel.invokeMethod("stopAudioListening");
  }

  @override
  Future<bool> isAudioRecordingPermissionGranted() async {
    return await methodChannel.invokeMethod<bool>("audioRecordingGranted") ?? false;
  }
}
