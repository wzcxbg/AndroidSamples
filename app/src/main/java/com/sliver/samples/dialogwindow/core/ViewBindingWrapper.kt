package com.sliver.samples.dialogwindow.core

import android.view.View
import androidx.viewbinding.ViewBinding

@JvmInline
value class ViewBindingWrapper(private val view: View) : ViewBinding {
    override fun getRoot(): View {
        return view
    }
}