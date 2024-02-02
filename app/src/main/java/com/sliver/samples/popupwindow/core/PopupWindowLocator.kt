package com.sliver.samples.popupwindow.core

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.view.WindowManager
import android.widget.PopupWindow
import kotlin.math.roundToInt

class PopupWindowLocator(private val popupWindow: PopupWindow) {
    private val p = Params()
    fun gravity(gravity: Int) = apply { p.gravity = gravity }
    fun startToStartOfAnchor() = apply { p.startToStart = 1 }
    fun startToEndOfAnchor() = apply { p.startToEnd = 1 }
    fun endToStartOfAnchor() = apply { p.endToStart = 1 }
    fun endToEndOfAnchor() = apply { p.endToEnd = 1 }
    fun topToTopOfAnchor() = apply { p.topToTop = 1 }
    fun topToBottomOfAnchor() = apply { p.topToBottom = 1 }
    fun bottomToTopOfAnchor() = apply { p.bottomToTop = 1 }
    fun bottomToBottomOfAnchor() = apply { p.bottomToBottom = 1 }
    fun marginStart(margin: Float) = apply { p.leftMargin = dpToPx(margin) }
    fun marginEnd(margin: Float) = apply { p.rightMargin = dpToPx(margin) }
    fun marginTop(margin: Float) = apply { p.topMargin = dpToPx(margin) }
    fun marginBottom(margin: Float) = apply { p.bottomMargin = dpToPx(margin) }

    fun showAtLocation(anchor: View) {
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        popupWindow.contentView.measure(widthMeasureSpec, heightMeasureSpec)

        val location = IntArray(2)
        anchor.getLocationInWindow(location)
        val anchorOffset = Point(location[0], location[1])
        val anchorSize = Size(anchor.measuredWidth, anchor.measuredHeight)
        val popupSize = Size(
            popupWindow.contentView.measuredWidth,
            popupWindow.contentView.measuredHeight
        )
        val offsetOfStartTop = calculateOffsetForShowAtLocation(
            p, anchorOffset,
            anchorSize, popupSize
        )

        val screenSize = getScreenSize(anchor.context)
        val layoutDirection = anchor.layoutDirection
        val offsetOfGravity = convertOffsetForShowAtLocation(
            offsetOfStartTop,
            screenSize, popupSize,
            p.gravity, layoutDirection
        )

        popupWindow.showAtLocation(anchor, p.gravity, offsetOfGravity.x, offsetOfGravity.y)
        popupWindow.contentView.post { updateForShowAtLocation(anchor) }
    }

    private fun updateForShowAtLocation(anchor: View) {
        val location = IntArray(2)
        anchor.getLocationInWindow(location)
        val anchorOffset = Point(location[0], location[1])
        val anchorSize = Size(anchor.measuredWidth, anchor.measuredHeight)
        val popupSize = Size(
            popupWindow.contentView.measuredWidth,
            popupWindow.contentView.measuredHeight
        )
        val offsetOfStartTop = calculateOffsetForShowAtLocation(
            p, anchorOffset,
            anchorSize, popupSize
        )

        val screenSize = getScreenSize(anchor.context)
        val layoutDirection = anchor.layoutDirection
        val offsetOfGravity = convertOffsetForShowAtLocation(
            offsetOfStartTop,
            screenSize, popupSize,
            p.gravity, layoutDirection
        )

        popupWindow.update(offsetOfGravity.x, offsetOfGravity.y, -1, -1)
        popupWindow.contentView.post { updateForShowAsDropDown(anchor) }
    }

    /**
     * 只适配Gravity.Start、Gravity.End、Gravity.Top、Gravity.Bottom
     */
    fun showAsDropDown(anchor: View) {
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        popupWindow.contentView.measure(widthMeasureSpec, heightMeasureSpec)
        val anchorSize = Size(anchor.measuredWidth, anchor.measuredHeight)
        val popupSize = Size(
            popupWindow.contentView.measuredWidth,
            popupWindow.contentView.measuredHeight
        )

        val offsetOfStart = calculateOffsetShowAsDropDown(
            p, Point(0, 0),
            anchorSize, popupSize
        )
        val offsetOfGravity = convertOffsetShowAsDropDown(
            offsetOfStart,
            anchorSize, popupSize,
            p.gravity, anchor.layoutDirection
        )
        popupWindow.showAsDropDown(
            anchor,
            offsetOfGravity.x, offsetOfGravity.y,
            p.gravity
        )
    }

    private fun updateForShowAsDropDown(anchor: View) {
        val anchorSize = Size(
            anchor.measuredWidth,
            anchor.measuredHeight
        )
        val popupSize = Size(
            popupWindow.contentView.measuredWidth,
            popupWindow.contentView.measuredHeight
        )
        val offsetOfStart = calculateOffsetShowAsDropDown(
            p, Point(0, 0),
            anchorSize, popupSize
        )
        val offsetOfGravity = convertOffsetShowAsDropDown(
            offsetOfStart,
            anchorSize, popupSize,
            p.gravity, anchor.layoutDirection
        )
        popupWindow.update(
            anchor,
            offsetOfGravity.x, offsetOfGravity.y,
            -1, -1
        )
    }

    /**
     * 计算Popup应该显示的位置x、y (Offset)
     */
    private fun calculateOffsetForShowAtLocation(
        lp: Params,
        anchorOffset: Point, anchorSize: Size,
        popupSize: Size,
    ): Point {
        val (dstX, dstY) = anchorOffset.x to anchorOffset.y
        val (dstW, dstH) = anchorSize.width to anchorSize.height
        val (srcW, srcH) = popupSize.width to popupSize.height
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
        return Point(srcX, srcY)
    }

