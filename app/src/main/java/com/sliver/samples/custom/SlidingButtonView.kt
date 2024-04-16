package com.sliver.samples.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import android.widget.TextView
import com.sliver.samples.R


class SlidingButtonView : HorizontalScrollView {
    private var lScrollWidth = 0
    private val tvDelete by lazy { findViewById<TextView>(R.id.tv_delete) }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        setOverScrollMode(OVER_SCROLL_NEVER)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        //默认隐藏删除按钮
        if (changed) {
            scrollTo(0, 0)
            //获取水平滚动条可以滑动的范围，即右侧按钮的宽度
            lScrollWidth = tvDelete.width
        }
    }

    /**
     * 滑动手指抬起时的手势判断
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                changeScrollx() //根据滑动距离判断是否显示删除按钮
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 根据滑动距离判断是否显示删除按钮
     */
    private fun changeScrollx() {
        //触摸滑动的距离大于删除按钮宽度的一半
        if (scrollX >= lScrollWidth / 2) {
            //显示删除按钮
            smoothScrollTo(lScrollWidth, 0)
        } else {
            //隐藏删除按钮
            smoothScrollTo(0, 0)
        }
    }
}

