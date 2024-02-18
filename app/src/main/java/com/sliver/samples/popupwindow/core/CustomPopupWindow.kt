package com.sliver.samples.popupwindow.core

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.transition.Transition
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * CustomPopupWindow：
 * 1. 如果要求其只专注于一件事，那么这就事是如何构建PopupWindow
 * 2. 如果要求其只专注于两件事，那么这两件事就是如何构建PopupView和PopupWindow
 * 3. 如果要求其只专注于三件事，那么这三件事是如何构建PopupView、PopupWindow和如何与外部进行交互
 */
@Suppress("UNCHECKED_CAST")
open class CustomPopupWindow<T : ViewBinding>(context: Context) : BasePopupWindow() {
    protected val binding by lazy { createBinding(context) }

    override fun initPopupWindow() {
        initView()
        initPopup()
        this.contentView = binding.root
    }

    protected open fun initView() {}
    protected open fun initPopup() {}

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
        return inflate.invoke(null, LayoutInflater.from(context)) as T
    }

    class Builder(private val context: Context) {
        private var view: View? = null
        private var width = WindowManager.LayoutParams.WRAP_CONTENT
        private var height = WindowManager.LayoutParams.WRAP_CONTENT
        private var animationStyle = -1
        private var outsideCancelable = true //点击外部取消显示
        private var clippingEnabled = true  //限制在屏幕内显示
        private var enterTransition: Transition? = null
        private var exitTransition: Transition? = null
        private var elevation = 0f
        private var listener: (View.(PopupWindow) -> Unit)? = null
        private var bindingBuilder: BindingBuilder<out ViewBinding>? = null

        fun <T : ViewBinding> customView(
            binding: T,
            bindView: T.(PopupWindow) -> Unit = {}
        ) = apply {
            this.bindingBuilder = BindingBuilder<T>(context)
                .customView(binding)
                .bindView(bindView)
        }

        fun <T : ViewBinding> customView(
            clazz: Class<T>,
            bindView: T.(PopupWindow) -> Unit = {}
        ) = apply {
            this.bindingBuilder = BindingBuilder<T>(context)
                .customView(createBinding(context, clazz) as T)
                .bindView(bindView)
        }

        fun customView(view: View) = apply { this.view = view }
        fun width(width: Int) = apply { this.width = width }
        fun height(height: Int) = apply { this.height = height }
        fun anime(anime: Int) = apply { this.animationStyle = anime }
        fun elevation(elevation: Float) = apply { this.elevation = elevation }
        fun enterTransition(transition: Transition) = apply { this.enterTransition = transition }
        fun exitTransition(transition: Transition) = apply { this.exitTransition = transition }
        fun outsideCancelable(cancelable: Boolean) = apply { this.outsideCancelable = cancelable }
        fun clippingEnabled(enabled: Boolean) = apply { this.clippingEnabled = enabled }

        fun applyParameter(popupWindow: PopupWindow): PopupWindow {
            popupWindow.width = width
            popupWindow.height = height
            popupWindow.contentView = view

            popupWindow.animationStyle = animationStyle
            if (outsideCancelable) {
                popupWindow.isFocusable = true
                popupWindow.isOutsideTouchable = true
            }
            popupWindow.isClippingEnabled = clippingEnabled
            popupWindow.enterTransition = enterTransition
            popupWindow.exitTransition = exitTransition

            if (elevation > 0f) {
                //需要先设置背景颜色不透明，elevation属性才有效
                //不建议在PopupWindow上设置elevation，因为其不支持圆角等特殊形状
                //推荐使用自定义View实现阴影效果来代替
                popupWindow.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                popupWindow.elevation = elevation
            }

            listener?.invoke(view ?: return popupWindow, popupWindow)
            bindingBuilder?.applyParameter(popupWindow)
            return popupWindow
        }

        fun build(): PopupWindow {
            val popupWindow = PopupWindow(context)
            applyParameter(popupWindow)
            return popupWindow
        }

        private fun createBinding(
            context: Context,
            bindingClass: Class<out ViewBinding>
        ): ViewBinding {
            val inflate = try {
                bindingClass.getMethod("inflate", LayoutInflater::class.java)
            } catch (e: Exception) {
                bindingClass.declaredMethods.first {
                    val argumentTypes = arrayOf(LayoutInflater::class.java)
                    it.parameterTypes.contentEquals(argumentTypes)
                }
            }
            return inflate.invoke(null, LayoutInflater.from(context)) as ViewBinding
        }
    }

    private class BindingBuilder<T : ViewBinding>(
        private val context: Context
    ) {
        private var binding: T? = null
        private var listener: (T.(PopupWindow) -> Unit)? = null

        fun customView(binding: T) = apply { this.binding = binding }
        fun bindView(listener: T.(PopupWindow) -> Unit) = apply { this.listener = listener }

        fun applyParameter(popupWindow: PopupWindow): PopupWindow {
            popupWindow.contentView = binding?.root
            listener?.invoke(binding ?: return popupWindow, popupWindow)
            return popupWindow
        }

        fun build(): PopupWindow {
            val popupWindow = PopupWindow(context)
            applyParameter(popupWindow)
            return popupWindow
        }
    }
}
