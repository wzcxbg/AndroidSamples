package com.sliver.samples

import android.content.Context
import android.view.WindowManager
import com.sliver.samples.databinding.PopupTestBinding
import com.sliver.samples.popup.CustomPopupWindow

class TestPopupWindow(
    private val context: Context
) : CustomPopupWindow<PopupTestBinding>(context) {

    override fun initView() {
        binding.title.text = "提示"
        binding.message.text = "确认要退出app吗"
        binding.cancel.text = "取消"
        binding.confirm.text = "确定"
        binding.cancel.setOnClickListener { dismiss() }
        binding.confirm.setOnClickListener { dismiss() }
    }

    override fun initPopup() {
        Builder(context)
            .width(WindowManager.LayoutParams.WRAP_CONTENT)
            .height(WindowManager.LayoutParams.WRAP_CONTENT)
            .anime(android.R.style.Animation_InputMethod)
            .outsideTouchable(false)
            .applyParameter(this)
    }
}