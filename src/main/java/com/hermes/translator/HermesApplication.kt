package com.hermes.translator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class HermesApplication : Application() {

    companion object {
        const val CHANNEL_ID_TRANSLATION = "hermes_translation_channel"
        const val CHANNEL_ID_AUDIO = "hermes_audio_channel"
        
        lateinit var instance: HermesApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val translationChannel = NotificationChannel(
                CHANNEL_ID_TRANSLATION,
                getString(R.string.channel_translation_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_translation_description)
                setShowBadge(false)
            }

            val audioChannel = NotificationChannel(
                CHANNEL_ID_AUDIO,
                getString(R.string.channel_audio_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_audio_description)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(translationChannel)
            notificationManager.createNotificationChannel(audioChannel)
        }
    }
}
