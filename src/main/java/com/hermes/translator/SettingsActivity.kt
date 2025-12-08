package com.hermes.translator

import android.os.Bundle
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hermes.translator.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { getSharedPreferences("hermes_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)
    }

    private fun loadSettings() {
        binding.switchAutoStart.isChecked = prefs.getBoolean("auto_start", false)
        binding.switchShowOriginal.isChecked = prefs.getBoolean("show_original", true)
        binding.switchTtsEnabled.isChecked = prefs.getBoolean("tts_enabled", true)
        binding.switchOfflineMode.isChecked = prefs.getBoolean("offline_mode", false)

        binding.seekBarOverlayOpacity.progress = prefs.getInt("overlay_opacity", 80)
        binding.seekBarTtsSpeed.progress = prefs.getInt("tts_speed", 50)
        binding.seekBarSensitivity.progress = prefs.getInt("audio_sensitivity", 50)

        updateOpacityLabel(binding.seekBarOverlayOpacity.progress)
        updateSpeedLabel(binding.seekBarTtsSpeed.progress)
        updateSensitivityLabel(binding.seekBarSensitivity.progress)

        val sourceLanguage = prefs.getString("source_language", "en")
        val targetLanguage = prefs.getString("target_language", "ru")
        val voiceType = prefs.getString("voice_type", "hermes_male")

        when (sourceLanguage) {
            "en" -> binding.radioSourceEnglish.isChecked = true
            "ja" -> binding.radioSourceJapanese.isChecked = true
            "ko" -> binding.radioSourceKorean.isChecked = true
            "auto" -> binding.radioSourceAuto.isChecked = true
        }

        when (voiceType) {
            "hermes_male" -> binding.radioVoiceHermes.isChecked = true
            "athena_female" -> binding.radioVoiceAthena.isChecked = true
            "cyber_neutral" -> binding.radioVoiceCyber.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_start", isChecked).apply()
        }

        binding.switchShowOriginal.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_original", isChecked).apply()
        }

        binding.switchTtsEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("tts_enabled", isChecked).apply()
        }

        binding.switchOfflineMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("offline_mode", isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, R.string.offline_mode_hint, Toast.LENGTH_LONG).show()
            }
        }

        binding.seekBarOverlayOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateOpacityLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                prefs.edit().putInt("overlay_opacity", seekBar?.progress ?: 80).apply()
            }
        })

        binding.seekBarTtsSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSpeedLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                prefs.edit().putInt("tts_speed", seekBar?.progress ?: 50).apply()
            }
        })

        binding.seekBarSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSensitivityLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                prefs.edit().putInt("audio_sensitivity", seekBar?.progress ?: 50).apply()
            }
        })

        binding.radioGroupSourceLang.setOnCheckedChangeListener { _, checkedId ->
            val lang = when (checkedId) {
                R.id.radioSourceEnglish -> "en"
                R.id.radioSourceJapanese -> "ja"
                R.id.radioSourceKorean -> "ko"
                R.id.radioSourceAuto -> "auto"
                else -> "en"
            }
            prefs.edit().putString("source_language", lang).apply()
        }

        binding.radioGroupVoice.setOnCheckedChangeListener { _, checkedId ->
            val voice = when (checkedId) {
                R.id.radioVoiceHermes -> "hermes_male"
                R.id.radioVoiceAthena -> "athena_female"
                R.id.radioVoiceCyber -> "cyber_neutral"
                else -> "hermes_male"
            }
            prefs.edit().putString("voice_type", voice).apply()
        }

        binding.btnDownloadModels.setOnClickListener {
            Toast.makeText(this, R.string.downloading_models, Toast.LENGTH_LONG).show()
        }

        binding.btnClearCache.setOnClickListener {
            prefs.edit().clear().apply()
            loadSettings()
            Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOpacityLabel(progress: Int) {
        binding.tvOpacityValue.text = "$progress%"
    }

    private fun updateSpeedLabel(progress: Int) {
        val speed = 0.5f + (progress / 100f) * 1.5f
        binding.tvSpeedValue.text = String.format("%.1fx", speed)
    }

    private fun updateSensitivityLabel(progress: Int) {
        val label = when {
            progress < 33 -> getString(R.string.sensitivity_low)
            progress < 66 -> getString(R.string.sensitivity_medium)
            else -> getString(R.string.sensitivity_high)
        }
        binding.tvSensitivityValue.text = label
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
