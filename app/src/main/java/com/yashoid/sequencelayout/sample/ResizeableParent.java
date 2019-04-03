package com.yashoid.sequencelayout.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.yashoid.sequencelayout.SequenceLayout;

public class ResizeableParent extends SequenceLayout {

    private int mMinimumWidth = -1;
    private int mMinimumHeight = -1;

    private float mTouchX = 0;
    private float mTouchY;

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

        if (event.getAction() != MotionEvent.ACTION_DOWN && event.getAction() != MotionEvent.ACTION_MOVE) {
            mTouchX = 0;
            mTouchY = 0;

            return true;
        }

        if (mTouchX == 0) {
            mTouchX = event.getX();
            mTouchY = event.getY();

            return true;
        }

        int dx = (int) (event.getX() - mTouchX);
        int dy = (int) (event.getY() - mTouchY);

        mTouchX = event.getX();
        mTouchY = event.getY();

        int width = Math.max(mMinimumWidth, getWidth() + dx);
        int height = Math.max(mMinimumHeight, getHeight() + dy);

        LayoutParams params = getLayoutParams();

        params.width = width;
        params.height = height;

        requestLayout();

        return true;
    }

}
