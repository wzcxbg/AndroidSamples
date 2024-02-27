package com.sliver.samples.floatingwindow.core

import android.view.Gravity
import android.view.WindowManager

class FloatingWindowParamsBuilder {
    private val floatingType = FloatingType.AppSelf
    private val gravity = Gravity.CENTER
    private val width = WindowManager.LayoutParams.WRAP_CONTENT
    private val height = WindowManager.LayoutParams.WRAP_CONTENT

    /**
     * TODO 应用参数
     */
    fun apply() {

    }

    enum class FloatingType {
        AppSelf, System
    }
}