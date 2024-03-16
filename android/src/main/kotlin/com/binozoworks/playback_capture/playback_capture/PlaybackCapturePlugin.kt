package com.binozoworks.playback_capture.playback_capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** PlaybackCapturePlugin */
class PlaybackCapturePlugin: FlutterPlugin, MethodCallHandler, ActivityAware,
  PluginRegistry.ActivityResultListener,
  EventChannel.StreamHandler {
  private lateinit var context: Context
  private lateinit var activity: Activity
  private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
  private lateinit var channel : MethodChannel

  private var permissionEvents: EventChannel.EventSink? = null

  companion object {
    private const val MEDIA_PROJECTION_REQUEST_CODE = 13
    private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 42

    private const val ERROR_NO_AUDIO_RECORD_PERMISSION = "-1"
    private const val ERROR_RECORD_REQUEST_DENIED = "-2"

    private const val AUDIO_RECORD_PERMISSION_GRANTED = "audio_record_permission_granted"
    private const val AUDIO_RECORD_PERMISSION_NOT_GRANTED = "audio_record_permission_not_granted"

    private const val PLAYBACK_CAPTURE_PERMISSION_GRANTED = "playback_capture_permission_granted"
    private const val PLAYBACK_CAPTURE_PERMISSION_NOT_GRANTED = "playback_capture_permission_not_granted"
  }

  private lateinit var mediaProjectionManager: MediaProjectionManager
  private var audioRecordEncoding: String = "pcm16"
  private var audioRecordSampleRate: Int = 16000


  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "playback_capture")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    this.flutterPluginBinding = flutterPluginBinding

    val eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "audio_data_callback"); // timeHandlerEvent event name
    eventChannel.setStreamHandler(MediaCaptureService)
    val permissionChannel = EventChannel(flutterPluginBinding.binaryMessenger, "permission_callback"); // timeHandlerEvent event name
    permissionChannel.setStreamHandler(this)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  override fun onMethodCall(call: MethodCall, result: Result) {
    if(call.method == "startAudioListening") {
      audioRecordEncoding = call.argument<String>("encoding") as String
      audioRecordSampleRate = call.argument<Int>("sample_rate") as Int

      if(!isRecordAudioPermissionGranted()) {
        result.error(ERROR_NO_AUDIO_RECORD_PERMISSION, null, null)
        return
      }

      mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
      activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE)

      result.success(null)
    } else if(call.method == "stopAudioListening") {
      val audioCaptureIntent = Intent(context, MediaCaptureService::class.java).apply {
        action = MediaCaptureService.ACTION_STOP
      }
      ContextCompat.startForegroundService(context, audioCaptureIntent)
      result.success(null)
    } else if(call.method == "audioRecordingGranted") {
      result.success(isRecordAudioPermissionGranted())
    } /*else if(call.method == "requestAudioRecordingPermission") {
      requestRecordAudioPermission()
      result.success(null)
    } */else {
      result.notImplemented()
    }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity;
    binding.addActivityResultListener(this)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        // Request allowed
        val audioCaptureIntent = Intent(context, MediaCaptureService::class.java).apply {
          action = MediaCaptureService.ACTION_START
          putExtra(MediaCaptureService.EXTRA_RESULT_DATA, data!!)
          putExtra("encoding", audioRecordEncoding)
          putExtra("sample_rate", audioRecordSampleRate)
        }
        ContextCompat.startForegroundService(context, audioCaptureIntent)
        permissionEvents?.success(PLAYBACK_CAPTURE_PERMISSION_GRANTED)
      } else {
        // Request denied
        permissionEvents?.success(PLAYBACK_CAPTURE_PERMISSION_NOT_GRANTED)
      }
    }

    return true
  }

  private fun isRecordAudioPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
  }

  /*private fun requestRecordAudioPermission() {
    ActivityCompat.requestPermissions(
      activity,
      arrayOf(Manifest.permission.RECORD_AUDIO),
      RECORD_AUDIO_PERMISSION_REQUEST_CODE
    )
  }

  // Does not work
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ): Boolean {
    println("Request $requestCode result: $grantResults")
    if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
      if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
        // Permission granted
        permissionEvents?.success(AUDIO_RECORD_PERMISSION_GRANTED)
      } else {
        // Permission denied
        permissionEvents?.success(AUDIO_RECORD_PERMISSION_NOT_GRANTED)
      }
    }
    return true
  }*/

  override fun onDetachedFromActivityForConfigChanges() {}

  override fun onDetachedFromActivity() {}

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    permissionEvents = events
  }

  override fun onCancel(arguments: Any?) {
    permissionEvents = null
  }
}
