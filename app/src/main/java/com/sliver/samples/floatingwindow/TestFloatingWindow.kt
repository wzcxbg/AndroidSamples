package com.sliver.samples.floatingwindow

import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.sliver.samples.dialogwindow.core.ViewBindingWrapper
import com.sliver.samples.floatingwindow.core.CustomFloatingWindow
import com.sliver.samples.floatingwindow.core.FloatingWindowParams

class TestFloatingWindow(context: Context) : CustomFloatingWindow<ViewBinding>(context) {
    override fun createBinding(context: Context): ViewBinding {
        val textView = TextView(context)
        textView.text = "Floating Window"
        return ViewBindingWrapper(textView)
    }

    override fun initParams() {
        FloatingWindowParams()
            .movable(true)
            .gravity(Gravity.CENTER)
            .apply(this)
    }
}