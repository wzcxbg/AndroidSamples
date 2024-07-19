package com.sliver.samples.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class CustomShadowLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    private val shadowColor = 0xFFDDDDDD.toInt()
    private val shadowRadius = 16f
    private val shadowOffsetX = 0f
    private val shadowOffsetY = 0f
    private val paint = Paint()

    private var bitmapCache: Bitmap? = null
    private var canvasCache: Canvas? = null

    init {
        paint.isAntiAlias = true
        paint.color = shadowColor
        paint.maskFilter = BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.NORMAL)
        setLayerType(LAYER_TYPE_SOFTWARE, paint)
        //阴影实现：setShadowLayer、BlurMaskFilter
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val (originWidth, originHeight) = measuredWidth to measuredHeight
        val adjustWidth = max(0f, abs(shadowOffsetX) - shadowRadius) +
                (shadowRadius + originWidth + shadowRadius)
        val adjustHeight = max(0f, abs(shadowOffsetY) - shadowRadius) +
                (shadowRadius + originHeight + shadowRadius)
        setMeasuredDimension(adjustWidth.roundToInt(), adjustHeight.roundToInt())
        createCanvas(adjustWidth.roundToInt(), adjustHeight.roundToInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val l = max(0f, shadowRadius - shadowOffsetX).roundToInt()
        val t = max(0f, shadowRadius - shadowOffsetY).roundToInt()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(
                l, t,
                child.measuredWidth + l,
                child.measuredHeight + t
            )
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val bitmapCache = bitmapCache
        val canvasCache = canvasCache
        if (canvasCache == null || bitmapCache == null) {
            super.dispatchDraw(canvas)
            return
        }
        canvasCache.drawColor(Color.TRANSPARENT)
        super.dispatchDraw(canvasCache)
        val bitmapAlphaChannel = bitmapCache.extractAlpha()
        canvas.drawBitmap(bitmapAlphaChannel, shadowOffsetX, shadowOffsetY, paint)
        bitmapAlphaChannel.recycle()
        canvas.drawBitmap(bitmapCache, 0f, 0f, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseCanvas()
    }

    private fun createCanvas(width: Int, height: Int) {
        if (bitmapCache?.width != width || bitmapCache?.height != height) {
            bitmapCache?.recycle()
            bitmapCache = Bitmap.createBitmap(
                measuredWidth,
                measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            canvasCache = Canvas(bitmapCache!!)
        }
    }

    private fun releaseCanvas() {
        bitmapCache?.recycle()
        bitmapCache = null
        canvasCache = null
    }
}