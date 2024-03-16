# Flutter Playback Capture

Easily capture your System's audio

#### ⚠️ Attention: This api is only available for Android

## Quick Example
1. Import:
```dart
import 'package:playback_capture/playback_capture.dart';
```

2. Implement the following:
```dart
final _playbackCapture = PlaybackCapture();
final PlaybackCaptureResult playbackCaptureResult = await _playbackCapture.listenAudio(
    audioDataCallback: (Uint8List data) {
        // TODO: Do something with your data
    },
);
if (playbackCaptureResult != PlaybackCaptureResult.recording) {
    if (playbackCaptureResult == PlaybackCaptureResult.missingAudioRecordPermission) {
      // You have to ask for permission to use the microphone
      // Ask for permission using https://pub.dev/packages/permission_handler
      await Permission.microphone.request();
    } else if(playbackCaptureResult == PlaybackCaptureResult.recordRequestDenied) {
      // TODO: User denied capturing
    }
} else {
// Recording successfully started
}
```

## Important Notes
- This api is only supported on Android Q (10) and up
- Apps that specify `android:allowAudioPlaybackCapture="false"` in their Manifest.xml will not be captured. [See docs](https://developer.android.com/media/platform/av-capture#capture_policy)
