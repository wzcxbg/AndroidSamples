package com.sliver.samples.floatingwindow.core

import android.Manifest
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class FloatingWindowParams {
    private var width: Int? = null
    private var height: Int? = null
    private var gravity: Int? = null
    private var type: WindowType? = null
    private var flags: Int? = null
    private var movable: Boolean? = null

    //TODO 自动贴边、前台服务
    fun width(width: Int) = apply { this.width = width }
    fun height(height: Int) = apply { this.height = height }
    fun gravity(gravity: Int) = apply { this.gravity = gravity }
    fun type(type: WindowType) = apply { this.type = type }
    fun setFlags(flags: Int) = apply { this.flags = flags }
    fun addFlag(flag: Int) = apply { this.flags = (this.flags ?: 0).or(flag) }
    fun removeFlag(flag: Int) = apply { this.flags = (this.flags ?: 0).and(flag.inv()) }
    fun movable(movable: Boolean) = apply { this.movable = movable }

    fun apply(floatingWindow: CustomFloatingWindow<*>) {
        val (immWidth, immHeight) = width to height
        val (immGravity, immType) = gravity to type
        val (immFlags, immMovable) = flags to movable

        val windowParams = floatingWindow.windowParams
        if (immWidth != null) windowParams.width = immWidth
        if (immHeight != null) windowParams.height = immHeight
        if (immGravity != null) windowParams.gravity = immGravity
        if (immType != null) windowParams.type = immType.type
        if (immFlags != null) windowParams.flags = immFlags

        if (immMovable == true) {
            FloatingWindowMovableTouchHandler(floatingWindow)
                .apply()
        }
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