package com.hermes.translator.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    const val REQUEST_CODE_AUDIO = 1001
    const val REQUEST_CODE_OVERLAY = 1002

    private val audioPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    fun hasAudioPermission(context: Context): Boolean {
        return audioPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun hasAllPermissions(context: Context): Boolean {
        return hasAudioPermission(context) && hasOverlayPermission(context)
    }

    fun requestAudioPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            audioPermissions,
            REQUEST_CODE_AUDIO
        )
    }

    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        }
    }

    fun shouldShowAudioPermissionRationale(activity: Activity): Boolean {
        return audioPermissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        onAudioGranted: () -> Unit,
        onAudioDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_CODE_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onAudioGranted()
                } else {
                    onAudioDenied()
                }
            }
        }
    }

    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!hasAudioPermission(context)) {
            missing.add("Запись аудио")
        }

        if (!hasOverlayPermission(context)) {
            missing.add("Отображение поверх других приложений")
        }

        return missing
    }
}
