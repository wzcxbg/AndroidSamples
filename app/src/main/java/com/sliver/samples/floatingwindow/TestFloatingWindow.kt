package com.sliver.samples.floatingwindow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import com.sliver.samples.floatingwindow.core.CustomFloatingWindow
import com.sliver.samples.floatingwindow.core.FloatingWindowParams

class TestFloatingWindow(context: Context) : CustomFloatingWindow(context) {

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(): View {
        val textView = TextView(context)
        textView.text = "Floating Window"
        val direction = getGravityDirection(Gravity.CENTER)
        textView.setOnTouchListener(object : OnTouchListener {
            private val lastPoint = PointF()

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastPoint.set(event.rawX, event.rawY)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - lastPoint.x
                        val dy = event.rawY - lastPoint.y
                        lastPoint.set(event.rawX, event.rawY)
                        move(
                            (dx * direction.x).toInt(),
                            (dy * direction.y).toInt(),
                        )
                    }
                }
                return true
            }
        })
        return textView
    }

    override fun initParams(): FloatingWindowParams {
        val windowParams = FloatingWindowParams()
        windowParams.gravity(Gravity.CENTER)
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