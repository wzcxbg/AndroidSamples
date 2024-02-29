package com.sliver.samples.floatingwindow.core

import android.graphics.PointF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class FloatingWindowParams {
    private var width: Int? = null
    private var height: Int? = null
    private var gravity: Int? = null
    private var type: Int? = null
    private var flags: Int? = null
    private var movable: Boolean? = null
    private var floatingType: FloatingType? = null

    fun width(width: Int) = apply { this.width = width }
    fun height(height: Int) = apply { this.height = height }
    fun gravity(gravity: Int) = apply { this.gravity = gravity }
    fun type(type: Int) = apply { this.type = type }
    fun flags(flags: Int) = apply { this.flags = flags }
    fun addFlag(flag: Int) = apply { this.flags = (this.flags ?: 0).or(flag) }
    fun removeFlag(flag: Int) = apply { this.flags = (this.flags ?: 0).and(flag.inv()) }
    fun movable(movable: Boolean) = apply { this.movable = movable }
    fun floatingType(floatingType: FloatingType) = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addFlag(
                when (floatingType) {
                    FloatingType.AppSelf -> WindowManager.LayoutParams.TYPE_APPLICATION
                    FloatingType.System -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }
            )
        } else {
            //TODO 设置正确的Type
            addFlag(
                when (floatingType) {
                    FloatingType.AppSelf -> WindowManager.LayoutParams.TYPE_PHONE
                    FloatingType.System -> WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
            )
            //WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            //WindowManager.LayoutParams.TYPE_PHONE
            //WindowManager.LayoutParams.TYPE_TOAST
        }
    }

    fun apply(floatingWindow: CustomFloatingWindow<*>) {
        val (immWidth, immHeight) = width to height
        val (immGravity, immType) = gravity to type
        val (immFlags, immMovable) = flags to movable

        val windowParams = floatingWindow.windowParams
        if (immWidth != null) windowParams.width = immWidth
        if (immHeight != null) windowParams.height = immHeight
        if (immGravity != null) windowParams.gravity = immGravity
        if (immType != null) windowParams.type = immType
        if (immFlags != null) windowParams.flags = immFlags

        val binding = floatingWindow.binding
        if (immMovable != null) {
            binding.root.setOnTouchListener(object : View.OnTouchListener {
                private val lastPoint = PointF()
                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastPoint.set(event.rawX, event.rawY)
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - lastPoint.x
                            val dy = event.rawY - lastPoint.y
                            lastPoint.set(event.rawX, event.rawY)

                            floatingWindow.update(
                                (windowParams.x + dx).toInt(),
                                (windowParams.y + dy).toInt(),
                                windowParams.width,
                                windowParams.height,
                                windowParams.gravity,
                            )
                        }
                    }
                    return true
                }
            })
        }
    }

    enum class FloatingType { AppSelf, System }
}