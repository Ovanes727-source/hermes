package com.hermes.translator.translation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class TTSManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TTSManager"
    }

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val speechQueue = ConcurrentLinkedQueue<String>()
    private var isSpeaking = false

    private val prefs by lazy { context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE) }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private var audioFocusRequest: AudioFocusRequest? = null

    enum class VoiceType(val displayName: String) {
        HERMES_MALE("Гермес"),
        ATHENA_FEMALE("Афина"),
        CYBER_NEUTRAL("Киборг")
    }

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("ru", "RU"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Russian language not supported")
                textToSpeech?.setLanguage(Locale.US)
            }

            applyVoiceSettings()
            setupUtteranceListener()

            isInitialized = true
            Log.d(TAG, "TTS initialized successfully")

            processQueue()
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
            isInitialized = false
        }
    }

    private fun applyVoiceSettings() {
        val speedProgress = prefs.getInt("tts_speed", 50)
        val speed = 0.5f + (speedProgress / 100f) * 1.5f
        textToSpeech?.setSpeechRate(speed)

        val voiceTypeString = prefs.getString("voice_type", "hermes_male")
        val voiceType = VoiceType.values().find { it.name.lowercase() == voiceTypeString }
            ?: VoiceType.HERMES_MALE

        applyVoiceProfile(voiceType)
    }

    private fun applyVoiceProfile(voiceType: VoiceType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = textToSpeech?.voices ?: return

            val targetVoice = when (voiceType) {
                VoiceType.HERMES_MALE -> voices.find {
                    it.locale.language == "ru" && it.name.contains("male", ignoreCase = true)
                }
                VoiceType.ATHENA_FEMALE -> voices.find {
                    it.locale.language == "ru" && it.name.contains("female", ignoreCase = true)
                }
                VoiceType.CYBER_NEUTRAL -> voices.find {
                    it.locale.language == "ru"
                }
            }

            targetVoice?.let {
                textToSpeech?.voice = it
                Log.d(TAG, "Voice set to: ${it.name}")
            }
        }

        val pitch = when (voiceType) {
            VoiceType.HERMES_MALE -> 0.9f
            VoiceType.ATHENA_FEMALE -> 1.1f
            VoiceType.CYBER_NEUTRAL -> 1.0f
        }
        textToSpeech?.setPitch(pitch)
    }

    private fun setupUtteranceListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                releaseAudioFocus()
                processQueue()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                releaseAudioFocus()
                processQueue()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "TTS error: $errorCode")
                isSpeaking = false
                releaseAudioFocus()
                processQueue()
            }
        })
    }

    fun speak(text: String) {
        if (text.isBlank()) return

        speechQueue.add(text)

        if (!isSpeaking && isInitialized) {
            processQueue()
        }
    }

    private fun processQueue() {
        if (isSpeaking || speechQueue.isEmpty()) return

        val text = speechQueue.poll() ?: return

        if (!requestAudioFocus()) {
            Log.w(TAG, "Could not get audio focus")
        }

        val utteranceId = UUID.randomUUID().toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = android.os.Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { }
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun stop() {
        speechQueue.clear()
        textToSpeech?.stop()
        isSpeaking = false
        releaseAudioFocus()
    }

    fun release() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }

    fun isReady(): Boolean = isInitialized

    fun isSpeaking(): Boolean = isSpeaking
}
