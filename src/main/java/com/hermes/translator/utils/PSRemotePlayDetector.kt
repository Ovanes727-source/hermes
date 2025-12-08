package com.hermes.translator.utils

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log

object PSRemotePlayDetector {

    private const val TAG = "PSRemotePlayDetector"

    private val PS_REMOTE_PLAY_PACKAGES = listOf(
        "com.playstation.remoteplay",
        "com.scee.psxandroid",
        "com.playstation.psplay"
    )

    private val GAME_STREAMING_PACKAGES = listOf(
        "com.nvidia.geforcenow",
        "com.google.stadia.android",
        "com.microsoft.xcloud",
        "com.valvesoftware.steamlink",
        "tv.parsec.client",
        "com.rainway",
        "com.moonlight_stream.android"
    )

    private val VIDEO_STREAMING_PACKAGES = listOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.hbo.hbonow",
        "com.apple.atve.androidtv.appletv",
        "com.crunchyroll.crunchyroid"
    )

    enum class AppType {
        PS_REMOTE_PLAY,
        GAME_STREAMING,
        VIDEO_STREAMING,
        GAME,
        OTHER
    }

    fun isPSRemotePlayRunning(context: Context): Boolean {
        return getRunningApps(context).any { it in PS_REMOTE_PLAY_PACKAGES }
    }

    fun isGameStreamingRunning(context: Context): Boolean {
        val runningApps = getRunningApps(context)
        return runningApps.any { it in GAME_STREAMING_PACKAGES }
    }

    fun isVideoStreamingRunning(context: Context): Boolean {
        val runningApps = getRunningApps(context)
        return runningApps.any { it in VIDEO_STREAMING_PACKAGES }
    }

    fun detectForegroundAppType(context: Context): AppType {
        val foregroundApp = getForegroundApp(context) ?: return AppType.OTHER

        return when {
            foregroundApp in PS_REMOTE_PLAY_PACKAGES -> AppType.PS_REMOTE_PLAY
            foregroundApp in GAME_STREAMING_PACKAGES -> AppType.GAME_STREAMING
            foregroundApp in VIDEO_STREAMING_PACKAGES -> AppType.VIDEO_STREAMING
            isGamePackage(foregroundApp) -> AppType.GAME
            else -> AppType.OTHER
        }
    }

    fun getForegroundApp(context: Context): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            getForegroundAppFromUsageStats(context)
        } else {
            getForegroundAppLegacy(context)
        }
    }

    private fun getForegroundAppFromUsageStats(context: Context): String? {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null

            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 10000

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            if (usageStats.isNullOrEmpty()) {
                return null
            }

            return usageStats.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            return null
        }
    }

    @Suppress("DEPRECATION")
    private fun getForegroundAppLegacy(context: Context): String? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return null

        return activityManager.runningAppProcesses
            ?.firstOrNull { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
            ?.processName
    }

    private fun getRunningApps(context: Context): List<String> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return emptyList()

        @Suppress("DEPRECATION")
        return activityManager.runningAppProcesses?.map { it.processName } ?: emptyList()
    }

    private fun isGamePackage(packageName: String): Boolean {
        val gameKeywords = listOf(
            "game", "play", "mobile", "quest", "saga",
            "craft", "strike", "battle", "war", "hero",
            "legend", "clash", "arena", "royale", "shooter"
        )

        return gameKeywords.any { packageName.contains(it, ignoreCase = true) }
    }

    fun getSuggestedMode(context: Context): String {
        return when (detectForegroundAppType(context)) {
            AppType.PS_REMOTE_PLAY -> "GAME"
            AppType.GAME_STREAMING -> "GAME"
            AppType.VIDEO_STREAMING -> "MOVIE"
            AppType.GAME -> "GAME"
            AppType.OTHER -> "FAST"
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return false

            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            stats?.isNotEmpty() == true
        } else {
            true
        }
    }
}
