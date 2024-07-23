package com.sliver.samples.screencapture

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewbinding.ViewBinding
import com.sliver.samples.floatingwindow.ViewBindingInitializer

class TestScreenCaptureWindowBinding(
    val imageView: ImageView
) : ViewBinding {
    override fun getRoot(): View {
        return imageView
    }

    companion object : ViewBindingInitializer<TestScreenCaptureWindowBinding> {
        @JvmStatic
        override fun inflate(inflater: LayoutInflater): TestScreenCaptureWindowBinding {
            return inflate(inflater, null, false)
        }

        @JvmStatic
        override fun inflate(
            inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean
        ): TestScreenCaptureWindowBinding {
            val imageView = ImageView(inflater.context)
            if (parent != null && attachToParent) {
                parent.addView(imageView)
            }
            return TestScreenCaptureWindowBinding(imageView)
        }

        @JvmStatic
        override fun bind(rootView: View): TestScreenCaptureWindowBinding? {
            return null
        }
    }
}