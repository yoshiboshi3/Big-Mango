package com.xrealcanvas.app

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.roundToInt

class CanvasPresentation(context: Context, display: Display) : Presentation(context, display) {
    private lateinit var root: FrameLayout
    private lateinit var window: TextView
    private var scale = 0.70f
    private var xNorm = 0f
    private var yNorm = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        window = TextView(context).apply {
            text = "XREAL CANVAS\nTEST WINDOW"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 22f
            setBackgroundResource(com.xrealcanvas.app.R.drawable.test_window)
        }
        root.addView(window)
        setContentView(root)
        root.post { applyTransform() }
    }

    fun setTransform(newScale: Float, newXNorm: Float, newYNorm: Float) {
        scale = newScale.coerceIn(0.20f, 1.00f)
        xNorm = newXNorm.coerceIn(-1f, 1f)
        yNorm = newYNorm.coerceIn(-1f, 1f)
        if (::root.isInitialized) applyTransform()
    }

    private fun applyTransform() {
        val rw = root.width
        val rh = root.height
        if (rw <= 0 || rh <= 0) return
        val aspect = 16f / 9f
        var w = (rw * scale).roundToInt()
        var h = (w / aspect).roundToInt()
        if (h > rh * scale) {
            h = (rh * scale).roundToInt()
            w = (h * aspect).roundToInt()
        }
        window.layoutParams = FrameLayout.LayoutParams(w, h, Gravity.CENTER)
        val maxX = (rw - w) / 2f
        val maxY = (rh - h) / 2f
        window.translationX = xNorm * maxX
        window.translationY = yNorm * maxY
    }
}
