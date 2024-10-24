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
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

open class BaseDialog<T : ViewBinding>(
    private val context: Context
) : ComponentDialog(context) {
    // 有未显示弹窗但需要设置弹窗视图状态的情况，因此将initView提到OnCreate之前执行
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

        //window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

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
}