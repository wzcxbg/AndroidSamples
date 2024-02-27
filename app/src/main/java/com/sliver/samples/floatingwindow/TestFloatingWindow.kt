package com.sliver.samples.floatingwindow

import android.content.Context
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.sliver.samples.floatingwindow.core.CustomFloatingWindow

class TestFloatingWindow(
    private val context: Context
) : CustomFloatingWindow<ViewBinding>(context) {

    override fun createBinding(context: Context): ViewBinding {
        val textView = TextView(context)
        textView.text = "Floating Window"
        return com.sliver.samples.dialogwindow.core.ViewBinding(textView)
    }

    override fun initView() {

    }

    override fun initParams() {

    }
}