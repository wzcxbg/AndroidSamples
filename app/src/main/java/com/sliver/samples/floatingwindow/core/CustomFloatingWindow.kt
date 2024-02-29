package com.sliver.samples.floatingwindow.core

import android.content.Context
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * 目标：
 * 1.能直接使用Builder创建悬浮窗，需要在最后build才正式创建悬浮窗
 * 2.能使用ParamsApplier在CustomFloatingWindow内部设置悬浮窗属性，并且在最后一步apply才生效，为了简便，没有设置的属性不要参与修改
 * 3.创建完成后需要能修改悬浮窗的属性，并需要实时生效，以实现弹窗软件盘、隐藏软件盘
 */
open class CustomFloatingWindow<T : ViewBinding>(protected val context: Context) {
    protected val windowManager: WindowManager by lazy { createManager(context) }
    val binding: T by lazy { createBinding(context) }
    val windowParams: WindowManager.LayoutParams by lazy { createParams() }
    private var isShowing = false

    private fun createManager(context: Context): WindowManager {
        initView()
        initParams()
        return context.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager
    }

    private fun createParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams()
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION
        params.format = PixelFormat.TRANSPARENT
        params.flags = 0 or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        return params
    }

    open fun createBinding(context: Context): T {
        val superClass = this.javaClass.genericSuperclass as ParameterizedType
        val bindingClass = superClass.actualTypeArguments[0] as Class<*>
        val inflate = try {
            bindingClass.getMethod("inflate", LayoutInflater::class.java)
        } catch (e: Exception) {
            bindingClass.declaredMethods.first {
                val argumentTypes = arrayOf(LayoutInflater::class.java)
                it.parameterTypes.contentEquals(argumentTypes)
            }
        }
        inflate.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return inflate.invoke(null, LayoutInflater.from(context)) as T
    }

    open fun initView() {

    }

    open fun initParams() {

    }

    fun show() {
        windowManager.addView(binding.root, windowParams)
        isShowing = true
    }

    fun isShowing(): Boolean {
        return isShowing
    }

    fun update(x: Int = -1, y: Int = -1, width: Int = -1, height: Int = -1, gravity: Int = -1) {
        if (x != -1) windowParams.x = x
        if (x != -1) windowParams.x = x
        if (y != -1) windowParams.y = y
        if (width != -1) windowParams.width = width
        if (height != -1) windowParams.height = height
        if (gravity != -1) windowParams.gravity = gravity
        windowManager.updateViewLayout(binding.root, windowParams)
    }

    fun hide() {
        windowManager.removeView(binding.root)
        isShowing = false
    }
}