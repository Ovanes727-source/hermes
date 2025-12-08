package com.hermes.translator.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.hermes.translator.R

class GameOverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var tvOriginal: TextView? = null
    private var tvTranslated: TextView? = null
    private var containerView: View? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    private val prefs by lazy { context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE) }

    private var isOverlayVisible = false

    fun showTranslation(originalText: String, translatedText: String) {
        mainHandler.post {
            if (overlayView == null) {
                createOverlay()
            }

            hideRunnable?.let { mainHandler.removeCallbacks(it) }

            tvOriginal?.text = originalText
            tvOriginal?.visibility = if (originalText.isNotBlank()) View.VISIBLE else View.GONE

            tvTranslated?.text = translatedText

            if (!isOverlayVisible) {
                overlayView?.visibility = View.VISIBLE
                val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                containerView?.startAnimation(fadeIn)
                isOverlayVisible = true
            }

            hideRunnable = Runnable {
                hideOverlayWithAnimation()
            }
            mainHandler.postDelayed(hideRunnable!!, 5000)
        }
    }

    private fun createOverlay() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val opacity = prefs.getInt("overlay_opacity", 80) / 100f

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams.x = 0
        layoutParams.y = 100

        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.layout_translation_overlay, null)

        containerView = overlayView?.findViewById(R.id.overlayContainer)
        tvOriginal = overlayView?.findViewById(R.id.tvOriginal)
        tvTranslated = overlayView?.findViewById(R.id.tvTranslated)

        containerView?.alpha = opacity

        overlayView?.visibility = View.GONE

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlayWithAnimation() {
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                overlayView?.visibility = View.GONE
                isOverlayVisible = false
            }
        })
        containerView?.startAnimation(fadeOut)
    }

    fun hideOverlay() {
        mainHandler.post {
            hideRunnable?.let { mainHandler.removeCallbacks(it) }

            try {
                overlayView?.let { view ->
                    windowManager?.removeView(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            overlayView = null
            tvOriginal = null
            tvTranslated = null
            containerView = null
            isOverlayVisible = false
        }
    }

    fun updateOpacity(opacity: Float) {
        mainHandler.post {
            containerView?.alpha = opacity
        }
    }

    fun isVisible(): Boolean = isOverlayVisible
}
