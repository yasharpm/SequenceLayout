package com.yashoid.sequencelayout;

import android.view.View;

public class SizeResolver {

    public static final int UNRESOLVABLE_SIZE = -1;

    private SizeResolverHost mHost;

    private int mTotalSize;

    private boolean mHasEncounteredPositionResolutionGap;
    private int mCurrentPosition;

    public SizeResolver() {

    }

    public void setHost(SizeResolverHost host) {
        mHost = host;
    }

    public void setResolutionInfo(int totalSize) {
        mTotalSize = totalSize;
    }

    public void setHasEncounteredPositionResolutionGap(boolean hasGap) {
        mHasEncounteredPositionResolutionGap = hasGap;
    }

    public void setCurrentPosition(int position) {
        mCurrentPosition = position;
    }

    public void reset() {
        mHasEncounteredPositionResolutionGap = false;
        mCurrentPosition = 0;
    }

    public int resolveSize(SizeInfo sizeInfo, boolean isHorizontal) {
        final ResolutionBox resolvedUnits = mHost.getResolvedUnits();
        final ResolutionBox unresolvedUnits = mHost.getUnresolvedUnits();

        if (sizeInfo.elementId != 0) {
            ResolveUnit unit = resolvedUnits.find(sizeInfo.elementId, isHorizontal);

            if (unit != null) {
                return unit.getSize();
            }
        }

        View view = sizeInfo.elementId == 0 ? null : mHost.findViewById(sizeInfo.elementId);

        if (sizeInfo instanceof Span) {
            if (view != null && view.getVisibility() == View.GONE) {
                return 0;
            }

            int visibilityElement = ((Span) sizeInfo).visibilityElement;

            if (visibilityElement != 0) {
                view = mHost.findViewById(visibilityElement);

                if (view != null && view.getVisibility() == View.GONE) {
                    return 0;
                }
            }
        }

        switch (sizeInfo.metric) {
            case SizeInfo.METRIC_PX:
            case SizeInfo.METRIC_MM:
            case SizeInfo.METRIC_PG:
            case SizeInfo.METRIC_RATIO:
                return sizeInfo.measureStaticSize(mTotalSize, mHost);
            case SizeInfo.METRIC_ELEMENT_RATIO:
                ResolveUnit ratioUnit = resolvedUnits.find(sizeInfo.relatedElementId, isHorizontal);

                if (ratioUnit != null) {
                    return (int) (sizeInfo.size * ratioUnit.getSize());
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_ALIGN:
                ResolveUnit resolveUnit = resolvedUnits.find(sizeInfo.relatedElementId, isHorizontal);

                if (!mHasEncounteredPositionResolutionGap && resolveUnit != null && resolveUnit.isPositionSet()) {
                    int targetPosition = resolveUnit.getStart();

                    return targetPosition > mCurrentPosition ? targetPosition - mCurrentPosition : 0;
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_WRAP:
                if (sizeInfo.elementId == 0) {
                    return 0;
                }

                ResolveUnit thisDirection = resolvedUnits.find(sizeInfo.elementId, isHorizontal);

                if (thisDirection != null) {
                    return thisDirection.getSize();
                }

                thisDirection = unresolvedUnits.find(sizeInfo.elementId, isHorizontal);

                if (thisDirection == null) {
                    throw new RuntimeException("Referenced element(" + sizeInfo.elementId + ") not found in unresovled units.");
                }

                thisDirection.setWrapping(true);

                ResolveUnit otherDirection = resolvedUnits.find(sizeInfo.elementId, !isHorizontal);

                if (otherDirection == null) {
                    otherDirection = unresolvedUnits.find(sizeInfo.elementId, !isHorizontal);

                    if (otherDirection == null) {
                        throw new RuntimeException("Other direction not found for referenced view(" + sizeInfo.elementId + ").");
                    }
                }

                if (otherDirection.isSizeSet()) {
                    int minSelf = 0;
                    int maxSelf = -1;

                    if (sizeInfo instanceof Span) {
                        Span span = (Span) sizeInfo;

                        if (span.min != null) {
                            minSelf = resolveSize(span.min, isHorizontal);
                        }

                        if (span.max != null) {
                            maxSelf = resolveSize(span.max, isHorizontal);
                        }
                    }

                    maxSelf = maxSelf == -1 ? mTotalSize : Math.min(maxSelf, mTotalSize);

                    if (isHorizontal) {
                        measureViewWidth(view, otherDirection.getSize(), minSelf, maxSelf);

                        return view.getMeasuredWidth();
                    }

                    measureViewHeight(view, otherDirection.getSize(), minSelf, maxSelf);

                    return view.getMeasuredHeight();
                }

                if (otherDirection.isWrappingKnown() && otherDirection.isWrapping()) {
                    int minSelf = 0;
                    int maxSelf = -1;

                    if (sizeInfo instanceof Span) {
                        Span span = (Span) sizeInfo;

                        if (span.min != null) {
                            minSelf = resolveSize(span.min, isHorizontal);
                        }

                        if (span.max != null) {
                            maxSelf = resolveSize(span.max, isHorizontal);
                        }
                    }

                    int minOther = 0;
                    int maxOther = -1;

                    if (otherDirection.getSpan() != null) {
                        Span span = otherDirection.getSpan();

                        if (span.min != null) {
                            minOther = resolveSize(span.min, !isHorizontal);
                        }

                        if (span.max != null) {
                            maxOther = resolveSize(span.max, !isHorizontal);
                        }
                    }

                    if (maxSelf == -1) {
                        maxSelf = mTotalSize;
                    }

                    if (isHorizontal) {
                        if (maxOther == -1) {
                            maxOther = mHost.getResolvingHeight();
                        }

                        measureView(view, minSelf, maxSelf, minOther, maxOther);

                        if (view.getMeasuredHeight() != UNRESOLVABLE_SIZE) {
                            otherDirection.setSize(view.getMeasuredHeight());

                            unresolvedUnits.take(otherDirection);
                            resolvedUnits.add(otherDirection);
                        }

                        return view.getMeasuredWidth();
                    }

                    if (maxOther == -1) {
                        maxOther = mHost.getResolvingWidth();
                    }

                    measureView(view, minOther, maxOther, minSelf, maxSelf);

                    if (view.getMeasuredWidth() != UNRESOLVABLE_SIZE) {
                        otherDirection.setSize(view.getMeasuredWidth());

                        unresolvedUnits.take(otherDirection);
                        resolvedUnits.add(otherDirection);
                    }

                    return view.getMeasuredHeight();
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_WEIGHT:
                return UNRESOLVABLE_SIZE;
            default:
                throw new RuntimeException("Unsupported metric for size info(" + sizeInfo + ").");
        }
    }

    private void measureViewWidth(View view, int height, int minWidth, int maxWidth) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    private void measureViewHeight(View view, int width, int minHeight, int maxHeight) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    }

    private void measureView(View view, int minWidth, int maxWidth, int minHeight, int maxHeight) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    }

}
