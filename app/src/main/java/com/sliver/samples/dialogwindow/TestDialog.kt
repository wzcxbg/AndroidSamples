package com.sliver.samples.dialogwindow

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.TextView
import com.sliver.samples.dialogwindow.core.CustomDialog
import com.sliver.samples.dialogwindow.core.ViewBindingWrapper

class TestDialog(context: Context) : CustomDialog<ViewBindingWrapper>(context) {

    override fun createBinding(context: Context): ViewBindingWrapper {
        val textView = TextView(context)
        textView.text = "Hello"
        textView.fitsSystemWindows = true
        return ViewBindingWrapper(textView)
    }

    override fun initView(binding: ViewBindingWrapper) {
        binding.root.setOnClickListener {

        }
    }

    override fun initDialog(dialog: Dialog) {
        CustomDialog.Builder(context)
            .width(WindowManager.LayoutParams.MATCH_PARENT)
            .height(WindowManager.LayoutParams.MATCH_PARENT)
            .applyParameter(dialog)
    }
}