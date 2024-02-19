package com.sliver.samples.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.roundToInt

/**
 * 自定义带阴影的TextView
 * 缺陷：Xml中设置的background不再生效
 */
class ShadowTextView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    private val backgroundColor = 0xFFFFFFFF.toInt()
    private val shadowColor = 0xFEDDDDDD.toInt()
    private val shadowRadius = 16f
    private val paint = Paint()

    init {
        paint.isAntiAlias = true
        paint.color = backgroundColor
        paint.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
        //阴影实现：setShadowLayer、BlurMaskFilter
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            (measuredWidth + shadowRadius + shadowRadius).roundToInt(),
            (measuredHeight + shadowRadius + shadowRadius).roundToInt(),
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(
            shadowRadius,
            shadowRadius,
            measuredWidth - shadowRadius,
            measuredHeight - shadowRadius,
            paint
        )
        canvas.save()
        canvas.translate(shadowRadius, shadowRadius)
        super.onDraw(canvas)
        canvas.restore()
    }
}