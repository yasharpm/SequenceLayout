package com.yashoid.sequencelayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

class PageResolver implements IPageResolver, SizeResolverHost {

    private static final int MAXIMUM_EXPECTED_NUMBER_OF_CHILDREN = 20;
    private static final int MAXIMUM_EXPECTED_NUMBER_OF_SPANS = MAXIMUM_EXPECTED_NUMBER_OF_CHILDREN * 3;

    private Environment mEnvironment;

    private float mPageWidth;
    private float mPageHeight;

    private List<Sequence> mSequences = new ArrayList<>();

    private int mResolvingWidth;
    private int mResolvingHeight;

    private List<Sequence> mUnresolvedSequences = new ArrayList<>();

    private List<Span> mUnresolvedSpans = new ArrayList<>(MAXIMUM_EXPECTED_NUMBER_OF_SPANS);
    private List<Span> mResolvedSpans = new ArrayList<>(MAXIMUM_EXPECTED_NUMBER_OF_SPANS);

    private int mResolvedWidth = -1;
    private int mResolvedHeight = -1;

    private String[] mIdMap = new String[MAXIMUM_EXPECTED_NUMBER_OF_CHILDREN];
    private int[] mSizeMap = new int[MAXIMUM_EXPECTED_NUMBER_OF_CHILDREN * 4];

    public PageResolver(Environment environment) {
        mEnvironment = environment;
    }

    PageResolver makeClone() {
        PageResolver clone = new PageResolver(mEnvironment);

        clone.mSequences.addAll(mSequences);

        return clone;
    }

    @Override
    public void setEnvironment(Environment environment) {
        mEnvironment = environment;
    }

    @Override
    public float getPageWidthUnitSize() {
        if (mPageWidth == 0) {
            return 1;
        }

        return mResolvingWidth / mPageWidth;
    }

    @Override
    public float getPageHeightUnitSize() {
        if (mPageHeight == 0) {
            return 1;
        }

        return mResolvingHeight / mPageHeight;
    }

    @Override
    public float getScreenDensity() {
        return mEnvironment.getScreenDensity();
    }

    @Override
    public float getScreenScaledDensity() {
        return mEnvironment.getScreenScaledDensity();
    }

    @Override
    public int measureElementWidth(String id, int height, int min, int max) {
        return mEnvironment.measureElementWidth(id, height, min, max);
    }

    @Override
    public int measureElementHeight(String id, int width, int min, int max) {
        return mEnvironment.measureElementHeight(id, width, min, max);
    }

    @Override
    public void measureElement(String id, int minWidth, int maxWidth, int minHeight, int maxHeight, int[] measuredSize) {
        mEnvironment.measureElement(id, minWidth, maxWidth, minHeight, maxHeight, measuredSize);
    }

    @Override
    public void onSequenceAdded(Sequence sequence) {
        mSequences.add(sequence);
    }

    @Override
    public void onSequenceRemoved(Sequence sequence) {
        mSequences.remove(sequence);
    }

    @Override
    public Sequence findSequenceById(String id) {
        if (id == null) {
            return null;
        }

        for (Sequence sequence: mSequences) {
            if (id.equals(sequence.getId())) {
                return sequence;
            }
        }

        return null;
    }

    @Override
    public void startResolution(int pageWidth, int pageHeight, boolean horizontalWrapping, boolean verticalWrapping) {
        mPageWidth = mEnvironment.getPageWidth();
        mPageHeight = mEnvironment.getPageHeight();

        mResolvingWidth = pageWidth;
        mResolvingHeight = pageHeight;

        mUnresolvedSpans.clear();
        mResolvedSpans.clear();

        for (Sequence sequence: mSequences) {
            mUnresolvedSpans.addAll(sequence.getSpans());
        }

        // Resolving visibility
        int unresolvedSpanCount = mUnresolvedSpans.size();

        while (!mUnresolvedSpans.isEmpty()) {
            ListIterator<Span> iterator = mUnresolvedSpans.listIterator();

            while (iterator.hasNext()) {
                Span span = iterator.next();

                if (span.visibilityElement == null) {
                    span.setVisible(mEnvironment.isElementVisible(span.id));

                    SpanUtil.add(span, mResolvedSpans);
                    iterator.remove();
                }
                else {
                    Span resolvedSpan = SpanUtil.find(span.visibilityElement, span.isHorizontal, mResolvedSpans);

                    if (resolvedSpan == null) {
                        resolvedSpan = SpanUtil.find(span.visibilityElement, !span.isHorizontal, mResolvedSpans);
                    }

                    if (resolvedSpan != null) {
                        span.setVisible(resolvedSpan.isVisible());

                        SpanUtil.add(span, mResolvedSpans);
                        iterator.remove();
                    }
                }
            }

            int remainingUnresolvedSpans = mUnresolvedSpans.size();

            if (unresolvedSpanCount == remainingUnresolvedSpans) {
                // We do this to be soft on the visibility resolution.
                break;
            }
            else {
                unresolvedSpanCount = remainingUnresolvedSpans;
            }
        }

        for (Span span: mUnresolvedSpans) {
            span.setVisible(true);
        }

        // Resolving positions
        mUnresolvedSpans.addAll(mResolvedSpans);
        mResolvedSpans.clear();
        Collections.sort(mUnresolvedSpans);

        mUnresolvedSequences.clear();
        mUnresolvedSequences.addAll(mSequences);

        int maxWidth = -1;
        int maxHeight = -1;

        unresolvedSpanCount = mUnresolvedSpans.size();

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

            int remainingUnresolvedSpans = mUnresolvedSpans.size();

            if (unresolvedSpanCount == remainingUnresolvedSpans) {
                // We have an unresolvable situation here.
                throw new RuntimeException("Sequences definitions are interrelated and unresolvable. Check 'mUnresolvedSpans' to investigate.");
            }
        }

        mResolvedWidth = maxWidth;
        mResolvedHeight = maxHeight;
    }

    @Override
    public int getResolvedWidth() {
        return mResolvedWidth;
    }

    @Override
    public int getResolvedHeight() {
        return mResolvedHeight;
    }

    @Override
    public void positionViews() {
        int elementCount = mEnvironment.getElementCount();

        if (mIdMap.length < elementCount) {
            mIdMap = new String[elementCount];
        }

        if (mSizeMap.length < elementCount * 4) {
            mSizeMap = new int[elementCount * 4];
        }

        elementCount = 0;

        for (Span unit: mResolvedSpans) {
            if (unit.id == null) {
                continue;
            }

            int index = Arrays.binarySearch(mIdMap, 0, elementCount, unit.id);

            if (index < 0) {
                index = elementCount;

                mIdMap[index] = unit.id;

                elementCount++;

                Arrays.sort(mIdMap, 0, elementCount);
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

        for (int i = 0; i < elementCount; i++) {
            String id = mIdMap[i];

            mEnvironment.positionElement(id, mSizeMap[i * 4], mSizeMap[i * 4 + 1], mSizeMap[i * 4 + 2], mSizeMap[i * 4 + 3]);
        }
    }

    @Override
    public Span findResolvedSpan(String id, boolean horizontal) {
        return SpanUtil.find(id, horizontal, mResolvedSpans);
    }

    @Override
    public Span findUnresolvedSpan(String id, boolean horizontal) {
        return SpanUtil.find(id, horizontal, mUnresolvedSpans);
    }

    @Override
    public void onSpanResolved(Span span) {
        if (mUnresolvedSpans.remove(span)) {
            mResolvedSpans.add(span);
            Collections.sort(mResolvedSpans);
        }
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
