import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'data/config/audioencoding.dart';
import 'playback_capture_method_channel.dart';

abstract class PlaybackCapturePlatform extends PlatformInterface {
  /// Constructs a PlaybackCapturePlatform.
  PlaybackCapturePlatform() : super(token: _token);

  static final Object _token = Object();

  static PlaybackCapturePlatform _instance = MethodChannelPlaybackCapture();

  /// The default instance of [PlaybackCapturePlatform] to use.
  ///
  /// Defaults to [MethodChannelPlaybackCapture].
  static PlaybackCapturePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PlaybackCapturePlatform] when
  /// they register themselves.
  static set instance(PlaybackCapturePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> listenAudio(AudioEncoding encoding, int sampleRate, int sampleReadSize) {
    throw UnimplementedError('listenAudio() has not been implemented.');
  }

  Future<void> stopAudioListening() {
    throw UnimplementedError('stopAudioListening() has not been implemented.');
  }

  Future<bool> isAudioRecordingPermissionGranted() {
    throw UnimplementedError('isAudioRecordingPermissionGranted() has not been implemented.');
  }
}
