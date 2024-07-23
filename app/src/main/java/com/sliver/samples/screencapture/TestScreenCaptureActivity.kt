package com.sliver.samples.screencapture

import android.app.Activity
import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityTestScreenCaptureBinding
import com.sliver.samples.floatingwindow.core.FloatingWindowUtils

class TestScreenCaptureActivity : BaseActivity<ActivityTestScreenCaptureBinding>() {
    private val launcher = ScreenCapturer.registerScreenCaptureLauncher(this)
    private var floatingWindow: TestScreenCaptureWindow? = null

    override fun initView() {
        binding.showCapturer.setOnClickListener {
            if (!FloatingWindowUtils.canDrawOverlays(this)) {
                FloatingWindowUtils.requestPermission(this)
            } else {
                when (floatingWindow?.isShowing()) {
                    null -> {
                        launcher.launch {
                            val resultData = it.data ?: return@launch
                            if (it.resultCode != Activity.RESULT_OK) return@launch
                            floatingWindow = TestScreenCaptureWindow(this, resultData)
                            floatingWindow?.show()
                        }
                    }

                    true -> floatingWindow?.hide()
                    false -> floatingWindow?.show()
                }
            }
        }
    }
}