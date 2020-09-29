package com.yashoid.sequencelayout;

import android.content.Context;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    static final String SEQUENCE_EDGE_START = "start";
    static final String SEQUENCE_EDGE_END = "end";

    public static class SequenceEdge {

        public static SequenceEdge read(String definition, Context context) {
            int targetId;
            float portionValue;

            int atIndex = definition.indexOf("@");

            String targetRawId = atIndex == -1 ? "" : definition.substring(atIndex + 1);

            if (targetRawId.isEmpty()) {
                targetId = 0;
            }
            else {
                targetId = SizeInfo.resolveViewId(targetRawId, context);
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

        public int targetId;
        public float portion;

        public SequenceEdge(int targetId, float portion) {
            this.targetId = targetId;
            this.portion = portion;
        }

        public boolean isRelativeToParent() {
            return targetId == 0;
        }

        protected int resolve(Sequence sequence, int totalSize, List<Span> resolvedSizes) {
            if (targetId == 0) {
                return (int) (totalSize * portion);
            }

            Span resolveUnit = SpanUtil.find(targetId, sequence.mIsHorizontal, resolvedSizes);

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

    public Sequence(String id, boolean isHorizontal, String start, String end, Context context) {
        this(id, isHorizontal, SequenceEdge.read(start, context), SequenceEdge.read(end, context));
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

        final List<Span> resolvedSpans = host.getResolvedSpans();
        final List<Span> unresolvedSpans = host.getUnresolvedSpans();

        int start = mStart.resolve(this, totalSize, resolvedSpans);
        int end = mEnd.resolve(this, totalSize, resolvedSpans);

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

            int min = span.min != null ? resolveSizeInfo(span.min, resolvedSpans, hasEncounteredPositionResolutionGap, currentPosition, totalSize) : Integer.MIN_VALUE;
            int max = span.max != null ? resolveSizeInfo(span.max, resolvedSpans, hasEncounteredPositionResolutionGap, currentPosition, totalSize) : Integer.MAX_VALUE;

            if (min != -1 && max != -1) {
                if (span.metric != SizeInfo.METRIC_WEIGHT) {
                    size = resolveSizeInfo(span, resolvedSpans, hasEncounteredPositionResolutionGap, currentPosition, totalSize);

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

                if (span.viewId != 0) {
                    unresolvedSpans.remove(span);

                    span.setResolvedSize(size);

                    resolvedSpans.add(span);
                }

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

                if (span.viewId != 0) {
                    unresolvedSpans.remove(span);

                    span.setResolvedSize(size);
                    span.setStart(currentPosition);
                    span.setEnd(currentPosition + size);

                    resolvedSpans.add(span);
                }

                currentPosition += size;

                calculatedSize += size;
            }
        }

        return start + calculatedSize;
    }

    private int resolveSizeInfo(SizeInfo sizeInfo, List<Span> resolvedSpans,
                                boolean hasEncounteredPositionResolutionGap,
                                int currentPosition, int totalSize) {
        if (sizeInfo.metric == SizeInfo.METRIC_ALIGN) {
            Span relatedSpan = SpanUtil.find(sizeInfo.relatedElementId, mIsHorizontal, resolvedSpans);

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
