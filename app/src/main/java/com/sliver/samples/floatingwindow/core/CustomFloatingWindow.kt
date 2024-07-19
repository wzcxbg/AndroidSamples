package com.sliver.samples.floatingwindow.core

import android.content.Context
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

open class CustomFloatingWindow<T : ViewBinding>(protected val context: Context) {
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    protected val binding by lazy(LazyThreadSafetyMode.NONE) { createBinding(context) }
    protected val windowParams = WindowManager.LayoutParams()
    private var isShowing = false
    private var isInitialized = false

    init {
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION
        windowParams.format = PixelFormat.TRANSPARENT
        windowParams.flags = 0 or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
    }


    protected open fun initParams() {}
    protected open fun initView() {}

    fun show() {
        if (!isInitialized) {
            initParams()
            initView()
            isInitialized = true
        }
        windowManager.addView(binding.root, windowParams)
        isShowing = true
    }

    fun isShowing(): Boolean {
        return isShowing
    }

    fun updateParams() {
        if (!isInitialized) {
            initParams()
            initView()
            isInitialized = true
        }
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

    private fun createBinding(context: Context): T {
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