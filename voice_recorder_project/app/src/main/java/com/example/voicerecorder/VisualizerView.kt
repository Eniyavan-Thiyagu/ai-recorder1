package com.example.voicerecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class VisualizerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply { strokeWidth = 2f }
    private var waveform: ShortArray = ShortArray(0)

    fun updateWaveform(data: ShortArray) {
        waveform = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (waveform.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2
        val step = waveform.size / w.coerceAtLeast(1f)
        var x = 0f
        var i = 0
        while (i < waveform.size) {
            val v = waveform[i].toFloat() / Short.MAX_VALUE
            val y = centerY + v * centerY
            canvas.drawLine(x, centerY, x, y, paint)
            x += 1f
            i += step.toInt().coerceAtLeast(1)
        }
    }
}