    /**
     * 将基于Start、Top的偏移转为指定Gravity的偏移
     */
    private fun convertOffsetForShowAtLocation(
        offset: Point,
        screenSize: Size, popupSize: Size,
        gravity: Int, layoutDirection: Int,
    ): Point {
        var (srcX, srcY) = offset.x to offset.y
        val (srcW, srcH) = popupSize.width to popupSize.height
        val absGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
        when (absGravity and (Gravity.AXIS_PULL_BEFORE
                or Gravity.AXIS_PULL_AFTER
                shl Gravity.AXIS_X_SHIFT)) {
            0 -> srcX = (srcX - (screenSize.width - srcW) / 2)
            Gravity.AXIS_PULL_BEFORE shl Gravity.AXIS_X_SHIFT -> srcX = (0 + srcX)
            Gravity.AXIS_PULL_AFTER shl Gravity.AXIS_X_SHIFT ->
                srcX = (screenSize.width - srcW - srcX)
        }
        when (absGravity and (Gravity.AXIS_PULL_BEFORE
                or Gravity.AXIS_PULL_AFTER
                shl Gravity.AXIS_Y_SHIFT)) {
            0 -> srcY = (srcY - (screenSize.height - srcH) / 2)
            Gravity.AXIS_PULL_BEFORE shl Gravity.AXIS_Y_SHIFT -> srcY = (0 + srcY)
            Gravity.AXIS_PULL_AFTER shl Gravity.AXIS_Y_SHIFT ->
                srcY = (screenSize.height - srcH - srcY)
        }
        return Point(srcX, srcY)
    }

    private fun calculateOffsetShowAsDropDown(
        lp: Params,
        anchorOffset: Point, anchorSize: Size,
        popupSize: Size,
    ): Point {
        val (dstW, dstH) = anchorSize.width to anchorSize.height
        val (srcW, srcH) = popupSize.width to popupSize.height
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
            srcX = 0 + ((xLeftEdge + lp.leftMargin) + (xRightEdge - lp.rightMargin)) / 2
        } else if (lp.startToStart == 1 || lp.startToEnd == 1) {
            srcX = 0 + (xLeftEdge + lp.leftMargin)
        } else if (lp.endToStart == 1 || lp.endToEnd == 1) {
            srcX = 0 + (xRightEdge - lp.rightMargin)
        }

        //计算y坐标
        var (yTopEdge, yBottomEdge) = 0 to 0
        if (lp.topToTop == 1 && lp.topToBottom == 1) {
            yTopEdge = -dstH / 2
        } else if (lp.topToTop == 1) {
            yTopEdge = -dstH
        } else if (lp.topToBottom == 1) {
            yTopEdge = 0
        }
        if (lp.bottomToTop == 1 && lp.bottomToBottom == 1) {
            yBottomEdge = -srcH - dstH / 2
        } else if (lp.bottomToTop == 1) {
            yBottomEdge = -srcH - dstH
        } else if (lp.bottomToBottom == 1) {
            yBottomEdge = -srcH
        }
        if ((lp.topToTop == 1 || lp.topToBottom == 1) &&
            (lp.bottomToTop == 1 || lp.bottomToBottom == 1)
        ) {
            srcY = 0 + ((yTopEdge + lp.topMargin) + (yBottomEdge - lp.bottomMargin)) / 2
        } else if (lp.topToTop == 1 || lp.topToBottom == 1) {
            srcY = 0 + (yTopEdge + lp.topMargin)
        } else if (lp.bottomToTop == 1 || lp.bottomToBottom == 1) {
            srcY = 0 + (yBottomEdge - lp.bottomMargin)
        }
        return Point(srcX, srcY)
    }

    private fun convertOffsetShowAsDropDown(
        offset: Point,
        anchorSize: Size, popupSize: Size,
        gravity: Int, layoutDirection: Int
    ): Point {
        var (offsetX, offsetY) = offset.x to offset.y
        val (dstW, dstH) = anchorSize.width to anchorSize.height
        val absGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
        when (absGravity and (Gravity.AXIS_PULL_BEFORE
                or Gravity.AXIS_PULL_AFTER
                shl Gravity.AXIS_X_SHIFT)) {
            0, Gravity.AXIS_PULL_BEFORE shl Gravity.AXIS_X_SHIFT -> {
                //Gravity.Start do nothing
            }

            Gravity.AXIS_PULL_AFTER shl Gravity.AXIS_X_SHIFT -> {
                //Gravity.END
                offsetX -= dstW
            }
        }
        return Point(offsetX, offsetY)
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

    private fun getScreenSize(context: Context): Size {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Size(bounds.width(), bounds.height())
        } else {
            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)
            Size(size.x, size.y)
        }
    }

    data class Params(
        var gravity: Int = Gravity.START or Gravity.TOP,
        var startToStart: Int = 0,
        var startToEnd: Int = 0,
        var endToStart: Int = 0,
        var endToEnd: Int = 0,
        var topToTop: Int = 0,
        var topToBottom: Int = 0,
        var bottomToTop: Int = 0,
        var bottomToBottom: Int = 0,
        var leftMargin: Int = 0,
        var topMargin: Int = 0,
        var rightMargin: Int = 0,
        var bottomMargin: Int = 0,
    )
}