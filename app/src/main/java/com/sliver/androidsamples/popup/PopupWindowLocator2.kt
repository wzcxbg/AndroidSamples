package com.sliver.androidsamples.popup

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.roundToInt

class PopupWindowLocator2(
    private val context: Context,
    private val popupWindow: PopupWindow) {
    private val lp = ConstraintLayout.LayoutParams(0, 0)
    fun startToStart() = apply { lp.startToStart = 1 }
    fun startToEnd() = apply { lp.startToEnd = 1 }
    fun endToStart() = apply { lp.endToStart = 1 }
    fun endToEnd() = apply { lp.endToEnd = 1 }
    fun topToTop() = apply { lp.topToTop = 1 }
    fun topToBottom() = apply { lp.topToBottom = 1 }
    fun bottomToTop() = apply { lp.bottomToTop = 1 }
    fun bottomToBottom() = apply { lp.bottomToBottom = 1 }
    fun marginStart(margin: Float) = apply { lp.setMargins(dpToPx(margin), lp.topMargin, lp.rightMargin, lp.bottomMargin) }
    fun marginEnd(margin: Float) = apply { lp.setMargins(lp.leftMargin, lp.topMargin, dpToPx(margin), lp.bottomMargin) }
    fun marginTop(margin: Float) = apply { lp.setMargins(lp.leftMargin, dpToPx(margin), lp.rightMargin, lp.bottomMargin) }
    fun marginBottom(margin: Float) = apply { lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, dpToPx(margin)) }
    //TODO startToParentStart
    //startToStart
    //设置宽高
    /**
     * 计算PopupWindow(src)在锚点控件(dst)上的位置
     */
    fun show(anchor: View) {
        val location = IntArray(2)
        anchor.getLocationInWindow(location)

        val (widthMeasureSpec, heightMeasureSpec) = getWidthMeasureSpec(popupWindow) to getHeightMeasureSpec(popupWindow)
        popupWindow.contentView.measure(widthMeasureSpec, heightMeasureSpec)

        val (dstX, dstY) = location[0] to location[1]
        val (dstW, dstH) = anchor.measuredWidth to anchor.measuredHeight
        val (srcW, srcH) = popupWindow.contentView.measuredWidth to popupWindow.contentView.measuredHeight
        var (srcX, srcY) = 0 to 0

        //计算x坐标
        var (xLeftEdge, xRightEdge) = 0 to 0
        if (lp.startToStart == 1 && lp.startToEnd == 1) {
            xLeftEdge = dstW / 2
        } else if (lp.startToStart == 1) {
            xLeftEdge = 0
        } else if (lp.startToEnd == 1) {
            xLeftEdge = dstW
        }
        if (lp.endToEnd == 1 && lp.endToStart == 1) {
            xRightEdge = (dstW / 2) - srcW
        } else if (lp.endToStart == 1) {
            xRightEdge = -srcW
        } else if (lp.endToEnd == 1) {
            xRightEdge = dstW - srcW
        }
        if ((lp.startToStart == 1 || lp.startToEnd == 1)
            && (lp.endToStart == 1 || lp.endToEnd == 1)
        ) {
            srcX = dstX + ((xLeftEdge + lp.leftMargin) + (xRightEdge - lp.rightMargin)) / 2
        } else if (lp.startToStart == 1 || lp.startToEnd == 1) {
            srcX = dstX + (xLeftEdge + lp.leftMargin)
        } else if (lp.endToStart == 1 || lp.endToEnd == 1) {
            srcX = dstX + (xRightEdge - lp.rightMargin)
        }

        //计算y坐标
        var (yTopEdge, yBottomEdge) = 0 to 0
        if (lp.topToTop == 1 && lp.topToBottom == 1) {
            yTopEdge = dstH / 2
        } else if (lp.topToTop == 1) {
            yTopEdge = 0
        } else if (lp.topToBottom == 1) {
            yTopEdge = dstH
        }
        if (lp.bottomToTop == 1 && lp.bottomToBottom == 1) {
            yBottomEdge = dstH / 2 - srcH
        } else if (lp.bottomToTop == 1) {
            yBottomEdge = -srcH
        } else if (lp.bottomToBottom == 1) {
            yBottomEdge = dstH - srcH
        }
        if ((lp.topToTop == 1 || lp.topToBottom == 1) &&
            (lp.bottomToTop == 1 || lp.bottomToBottom == 1)
        ) {
            srcY = dstY + ((yTopEdge + lp.topMargin) + (yBottomEdge - lp.bottomMargin)) / 2
        } else if (lp.topToTop == 1 || lp.topToBottom == 1) {
            srcY = dstY + (yTopEdge + lp.topMargin)
        } else if (lp.bottomToTop == 1 || lp.bottomToBottom == 1) {
            srcY = dstY + (yBottomEdge - lp.bottomMargin)
        }

        //popupWindow.showAtLocation(anchor, 0, srcX - parentOffsetX, srcY - parentOffsetY)
        popupWindow.showAtLocation(anchor, 0, srcX, srcY)
    }

    private fun getWidthMeasureSpec(popupWindow: PopupWindow): Int {
        val screenWidth = getDisplayMetrics().widthPixels
        return if (popupWindow.width == WindowManager.LayoutParams.WRAP_CONTENT) {
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST)
        } else if (popupWindow.width == WindowManager.LayoutParams.MATCH_PARENT) {
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY)
        } else if (popupWindow.width >= 0) {
            View.MeasureSpec.makeMeasureSpec(popupWindow.width, View.MeasureSpec.EXACTLY)
        } else 0
    }

    private fun getHeightMeasureSpec(popupWindow: PopupWindow): Int {
        val screenHeight = getDisplayMetrics().heightPixels
        return if (popupWindow.height == WindowManager.LayoutParams.WRAP_CONTENT) {
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        } else if (popupWindow.height == WindowManager.LayoutParams.MATCH_PARENT) {
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.EXACTLY)
        } else if (popupWindow.height >= 0) {
            View.MeasureSpec.makeMeasureSpec(popupWindow.height, View.MeasureSpec.EXACTLY)
        } else 0
    }

    private fun dpToPx(dp: Float): Int {
        val displayMetrics = getDisplayMetrics()
        val result = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)
        return result.roundToInt()
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics
    }
}