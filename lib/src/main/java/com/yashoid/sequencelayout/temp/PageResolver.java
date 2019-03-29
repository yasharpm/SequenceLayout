package com.yashoid.sequencelayout.temp;

import android.view.View;

import com.yashoid.sequencelayout.SequenceLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class PageResolver implements PageSizeProvider {

    private SequenceLayout mView;
    private float mPgSize;

    private List<Sequence> mSequences = new ArrayList<>();

    private List<Sequence> mUnresolvedSequences = new ArrayList<>();

    private ResolutionBox mUnresolvedUnits = new ResolutionBox();
    private ResolutionBox mResolvedUnits = new ResolutionBox();

    private int mResolvedWidth = -1;
    private int mResolvedHeight = -1;

    public PageResolver(SequenceLayout view) {
        mView = view;

        mPgSize = mView.getPgSize();
    }

    @Override
    public float getPgSize() {
        return mPgSize;
    }

    private int mResolvingWidth; // TODO Dirty much!

    @Override
    public float getPgUnitSize() {
        if (mPgSize == 0) {
            return 1;
        }

        return mResolvingWidth / mPgSize;
    }

    public void reset() {
        mUnresolvedSequences.clear();

        mResolvedUnits.passUnits(mUnresolvedUnits);
        mUnresolvedUnits.resetUnits();

        mResolvedWidth = -1;
        mResolvedHeight = -1;
    }

    public void onSequenceAdded(Sequence sequence) {
        sequence.setup(mView, this);

        mSequences.add(sequence);

        final boolean horizontal = sequence.isHorizontal();
        List<SizeInfo> sizeInfo = sequence.getSizeInfo();

        for (SizeInfo size: sizeInfo) {
            if (size.elementId != 0) {
                mUnresolvedUnits.add(ResolveUnit.obtain(size.elementId, horizontal, size));
            }
        }
    }

    protected void onSequenceRemoved(Sequence sequence) {
        mSequences.remove(sequence);

        final boolean horizontal = sequence.isHorizontal();
        List<SizeInfo> sizeInfo = sequence.getSizeInfo();

        for (SizeInfo size: sizeInfo) {
            if (size.elementId != 0) {
                ResolveUnit unit = mUnresolvedUnits.take(size.elementId, horizontal);

                if (unit != null) {
                    unit.release();
                }

                unit = mResolvedUnits.take(size.elementId, horizontal);

                if (unit != null) {
                    unit.release();
                }
            }
        }
    }

    public void startResolution(int pageWidth, int pageHeight, boolean horizontalWrapping, boolean verticalWrapping) {
        mResolvingWidth = pageWidth;

        mUnresolvedSequences.addAll(mSequences);

        int maxWidth = -1;
        int maxHeight = -1;

        while (!mUnresolvedSequences.isEmpty()) {
            ListIterator<Sequence> iterator = mUnresolvedSequences.listIterator();

            while (iterator.hasNext()) {
                Sequence sequence = iterator.next();

                int size = sequence.resolve(mResolvedUnits, mUnresolvedUnits, pageWidth, pageHeight, sequence.isHorizontal() ? horizontalWrapping : verticalWrapping);

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

    public void layoutViews() {
        for (ResolveUnit unit: mResolvedUnits.getUnits()) {
            View child = mView.findViewById(unit.getElementId());

            if (unit.isHorizontal()) {
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

    public int getResolvedWidth() {
        return mResolvedWidth;
    }

    public int getResolvedHeight() {
        return mResolvedHeight;
    }

}
