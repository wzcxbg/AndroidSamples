package com.sliver.samples.custom

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnDrawListener
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.delay

class CustomBlurView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    private var drawEnable = true
    private var decorView: View? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        decorView = getActivityDecorView()
    }

    override fun onDraw(canvas: Canvas) {
        if (drawEnable) {
            super.onDraw(canvas)
            drawEnable = false
            val parentLayout = parent as ViewGroup
            val bitmap = parentLayout.drawToBitmap()
            Log.e("TAG", "onDraw: ${bitmap.width} ${bitmap.height}")

            val parentLocation = IntArray(2)
            parentLayout.getLocationOnScreen(parentLocation)

            val viewLocation = IntArray(2)
            this.getLocationOnScreen(viewLocation)

            val offsetX = parentLocation[0] - viewLocation[0]
            val offsetY = parentLocation[1] - viewLocation[1]

            canvas.drawBitmap(bitmap, offsetX.toFloat(), offsetY.toFloat(), null)

            bitmap.recycle()
            drawEnable = true


            decorView?.viewTreeObserver?.addOnPreDrawListener(object : OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    decorView?.viewTreeObserver?.removeOnPreDrawListener(this)
                    invalidate()
                    return false
                }
            })
        }
    }

    private fun getActivityDecorView(): View? {
        var ctx: Context? = context
        for (i in 0 until 4) {
            if (ctx is Activity) break
            if (ctx !is ContextWrapper) break
            ctx = ctx.baseContext
        }
        if (ctx is Activity) {
            return ctx.window.decorView
        }
        return null
    }
}