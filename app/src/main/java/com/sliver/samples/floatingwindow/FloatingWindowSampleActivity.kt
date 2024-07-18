package com.sliver.samples.floatingwindow

import android.graphics.BitmapFactory
import android.util.Log
import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityFloatingWindowSampleBinding
import kotlin.system.measureTimeMillis

class FloatingWindowSampleActivity : BaseActivity<ActivityFloatingWindowSampleBinding>() {
    private val floatingWindow by lazy { TestFloatingWindow(this) }

    override fun initView() {
        val commandProcess = CommandProcess("su")
        binding.toggleFloatingWindow.setOnClickListener {
            measureTimeMillis {
                val outputsFuture = commandProcess.submit("screencap -p")
                val commandOutputs = outputsFuture.get()
                Log.e(TAG, "initView: ${String(commandOutputs.outputBytes)}")
                val bitmap = BitmapFactory.decodeByteArray(
                    commandOutputs.outputBytes, 0,
                    commandOutputs.outputBytes.size
                )
                binding.imageView.setImageBitmap(bitmap)
            }.also { Log.e(TAG, "initView: $it") }

//            if (!floatingWindow.isShowing()) {
//                floatingWindow.show()
//            } else {
//                floatingWindow.hide()
//            }
        }
    }
}