package com.yashoid.sequencelayout.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.yashoid.sequencelayout.SequenceLayout;

public class ResizeableParent extends SequenceLayout {

    private int mMinimumWidth = -1;
    private int mMinimumHeight = -1;

    public ResizeableParent(Context context) {
        super(context);
    }

    public ResizeableParent(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizeableParent(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mMinimumWidth == -1) {
            measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            mMinimumWidth = getMeasuredWidth();
            mMinimumHeight = getMeasuredHeight();
        }

        int width = Math.max(mMinimumWidth, (int) event.getX());
        int height = Math.max(mMinimumHeight, (int) event.getY());

        LayoutParams params = getLayoutParams();

        params.width = width;
        params.height = height;

        requestLayout();

        return true;
    }

}
