package com.sliver.samples.floatingwindow.core

import android.Manifest
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class FloatingWindowParams {
    private var x: Int? = null
    private var y: Int? = null
    private var width: Int? = null
    private var height: Int? = null
    private var gravity: Int? = null
    private var format: Int? = null
    private var type: WindowType? = null
    private var flags: Int? = null
    private var movable: Boolean? = null

    init {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        type = WindowType.TYPE_APPLICATION
        format = PixelFormat.TRANSPARENT
        flags = 0 or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
    }

    fun setX(x: Int) = apply { this.x = x }
    fun setY(y: Int) = apply { this.y = y }
    fun moveX(x: Int) = apply { this.x = (this.x ?: 0) + x }
    fun moveY(y: Int) = apply { this.y = (this.y ?: 0) + y }
    fun width(width: Int) = apply { this.width = width }
    fun height(height: Int) = apply { this.height = height }
    fun gravity(gravity: Int) = apply { this.gravity = gravity }
    fun format(format: Int) = apply { this.format = format }
    fun type(type: WindowType) = apply { this.type = type }
    fun setFlags(flags: Int) = apply { this.flags = flags }
    fun addFlag(flag: Int) = apply { this.flags = (this.flags ?: 0).or(flag) }
    fun removeFlag(flag: Int) = apply { this.flags = (this.flags ?: 0).and(flag.inv()) }
    fun movable(movable: Boolean) = apply { this.movable = movable }

    fun toLayoutParams(): WindowManager.LayoutParams {
        val (immX, immY) = x to y
        val (immWidth, immHeight) = width to height
        val (immGravity, immFormat) = gravity to format
        val (immType, immFlags) = type to flags

        val windowParams = WindowManager.LayoutParams()
        if (immX != null) windowParams.x = immX
        if (immY != null) windowParams.y = immY
        if (immWidth != null) windowParams.width = immWidth
        if (immHeight != null) windowParams.height = immHeight
        if (immGravity != null) windowParams.gravity = immGravity
        if (immFormat != null) windowParams.format = immFormat
        if (immType != null) windowParams.type = immType.type
        if (immFlags != null) windowParams.flags = immFlags
        return windowParams
    }

    enum class WindowType(val type: Int) {
        TYPE_APPLICATION(WindowManager.LayoutParams.TYPE_APPLICATION),
        @RequiresApi(Build.VERSION_CODES.O)
        @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
        TYPE_APPLICATION_OVERLAY(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY),
        TYPE_SYSTEM_ALERT(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT),
        TYPE_PHONE(WindowManager.LayoutParams.TYPE_PHONE),
        TYPE_TOAST(WindowManager.LayoutParams.TYPE_TOAST),
    }
}