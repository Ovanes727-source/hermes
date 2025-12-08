package com.hermes.translator.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.hermes.translator.R

class TranslationOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val cornerRadius = 24f

    private var animationOffset = 0f
    private val animationSpeed = 0.02f

    private val neonBlue = ContextCompat.getColor(context, R.color.neon_blue)
    private val neonPurple = ContextCompat.getColor(context, R.color.neon_purple)
    private val cyberGold = ContextCompat.getColor(context, R.color.cyber_gold)
    private val obsidianBlack = ContextCompat.getColor(context, R.color.obsidian_black)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(8f, 8f, w - 8f, h - 8f)
        updateGradients()
    }

    private fun updateGradients() {
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(neonBlue, neonPurple, cyberGold),
            floatArrayOf(animationOffset, 0.5f + animationOffset, 1f),
            Shader.TileMode.MIRROR
        )

        borderPaint.shader = gradient
        glowPaint.shader = gradient

        backgroundPaint.color = obsidianBlack
        backgroundPaint.alpha = 200
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        glowPaint.setShadowLayer(16f, 0f, 0f, neonBlue)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        animationOffset += animationSpeed
        if (animationOffset > 1f) {
            animationOffset = 0f
        }
        updateGradients()
        postInvalidateDelayed(16)
    }

    fun setGlowIntensity(intensity: Float) {
        glowPaint.setShadowLayer(16f * intensity, 0f, 0f, neonBlue)
        invalidate()
    }
}
