package com.sliver.samples.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 自定义带箭头的TextView
 * 在原有Padding的基础上，添加shadowSize的内边距，用于腾出空间显示阴影
 * 在原有Padding的基础上，添加trHeight的内边距，用于显示箭头
 * 使用时需要注意实际Padding的计算
 */
public class CustomBubbleTextView extends AppCompatTextView {
    public CustomBubbleTextView(@NonNull Context context) {
        super(context);
        init();
    }

    public CustomBubbleTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomBubbleTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public static final int DIRECTION_LEFT = 0;
    public static final int DIRECTION_TOP = 1;
    public static final int DIRECTION_RIGHT = 2;
    public static final int DIRECTION_BOTTOM = 3;

    @IntDef({DIRECTION_LEFT, DIRECTION_TOP, DIRECTION_RIGHT, DIRECTION_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    @interface Direction {

    }

    //三角形设置
    @Direction
    protected int trDirection = DIRECTION_RIGHT;    //箭头方向
    protected float trOffset = 20;    //箭头指向偏移
    protected float trHeight = 6;

    //气泡设置
    protected float conorRadius = 4;
    protected float shadowSize = 2;
    protected final int bubbleColor = 0xFF1F85FF;
    protected final int shadowColor = 0xFF333333;

    //缓存
    private Paint paint;
    private RectF rect;
    private Path path;

    private void init() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        trOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, trOffset, displayMetrics);
        conorRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, conorRadius, displayMetrics);
        shadowSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, shadowSize, displayMetrics);
        trHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, trHeight, displayMetrics);

        rect = new RectF();
        paint = new Paint();
        path = new Path();
        paint.setAntiAlias(true);
        paint.setColor(bubbleColor);
        paint.setShadowLayer(shadowSize, 0, 0, shadowColor);

        //如果未设置Padding则默认给阴影半径
        //根据方向给要加箭头的一方加内边距
        Rect originPadding = new Rect(getPaddingStart(), getPaddingTop(), getPaddingEnd(), getPaddingBottom());
        Rect paddingWidthShadowSize = new Rect((int) shadowSize, (int) shadowSize, (int) shadowSize, (int) shadowSize);
        Rect paddingWithTrHeight = new Rect();
        if (trDirection == DIRECTION_LEFT) {
            paddingWithTrHeight.set((int) trHeight, 0, 0, 0);
        } else if (trDirection == DIRECTION_TOP) {
            paddingWithTrHeight.set(0, (int) trHeight, 0, 0);
        } else if (trDirection == DIRECTION_RIGHT) {
            paddingWithTrHeight.set(0, 0, (int) trHeight, 0);
        } else if (trDirection == DIRECTION_BOTTOM) {
            paddingWithTrHeight.set(0, 0, 0, (int) trHeight);
        }
        setPaddingRelative(originPadding.left + paddingWidthShadowSize.left + paddingWithTrHeight.left,
                originPadding.top + paddingWidthShadowSize.top + paddingWithTrHeight.top,
                originPadding.right + paddingWidthShadowSize.right + paddingWithTrHeight.right,
                originPadding.bottom + paddingWidthShadowSize.bottom + paddingWithTrHeight.bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (trDirection == DIRECTION_LEFT) {
            rect.set(trHeight + shadowSize, shadowSize, w - shadowSize, h - shadowSize);
        } else if (trDirection == DIRECTION_TOP) {
            rect.set(+shadowSize, trHeight + shadowSize, w - shadowSize, h - shadowSize);
        } else if (trDirection == DIRECTION_RIGHT) {
            rect.set(+shadowSize, +shadowSize, w - trHeight - shadowSize, h - shadowSize);
        } else if (trDirection == DIRECTION_BOTTOM) {
            rect.set(+shadowSize, +shadowSize, w - shadowSize, h - trHeight - shadowSize);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (trDirection == DIRECTION_LEFT) {
            //三角形的高
            //三角形底部的半边宽
            float trBottomHalfLength = (float) (Math.tan(Math.toRadians(30)) * trHeight);
            path.reset();
            path.addRoundRect(rect, conorRadius, conorRadius, Path.Direction.CCW);
            path.moveTo(+shadowSize, trOffset);
            path.rLineTo(trHeight, -trBottomHalfLength);
            path.rLineTo(0, 2 * trBottomHalfLength);
            path.close();
        } else if (trDirection == DIRECTION_TOP) {
            float trBottomHalfLength = (float) (Math.tan(Math.toRadians(30)) * trHeight);
            path.reset();
            path.addRoundRect(rect, conorRadius, conorRadius, Path.Direction.CCW);
            path.moveTo(trOffset, +shadowSize);
            path.rLineTo(-trBottomHalfLength, trHeight);
            path.rLineTo(2 * trBottomHalfLength, 0);
            path.close();
        } else if (trDirection == DIRECTION_RIGHT) {
            float trBottomHalfLength = (float) (Math.tan(Math.toRadians(30)) * trHeight);
            path.reset();
            path.addRoundRect(rect, conorRadius, conorRadius, Path.Direction.CCW);
            path.moveTo(getMeasuredWidth() - shadowSize, trOffset);
            path.rLineTo(-trHeight, -trBottomHalfLength);
            path.rLineTo(0, 2 * trBottomHalfLength);
            path.close();
        } else if (trDirection == DIRECTION_BOTTOM) {
            float trBottomHalfLength = (float) (Math.tan(Math.toRadians(30)) * trHeight);
            path.reset();
            path.addRoundRect(rect, conorRadius, conorRadius, Path.Direction.CCW);
            path.moveTo(trOffset, getMeasuredHeight() - shadowSize);
            path.rLineTo(-trBottomHalfLength, -trHeight);
            path.rLineTo(2 * trBottomHalfLength, 0);
            path.close();
        }
        canvas.drawPath(path, paint);
        super.onDraw(canvas);
    }
}
