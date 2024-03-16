import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:playback_capture/data/config/audioencoding.dart';
import 'package:playback_capture/playback_capture_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelPlaybackCapture platform = MethodChannelPlaybackCapture();
  const MethodChannel channel = MethodChannel('playback_capture');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        switch (methodCall.method) {
          case "startAudioListening":
            return null;
          case "stopAudioListening":
            return null;
          case "audioRecordingGranted":
            return true;
        }
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('listenAudio', () async {
    await platform.listenAudio(AudioEncoding.pcm16, 16000);
  });

  test('stopAudioListening', () async {
    await platform.stopAudioListening();
  });

  test('isAudioRecordingPermissionGranted', () async {
    final granted = await platform.isAudioRecordingPermissionGranted();
    expect(granted, true);
  });
}
