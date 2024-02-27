package com.sliver.samples

import android.content.Intent
import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityMainBinding
import com.sliver.samples.dialog.DialogSampleActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun initView() {
        binding.hello.setOnClickListener {
            val intent = Intent(this, DialogSampleActivity::class.java)
            startActivity(intent)
        }
    }
}