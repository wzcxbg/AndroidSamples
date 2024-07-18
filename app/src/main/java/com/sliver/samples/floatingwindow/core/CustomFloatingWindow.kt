package com.sliver.samples.floatingwindow.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding

/**
 * 目标：
 * 1.能直接使用Builder创建悬浮窗，需要在最后build才正式创建悬浮窗
 * 2.能使用ParamsApplier在CustomFloatingWindow内部设置悬浮窗属性，并且在最后一步apply才生效，为了简便，没有设置的属性不要参与修改
 * 3.创建完成后需要能修改悬浮窗的属性，并需要实时生效，以实现弹窗软件盘、隐藏软件盘
 */
open class CustomFloatingWindow(protected val context: Context) {
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    protected val rootView by lazy { initView() }
    protected val windowParams by lazy { initParams() }
    private var isShowing = false

    protected open fun initView(): View {
        return View(context)
    }

    protected open fun initParams(): FloatingWindowParams {
        return FloatingWindowParams()
    }

    fun show() {
        windowManager.addView(rootView, windowParams.toLayoutParams())
        isShowing = true
    }

    fun isShowing(): Boolean {
        return isShowing
    }

    fun move(x: Int, y: Int) {
        windowParams.moveX(x)
        windowParams.moveY(y)
        windowManager.updateViewLayout(rootView, windowParams.toLayoutParams())
    }

    fun update(
        x: Int = -1, y: Int = -1,
        width: Int = -1, height: Int = -1,
        gravity: Int = -1
    ) {
        if (x != -1) windowParams.setX(x)
        if (y != -1) windowParams.setY(y)
        if (width != -1) windowParams.width(width)
        if (height != -1) windowParams.height(height)
        if (gravity != -1) windowParams.gravity(gravity)
        windowManager.updateViewLayout(rootView, windowParams.toLayoutParams())
    }

    fun hide() {
        windowManager.removeView(rootView)
        isShowing = false
    }

    protected inline fun <reified T : ViewBinding> createView(): Lazy<T> {
        return lazy(LazyThreadSafetyMode.NONE) {
            val bindingClass = T::class.java
            val inflate = try {
                bindingClass.getMethod("inflate", LayoutInflater::class.java)
            } catch (e: Exception) {
                bindingClass.declaredMethods.first {
                    val argumentTypes = arrayOf(LayoutInflater::class.java)
                    it.parameterTypes.contentEquals(argumentTypes)
                }
            }
            inflate.isAccessible = true
            inflate.invoke(null, LayoutInflater.from(context)) as T
        }
    }

    protected fun createView(@LayoutRes layoutId: Int): Lazy<View> {
        return lazy(LazyThreadSafetyMode.NONE) {
            LayoutInflater.from(context).inflate(
                layoutId, null, false
            )
        }
    }
}