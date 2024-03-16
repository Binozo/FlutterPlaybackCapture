import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:playback_capture/data/config/audioencoding.dart';
import 'package:playback_capture/data/playback_capture_result.dart';
import 'package:playback_capture/playback_capture.dart';
import 'package:playback_capture/playback_capture_method_channel.dart';
import 'package:playback_capture/playback_capture_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockPlaybackCapturePlatform with MockPlatformInterfaceMixin implements PlaybackCapturePlatform {
  @override
  Future<bool> isAudioRecordingPermissionGranted() async {
    return true;
  }

  @override
  Future<void> listenAudio(AudioEncoding encoding, int sampleRate) async {
    return;
  }

  @override
  Future<void> stopAudioListening() async {
    return;
  }
}

void main() {
  final PlaybackCapturePlatform initialPlatform = PlaybackCapturePlatform.instance;

  test('$MethodChannelPlaybackCapture is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelPlaybackCapture>());
  });

  test('listenAudio', () async {
    PlaybackCapture playbackCapturePlugin = PlaybackCapture();
    MockPlaybackCapturePlatform fakePlatform = MockPlaybackCapturePlatform();
    PlaybackCapturePlatform.instance = fakePlatform;

    expect(await playbackCapturePlugin.listenAudio(audioDataCallback: (Uint8List data) {}), PlaybackCaptureResult.recording);
  });

  test('stopAudioListening', () async {
    PlaybackCapture playbackCapturePlugin = PlaybackCapture();
    MockPlaybackCapturePlatform fakePlatform = MockPlaybackCapturePlatform();
    PlaybackCapturePlatform.instance = fakePlatform;

    await playbackCapturePlugin.stopListening();
  });

  test('isAudioRecordingPermissionGranted', () async {
    PlaybackCapture playbackCapturePlugin = PlaybackCapture();
    MockPlaybackCapturePlatform fakePlatform = MockPlaybackCapturePlatform();
    PlaybackCapturePlatform.instance = fakePlatform;

    expect(await playbackCapturePlugin.isAudioRecordingPermissionGranted(), true);
  });
}
