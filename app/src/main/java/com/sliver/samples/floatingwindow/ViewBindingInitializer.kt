package com.sliver.samples.floatingwindow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

interface ViewBindingInitializer<T : ViewBinding> {
    fun inflate(inflater: LayoutInflater): T
    fun inflate(inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean): T
    fun bind(rootView: View): T?
}