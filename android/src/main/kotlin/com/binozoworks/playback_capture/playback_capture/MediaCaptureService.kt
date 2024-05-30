package com.binozoworks.playback_capture.playback_capture

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.flutter.embedding.engine.plugins.service.ServiceAware
import io.flutter.embedding.engine.plugins.service.ServicePluginBinding
import io.flutter.plugin.common.EventChannel
import kotlin.concurrent.thread

class MediaCaptureService : ServicePluginBinding, Service(), EventChannel.StreamHandler {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private lateinit var audioCaptureThread: Thread

    companion object : EventChannel.StreamHandler {
        private const val LOG_TAG = "AudioCaptureService"
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"

        private var CHANNELS = AudioFormat.CHANNEL_IN_STEREO
        private var NUM_SAMPLES_PER_READ = 1024 * 4
        private var BYTES_PER_SAMPLE = 2 // default 16-bit PCM
        private var BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE // default 16-bit PCM

        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"

        private var eventSink: EventChannel.EventSink? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            eventSink = events
        }

        override fun onCancel(arguments: Any?) {
            eventSink = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(SERVICE_ID, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build())

        mediaProjectionManager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent.getParcelableExtra(EXTRA_RESULT_DATA)!!) as MediaProjection
                    startAudioCapture(
                            intent.getStringExtra("encoding") as String,
                            intent.getIntExtra("sample_rate", 16000),
                            intent.getIntExtra("sample_read_size", NUM_SAMPLES_PER_READ),
                            intent.getIntExtra("channel_count", CHANNELS))
                    Service.START_STICKY
                }
                ACTION_STOP -> {
                    stopAudioCapture()
                    Service.START_NOT_STICKY
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            Service.START_NOT_STICKY
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
            "Audio Capture Service", NotificationManager.IMPORTANCE_DEFAULT)

        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startAudioCapture(encoding: String, sampleRate: Int, sampleReadSize: Int, channels: Int) {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        var audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        // Parse Audio Encoding
        when(encoding) {
            "pcm8" -> {
                audioEncoding = AudioFormat.ENCODING_PCM_8BIT
                BYTES_PER_SAMPLE = 1
            }
            "pcm16" -> {
                audioEncoding = AudioFormat.ENCODING_PCM_16BIT
                BYTES_PER_SAMPLE = 2
            }
            "pcm24" -> {
                audioEncoding = AudioFormat.ENCODING_PCM_24BIT_PACKED
                BYTES_PER_SAMPLE = 3
            }
            "pcm32" -> {
                audioEncoding = AudioFormat.ENCODING_PCM_32BIT
                BYTES_PER_SAMPLE = 4
            }
        }
        NUM_SAMPLES_PER_READ = sampleReadSize
        BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE

        val audioFormat = AudioFormat.Builder()
            .setEncoding(audioEncoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channels)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord!!.startRecording()

        audioCaptureThread = thread(start = true) {
            listenAudio()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun stopAudioCapture() {
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }

        audioCaptureThread.interrupt()
        audioCaptureThread.join()

        audioRecord!!.stop()
        audioRecord!!.release()
        audioRecord = null

        mediaProjection!!.stop()
        stopSelf()
    }

    private fun listenAudio() {
        val capturedAudioSamples = ByteArray(NUM_SAMPLES_PER_READ)

        while (!audioCaptureThread.isInterrupted) {
            audioRecord?.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)
            Handler(mainLooper).post {
                eventSink?.success(capturedAudioSamples)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun getService(): Service {
        return this
    }

    override fun getLifecycle(): Any? {
        TODO("Not yet implemented")
    }

    override fun addOnModeChangeListener(listener: ServiceAware.OnModeChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnModeChangeListener(listener: ServiceAware.OnModeChangeListener) {
        TODO("Not yet implemented")
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        TODO("Not yet implemented")
    }

    override fun onCancel(arguments: Any?) {
        TODO("Not yet implemented")
    }
}