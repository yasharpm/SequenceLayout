package com.yashoid.sequencelayout;

import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SequenceLayoutAnimatorCreator {

    private SequenceLayout mParent;

    private PageResolver mStartResolver;
    private PageResolver mEndResolver;

    private List<View> mAddingViews = new ArrayList<>();
    private List<View> mRemovingViews = new ArrayList<>();

    SequenceLayoutAnimatorCreator(SequenceLayout parent) {
        mParent = parent;

        IPageResolver pageResolver = parent.getPageResolver();

        if (!(pageResolver instanceof PageResolver)) {
            throw new UnsupportedOperationException("Layout is already in animation.");
        }

        mStartResolver = (PageResolver) pageResolver;
        mEndResolver = mStartResolver.makeClone();
    }

    public SequenceLayoutAnimatorCreator addSequence(Sequence sequence) {
        mEndResolver.onSequenceAdded(sequence);

        return this;
    }

    public SequenceLayoutAnimatorCreator addSequences(List<Sequence> sequences) {
        for (Sequence sequence: sequences) {
            mEndResolver.onSequenceAdded(sequence);
        }

        return this;
    }

    public SequenceLayoutAnimatorCreator removeSequence(Sequence sequence) {
        mEndResolver.onSequenceRemoved(sequence);

        return this;
    }

    public SequenceLayoutAnimatorCreator removeSequences(List<Sequence> sequences) {
        for (Sequence sequence: sequences) {
            mEndResolver.onSequenceRemoved(sequence);
        }

        return this;
    }

    public SequenceLayoutAnimatorCreator addView(View view) {
        mAddingViews.add(view);

        return this;
    }

    public SequenceLayoutAnimatorCreator addViews(List<View> views) {
        mAddingViews.addAll(views);

        return this;
    }

    public SequenceLayoutAnimatorCreator addViews(View... views) {
        mAddingViews.addAll(Arrays.asList(views));

        return this;
    }

    public SequenceLayoutAnimatorCreator removeView(View view) {
        mRemovingViews.add(view);

        return this;
    }

    public SequenceLayoutAnimatorCreator removeViews(List<View> views) {
        mRemovingViews.addAll(views);

        return this;
    }

    public SequenceLayoutAnimatorCreator removeView(View... views) {
        mRemovingViews.addAll(Arrays.asList(views));

        return this;
    }

    public SequenceLayoutAnimator create() {
        return new SequenceLayoutAnimator(mParent, mStartResolver, mEndResolver, mAddingViews, mRemovingViews);
    }

}
