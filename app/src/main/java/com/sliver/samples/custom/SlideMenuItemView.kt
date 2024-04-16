package com.sliver.samples.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.OverScroller
import kotlin.math.abs

class SlideMenuItemView : FrameLayout {
    private val binding = createBinding()

    private var pointerId: Int = -1
    private val lastPoint = PointF()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isScrolling = false
    private var scrollDx = 0f
    private val scroller = OverScroller(context)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    fun getLayoutContent(): FrameLayout {
        return binding.layoutContent
    }

    fun getLayoutMenu(): FrameLayout {
        return binding.layoutMenu
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val contentView = binding.layoutContent
        contentView.layout(
            (contentView.left + scrollDx).toInt(), contentView.top,
            (contentView.right + scrollDx).toInt(), contentView.bottom
        )
        val menuView = binding.layoutMenu
        menuView.layout(
            (right + scrollDx).toInt(), menuView.top,
            (right + menuView.width + scrollDx).toInt(), menuView.bottom
        )
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
                    scrollDx += dx
                    lastPoint.set(x, y)
                    requestLayout()
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

                val startX = scrollDx
                val stopX =
                    if (scrollDx > binding.layoutMenu.measuredWidth * -0.5f) 0
                    else -binding.layoutMenu.measuredWidth
                scroller.startScroll(
                    (startX * 1000).toInt(), 0,
                    ((stopX - startX) * 1000).toInt(), 0,
                )
                requestLayout()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollDx = (scroller.currX * 1e-3).toFloat()
            requestLayout()
        }
    }

    private fun createBinding(): SlideMenuItemBinding {
        return SlideMenuItemBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }

    class SlideMenuItemBinding(
        val layoutContent: FrameLayout,
        val layoutMenu: FrameLayout,
    ) {
        companion object {
            fun inflate(
                inflater: LayoutInflater,
                parent: ViewGroup?,
                attachToParent: Boolean
            ): SlideMenuItemBinding {
                val context = inflater.context

                val layoutContentContainer = FrameLayout(inflater.context)
                layoutContentContainer.layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                )

                val layoutMenuContainer = FrameLayout(context)
                layoutMenuContainer.layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT,
                )

                if (attachToParent) {
                    parent?.addView(layoutContentContainer)
                    parent?.addView(layoutMenuContainer)
                }
                return SlideMenuItemBinding(
                    layoutContentContainer,
                    layoutMenuContainer
                )
            }
        }
    }
}