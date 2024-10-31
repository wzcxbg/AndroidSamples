package com.sliver.samples.viewpager

import com.sliver.samples.base.BaseActivity
import com.sliver.samples.databinding.ActivityViewPager2Binding

class ViewPager2Activity : BaseActivity<ActivityViewPager2Binding>() {
    private val fragments = listOf(
        ViewPager2PageFragment.newInstance(),
        ViewPager2PageFragment.newInstance(),
        ViewPager2PageFragment.newInstance(),
    )

    override fun initView() {
        // 嵌套滑动的类型: 同方向、不同方向
        // 嵌套滑动解决方式1: onInterceptTouchEvent() 和 onTouchEvent()
        // 嵌套滑动解决方式2: NestedScrollParent 和 NestedScrollChild
        binding.viewPager.adapter = ViewPager2Adapter(
            fragments, supportFragmentManager, lifecycle
        )
    }
}