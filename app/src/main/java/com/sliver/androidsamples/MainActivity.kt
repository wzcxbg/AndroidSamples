package com.sliver.androidsamples

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sliver.androidsamples.databinding.PopupTestBinding

class MainActivity : AppCompatActivity() {
    private val anchor by lazy { findViewById<TextView>(R.id.anchor) }
    private val showAtLocation by lazy { findViewById<TextView>(R.id.show_at_location) }
    private val showAsDropDown by lazy { findViewById<TextView>(R.id.show_as_dropdown) }
    private val seekBarX by lazy { findViewById<SeekBar>(R.id.seekbar_x) }
    private val seekBarY by lazy { findViewById<SeekBar>(R.id.seekbar_y) }
    private val seekBarXValue by lazy { findViewById<TextView>(R.id.seekbar_x_value) }
    private val seekBarYValue by lazy { findViewById<TextView>(R.id.seekbar_y_value) }
    private val seekBarXDesc by lazy { findViewById<TextView>(R.id.seekbar_x_desc) }
    private val seekBarYDesc by lazy { findViewById<TextView>(R.id.seekbar_y_desc) }
    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }
    private val popupWindow by lazy { createPopupWindow(this) }
    private val adapter = GravityAdapter()
    private var showType = ShowType.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

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
        seekBarX.setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBarXValue.text = progress.toString()
                updatePopupWindow()
            }
        })
        seekBarY.setOnSeekBarChangeListener(object : SimpleSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBarYValue.text = progress.toString()
                updatePopupWindow()
            }
        })
        seekBarXDesc.setOnClickListener { seekBarX.progress = 0 }
        seekBarYDesc.setOnClickListener { seekBarY.progress = 0 }
        showAtLocation.setOnClickListener { showPopupWindowAtLocation() }
        showAsDropDown.setOnClickListener { showPopupWindowAsDropDown() }
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
            anchor,
            adapter.getSelectedGravity(),
            seekBarX.progress,
            seekBarY.progress,
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
            anchor,
            seekBarX.progress,
            seekBarY.progress,
            adapter.getSelectedGravity()
        )
        showType = ShowType.SHOW_AS_DROPDOWN
    }

    private fun updatePopupWindow() {
        //SHOW_AT_LOCATION能直接Update为SHOW_AS_DROPDOWN
        //SHOW_AS_DROPDOWN不能直接Update为SHOW_AT_LOCATION
        if (showType == ShowType.SHOW_AT_LOCATION) {
            popupWindow.update(
                seekBarX.progress,
                seekBarY.progress,
                -1, -1,
            )
        } else if (showType == ShowType.SHOW_AS_DROPDOWN) {
            popupWindow.update(
                anchor,
                seekBarX.progress,
                seekBarY.progress,
                -1, -1,
            )
        }
    }

    private fun createPopupWindow(context: Context): PopupWindow {
        val textView = TextView(context)
        textView.text = "Popup"
        textView.setTextColor(Color.WHITE)
        //textView.background = ColorDrawable(0xFF03A9F4.toInt())
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
//        val popupWindow = PopupWindow(context)
//        popupWindow.contentView = textView
//        popupWindow.setBackgroundDrawable(ColorDrawable(0xFF03A9F4.toInt()))
//        return popupWindow
        val popupWindow = CustomPopupWindow.Builder(context)
            .width(WindowManager.LayoutParams.WRAP_CONTENT)
            .height(WindowManager.LayoutParams.WRAP_CONTENT)
            .customView(PopupTestBinding::class.java) {
                title.text = "标题"
                message.text = "信息"
                cancel.text = "取消"
                confirm.text = "确定"
            }
            .outsideTouchable(false)
            .elevation(16f)
            .build()
        return popupWindow
    }

    enum class ShowType {
        NONE,
        SHOW_AT_LOCATION,
        SHOW_AS_DROPDOWN,
    }
}