package com.yashoid.sequencelayout;

import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    static final String SEQUENCE_EDGE_START = "start";
    static final String SEQUENCE_EDGE_END = "end";

    public static class SequenceEdge {

        public static SequenceEdge read(String definition) {
            float portionValue;

            int atIndex = definition.indexOf("@");

            String targetId = atIndex == -1 ? null : definition.substring(atIndex + 1);

            if (targetId != null && targetId.isEmpty()) {
                targetId = null;
            }

            String portion = definition.substring(0, atIndex == -1 ? definition.length() : atIndex);

            switch (portion) {
                case SEQUENCE_EDGE_START:
                    portionValue = 0;
                    break;
                case SEQUENCE_EDGE_END:
                    portionValue = 1;
                    break;
                default:
                    try {
                        portionValue = Float.parseFloat(portion) / 100f;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid edge portion value '" +
                                portion + "'. Valid portion values are " + SEQUENCE_EDGE_START +
                                ", " + SEQUENCE_EDGE_END + " or a float number.", e);
                    }
                    break;
            }

            return new SequenceEdge(targetId, portionValue);
        }

        public String targetId;
        public float portion;

        public SequenceEdge(String targetId, float portion) {
            this.targetId = targetId;
            this.portion = portion;
        }

        public boolean isRelativeToParent() {
            return targetId == null;
        }

        protected int resolve(Sequence sequence, int totalSize, SizeResolverHost host) {
            if (targetId == null) {
                return (int) (totalSize * portion);
            }

            Span resolveUnit = host.findResolvedSpan(targetId, sequence.mIsHorizontal);

            if (resolveUnit == null) {
                return -1;
            }

            if (!resolveUnit.isPositionSet()) {
                return -1;
            }

            return (int) (resolveUnit.getStart() + portion * (resolveUnit.getEnd() - resolveUnit.getStart()));
        }

    }

    private String mId;

    private boolean mIsHorizontal;

    private SequenceEdge mStart;
    private SequenceEdge mEnd;

    private List<Span> mSpans = new ArrayList<>();
    private SparseIntArray mMeasuredSizes = new SparseIntArray(12);
    private SparseIntArray mMeasuredMins = new SparseIntArray(12);
    private SparseIntArray mMeasuredMaxs = new SparseIntArray(12);

    private SizeResolver mSizeResolver;

    public Sequence(String id, boolean isHorizontal, String start, String end) {
        this(id, isHorizontal, SequenceEdge.read(start), SequenceEdge.read(end));
    }

    public Sequence(String id, boolean isHorizontal, SequenceEdge start, SequenceEdge end) {
        mId = id;

        mIsHorizontal = isHorizontal;

        mStart = start;
        mEnd = end;

        mSizeResolver = new SizeResolver();
    }

    public String getId() {
        return mId;
    }

    public boolean isHorizontal() {
        return mIsHorizontal;
    }

    public void addSpan(Span span) {
        mSpans.add(span);
    }

    public void removeSpan(Span span) {
        mSpans.remove(span);
    }

    public List<Span> getSpans() {
        return mSpans;
    }

    public void setSpans(List<Span> spans) {
        mSpans.clear();
        mSpans.addAll(spans);
    }

    public int resolve(SizeResolverHost host, boolean wrapping) {
        if (!mStart.isRelativeToParent() && !mEnd.isRelativeToParent()) {
            wrapping = false;
        }

        mSizeResolver.setHost(host);

        int totalSize = mIsHorizontal ? host.getResolvingWidth() : host.getResolvingHeight();

        int start = mStart.resolve(this, totalSize, host);
        int end = mEnd.resolve(this, totalSize, host);

        boolean boundariesAreKnown = start >= 0 && end >= 0;

        if (end < start) {
            end = start;
        }

        totalSize = boundariesAreKnown ? end - start : -1;

        float weightSum = 0;
        int calculatedSize = 0;
        int currentPosition = start;

        mMeasuredSizes.clear();
        mMeasuredMins.clear();
        mMeasuredMaxs.clear();

        boolean hasUnresolvedSizes = false;
        boolean hasEncounteredPositionResolutionGap = !boundariesAreKnown;

        for (int index = 0; index < mSpans.size(); index++) {
            Span span = mSpans.get(index);

            int size = -1;
            boolean sizeUnresolved = false;

            int min = span.min != null ? resolveSizeInfo(span.min, host, hasEncounteredPositionResolutionGap, currentPosition, totalSize) : Integer.MIN_VALUE;
            int max = span.max != null ? resolveSizeInfo(span.max, host, hasEncounteredPositionResolutionGap, currentPosition, totalSize) : Integer.MAX_VALUE;

            if (min != -1 && max != -1) {
                if (span.metric != SizeInfo.METRIC_WEIGHT) {
                    size = resolveSizeInfo(span, host, hasEncounteredPositionResolutionGap, currentPosition, totalSize);

                    if (size != -1) {
                        size = Math.max(min, Math.min(max, size));
                    }
                    else {
                        sizeUnresolved = true;
                    }
                }
                else {
                    weightSum += span.size;

                    mMeasuredSizes.put(index, SizeInfo.SIZE_WEIGHTED);
                }

                mMeasuredMins.put(index, min);
                mMeasuredMaxs.put(index, max);
            }
            else {
                sizeUnresolved = true;
            }

            if (size != -1) {
                mMeasuredSizes.put(index, size);
                calculatedSize += size;

                span.setResolvedSize(size);

                host.onSpanResolved(span);

                if (!hasEncounteredPositionResolutionGap) {
                    span.setStart(currentPosition);
                    span.setEnd(currentPosition + size);

                    currentPosition += size;
                }
            }
            else {
                hasUnresolvedSizes |= sizeUnresolved;
                hasEncounteredPositionResolutionGap = true;
            }
        }

        if (hasUnresolvedSizes || !boundariesAreKnown) {
            return -1;
        }

        if (hasEncounteredPositionResolutionGap) {
            int remainingSize = totalSize - calculatedSize;
            float remainingWeight = weightSum;

            calculatedSize = 0;

            currentPosition = start;

            for (int index = 0; index < mSpans.size(); index++) {
                Span span = mSpans.get(index);

                int size = mMeasuredSizes.get(index, -1);

                if (size == SizeInfo.SIZE_WEIGHTED) {
                    if (!wrapping) {
                        size = (int) (span.size * remainingSize / remainingWeight);

                        size = Math.max(mMeasuredMins.get(index), Math.min(mMeasuredMaxs.get(index), size));
                    }
                    else {
                        size = Math.max(mMeasuredMins.get(index), Math.min(mMeasuredMaxs.get(index), 0));
                    }

                    remainingWeight -= span.size;
                    remainingSize -= size;
                }

                if (span.id != null) {
                    span.setResolvedSize(size);
                    span.setStart(currentPosition);
                    span.setEnd(currentPosition + size);

                    host.onSpanResolved(span);
                }

                currentPosition += size;

                calculatedSize += size;
            }
        }

        return start + calculatedSize;
    }

    private int resolveSizeInfo(SizeInfo sizeInfo, SizeResolverHost host,
                                boolean hasEncounteredPositionResolutionGap,
                                int currentPosition, int totalSize) {
        if (sizeInfo.metric == SizeInfo.METRIC_ALIGN) {
            Span relatedSpan = host.findResolvedSpan(sizeInfo.relatedElementId, mIsHorizontal);

            if (!hasEncounteredPositionResolutionGap && relatedSpan != null && relatedSpan.isPositionSet()) {
                int targetPosition = relatedSpan.getStart();

                return targetPosition - currentPosition;
            }
            else {
                return -1;
            }
        }
        else if (sizeInfo.metric != SizeInfo.METRIC_WEIGHT) {
            return mSizeResolver.resolveSize(sizeInfo, mIsHorizontal, totalSize);
        }
        else {
            return -1;
        }
    }

}
