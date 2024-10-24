package com.sliver.samples.dialogwindow.core

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * 自定义弹窗基类，需要自定义时继承该基类；要通用时继承该基类实现CommonDialog
 * 1. CustomDialog不应该强制依赖ViewBinding，但是不依赖使用起来又会特别麻烦，因此还是依赖了ViewBinding
 * 2. 不再提供CustomDialog.Builder以防止在Activity中随意创建Dialog的乱象
 * 3. 支持View、ViewBinding，可选择自己创建View或通过基类创建ViewBinding
 * 4. 使用最原始的方式设置弹窗的样式，其他人使用时不在需要熟悉新的API
 * 5. 有未创建弹窗但需要先设置弹窗View状态的需求，因此将initView提前到OnCreate之前执行
 * TODO 研究未设置弹窗初始Style时，如何修改状态栏颜色
 */
open class CustomDialog<T : ViewBinding>(
    private val context: Context
) : ComponentDialog(context) {
    protected val binding by lazy { createBinding(context).also { initView(it) } }

    protected open fun createView(): View? {
        return null
    }

    protected open fun initView(binding: T) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initDialog(this)
    }

    protected open fun initDialog(dialog: Dialog) {
        val window = dialog.window

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setWindowAnimations(android.R.style.Animation_Dialog)
        window?.setGravity(Gravity.CENTER)
        window?.setDimAmount(0.5f)

        //状态栏半透明，另两个效果: 使弹窗能延伸进状态栏，会导致软键盘无法顶起弹窗
        //如果需要实现沉浸式状态栏，需要:
        // 保持根布局延伸进状态栏：fitsSystemWindows = false
        // 子View设置为不需要延伸进状态栏：fitsSystemWindows = true
        window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        //window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        window?.attributes?.width = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes?.height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        window?.decorView?.setPadding(0, 0, 0, 0)
    }

    private fun createBinding(context: Context): T {
        val superClass = this.javaClass.genericSuperclass as ParameterizedType
        val bindingClass = superClass.actualTypeArguments[0] as Class<*>
        @Suppress("UNCHECKED_CAST")
        return if (bindingClass != ViewBinding::class.java) {
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
        } else {
            ViewBindingWrapper(requireNotNull(createView())) as T
        }
    }

    @JvmInline
    value class ViewBindingWrapper(private val view: View) : ViewBinding {
        override fun getRoot(): View {
            return view
        }
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