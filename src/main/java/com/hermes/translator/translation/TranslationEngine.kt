package com.hermes.translator.translation

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationEngine(private val context: Context) {

    companion object {
        private const val TAG = "TranslationEngine"
        private const val CACHE_SIZE = 500
    }

    private var translator: Translator? = null
    private var currentSourceLanguage: String = TranslateLanguage.ENGLISH
    private val targetLanguage: String = TranslateLanguage.RUSSIAN

    private val translationCache = LruCache<String, String>(CACHE_SIZE)

    private val prefs by lazy { context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE) }

    private val gamingPhrases = mapOf(
        "gg" to "хорошая игра",
        "wp" to "хорошо сыграно",
        "noob" to "новичок",
        "pro" to "профессионал",
        "afk" to "отошел от клавиатуры",
        "lol" to "ржу в голос",
        "omg" to "о боже мой",
        "wtf" to "что за ерунда",
        "ez" to "легко",
        "rekt" to "разгромлен",
        "rush" to "штурм",
        "camp" to "засада",
        "spawn" to "точка появления",
        "respawn" to "перерождение",
        "headshot" to "выстрел в голову",
        "combo" to "комбо",
        "buff" to "усиление",
        "nerf" to "ослабление",
        "lag" to "задержка",
        "ping" to "пинг"
    )

    init {
        initializeTranslator()
    }

    private fun initializeTranslator() {
        val sourceLanguage = prefs.getString("source_language", "en") ?: "en"
        setSourceLanguage(sourceLanguage)
    }

    fun setSourceLanguage(languageCode: String) {
        val mlkitLanguage = when (languageCode) {
            "en" -> TranslateLanguage.ENGLISH
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "zh" -> TranslateLanguage.CHINESE
            "de" -> TranslateLanguage.GERMAN
            "fr" -> TranslateLanguage.FRENCH
            "es" -> TranslateLanguage.SPANISH
            else -> TranslateLanguage.ENGLISH
        }

        if (mlkitLanguage != currentSourceLanguage) {
            translator?.close()
            currentSourceLanguage = mlkitLanguage
            createTranslator()
        }
    }

    private fun createTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(currentSourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()

        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Log.d(TAG, "Translation model downloaded successfully")
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Error downloading translation model", e)
            }
    }

    suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        val normalizedText = text.trim().lowercase()

        gamingPhrases[normalizedText]?.let { return@withContext it }

        translationCache.get(normalizedText)?.let { return@withContext it }

        return@withContext try {
            val result = translateWithMLKit(text)
            translationCache.put(normalizedText, result)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            text
        }
    }

    private suspend fun translateWithMLKit(text: String): String =
        suspendCancellableCoroutine { continuation ->
            translator?.translate(text)
                ?.addOnSuccessListener { translatedText ->
                    if (continuation.isActive) {
                        continuation.resume(translatedText)
                    }
                }
                ?.addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
        }

    fun clearCache() {
        translationCache.evictAll()
    }

    fun getCacheSize(): Int = translationCache.size()

    fun release() {
        translator?.close()
        translator = null
        translationCache.evictAll()
    }

    suspend fun downloadModels(
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val conditions = DownloadConditions.Builder()
                .build()

            suspendCancellableCoroutine<Unit> { continuation ->
                translator?.downloadModelIfNeeded(conditions)
                    ?.addOnSuccessListener {
                        onComplete()
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                    ?.addOnFailureListener { e ->
                        onError(e)
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
