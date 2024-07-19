package com.sliver.samples.floatingwindow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.widget.ImageView
import com.sliver.samples.custom.CustomShadowLayout
import com.sliver.samples.floatingwindow.core.CustomFloatingWindow
import com.sliver.samples.floatingwindow.core.FloatingWindowParams
import kotlin.math.abs
import kotlin.system.measureTimeMillis

class TestFloatingWindow(context: Context) : CustomFloatingWindow(context) {
    private val commandProcess = CommandProcess("su")

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(): View {
        val imageView = ImageView(context)
        //imageView.setImageDrawable(ColorDrawable(Color.CYAN))
        imageView.setOnClickListener {
            measureTimeMillis {
                val outputsFuture = commandProcess.submit("screencap -p")
                val commandOutputs = outputsFuture.get()
                val bitmap = BitmapFactory.decodeByteArray(
                    commandOutputs.outputBytes, 0,
                    commandOutputs.outputBytes.size
                )
                imageView.setImageBitmap(bitmap)
            }.also { Log.e("TAG", "initView: $it") }
        }
        imageView.performClick()
        imageView.setOnTouchListener(object : OnTouchListener {
            private var lastPoint = PointF()
            private var lastTime = 0L
            private var isClick = false
            private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
            private val tapTimeout = ViewConfiguration.getTapTimeout()
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastPoint.set(event.rawX, event.rawY)
                        lastTime = System.currentTimeMillis()
                        isClick = true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = lastPoint.x - event.rawX
                        val dy = lastPoint.y - event.rawY
                        if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                            isClick = false
                        }
                        lastPoint.set(event.rawX, event.rawY)
                        move(-dx.toInt(), -dy.toInt())
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isClick && System.currentTimeMillis() - lastTime < tapTimeout) {
                            v.performClick()
                        }
                    }
                }
                return true
            }
        })
        val rootView = CustomShadowLayout(context)
        rootView.addView(imageView)
        return rootView
    }

    override fun initParams(): FloatingWindowParams {
        val windowParams = FloatingWindowParams()
        windowParams.width(400)
        windowParams.height(400)
        windowParams.gravity(Gravity.CENTER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowParams.type(FloatingWindowParams.WindowType.TYPE_APPLICATION_OVERLAY)
        } else {
            windowParams.type(FloatingWindowParams.WindowType.TYPE_APPLICATION)
        }
        return windowParams
    }

    private fun getGravityDirection(gravity: Int): Point {
        val direction = Point(1, 1)
        when (gravity and (Gravity.AXIS_PULL_BEFORE
                or Gravity.AXIS_PULL_AFTER
                shl Gravity.AXIS_X_SHIFT)) {
            0 -> direction.x = 1
            Gravity.AXIS_PULL_BEFORE shl Gravity.AXIS_X_SHIFT -> direction.x = 1
            Gravity.AXIS_PULL_AFTER shl Gravity.AXIS_X_SHIFT -> direction.x = -1
        }
        when (gravity and (Gravity.AXIS_PULL_BEFORE
                or Gravity.AXIS_PULL_AFTER
                shl Gravity.AXIS_Y_SHIFT)) {
            0 -> direction.y = 1
            Gravity.AXIS_PULL_BEFORE shl Gravity.AXIS_Y_SHIFT -> direction.y = 1
            Gravity.AXIS_PULL_AFTER shl Gravity.AXIS_Y_SHIFT -> direction.y = -1
        }
        return direction
    }
}