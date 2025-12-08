package com.hermes.translator.translation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SpeechRecognitionHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognition"
        private const val SAMPLE_RATE = 16000f
        private const val MODEL_PATH = "vosk-model-small-en-us"
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var isInitialized = false

    private val prefs by lazy { context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE) }

    init {
        initializeModel()
    }

    private fun initializeModel() {
        try {
            val modelDir = File(context.filesDir, MODEL_PATH)

            if (modelDir.exists() && modelDir.isDirectory) {
                model = Model(modelDir.absolutePath)
                recognizer = Recognizer(model, SAMPLE_RATE)
                isInitialized = true
                Log.d(TAG, "Vosk model loaded successfully")
            } else {
                Log.w(TAG, "Vosk model not found. Please download the model.")
                isInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Vosk model", e)
            isInitialized = false
        }
    }

    suspend fun recognize(audioBuffer: ByteArray): String = withContext(Dispatchers.Default) {
        if (!isInitialized || recognizer == null) {
            return@withContext ""
        }

        try {
            val isFinal = recognizer?.acceptWaveForm(audioBuffer, audioBuffer.size) ?: false

            val resultJson = if (isFinal) {
                recognizer?.result
            } else {
                recognizer?.partialResult
            }

            return@withContext parseResult(resultJson, isFinal)

        } catch (e: Exception) {
            Log.e(TAG, "Error during recognition", e)
            return@withContext ""
        }
    }

    private fun parseResult(jsonString: String?, isFinal: Boolean): String {
        if (jsonString.isNullOrBlank()) return ""

        return try {
            val json = JSONObject(jsonString)
            if (isFinal) {
                json.optString("text", "")
            } else {
                json.optString("partial", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing recognition result", e)
            ""
        }
    }

    fun reset() {
        try {
            recognizer?.reset()
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting recognizer", e)
        }
    }

    fun release() {
        try {
            recognizer?.close()
            model?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }

        recognizer = null
        model = null
        isInitialized = false
    }

    fun isReady(): Boolean = isInitialized

    suspend fun downloadModel(
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Model download placeholder - implement actual download")
            onComplete()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            onError(e)
        }
    }

    fun getModelSize(): Long {
        val modelDir = File(context.filesDir, MODEL_PATH)
        return if (modelDir.exists()) {
            modelDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else {
            0L
        }
    }

    fun isModelDownloaded(): Boolean {
        val modelDir = File(context.filesDir, MODEL_PATH)
        return modelDir.exists() && modelDir.isDirectory && modelDir.listFiles()?.isNotEmpty() == true
    }
}
