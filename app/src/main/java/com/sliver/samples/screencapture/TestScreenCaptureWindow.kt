package com.sliver.samples.screencapture

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.WindowManager
import com.sliver.samples.floatingwindow.core.CustomFloatingWindow
import kotlin.math.abs

class TestScreenCaptureWindow(
    context: Context,
    captureIntent: Intent,
) : CustomFloatingWindow<TestScreenCaptureWindowBinding>(context) {
    private val screenCapturer = ScreenCapturer(context, captureIntent)
    private val handler = Handler(Looper.getMainLooper())

    override fun initParams() {
        windowParams.width = 360
        windowParams.height = 480
        windowParams.gravity = Gravity.CENTER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            windowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        binding.imageView.setBackgroundColor(Color.CYAN)
        binding.imageView.setOnClickListener {
            if (screenCapturer.isCapturing()) {
                screenCapturer.stop()
            } else {
                screenCapturer.start()
            }
        }
        var lastTime = 0L
        screenCapturer.setOnBitmapAvailableListener { bitmap ->
            val curTime = System.currentTimeMillis()
            val timeInterval = curTime - lastTime
            lastTime = curTime
            Log.e("TAG", "onBitmapAvailable: time: $timeInterval")
            handler.post {
                binding.imageView.setImageBitmap(bitmap)
            }
        }
        screenCapturer.start()
        binding.imageView.setOnTouchListener(object : OnTouchListener {
            private var lastPoint = PointF()
            private var isClick = false
            private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
            private val tapTimeout = ViewConfiguration.getTapTimeout()
            private val direction = getGravityDirection(windowParams.gravity)
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
                        windowParams.x -= dx.toInt() * direction.x
                        windowParams.y -= dy.toInt() * direction.y
                        updateParams()
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