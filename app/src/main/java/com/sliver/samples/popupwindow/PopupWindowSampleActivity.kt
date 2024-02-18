package com.sliver.samples.popupwindow

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityPopupWindowSampleBinding
import com.sliver.samples.databinding.PopupTestBinding
import com.sliver.samples.popupwindow.adapter.GravityAdapter
import com.sliver.samples.popupwindow.core.CustomPopupWindow
import com.sliver.samples.popupwindow.core.PopupWindowLocator
import com.sliver.samples.popupwindow.simple.SimpleSeekBarChangeListener

class PopupWindowSampleActivity : BaseActivity<ActivityPopupWindowSampleBinding>() {
    private val popupWindow by TestPopupWindow(this)
    private val adapter = GravityAdapter()
    private var showType = ShowType.NONE

    override fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.anchor.setOnClickListener {
            PopupWindowLocator(popupWindow)
                .startToStartOfAnchor()
                .topToBottomOfAnchor()
                .showAsDropDown(binding.anchor)
        }
        adapter.setGravityChangedListener(object : GravityAdapter.GravityListener {
            override fun onGravityChanged(gravity: Int) {
                popupWindow.dismiss()
                if (showType == ShowType.SHOW_AT_LOCATION) {
                    showPopupWindowAtLocation()
                } else if (showType == ShowType.SHOW_AS_DROPDOWN) {
                    showPopupWindowAsDropDown()
                }
            }
        })

        binding.seekbarX.setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.seekbarXValue.text = progress.toString()
                updatePopupWindow()
            }
        })
        binding.seekbarY.setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.seekbarYValue.text = progress.toString()
                updatePopupWindow()
            }
        })
        binding.seekbarXDesc.setOnClickListener { binding.seekbarX.progress = 0 }
        binding.seekbarYDesc.setOnClickListener { binding.seekbarY.progress = 0 }
        binding.showAtLocation.setOnClickListener { showPopupWindowAtLocation() }
        binding.showAsDropdown.setOnClickListener { showPopupWindowAsDropDown() }
    }

    private fun showPopupWindowAtLocation() {
        if (popupWindow.isShowing &&
            showType == ShowType.SHOW_AT_LOCATION
        ) {
            popupWindow.dismiss()
            return
        } else if (popupWindow.isShowing &&
            showType != ShowType.SHOW_AT_LOCATION
        ) {
            popupWindow.dismiss()
        }
        popupWindow.showAtLocation(
            binding.anchor,
            adapter.getSelectedGravity(),
            binding.seekbarX.progress,
            binding.seekbarY.progress,
        )
        showType = ShowType.SHOW_AT_LOCATION
    }

    private fun showPopupWindowAsDropDown() {
        if (popupWindow.isShowing &&
            showType == ShowType.SHOW_AS_DROPDOWN
        ) {
            popupWindow.dismiss()
            return
        } else if (popupWindow.isShowing &&
            showType != ShowType.SHOW_AS_DROPDOWN
        ) {
            popupWindow.dismiss()
        }
        popupWindow.showAsDropDown(
            binding.anchor,
            binding.seekbarX.progress,
            binding.seekbarY.progress,
            adapter.getSelectedGravity()
        )
        showType = ShowType.SHOW_AS_DROPDOWN
    }

    private fun updatePopupWindow() {
        //SHOW_AT_LOCATION能直接Update为SHOW_AS_DROPDOWN
        //SHOW_AS_DROPDOWN不能直接Update为SHOW_AT_LOCATION
        if (showType == ShowType.SHOW_AT_LOCATION) {
            popupWindow.update(
                binding.seekbarX.progress,
                binding.seekbarY.progress,
                -1, -1,
            )
        } else if (showType == ShowType.SHOW_AS_DROPDOWN) {
            popupWindow.update(
                binding.anchor,
                binding.seekbarX.progress,
                binding.seekbarY.progress,
                -1, -1,
            )
        }
    }

    private fun createPopupWindow1(context: Context): PopupWindow {
        val textView = TextView(context)
        textView.text = "文本"
        textView.setTextColor(Color.WHITE)
        //textView.background = ColorDrawable(0xFF03A9F4.toInt())
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        val popupWindow = PopupWindow(context)
        popupWindow.contentView = textView
        popupWindow.setBackgroundDrawable(ColorDrawable(0xFF03A9F4.toInt()))
        return popupWindow
    }

    private fun createPopupWindow2(context: Context): PopupWindow {
        return CustomPopupWindow.Builder(context)
            .width(WindowManager.LayoutParams.WRAP_CONTENT)
            .height(WindowManager.LayoutParams.WRAP_CONTENT)
            .customView(PopupTestBinding::class.java) {
                title.text = "标题"
                message.text = "信息"
                cancel.text = "取消"
                confirm.text = "确定"
            }
            .outsideCancelable(false)
            .elevation(16f)
            .build()
    }

    enum class ShowType {
        NONE,
        SHOW_AT_LOCATION,
        SHOW_AS_DROPDOWN,
    }
}