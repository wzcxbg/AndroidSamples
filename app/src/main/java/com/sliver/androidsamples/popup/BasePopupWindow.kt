package com.sliver.androidsamples.popup

import android.view.View
import android.widget.PopupWindow

open class BasePopupWindow : PopupWindow(), Lazy<PopupWindow> {
    private var isInitialized = false

    override val value: PopupWindow get() = this
    override fun isInitialized() = isInitialized

    private fun initializeFirst() {
        if (!isInitialized) {
            initPopupWindow()
            isInitialized = true
        }
    }

    protected open fun initPopupWindow() {}

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        initializeFirst()
        super.showAtLocation(parent, gravity, x, y)
    }

    override fun showAsDropDown(anchor: View?) {
        initializeFirst()
        super.showAsDropDown(anchor)
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int) {
        initializeFirst()
        super.showAsDropDown(anchor, xoff, yoff)
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        initializeFirst()
        super.showAsDropDown(anchor, xoff, yoff, gravity)
    }
}