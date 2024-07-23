package com.sliver.samples.floatingwindow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewbinding.ViewBinding
import com.sliver.samples.custom.CustomShadowLayout

class TestFloatingWindowBinding(
    val shadowLayout: CustomShadowLayout,
    val imageView: ImageView
) : ViewBinding {
    override fun getRoot(): View {
        return shadowLayout
    }

    companion object : ViewBindingInitializer<TestFloatingWindowBinding> {
        @JvmStatic
        override fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean
        ): TestFloatingWindowBinding {
            val rootView = CustomShadowLayout(inflater.context)
            val imageView = ImageView(inflater.context)
            rootView.addView(imageView)
            if (attachToParent && parent != null) {
                parent.addView(rootView)
            }
            return TestFloatingWindowBinding(rootView, imageView)
        }

        @JvmStatic
        override fun bind(rootView: View): TestFloatingWindowBinding? {
            return null
        }

        @JvmStatic
        override fun inflate(
            inflater: LayoutInflater,
        ): TestFloatingWindowBinding {
            return inflate(inflater, null, false)
        }
    }
}