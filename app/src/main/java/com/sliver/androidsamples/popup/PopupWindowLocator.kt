package com.sliver.androidsamples.popup

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.roundToInt

class PopupWindowLocator(private val popupWindow: PopupWindow) {
    private val lp = ConstraintLayout.LayoutParams(0, 0)
    fun startToStartOfAnchor() = apply { lp.startToStart = 1 }
    fun startToEndOfAnchor() = apply { lp.startToEnd = 1 }
    fun endToStartOfAnchor() = apply { lp.endToStart = 1 }
    fun endToEndOfAnchor() = apply { lp.endToEnd = 1 }
    fun topToTopOfAnchor() = apply { lp.topToTop = 1 }
    fun topToBottomOfAnchor() = apply { lp.topToBottom = 1 }
    fun bottomToTopOfAnchor() = apply { lp.bottomToTop = 1 }
    fun bottomToBottomOfAnchor() = apply { lp.bottomToBottom = 1 }
    fun marginStart(margin: Float) = apply {
        lp.setMargins(dpToPx(margin), lp.topMargin, lp.rightMargin, lp.bottomMargin)
    }

    fun marginEnd(margin: Float) = apply {
        lp.setMargins(lp.leftMargin, lp.topMargin, dpToPx(margin), lp.bottomMargin)
    }

    fun marginTop(margin: Float) = apply {
        lp.setMargins(lp.leftMargin, dpToPx(margin), lp.rightMargin, lp.bottomMargin)
    }

    fun marginBottom(margin: Float) = apply {
        lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, dpToPx(margin))
    }

    fun show(anchor: View) {
        popupWindow.showAtLocation(anchor, 0, 0, 0)
        popupWindow.contentView.post {
            updateLocation(anchor)
        }
    }

    fun updateLocation(anchor: View) {
        //val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        //val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        //popupWindow.contentView.measure(widthMeasureSpec, heightMeasureSpec)

        val location = IntArray(2)
        anchor.getLocationInWindow(location)
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
        popupWindow.update(anchor, srcX, srcY)
    }

    private fun dpToPx(dp: Float): Int {
        val displayMetrics = getDisplayMetrics()
        val result = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)
        return result.roundToInt()
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val context = popupWindow.contentView.context
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics
    }
}