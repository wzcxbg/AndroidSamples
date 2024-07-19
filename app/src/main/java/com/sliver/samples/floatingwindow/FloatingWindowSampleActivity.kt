package com.sliver.samples.floatingwindow

import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityFloatingWindowSampleBinding
import com.sliver.samples.floatingwindow.core.FloatingWindowUtils

class FloatingWindowSampleActivity : BaseActivity<ActivityFloatingWindowSampleBinding>() {
    private val floatingWindow by lazy { TestFloatingWindow(this) }

    override fun initView() {
        binding.toggleFloatingWindow.setOnClickListener {
            if (!FloatingWindowUtils.canDrawOverlays(this)) {
                FloatingWindowUtils.requestPermission(this)
                return@setOnClickListener
            }
            if (!floatingWindow.isShowing()) {
                floatingWindow.show()
            } else {
                floatingWindow.hide()
            }
        }
    }
}