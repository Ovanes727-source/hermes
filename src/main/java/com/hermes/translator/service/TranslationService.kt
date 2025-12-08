package com.hermes.translator.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.hermes.translator.HermesApplication
import com.hermes.translator.MainActivity
import com.hermes.translator.R
import com.hermes.translator.audio.AudioCaptureManager
import com.hermes.translator.overlay.GameOverlayManager
import com.hermes.translator.translation.SpeechRecognitionHelper
import com.hermes.translator.translation.TTSManager
import com.hermes.translator.translation.TranslationEngine
import kotlinx.coroutines.*

class TranslationService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        
        @Volatile
        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var audioCaptureManager: AudioCaptureManager
    private lateinit var speechRecognizer: SpeechRecognitionHelper
    private lateinit var translationEngine: TranslationEngine
    private lateinit var ttsManager: TTSManager
    private lateinit var overlayManager: GameOverlayManager

    private val prefs by lazy { getSharedPreferences("hermes_prefs", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        audioCaptureManager = AudioCaptureManager(this)
        speechRecognizer = SpeechRecognitionHelper(this)
        translationEngine = TranslationEngine(this)
        ttsManager = TTSManager(this)
        overlayManager = GameOverlayManager(this)

        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTranslationPipeline()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false

        serviceScope.cancel()
        audioCaptureManager.stopCapture()
        speechRecognizer.release()
        ttsManager.release()
        overlayManager.hideOverlay()
    }

    private fun startTranslationPipeline() {
        val sourceLanguage = prefs.getString("source_language", "en") ?: "en"
        val showOriginal = prefs.getBoolean("show_original", true)
        val ttsEnabled = prefs.getBoolean("tts_enabled", true)

        translationEngine.setSourceLanguage(sourceLanguage)

        serviceScope.launch {
            audioCaptureManager.startSystemAudioCapture { audioBuffer ->
                processAudioChunk(audioBuffer, showOriginal, ttsEnabled)
            }
        }
    }

    private fun processAudioChunk(audioBuffer: ByteArray, showOriginal: Boolean, ttsEnabled: Boolean) {
        serviceScope.launch {
            try {
                val recognizedText = speechRecognizer.recognize(audioBuffer)

                if (recognizedText.isNotBlank() && recognizedText.length > 2) {
                    val translatedText = translationEngine.translate(recognizedText)

                    mainHandler.post {
                        if (showOriginal) {
                            overlayManager.showTranslation(recognizedText, translatedText)
                        } else {
                            overlayManager.showTranslation("", translatedText)
                        }
                    }

                    if (ttsEnabled && translatedText.isNotBlank()) {
                        ttsManager.speak(translatedText)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TranslationService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, HermesApplication.CHANNEL_ID_TRANSLATION)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_hermes_wings)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.stop), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
