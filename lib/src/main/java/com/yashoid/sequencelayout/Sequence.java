package com.yashoid.sequencelayout;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseIntArray;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class Sequence {

    public static class SequenceEdge {

        public int targetId;
        public float portion;

        public SequenceEdge(String definition, Context context) {
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
                    this.portion = 0;
                    break;
                case "end":
                    this.portion = 1;
                    break;
                default:
                    this.portion = Float.parseFloat(portion) / 100f;
                    break;
            }
        }

        public int resolve(Sequence sequence, int totalSize, ResolutionBox resolvedSizes) {
            if (targetId == 0) {
                return (int) (totalSize * portion);
            }

            ResolveUnit resolveUnit = resolvedSizes.find(targetId, sequence.mIsHorizontal);

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
        this(id, isHorizontal, new SequenceEdge(start, context), new SequenceEdge(end, context));
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

    public List<Span> getSpans() {
        return mSpans;
    }

    public int resolve(SizeResolverHost host, boolean wrapping) {
        mSizeResolver.setHost(host);

        int totalSize = mIsHorizontal ? host.getResolvingWidth() : host.getResolvingHeight();

        final ResolutionBox resolvedUnits = host.getResolvedUnits();
        final ResolutionBox unresolvedUnits = host.getUnresolvedUnits();

        int start = mStart.resolve(this, totalSize, resolvedUnits);
        int end = mEnd.resolve(this, totalSize, resolvedUnits);

        if (start < 0 || end < 0) {
            for (int index = 0; index < mSpans.size(); index++) {
                Span span = mSpans.get(index);

                if (span.elementId != 0) {
                    if (span.isStatic()) {
                        ResolveUnit unit = unresolvedUnits.take(span.elementId, mIsHorizontal);

                        if (unit != null) {
                            unit.setSize(mSizeResolver.resolveSize(span, mIsHorizontal));

                            resolvedUnits.add(unit);
                        }
                    }
                }
            }

            return -1;
        }

        if (end < start) {
            end = start;
        }

        totalSize = end - start;

        float weightSum = 0;
        int calculatedSize = 0;
        int currentPosition = start;

        mMeasuredSizes.clear();

        boolean hasUnresolvedSizes = false;
        boolean hasEncounteredPositionResolutionGap = false;

        for (int index = 0; index < mSpans.size(); index++) {
            SizeInfo sizeInfo = mSpans.get(index);

            if (sizeInfo.metric != SizeInfo.METRIC_WEIGHT) {
                int size = mSizeResolver.resolveSize(sizeInfo, mIsHorizontal);

                if (size != -1) {
                    mMeasuredSizes.put(index, size);
                    calculatedSize += size;

                    mSizeResolver.setCurrentPosition(calculatedSize);

                    ResolveUnit unit = null;

                    if (sizeInfo.elementId != 0) {
                        unit = unresolvedUnits.take(sizeInfo.elementId, mIsHorizontal);

                        if (unit != null) {
                            unit.setSize(size);

                            resolvedUnits.add(unit);
                        }
                    }

                    if (!hasEncounteredPositionResolutionGap) {
                        if (unit != null) {
                            unit.setStart(currentPosition);
                            unit.setEnd(currentPosition + size);
                        }

                        currentPosition += size;
                    }
                }
                else {
                    hasUnresolvedSizes |= size == -1;
                    hasEncounteredPositionResolutionGap = true;
                    mSizeResolver.setHasEncounteredPositionResolutionGap(true);
                }
            }
            else {
                hasEncounteredPositionResolutionGap = true;
                mSizeResolver.setHasEncounteredPositionResolutionGap(true);

                weightSum += sizeInfo.size;

                mMeasuredSizes.put(index, SizeInfo.SIZE_WEIGHTED);
            }
        }

        if (hasUnresolvedSizes) {
            mSizeResolver.reset();
            return -1;
        }

        if (hasEncounteredPositionResolutionGap) {
            int remainingSize = totalSize - calculatedSize;
            float remainingWeight = weightSum;

            calculatedSize = 0;

            currentPosition = start;

            for (int index = 0; index < mSpans.size(); index++) {
                SizeInfo sizeInfo = mSpans.get(index);

                int size = mMeasuredSizes.get(index, -1);

                if (size == SizeInfo.SIZE_WEIGHTED) {
                    if (!wrapping) {
                        size = (int) (sizeInfo.size * remainingSize / remainingWeight);
                    }
                    else {
                        size = 0;

                        if (sizeInfo instanceof Span) {
                            Span span = (Span) sizeInfo;

                            if (span.min != null) {
                                size = mSizeResolver.resolveSize(span.min, mIsHorizontal);
                            }

                            if (size < 0) {
                                mSizeResolver.reset();

                                return -1;
                            }
                        }
                    }

                    remainingWeight -= sizeInfo.size;
                    remainingSize -= size;
                }

                if (sizeInfo.elementId != 0) {
                    ResolveUnit unit = unresolvedUnits.take(sizeInfo.elementId, mIsHorizontal);

                    if (unit == null) {
                        unit = resolvedUnits.find(sizeInfo.elementId, mIsHorizontal);
                    }

                    if (unit != null) {
                        unit.setSize(size);
                        unit.setStart(currentPosition);
                        unit.setEnd(currentPosition + size);

                        resolvedUnits.add(unit);
                    }
                }

                currentPosition += size;

                calculatedSize += size;
            }
        }

        mSizeResolver.reset();

        return start + calculatedSize;
    }

}
