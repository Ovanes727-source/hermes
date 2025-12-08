package com.hermes.translator.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class AudioCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioCaptureManager"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var captureThread: Thread? = null

    private val prefs by lazy { context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE) }

    @SuppressLint("MissingPermission")
    fun startSystemAudioCapture(onAudioChunk: (ByteArray) -> Unit) {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size: $minBufferSize")
            return
        }

        val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            captureThread = Thread {
                val sensitivity = prefs.getInt("audio_sensitivity", 50)
                val threshold = calculateThreshold(sensitivity)
                val buffer = ByteArray(minBufferSize)
                val accumulatedBuffer = mutableListOf<Byte>()
                val chunkSize = SAMPLE_RATE / 2

                Log.d(TAG, "Audio capture started with threshold: $threshold")

                while (isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1

                    if (bytesRead > 0) {
                        val amplitude = calculateAmplitude(buffer, bytesRead)

                        if (amplitude > threshold) {
                            accumulatedBuffer.addAll(buffer.take(bytesRead))

                            if (accumulatedBuffer.size >= chunkSize * 2) {
                                val chunk = accumulatedBuffer.toByteArray()
                                onAudioChunk(chunk)
                                accumulatedBuffer.clear()
                            }
                        } else if (accumulatedBuffer.isNotEmpty()) {
                            if (accumulatedBuffer.size > chunkSize / 2) {
                                val chunk = accumulatedBuffer.toByteArray()
                                onAudioChunk(chunk)
                            }
                            accumulatedBuffer.clear()
                        }
                    } else if (bytesRead < 0) {
                        Log.e(TAG, "Error reading audio: $bytesRead")
                        break
                    }
                }

                Log.d(TAG, "Audio capture stopped")
            }

            captureThread?.priority = Thread.MAX_PRIORITY
            captureThread?.start()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio capture", e)
            stopCapture()
        }
    }

    fun stopCapture() {
        isRecording.set(false)

        try {
            captureThread?.join(1000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while stopping capture thread")
        }

        captureThread = null

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord", e)
        }

        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord", e)
        }

        audioRecord = null
        Log.d(TAG, "Audio capture resources released")
    }

    private fun calculateAmplitude(buffer: ByteArray, length: Int): Double {
        var sum = 0.0
        for (i in 0 until length step 2) {
            if (i + 1 < length) {
                val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                sum += sample * sample
            }
        }
        return kotlin.math.sqrt(sum / (length / 2))
    }

    private fun calculateThreshold(sensitivity: Int): Double {
        return when {
            sensitivity < 33 -> 3000.0
            sensitivity < 66 -> 1500.0
            else -> 500.0
        }
    }

    fun isRecording(): Boolean = isRecording.get()
}
