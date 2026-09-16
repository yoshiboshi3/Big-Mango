package com.xrealcanvas.app

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity(), DisplayManager.DisplayListener {
    private lateinit var dm: DisplayManager
    private var presentation: CanvasPresentation? = null
    private lateinit var status: TextView
    private lateinit var scale: SeekBar
    private lateinit var x: SeekBar
    private lateinit var y: SeekBar
    private lateinit var scaleLabel: TextView
    private lateinit var xLabel: TextView
    private lateinit var yLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dm = getSystemService(DisplayManager::class.java)
        status = findViewById(R.id.status)
        scale = findViewById(R.id.scale)
        x = findViewById(R.id.x)
        y = findViewById(R.id.y)
        scaleLabel = findViewById(R.id.scaleLabel)
        xLabel = findViewById(R.id.xLabel)
        yLabel = findViewById(R.id.yLabel)

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = updateTransform()
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        scale.setOnSeekBarChangeListener(listener)
        x.setOnSeekBarChangeListener(listener)
        y.setOnSeekBarChangeListener(listener)
        findViewById<Button>(R.id.center).setOnClickListener { x.progress = 100; y.progress = 100; updateTransform() }
        findViewById<Button>(R.id.reset).setOnClickListener { scale.progress = 50; x.progress = 100; y.progress = 100; updateTransform() }
    }

    override fun onResume() {
        super.onResume()
        dm.registerDisplayListener(this, null)
        connectExternalDisplay()
    }

    override fun onPause() {
        dm.unregisterDisplayListener(this)
        presentation?.dismiss()
        presentation = null
        super.onPause()
    }

    private fun connectExternalDisplay() {
        val candidates = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        val external = candidates.firstOrNull { it.displayId != display?.displayId }
        if (external == null) {
            status.text = "No presentation display detected. Plug in the XREAL glasses and use Extended mode if Samsung offers it."
            presentation?.dismiss(); presentation = null
            return
        }
        if (presentation?.display?.displayId != external.displayId) {
            presentation?.dismiss()
            presentation = CanvasPresentation(this, external).also { it.show() }
        }
        val mode = external.mode
        status.text = "External display: ${external.name} • ${mode.physicalWidth}×${mode.physicalHeight} • ${"%.1f".format(mode.refreshRate)} Hz"
        updateTransform()
    }

    private fun updateTransform() {
        val s = 0.20f + (scale.progress / 80f) * 0.80f
        val xn = (x.progress - 100) / 100f
        val yn = (y.progress - 100) / 100f
        scaleLabel.text = "Scale ${(s * 100).toInt()}%"
        xLabel.text = "X ${x.progress - 100}%"
        yLabel.text = "Y ${y.progress - 100}%"
        presentation?.setTransform(s, xn, yn)
    }

    override fun onDisplayAdded(displayId: Int) = connectExternalDisplay()
    override fun onDisplayRemoved(displayId: Int) = connectExternalDisplay()
    override fun onDisplayChanged(displayId: Int) = connectExternalDisplay()
}
