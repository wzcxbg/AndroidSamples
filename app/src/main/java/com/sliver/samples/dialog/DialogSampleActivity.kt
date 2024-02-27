package com.sliver.samples.dialog

import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityDialogSampleBinding

class DialogSampleActivity : BaseActivity<ActivityDialogSampleBinding>() {
    private val testDialog by lazy { TestDialog(this) }

    override fun initView() {
        binding.toggleDialog.setOnClickListener {
            if (!testDialog.isShowing) {
                testDialog.show()
            } else {
                testDialog.dismiss()
            }
        }
    }
}