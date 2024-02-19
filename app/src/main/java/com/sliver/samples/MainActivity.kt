package com.sliver.samples

import android.content.Intent
import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityMainBinding
import com.sliver.samples.popupwindow.PopupWindowSampleActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun initView() {
        binding.hello.setOnClickListener {
            val intent = Intent(this, PopupWindowSampleActivity::class.java)
            startActivity(intent)
        }
    }
}