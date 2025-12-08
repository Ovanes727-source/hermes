package com.hermes.translator.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hermes.translator.HermesApplication
import com.hermes.translator.MainActivity
import com.hermes.translator.R
import com.hermes.translator.audio.AudioCaptureManager
import kotlinx.coroutines.*

class AudioCaptureService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1002

        @Volatile
        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var audioCaptureManager: AudioCaptureManager

    private var audioCallback: ((ByteArray) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        audioCaptureManager = AudioCaptureManager(this)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startCapture()
            "STOP" -> stopCapture()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        audioCaptureManager.stopCapture()
    }

    fun setAudioCallback(callback: (ByteArray) -> Unit) {
        audioCallback = callback
    }

    private fun startCapture() {
        serviceScope.launch {
            audioCaptureManager.startSystemAudioCapture { buffer ->
                audioCallback?.invoke(buffer)
            }
        }
    }

    private fun stopCapture() {
        audioCaptureManager.stopCapture()
        stopSelf()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, HermesApplication.CHANNEL_ID_AUDIO)
            .setContentTitle(getString(R.string.audio_capture_title))
            .setContentText(getString(R.string.audio_capture_text))
            .setSmallIcon(R.drawable.ic_microphone)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
