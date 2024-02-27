package com.sliver.samples.dialogwindow

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.TextView
import com.sliver.samples.dialogwindow.core.CustomDialog
import com.sliver.samples.dialogwindow.core.ViewBinding

class TestDialog(context: Context) : CustomDialog<ViewBinding>(context) {

    override fun createBinding(context: Context): ViewBinding {
        val textView = TextView(context)
        textView.text = "Hello"
        textView.fitsSystemWindows = true
        return ViewBinding(textView)
    }

    override fun initView(binding: ViewBinding) {
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