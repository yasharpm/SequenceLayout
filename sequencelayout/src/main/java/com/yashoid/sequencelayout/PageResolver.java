package com.yashoid.sequencelayout;

import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

class PageResolver implements SizeResolverHost {

    private static final int MAXIMUM_EXPECTED_NUMBER_OF_CHILDREN = 50;

    private final SequenceLayout mView;
    private float mPgSize;

    private List<Sequence> mSequences = new ArrayList<>();

    private int mResolvingWidth;
    private int mResolvingHeight;

    private List<Sequence> mUnresolvedSequences = new ArrayList<>();

    private List<Span> mUnresolvedSpans = new ArrayList<>();
    private List<Span> mResolvedSpans = new ArrayList<>();

    private int mResolvedWidth = -1;
    private int mResolvedHeight = -1;

    private SparseIntArray mIdMap = new SparseIntArray(MAXIMUM_EXPECTED_NUMBER_OF_CHILDREN);
    private int[] mSizeMap = new int[MAXIMUM_EXPECTED_NUMBER_OF_CHILDREN * 5];

    PageResolver(SequenceLayout view) {
        mView = view;
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

    @Override
    public float getScreenScaledDensity() {
        return mView.getResources().getDisplayMetrics().scaledDensity;
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
        mPgSize = mView.getPgSize();

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

    void layoutViews() {
        mIdMap.clear();

        for (Span unit: mResolvedSpans) {
            int index = mIdMap.get(unit.elementId, -1);

            if (index == -1) {
                index = mIdMap.size();

                mIdMap.put(unit.elementId, index);
            }

            if (unit.isHorizontal) {
                mSizeMap[index * 4] = unit.getStart();
                mSizeMap[index * 4 + 2] = unit.getEnd();
            }
            else {
                mSizeMap[index * 4 + 1] = unit.getStart();
                mSizeMap[index * 4 + 3] = unit.getEnd();
            }
        }

        for (int i = 0; i < mIdMap.size(); i++) {
            View child = mView.findViewById(mIdMap.keyAt(i));

            if (child == null) {
                continue;
            }

            int index = mIdMap.valueAt(i);

            child.measure(
                    View.MeasureSpec.makeMeasureSpec(mSizeMap[index * 4 + 2] - mSizeMap[index * 4], View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(mSizeMap[index * 4 + 3] - mSizeMap[index * 4 + 1], View.MeasureSpec.EXACTLY)
            );

            child.layout(mSizeMap[index * 4], mSizeMap[index * 4 + 1], mSizeMap[index * 4 + 2], mSizeMap[index * 4 + 3]);
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
