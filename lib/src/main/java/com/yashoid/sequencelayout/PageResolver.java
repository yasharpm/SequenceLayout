package com.yashoid.sequencelayout;

import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

class PageResolver implements SizeResolverHost {

    private final SequenceLayout mView;
    private final float mPgSize;

    private List<Sequence> mSequences = new ArrayList<>();

    private int mResolvingWidth;
    private int mResolvingHeight;

    private List<Sequence> mUnresolvedSequences = new ArrayList<>();

    private List<Span> mUnresolvedSpans = new ArrayList<>();
    private List<Span> mResolvedSpans = new ArrayList<>();

    private int mResolvedWidth = -1;
    private int mResolvedHeight = -1;

    PageResolver(SequenceLayout view) {
        mView = view;

        mPgSize = mView.getPgSize();
    }

    @Override
    public float getPgSize() {
        return mPgSize;
    }

    @Override
    public float getPgUnitSize() {
        if (mPgSize == 0) {
            return 1;
        }

        return mResolvingWidth / mPgSize;
    }

    @Override
    public float getScreenDensity() {
        return mView.getResources().getDisplayMetrics().density;
    }

    void onSequenceAdded(Sequence sequence) {
        mSequences.add(sequence);

        mUnresolvedSpans.addAll(sequence.getSpans());
    }

    void onSequenceRemoved(Sequence sequence) {
        mSequences.remove(sequence);

        mResolvedSpans.removeAll(sequence.getSpans());
        mUnresolvedSpans.removeAll(sequence.getSpans());
    }

    Sequence findSequenceById(String id) {
        for (Sequence sequence: mSequences) {
            if (TextUtils.equals(id, sequence.getId())) {
                return sequence;
            }
        }

        return null;
    }

    void startResolution(int pageWidth, int pageHeight, boolean horizontalWrapping, boolean verticalWrapping) {
        mResolvingWidth = pageWidth;
        mResolvingHeight = pageHeight;

        mUnresolvedSpans.addAll(mResolvedSpans);
        mResolvedSpans.clear();

        mUnresolvedSequences.clear();
        mUnresolvedSequences.addAll(mSequences);

        int maxWidth = -1;
        int maxHeight = -1;

        while (!mUnresolvedSequences.isEmpty()) {
            ListIterator<Sequence> iterator = mUnresolvedSequences.listIterator();

            while (iterator.hasNext()) {
                Sequence sequence = iterator.next();

                int size = sequence.resolve(this, sequence.isHorizontal() ? horizontalWrapping : verticalWrapping);

                if (size != -1) {
                    if (sequence.isHorizontal()) {
                        maxWidth = Math.max(maxWidth, size);
                    }
                    else {
                        maxHeight = Math.max(maxHeight, size);
                    }

                    iterator.remove();
                }
            }
        }

        mResolvedWidth = maxWidth;
        mResolvedHeight = maxHeight;
    }

    void layoutViews() { // TODO Improve for real!
        for (Span unit: mResolvedSpans) {
            View child = mView.findViewById(unit.elementId);

            if (unit.isHorizontal) {
                child.layout(unit.getStart(), child.getTop(), unit.getEnd(), child.getBottom());
            }
            else {
                child.layout(child.getLeft(), unit.getStart(), child.getRight(), unit.getEnd());
            }

            if (child.getWidth() > 0 && child.getHeight() > 0) {
                child.measure(View.MeasureSpec.makeMeasureSpec(child.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(child.getHeight(), View.MeasureSpec.EXACTLY));
            }
        }
    }

    int getResolvedWidth() {
        return mResolvedWidth;
    }

    int getResolvedHeight() {
        return mResolvedHeight;
    }

    @Override
    public View findViewById(int id) {
        return mView.findViewById(id);
    }

    @Override
    public List<Span> getResolvedSpans() {
        return mResolvedSpans;
    }

    @Override
    public List<Span> getUnresolvedSpans() {
        return mUnresolvedSpans;
    }

    @Override
    public int getResolvingWidth() {
        return mResolvingWidth;
    }

    @Override
    public int getResolvingHeight() {
        return mResolvingHeight;
    }

}
