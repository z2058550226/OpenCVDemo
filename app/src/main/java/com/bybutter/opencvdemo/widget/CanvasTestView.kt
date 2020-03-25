package com.bybutter.opencvdemo.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.dip

class CanvasTestView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = dip(3).toFloat()
        color = 0xFF66CCFF.toInt()
    }
    private val rect = Rect(100, 100, 300, 200)
    private val path = Path().apply {
        moveTo(200f, 150f)
        rLineTo(30f, 30f)
        rLineTo(30f, -30f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(rect, paint)
        canvas.save()
        canvas.translate(100f, 0f)
        canvas.scale(2f, 2f)
        canvas.drawPath(path, paint)
        canvas.restore()
        canvas.drawCircle(200f, 150f, 20f, paint)
    }

}