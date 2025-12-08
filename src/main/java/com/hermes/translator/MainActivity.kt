package com.hermes.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hermes.translator.databinding.ActivityMainBinding
import com.hermes.translator.model.TranslationMode
import com.hermes.translator.service.TranslationService
import com.hermes.translator.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isTranslationRunning = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 100
        private const val REQUEST_CODE_OVERLAY = 101
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupAnimations()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun setupUI() {
        binding.btnStartTranslation.setOnClickListener {
            if (isTranslationRunning) {
                stopTranslationService()
            } else {
                if (checkAllPermissions()) {
                    startTranslationService()
                }
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnModeGame.setOnClickListener {
            selectMode(TranslationMode.GAME)
        }

        binding.btnModeMovie.setOnClickListener {
            selectMode(TranslationMode.MOVIE)
        }

        binding.btnModeStream.setOnClickListener {
            selectMode(TranslationMode.STREAM)
        }

        binding.btnModeFast.setOnClickListener {
            selectMode(TranslationMode.FAST)
        }
    }

    private fun setupAnimations() {
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        binding.ivHermesWings.startAnimation(pulseAnimation)
    }

    private fun selectMode(mode: TranslationMode) {
        binding.btnModeGame.isSelected = mode == TranslationMode.GAME
        binding.btnModeMovie.isSelected = mode == TranslationMode.MOVIE
        binding.btnModeStream.isSelected = mode == TranslationMode.STREAM
        binding.btnModeFast.isSelected = mode == TranslationMode.FAST

        val prefs = getSharedPreferences("hermes_prefs", MODE_PRIVATE)
        prefs.edit().putString("translation_mode", mode.name).apply()

        Toast.makeText(this, getString(R.string.mode_selected, mode.displayName), Toast.LENGTH_SHORT).show()
    }

    private fun checkAllPermissions(): Boolean {
        if (!hasAudioPermission()) {
            requestAudioPermission()
            return false
        }

        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return false
        }

        return true
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.RECORD_AUDIO
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_audio_title)
                .setMessage(R.string.permission_audio_rationale)
                .setPositiveButton(R.string.grant) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_overlay_title)
                .setMessage(R.string.permission_overlay_rationale)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun checkPermissions() {
        if (!hasAudioPermission()) {
            binding.tvStatus.text = getString(R.string.status_need_permissions)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.circuit_orange))
        }
    }

    private fun startTranslationService() {
        val serviceIntent = Intent(this, TranslationService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        isTranslationRunning = true
        updateUI()

        Toast.makeText(this, R.string.translation_started, Toast.LENGTH_LONG).show()
    }

    private fun stopTranslationService() {
        val serviceIntent = Intent(this, TranslationService::class.java)
        stopService(serviceIntent)

        isTranslationRunning = false
        updateUI()

        Toast.makeText(this, R.string.translation_stopped, Toast.LENGTH_SHORT).show()
    }

    private fun updateServiceStatus() {
        isTranslationRunning = TranslationService.isRunning
        updateUI()
    }

    private fun updateUI() {
        if (isTranslationRunning) {
            binding.btnStartTranslation.text = getString(R.string.stop_translation)
            binding.btnStartTranslation.setBackgroundResource(R.drawable.button_cyber_stop)
            binding.tvStatus.text = getString(R.string.status_translating)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_blue))
            binding.animationIndicator.visibility = View.VISIBLE
        } else {
            binding.btnStartTranslation.text = getString(R.string.start_translation)
            binding.btnStartTranslation.setBackgroundResource(R.drawable.button_cyber_antique)
            binding.tvStatus.text = getString(R.string.status_ready)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.cyber_gold))
            binding.animationIndicator.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
                if (checkAllPermissions()) {
                    startTranslationService()
                }
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (hasOverlayPermission()) {
                Toast.makeText(this, R.string.overlay_permission_granted, Toast.LENGTH_SHORT).show()
                if (checkAllPermissions()) {
                    startTranslationService()
                }
            }
        }
    }

}
