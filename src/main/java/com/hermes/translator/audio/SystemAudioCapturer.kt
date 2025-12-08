package com.hermes.translator.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.Q)
class SystemAudioCapturer(private val context: Context) {

    companion object {
        private const val TAG = "SystemAudioCapturer"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private val isCapturing = AtomicBoolean(false)
    private var captureThread: Thread? = null

    fun startCapture(
        mediaProjection: MediaProjection,
        onAudioChunk: (ByteArray) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (isCapturing.get()) {
            Log.w(TAG, "Already capturing system audio")
            return
        }

        try {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufferSize * 2)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord failed to initialize for system capture")
            }

            audioRecord?.startRecording()
            isCapturing.set(true)

            captureThread = Thread {
                val buffer = ByteArray(minBufferSize)
                val accumulatedBuffer = mutableListOf<Byte>()
                val chunkSize = SAMPLE_RATE

                Log.d(TAG, "System audio capture started")

                while (isCapturing.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1

                    if (bytesRead > 0) {
                        accumulatedBuffer.addAll(buffer.take(bytesRead))

                        if (accumulatedBuffer.size >= chunkSize * 2) {
                            val chunk = accumulatedBuffer.toByteArray()
                            onAudioChunk(chunk)
                            accumulatedBuffer.clear()
                        }
                    } else if (bytesRead < 0) {
                        Log.e(TAG, "Error reading system audio: $bytesRead")
                        break
                    }
                }

                if (accumulatedBuffer.isNotEmpty()) {
                    onAudioChunk(accumulatedBuffer.toByteArray())
                }

                Log.d(TAG, "System audio capture stopped")
            }

            captureThread?.priority = Thread.MAX_PRIORITY
            captureThread?.start()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting system audio capture", e)
            onError(e)
            stopCapture()
        }
    }

    fun stopCapture() {
        isCapturing.set(false)

        try {
            captureThread?.join(1000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while stopping capture thread")
        }

        captureThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord", e)
        }

        audioRecord = null
        Log.d(TAG, "System audio capture resources released")
    }

    fun isCapturing(): Boolean = isCapturing.get()
}
