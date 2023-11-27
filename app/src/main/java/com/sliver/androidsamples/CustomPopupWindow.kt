package com.sliver.androidsamples

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

open class CustomPopupWindow<T : ViewBinding>(context: Context) : PopupWindow(context) {
    private val builder = Builder(context)
    protected val binding = createBinding(context)
    private var isInitialized = false

    protected open fun initView(builder: Builder) {

    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        initPopupWindow()
        super.showAtLocation(parent, gravity, x, y)
    }

    override fun showAsDropDown(anchor: View?) {
        initPopupWindow()
        super.showAsDropDown(anchor)
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int) {
        initPopupWindow()
        super.showAsDropDown(anchor, xoff, yoff)
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        initPopupWindow()
        super.showAsDropDown(anchor, xoff, yoff, gravity)
    }

    private fun initPopupWindow() {
        if (!isInitialized) {
            builder.customView(binding.root)
            initView(builder)
            builder.applyParameter(this)
            isInitialized = true
        }
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
        return inflate.invoke(null, LayoutInflater.from(context)) as T
    }

    class Builder(private val context: Context) {
        private var view: View? = null
        private var width = WindowManager.LayoutParams.WRAP_CONTENT
        private var height = WindowManager.LayoutParams.WRAP_CONTENT
        private var animationStyle = -1
        private var outsideTouchable = true //点击外部取消显示
        private var clippingEnabled = true  //限制在屏幕内显示
        private var enterTransition: Transition? = null
        private var exitTransition: Transition? = null
        private var elevation = 16f
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
        fun outsideTouchable(touchable: Boolean) = apply { this.outsideTouchable = touchable }
        fun clippingEnabled(enabled: Boolean) = apply { this.outsideTouchable = enabled }

        fun applyParameter(popupWindow: PopupWindow): PopupWindow {
            popupWindow.width = width
            popupWindow.height = height
            popupWindow.contentView = view

            popupWindow.animationStyle = animationStyle
            popupWindow.isOutsideTouchable = outsideTouchable
            popupWindow.isClippingEnabled = clippingEnabled
            popupWindow.enterTransition = enterTransition
            popupWindow.exitTransition = exitTransition

            //需要先设置背景颜色不透明，elevation属性才有效
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.elevation = elevation

            listener?.invoke(view ?: return popupWindow, popupWindow)
            return popupWindow
        }

        fun build(): PopupWindow {
            val popupWindow = PopupWindow(context)
            applyParameter(popupWindow)
            bindingBuilder?.applyParameter(popupWindow)
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
