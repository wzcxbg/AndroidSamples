package com.sliver.samples.dialogwindow

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.sliver.samples.dialogwindow.core.CustomDialog

class TestDialog(context: Context) : CustomDialog<ViewBinding>(context) {

    override fun createView(): View {
        val textView = TextView(context)
        textView.text = "Hello"
        textView.setBackgroundColor(0xFF550000.toInt())
        return textView
    }

    override fun initView(binding: ViewBinding) {
        binding.root.setOnClickListener {

        }
    }

    override fun initDialog(dialog: Dialog) {
        super.initDialog(dialog)
        window?.attributes?.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes?.height = WindowManager.LayoutParams.MATCH_PARENT
        window?.setWindowAnimations(android.R.style.Animation_InputMethod)
        binding.root.fitsSystemWindows = true
    }
}