import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:playback_capture/data/config/audioencoding.dart';
import 'package:playback_capture/data/playback_capture_result.dart';
import 'package:playback_capture/playback_capture.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _playbackCapturePlugin = PlaybackCapture();
  int _readTotal = 0;
  bool _capturing = false;

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    _playbackCapturePlugin.stopListening();
    return super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Playback capture example'),
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ElevatedButton(
                onPressed: () async {
                  if (_capturing) {
                    await _playbackCapturePlugin.stopListening();
                    setState(() {
                      _capturing = false;
                    });
                    return;
                  }

                  final PlaybackCaptureResult playbackCaptureResult = await _playbackCapturePlugin.listenAudio(
                    encoding: AudioEncoding.pcm16,
                    sampleRate: 16000,
                    audioDataCallback: (Uint8List data) {
                      setState(() {
                        _readTotal += data.length;
                      });
                    },
                  );
                  if (playbackCaptureResult != PlaybackCaptureResult.recording) {
                    if (playbackCaptureResult == PlaybackCaptureResult.missingAudioRecordPermission) {
                      await Permission.microphone.request();
                    } else if (playbackCaptureResult == PlaybackCaptureResult.recordRequestDenied) {
                      // TODO: User denied capturing
                    }
                  } else {
                    setState(() {
                      _capturing = true;
                    });
                  }
                },
                child: Text(_capturing ? "Stop" : "Start"),
              ),
              const SizedBox(
                height: 20,
              ),
              Text("Data read total: $_readTotal bytes"),
            ],
          ),
        ),
      ),
    );
  }
}
