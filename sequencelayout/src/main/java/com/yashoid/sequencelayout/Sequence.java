package com.yashoid.sequencelayout;

import android.content.Context;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    public static class SequenceEdge {

        public static SequenceEdge read(String definition, Context context) {
            int targetId;
            float portionValue;

            int atIndex = definition.indexOf("@");

            String targetRawId = definition.substring(atIndex + 1);

            if (targetRawId.isEmpty()) {
                targetId = 0;
            }
            else {
                targetId = SizeInfo.resolveViewId(targetRawId, context);
            }

            String portion = definition.substring(0, atIndex);

            switch (portion) {
                case "start":
                    portionValue = 0;
                    break;
                case "end":
                    portionValue = 1;
                    break;
                default:
                    portionValue = Float.parseFloat(portion) / 100f;
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

        boolean hasUnresolvedSizes = false;
        boolean hasEncounteredPositionResolutionGap = !boundariesAreKnown;

        for (int index = 0; index < mSpans.size(); index++) {
            Span span = mSpans.get(index);

            int size = -1;
            boolean sizeUnresolved = false;

            if (span.metric == SizeInfo.METRIC_ALIGN) {
                Span relatedSpan = SpanUtil.find(span.relatedElementId, mIsHorizontal, resolvedSpans);

                if (!hasEncounteredPositionResolutionGap && relatedSpan != null && relatedSpan.isPositionSet()) {
                    int targetPosition = relatedSpan.getStart();

                    size = targetPosition - currentPosition;
                }
                else {
                    sizeUnresolved = true;
                }
            }
            else if (span.metric != SizeInfo.METRIC_WEIGHT) {
                size = mSizeResolver.resolveSize(span, mIsHorizontal, totalSize);

                sizeUnresolved = size == -1;
            }
            else {
                weightSum += span.size;

                mMeasuredSizes.put(index, SizeInfo.SIZE_WEIGHTED);
            }

            if (size != -1) {
                mMeasuredSizes.put(index, size);
                calculatedSize += size;

                if (span.elementId != 0) {
                    unresolvedSpans.remove(span);

                    if (span != null) {
                        span.setResolvedSize(size);

                        resolvedSpans.add(span);
                    }
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

                        if (span.min != null) {
                            int minSize = mSizeResolver.resolveSize(span.min, mIsHorizontal, totalSize);

                            if (minSize != -1) {
                                size = Math.max(size, minSize);
                            }
                        }

                        if (span.max != null) {
                            int maxSize = mSizeResolver.resolveSize(span.max, mIsHorizontal, totalSize);

                            if (maxSize != -1) {
                                size = Math.min(size, maxSize);
                            }
                        }
                    }
                    else {
                        size = 0;

                        if (span.min != null) {
                            size = mSizeResolver.resolveSize(span.min, mIsHorizontal, totalSize);
                        }

                        if (size < 0) {
                            return -1;
                        }
                    }

                    remainingWeight -= span.size;
                    remainingSize -= size;
                }

                if (span.elementId != 0) {
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

}
