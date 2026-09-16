package com.xrealcanvas.proof

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.xrealcanvas.bridge.XrealCanvasBridge
import java.nio.ByteBuffer

class DiagnosticActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var image: ImageView
    private var displayId = -1
    private var selected: String? = null
    private var lastFrameCount = -1L
    private val bitmap = Bitmap.createBitmap(960, 540, Bitmap.Config.ARGB_8888)

    private val ticker = object : Runnable {
        override fun run() {
            try {
                consumeSelection()
                updateFrame()
                renderStatus()
            } catch (t: Throwable) {
                status.text = "ERROR: ${t.javaClass.simpleName}: ${t.message}"
            }
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Scottish Targe Proof"
        setContentView(buildUi())
        XrealCanvasBridge.initializeShizuku(this)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun buildUi(): ScrollView {
        val density = resources.displayMetrics.density
        val pad = (14 * density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "SCOTTISH TARGE\nArbitrary-app feasibility gate"
            textSize = 24f
            setTextColor(Color.BLACK)
        })
        root.addView(Button(this).apply {
            text = "ADD WINDOW"
            textSize = 20f
            setOnClickListener { XrealCanvasBridge.openPicker(this@DiagnosticActivity) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (64 * density).toInt()))

        image = ImageView(this).apply {
            setBackgroundColor(Color.DKGRAY)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Hosted Android app display"
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP && displayId >= 0) {
                    val x = (event.x / view.width * 959f).toInt().coerceIn(0, 959)
                    val y = (event.y / view.height * 539f).toInt().coerceIn(0, 539)
                    XrealCanvasBridge.tap(displayId, x, y)
                    view.performClick()
                }
                true
            }
        }
        root.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (300 * density).toInt()).apply {
            topMargin = pad
        })

        status = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.BLACK)
            gravity = Gravity.START
            setPadding(0, pad, 0, pad)
        }
        root.addView(status)
        root.addView(TextView(this).apply {
            text = "PASS condition: an ordinary app appears live in the gray panel and reacts when you tap the panel. This does not test XREAL spatial placement yet."
            textSize = 16f
            setTextColor(Color.DKGRAY)
        })
        return ScrollView(this).apply { addView(root) }
    }

    private fun consumeSelection() {
        if (selected != null) return
        val component = XrealCanvasBridge.consumeSelectedComponent() ?: return
        selected = component
        if (displayId < 0) displayId = XrealCanvasBridge.startVirtualDisplay(this)
        XrealCanvasBridge.launchOnDisplay(displayId, component)
    }

    private fun updateFrame() {
        val count = XrealCanvasBridge.frameCount()
        if (count <= 0 || count == lastFrameCount) return
        val bytes = XrealCanvasBridge.latestFrame() ?: return
        if (bytes.size != 960 * 540 * 4) return
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
        image.setImageBitmap(bitmap)
        lastFrameCount = count
    }

    private fun renderStatus() {
        status.text = buildString {
            appendLine("Shizuku: ${XrealCanvasBridge.shizukuStatus()}")
            appendLine("Server UID: ${XrealCanvasBridge.shizukuServerUid()} (must be 2000)")
            appendLine("Service UID: ${XrealCanvasBridge.shizukuServiceUid()} (must be 2000)")
            appendLine("Virtual display: $displayId")
            appendLine("Selected app: ${selected ?: "none"}")
            appendLine("Frames: ${XrealCanvasBridge.frameCount()}")
            appendLine("Launch exit: ${XrealCanvasBridge.lastLaunchExitCode()}")
            appendLine("Launch output: ${XrealCanvasBridge.lastLaunchOutput()}")
            appendLine("Tap exit: ${XrealCanvasBridge.lastTapExitCode()}")
            append("Tap output: ${XrealCanvasBridge.lastTapOutput()}")
        }
    }
}
