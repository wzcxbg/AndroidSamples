package com.sliver.samples.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.OverScroller
import android.widget.TextView
import kotlin.math.abs

class SlideMenuItemView : FrameLayout {
    private val scroller = OverScroller(context)
    private val layoutMenuContainer = FrameLayout(context)

    //tmp
    private var pointerId: Int = -1
    private val lastPoint = PointF()
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private var isScrolling = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        val layoutContentContainer = FrameLayout(context)
        layoutContentContainer.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )
        layoutContentContainer.background = ColorDrawable(Color.CYAN)
        val textView = TextView(context)
        textView.text = "Text"
        textView.height = 200
        layoutContentContainer.addView(textView)
        addView(layoutContentContainer)

        layoutMenuContainer.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        ).also {
            it.gravity = Gravity.END
        }
        layoutMenuContainer.background = ColorDrawable(Color.RED)
        val tvDelete = TextView(context)
        tvDelete.text = "Delete"
        layoutMenuContainer.addView(tvDelete)
        addView(layoutMenuContainer)

        layoutMenuContainer.post {
            layoutMenuContainer.x = layoutContentContainer.measuredWidth.toFloat()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                pointerId = event.getPointerId(event.actionIndex)
                val x = event.getX(event.findPointerIndex(pointerId))
                val y = event.getY(event.findPointerIndex(pointerId))
                lastPoint.set(x, y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.getX(event.findPointerIndex(pointerId))
                val y = event.getY(event.findPointerIndex(pointerId))
                val dx = x - lastPoint.x
                val dy = y - lastPoint.y
                if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    isScrolling = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isScrolling) {
                    layoutMenuContainer.x += dx
                    lastPoint.set(x, y)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == pointerId) {
                    val newPointerIndex = (0 until event.pointerCount).reversed()
                        .first { event.actionIndex != it }
                    pointerId = event.getPointerId(newPointerIndex)
                    val x = event.getX(event.findPointerIndex(pointerId))
                    val y = event.getY(event.findPointerIndex(pointerId))
                    lastPoint.set(x, y)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScrolling = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }
}