package com.sliver.samples.floatingwindow.core

import android.graphics.Point
import android.graphics.PointF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View

class FloatingWindowMovableTouchHandler(
    private val floatingWindow: CustomFloatingWindow<*>
) : View.OnTouchListener {
    private val windowParams = floatingWindow.windowParams
    private val direction = getGravityDirection(windowParams.gravity)
    private val lastPoint = PointF()

    fun apply() {
        val binding = floatingWindow.binding
        binding.root.setOnTouchListener(this)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint.set(event.rawX, event.rawY)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastPoint.x
                val dy = event.rawY - lastPoint.y
                lastPoint.set(event.rawX, event.rawY)

                floatingWindow.update(
                    (windowParams.x + dx * direction.x).toInt(),
                    (windowParams.y + dy * direction.y).toInt(),
                    windowParams.width,
                    windowParams.height,
                    windowParams.gravity,
                )
            }
        }
        return true
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