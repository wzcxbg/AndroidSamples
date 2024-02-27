package com.sliver.samples.floatingwindow.core

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

open class CustomFloatingWindow<T : ViewBinding>(private val context: Context) {
    protected val binding by lazy { createBinding(context) }
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isInitialized = false
    private var isShowing = false

    private val windowParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        0, 0,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or   //可出界
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or     //不获取焦点
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or   //允许触控悬浮窗外的控件
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or  //接收外部点击事件
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,

        PixelFormat.TRANSLUCENT
    )

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
        if (!isInitialized) {
            initView()
            initParams()
            isInitialized = true
        }
        windowManager.addView(binding.root, windowParams)
        isShowing = true
    }

    fun isShowing(): Boolean {
        return isShowing
    }

    fun update(x: Int, y: Int, width: Int, height: Int, gravity: Int) {
        windowParams.x = x
        windowParams.y = y
        windowParams.width = width
        windowParams.height = height
        windowParams.gravity = gravity
        windowManager.updateViewLayout(binding.root, windowParams)
    }

    fun hide() {
        if (!isInitialized) {
            initView()
            initParams()
            isInitialized = true
        }
        windowManager.removeView(binding.root)
        isShowing = false
    }
}