package com.sliver.samples.dialogwindow.core

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType
import kotlin.math.roundToInt


/**
 * CustomDialog不应该强制依赖ViewBinding，但是不依赖又特别麻烦
 * TODO 不应该提供CustomDialog.Builder以防其随意创建Dialog
 * TODO 统一View、ViewBinding
 * TODO 修改弹窗的初始样式
 * TODO 寻找更合适的设置dialog属性的方式
 */
open class CustomDialog<T : ViewBinding>(private val context: Context) : Dialog(context) {
    protected val binding by lazy { createBinding(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(binding.root)
        initView(binding)
        initDialog(this)
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

    open fun initView(binding: T) {

    }

    open fun initDialog(dialog: Dialog) {

    }

    class Builder(private val context: Context) {
        private var view: View? = null
        private var gray = 0.5f
        private var width = WindowManager.LayoutParams.WRAP_CONTENT
        private var height = WindowManager.LayoutParams.WRAP_CONTENT
        private var gravity = Gravity.CENTER
        private var anime = android.R.style.Animation_Dialog
        private var cancelable = true
        private var padding = RectF(0f, 0f, 0f, 0f)
        private var bindingBuilder: BindingBuilder<out ViewBinding>? = null

        fun <T : ViewBinding> customView(
            binding: T,
            bindView: T.(Dialog) -> Unit = {}
        ) = apply {
            this.bindingBuilder = BindingBuilder<T>(context)
                .customView(binding)
                .bindView(bindView)
        }

        fun <T : ViewBinding> customView(
            clazz: Class<T>,
            bindView: T.(Dialog) -> Unit = {}
        ) = apply {
            this.bindingBuilder = BindingBuilder<T>(context)
                .customView(createBinding(context, clazz) as T)
                .bindView(bindView)
        }

        fun customView(view: View) = apply { this.view = view }
        fun backgroundGray(@DimAmountRange gray: Float) = apply { this.gray = gray }
        fun width(@LayoutParamFlags width: Int) = apply { this.width = width }
        fun height(@LayoutParamFlags height: Int) = apply { this.height = height }
        fun gravity(@GravityFlags gravity: Int) = apply { this.gravity = gravity }
        fun animate(@AnimationFlags animateId: Int) = apply { this.anime = animateId }
        fun cancelable(cancelable: Boolean) = apply { this.cancelable = cancelable }
        fun padding(
            @Dimension(unit = Dimension.DP) padding: Float
        ) = apply { this.padding = RectF(padding, padding, padding, padding) }

        fun padding(
            @Dimension(unit = Dimension.DP) left: Float,
            @Dimension(unit = Dimension.DP) top: Float,
            @Dimension(unit = Dimension.DP) right: Float,
            @Dimension(unit = Dimension.DP) bottom: Float
        ) = apply { this.padding = RectF(left, top, right, bottom) }

        fun applyParameter(dialog: Dialog): Dialog {
            val window = dialog.window
            val decorView = window?.decorView
            val attributes = window?.attributes

            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setWindowAnimations(anime)
            window?.setGravity(gravity)
            window?.setDimAmount(gray)

            //状态栏半透明，另一个效果: 使弹窗能延伸进状态栏
            //如果需要实现沉浸式状态栏，需要:
            // 保持根布局延伸进状态栏：fitsSystemWindows = false
            // 子View设置为不需要延伸进状态栏：fitsSystemWindows = true
            window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

            attributes?.width = width
            attributes?.height = height

            dialog.setCanceledOnTouchOutside(cancelable)
            dialog.setCancelable(cancelable)
            dialog.setContentView(view ?: return dialog)

            val displayMetrics = context.resources.displayMetrics
            decorView?.setPadding(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    padding.left, displayMetrics
                ).roundToInt(),
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    padding.top, displayMetrics
                ).roundToInt(),
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    padding.right, displayMetrics
                ).roundToInt(),
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    padding.bottom, displayMetrics
                ).roundToInt(),
            )

            bindingBuilder?.applyParameter(dialog)
            return dialog
        }

        fun build(): Dialog {
            val dialog = Dialog(context)
            return applyParameter(dialog)
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
            inflate.isAccessible = true
            return inflate.invoke(null, LayoutInflater.from(context)) as ViewBinding
        }

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            Gravity.FILL,
            Gravity.FILL_HORIZONTAL,
            Gravity.FILL_VERTICAL,
            Gravity.START,
            Gravity.END,
            Gravity.TOP,
            Gravity.BOTTOM,
            Gravity.CENTER,
            Gravity.CENTER_HORIZONTAL,
            Gravity.CENTER_VERTICAL,
            Gravity.NO_GRAVITY,
            flag = false,
            open = true,
        )
        annotation class GravityFlags

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            flag = false,
            open = true,
        )
        annotation class LayoutParamFlags

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            android.R.style.Animation_Dialog,
            android.R.style.Animation_InputMethod,
            android.R.style.Animation_Toast,
            android.R.style.Animation_Translucent,
            flag = false,
            open = true,
        )
        annotation class AnimationFlags

        @Retention(AnnotationRetention.SOURCE)
        @FloatRange(0.0, 1.0, true, true)
        annotation class DimAmountRange
    }

    private class BindingBuilder<T : ViewBinding>(
        private val context: Context
    ) {
        private var binding: T? = null
        private var listener: (T.(Dialog) -> Unit)? = null

        fun customView(binding: T) = apply { this.binding = binding }
        fun bindView(listener: T.(Dialog) -> Unit) = apply { this.listener = listener }

        fun applyParameter(dialog: Dialog): Dialog {
            dialog.setContentView(binding?.root ?: return dialog)
            listener?.invoke(binding ?: return dialog, dialog)
            return dialog
        }

        fun build(): Dialog {
            val dialog = Dialog(context)
            applyParameter(dialog)
            return dialog
        }
    }
}