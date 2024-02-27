package com.sliver.samples.floatingwindow

import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityFloatingWindowSampleBinding

class FloatingWindowSampleActivity : BaseActivity<ActivityFloatingWindowSampleBinding>() {
    private val floatingWindow by lazy { TestFloatingWindow(this) }

    override fun initView() {
        binding.toggleFloatingWindow.setOnClickListener {
            if (!floatingWindow.isShowing()) {
                floatingWindow.show()
            } else {
                floatingWindow.hide()
            }
        }
    }
}