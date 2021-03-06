package com.yashoid.sequencelayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class SwipeAnimator implements View.OnTouchListener, GestureDetector.OnGestureListener,
        ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    public static final int STATE_IDLE = 0;
    public static final int STATE_SCOlLING = 1;
    public static final int STATE_FLING = 2;

    private static final long FLING_DURATION = 1_000;
    private static final float MINIMUM_FLING_SPEED = 1_000;

    private static final Interpolator INTERPOLATOR = new LinearInterpolator();

    public interface OnStateChangedListener {

        void onStateChanged(int state);

    }

    public interface OnProgressChangedListener {

        void onProgressChanged(float ratio);

    }

    private final float mMinimumFlingSpeed;

    private GestureDetector mGestureDetector;

    private OnStateChangedListener mOnStateChangedListener = null;
    private OnProgressChangedListener mOnProgressChangedListener = null;

    private Rect mActiveArea = null;

    private float mActiveDirectionX = 1;
    private float mActiveDirectionY = 0;

    private float mFullDistance = 0;

    private SequenceLayoutAnimator mAnimator = null;

    private int mState = STATE_IDLE;

    private float mTraveledDistance = 0;

    private ValueAnimator mFlingAnimator = null;
    private boolean mEnding;

    public SwipeAnimator(Context context) {
        mMinimumFlingSpeed = MINIMUM_FLING_SPEED * context.getResources().getDisplayMetrics().density;

        mGestureDetector = new GestureDetector(context, this);
    }

    public void setActiveArea(int left, int top, int right, int bottom) {
        if (mActiveArea == null) {
            mActiveArea = new Rect();
        }

        mActiveArea.set(left, top, right, bottom);
    }

    public void setActiveArea(Rect rect) {
        if (rect == null) {
            mActiveArea = null;
        }
        else {
            mActiveArea.set(rect);
        }
    }

    public void setActiveDirection(float direction) {
        mActiveDirectionX = (float) Math.cos(Math.toRadians(direction));
        mActiveDirectionY = (float) Math.sin(Math.toRadians(direction));
    }

    public void setFullDistance(float distance) {
        mFullDistance = distance;
    }

    public void setAnimator(SequenceLayoutAnimator animator) {
        mAnimator = animator;
    }

    public int getState() {
        return mState;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public void setOnProgressChangedListener(OnProgressChangedListener listener) {
        mOnProgressChangedListener = listener;
    }

    private void setState(int state) {
        mState = state;

        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onStateChanged(mState);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean result = mGestureDetector.onTouchEvent(event);

        if (event.getAction() != MotionEvent.ACTION_DOWN && event.getAction() != MotionEvent.ACTION_MOVE) {
            if (mState == STATE_SCOlLING) {
                if (mTraveledDistance == mFullDistance) {
                    mAnimator.end();

                    setState(STATE_IDLE);
                }
                else if (mTraveledDistance == 0) {
                    mAnimator.cancel();

                    setState(STATE_IDLE);
                }
                else {
                    float progress = mAnimator.getInterpolator().getInterpolation(mAnimator.getPassedTimeFraction());

                    fling(progress > 0.5f);
                }
            }
        }

        return result;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mAnimator == null || mAnimator.isRunning() || mFlingAnimator != null) {
            return false;
        }

        boolean accepted = mActiveArea == null || mActiveArea.contains((int) e.getX(), (int) e.getY());

        if (accepted) {
            mTraveledDistance = 0;
        }

        return accepted;
    }

    @Override
    public void onShowPress(MotionEvent e) { }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) { }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mState != STATE_SCOlLING) {
            setState(STATE_SCOlLING);
        }

        // |A||B|cos = AxBx + AyBy
        float distance = - mActiveDirectionX * distanceX - mActiveDirectionY * distanceY;

        mTraveledDistance += distance;

        mTraveledDistance = Math.max(0, Math.min(mFullDistance, mTraveledDistance));

        setAnimatorPassedTimeFraction(mTraveledDistance / mFullDistance);

        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float direction = mActiveDirectionX * velocityX + mActiveDirectionY * velocityY;

        if (Math.abs(direction) > mMinimumFlingSpeed) {
            boolean open = direction > 0;

            if ((open && mTraveledDistance == mFullDistance) || (!open && mTraveledDistance == 0)) {
                return false;
            }

            fling(open);
            return true;
        }

        return false;
    }

    private void fling(boolean open) {
        setState(STATE_FLING);

        float target;

        if (open) {
            target = 1;
            mEnding = true;
        }
        else {
            target = 0;
            mEnding = false;
        }

        float progress = mTraveledDistance / mFullDistance;

        mFlingAnimator = ValueAnimator.ofFloat(progress, target);
        mFlingAnimator.addUpdateListener(this);
        mFlingAnimator.addListener(this);
        mFlingAnimator.setDuration((long) ((1 - progress) * FLING_DURATION));
        mFlingAnimator.setInterpolator(INTERPOLATOR);
        mFlingAnimator.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        setAnimatorPassedTimeFraction((Float) animation.getAnimatedValue());
    }

    private void setAnimatorPassedTimeFraction(float fraction) {
        mAnimator.setPassedTimeFraction(fraction);

        if (mOnProgressChangedListener != null) {
            mOnProgressChangedListener.onProgressChanged(fraction);
        }
    }

    @Override
    public void onAnimationStart(Animator animation) { }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (mEnding) {
            mAnimator.end();
        }
        else {
            mAnimator.cancel();
        }

        mFlingAnimator = null;

        setState(STATE_IDLE);
    }

    @Override
    public void onAnimationCancel(Animator animation) { }

    @Override
    public void onAnimationRepeat(Animator animation) { }

}
