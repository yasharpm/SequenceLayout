package com.yashoid.sequencelayout;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SequenceLayoutAnimator extends Animator implements Environment, IPageResolver,
        Choreographer.FrameCallback {

    private static final long DEFAULT_DURATION = 200;
    private static final TimeInterpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private SequenceLayout mParent;

    private IPageResolver mStartResolver;
    private IPageResolver mEndResolver;
    private List<View> mAddingViews;
    private List<View> mRemovingViews;

    private Choreographer mChoreographer;

    private long mStartDelay = 0;
    private long mDuration = DEFAULT_DURATION;
    private TimeInterpolator mInterpolator = DEFAULT_INTERPOLATOR;

    private Handler mNotifyHandler;

    private boolean mHasPendingFrameCall = false;

    private long mLastFrameTimeNanos = -1;
    private long mPassedNanos = 0;

    private float mPassedTimeFraction = 0;
    private float mApplicablePassedTimeFraction = 0;

    private boolean mStarted = false;
    private boolean mRunning = false;
    private boolean mCancelled = false;

    private Map<String, Rect> mStartViewPositions = new Hashtable<>();
    private Map<String, Rect> mEndViewPositions = new Hashtable<>();
    private Map<String, Rect> mAcceptingViewPositions = null;

    SequenceLayoutAnimator(SequenceLayout parent,
                           IPageResolver startResolver, IPageResolver endResolver,
                           List<View> addingViews, List<View> removingViews) {
        mParent = parent;

        mStartResolver = startResolver;
        mEndResolver = endResolver;

        mAddingViews = addingViews;
        mRemovingViews = removingViews;

        mChoreographer = Choreographer.getInstance();
    }

    @Override
    public long getStartDelay() {
        return mStartDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        mStartDelay = startDelay;
    }

    @Override
    public SequenceLayoutAnimator setDuration(long duration) {
        mDuration = duration;
        return this;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        if (value != null) {
            mInterpolator = value;
        }
        else {
            mInterpolator = new LinearInterpolator();
        }
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    public float getPassedTimeFraction() {
        return mPassedTimeFraction;
    }

    /**
     * This method can only be used if the animation start call is not called yet.
     * Interpolation is applied to fraction.
     * You must call either end() or cancel() methods after the animator is no longer needed.
     * @param fraction
     */
    public void setPassedTimeFraction(float fraction) {
        fraction = Math.max(0, Math.min(1, fraction));

        if (mStarted) {
            throw new UnsupportedOperationException("Can not modify the animation after it has started.");
        }

        if (!mRunning) {
            prepareToRun();

            mNotifyHandler = new Handler(Looper.myLooper());
        }

        mPassedTimeFraction = fraction;
        mApplicablePassedTimeFraction = mInterpolator.getInterpolation(mPassedTimeFraction);

        positionViews();
    }

    @Override
    public void start() {
        if (mStarted) {
            return;
        }

        mStarted = true;

        mNotifyHandler = new Handler(Looper.myLooper());

        mLastFrameTimeNanos = -1;
        mPassedNanos = 0;
        mPassedTimeFraction = 0;
        mApplicablePassedTimeFraction = 0;

        if (mStartDelay == 0) {
            mChoreographer.postFrameCallback(this);
        }
        else {
            mChoreographer.postFrameCallbackDelayed(this, mStartDelay);
        }

        mHasPendingFrameCall = true;

        notifyAnimationStarted();
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public void cancel() {
        if (mHasPendingFrameCall) {
            mCancelled = true;
        }

        attachStartResolverToLayout();

        mRunning = false;
        mStarted = false;

        notifyAnimationCancelled();
    }

    @Override
    public void end() {
        mPassedNanos = mDuration * 1_000_000;

        if (!mHasPendingFrameCall) {
            mChoreographer.postFrameCallback(this);

            mHasPendingFrameCall = true;
        }
    }

    @Override
    public void resume() {
        if (isPaused()) {
            if (mHasPendingFrameCall) {
                return;
            }

            mChoreographer.postFrameCallback(this);

            mHasPendingFrameCall = true;
        }

        super.resume();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (mCancelled) {
            mHasPendingFrameCall = false;
            mCancelled = false;
            return;
        }

        if (isPaused()) {
            // If there are any flickers during rapid resume/pause calls, mPassedNanos must be updated at pause() call.

            mLastFrameTimeNanos = -1;

            mHasPendingFrameCall = false;
            return;
        }

        if (!mRunning) {
            prepareToRun();
        }

        if (mLastFrameTimeNanos == -1) {
            mLastFrameTimeNanos = frameTimeNanos;
        }

        mPassedNanos += frameTimeNanos - mLastFrameTimeNanos;

        mLastFrameTimeNanos = frameTimeNanos;

        long passedDuration = mPassedNanos / 1_000_000;

        mPassedTimeFraction = (float) passedDuration / mDuration;

        if (mPassedTimeFraction >= 1) {
            mRunning = false;
            mStarted = false;

            attachEndResolverToLayout();

            notifyAnimationEnded();

            return;
        }

        mApplicablePassedTimeFraction = mInterpolator.getInterpolation(mPassedTimeFraction);

        positionViews();

        mChoreographer.postFrameCallback(this);

        mHasPendingFrameCall = true;
    }

    private void prepareToRun() {
        ensureViewsAdded(mAddingViews);
        ensureViewsAdded(mRemovingViews);

        setSelfAsEnvironmentForResolvers();
        resolveViewPositions();

        attachSelfToLayout();

        mRunning = true;
    }

    private void setSelfAsEnvironmentForResolvers() {
        mStartResolver.setEnvironment(this);
        mEndResolver.setEnvironment(this);
    }

    private void resolveViewPositions() {
        int layoutWidth = mParent.getWidth();
        int layoutHeight = mParent.getHeight();

        startResolution(layoutWidth, layoutHeight, false, false);
    }

    private void attachSelfToLayout() {
        mParent.setPageResolver(this);
    }

    private void attachStartResolverToLayout() {
        ensureViewsAdded(mRemovingViews);
        ensureViewsRemoved(mAddingViews);

        mStartResolver.setEnvironment(mParent);
        mParent.setPageResolver(mStartResolver);

        if (mRemovingViews != null) {
            for (View view: mRemovingViews) {
                view.setAlpha(1);
            }
        }
    }

    private void attachEndResolverToLayout() {
        ensureViewsAdded(mAddingViews);
        ensureViewsRemoved(mRemovingViews);

        mEndResolver.setEnvironment(mParent);
        mParent.setPageResolver(mEndResolver);

        if (mAddingViews != null) {
            for (View view: mAddingViews) {
                view.setAlpha(1);
            }
        }
    }

    private void ensureViewsAdded(List<View> views) {
        if (views == null) {
            return;
        }

        for (View view: views) {
            if (view.getParent() == null) {
                mParent.addView(view);
            }
        }
    }

    private void ensureViewsRemoved(List<View> views) {
        if (views == null) {
            return;
        }

        for (View view: views) {
            mParent.removeView(view);
        }
    }

    private void notifyAnimationStarted() {
        mNotifyHandler.post(mAnimationStartedNotifier);
    }

    private Runnable mAnimationStartedNotifier = new Runnable() {

        @Override
        public void run() {
            List<AnimatorListener> listeners = getListenersCopy();

            for (AnimatorListener listener: listeners) {
                listener.onAnimationStart(SequenceLayoutAnimator.this);
            }
        }

    };

    private void notifyAnimationCancelled() {
        mNotifyHandler.post(mAnimationCancelledNotifier);
    }

    private Runnable mAnimationCancelledNotifier = new Runnable() {

        @Override
        public void run() {
            List<AnimatorListener> listeners = getListenersCopy();

            for (AnimatorListener listener: listeners) {
                listener.onAnimationCancel(SequenceLayoutAnimator.this);
            }
        }

    };

    private void notifyAnimationEnded() {
        mNotifyHandler.post(mAnimationEndedNotifier);
    }

    private Runnable mAnimationEndedNotifier = new Runnable() {

        @Override
        public void run() {
            List<AnimatorListener> listeners = getListenersCopy();

            for (AnimatorListener listener: listeners) {
                listener.onAnimationEnd(SequenceLayoutAnimator.this);
            }
        }

    };

    private List<AnimatorListener> getListenersCopy() {
        List<AnimatorListener> listeners = getListeners();

        if (listeners == null) {
            return Collections.EMPTY_LIST;
        }
        else {
            return new ArrayList<>(listeners);
        }
    }

    @Override
    public float getScreenDensity() {
        return mParent.getScreenDensity();
    }

    @Override
    public float getScreenScaledDensity() {
        return mParent.getScreenScaledDensity();
    }

    @Override
    public float getPageWidth() {
        return mParent.getPageWidth();
    }

    @Override
    public float getPageHeight() {
        return mParent.getPageHeight();
    }

    @Override
    public boolean readSizeInfo(String src, SizeInfo dst) {
        return mParent.readSizeInfo(src, dst);
    }

    @Override
    public int getElementCount() {
        return mParent.getElementCount();
    }

    @Override
    public boolean isElementVisible(String id) {
        return mParent.isElementVisible(id);
    }

    @Override
    public int measureElementWidth(String id, int height, int min, int max) {
        return mParent.measureElementWidth(id, height, min, max);
    }

    @Override
    public int measureElementHeight(String id, int width, int min, int max) {
        return mParent.measureElementHeight(id, width, min, max);
    }

    @Override
    public void measureElement(String id, int minWidth, int maxWidth, int minHeight, int maxHeight, int[] measuredSize) {
        mParent.measureElement(id, minWidth, maxWidth, minHeight, maxHeight, measuredSize);
    }

    @Override
    public void positionElement(String id, int left, int top, int right, int bottom) {
        Rect rect = mAcceptingViewPositions.get(id);

        if (rect == null) {
            rect = new Rect();

            mAcceptingViewPositions.put(id, rect);
        }

        rect.set(left, top, right, bottom);
    }

    @Override
    public void setEnvironment(Environment environment) {
        throw new UnsupportedOperationException("Can not modify the layout while it is in animation.");
    }

    @Override
    public void onSequenceAdded(Sequence sequence) {
        throw new UnsupportedOperationException("Can not modify the layout while it is in animation.");
    }

    @Override
    public void onSequenceRemoved(Sequence sequence) {
        throw new UnsupportedOperationException("Can not modify the layout while it is in animation.");
    }

    @Override
    public Sequence findSequenceById(String id) {
        Sequence sequence = mStartResolver.findSequenceById(id);

        if (sequence == null) {
            sequence = mEndResolver.findSequenceById(id);
        }

        return sequence;
    }

    @Override
    public void startResolution(int pageWidth, int pageHeight, boolean horizontalWrapping, boolean verticalWrapping) {
        mAcceptingViewPositions = mStartViewPositions;
        mStartResolver.startResolution(pageWidth, pageHeight, horizontalWrapping, verticalWrapping);
        mStartResolver.positionViews();

        mAcceptingViewPositions = mEndViewPositions;
        mEndResolver.startResolution(pageWidth, pageHeight, horizontalWrapping, verticalWrapping);
        mEndResolver.positionViews();
    }

    @Override
    public int getResolvedWidth() {
        float width = applyPassedTime(mStartResolver.getResolvedWidth(), mEndResolver.getResolvedWidth());

        return width < 0 ? 0 : (int) width;
    }

    @Override
    public int getResolvedHeight() {
        float height = applyPassedTime(mStartResolver.getResolvedHeight(), mEndResolver.getResolvedHeight());

        return height < 0 ? 0 : (int) height;
    }

    @Override
    public void positionViews() {
        for (Map.Entry<String, Rect> entry: mStartViewPositions.entrySet()) {
            String id = entry.getKey();

            Rect startRect = entry.getValue();
            Rect endRect = mEndViewPositions.get(id);

            if (endRect == null) {
                continue;
            }

            float left = applyPassedTime(startRect.left, endRect.left);
            float top = applyPassedTime(startRect.top, endRect.top);
            float right = applyPassedTime(startRect.right, endRect.right);
            float bottom = applyPassedTime(startRect.bottom, endRect.bottom);

            mParent.positionElement(id, (int) left, (int) top, (int) right, (int) bottom);

            View view = mParent.findViewById(id);

            if (view != null) {
                float fraction = Math.max(0, Math.min(mApplicablePassedTimeFraction, 1));

                if (mAddingViews.contains(view)) {
                    view.setAlpha(fraction);
                }

                if (mRemovingViews.contains(view)) {
                    view.setAlpha(1 - fraction);
                }
            }
        }
    }

    private float applyPassedTime(float start, float end) {
        return (1 - mApplicablePassedTimeFraction) * start + mApplicablePassedTimeFraction * end;
    }

}
