package com.yashoid.sequencelayout.temp;

import android.util.DisplayMetrics;
import android.view.View;

public class SizeResolver {

    public static final int UNRESOLVABLE_SIZE = -1;

    private View mView;
    private PageSizeProvider mPageSizeProvider;
    private DisplayMetrics mMetrics;

    private ResolutionBox mResolvedSizes;
    private ResolutionBox mUnresolvedSizes;
    private int mTotalSize;
    private int mPageWidth;
    private int mPageHeight;

    private boolean mHasEncounteredPositionResolutionGap;
    private int mCurrentPosition;

    public SizeResolver() {
        reset();
    }

    public void setup(View view, DisplayMetrics metrics, PageSizeProvider sizeProvider) {
        mView = view;
        mMetrics = metrics;
        mPageSizeProvider = sizeProvider;
    }

    public void setResolutionInfo(
            ResolutionBox resolvedSizes,
            ResolutionBox unresolvedSizes,
            int totalSize,
            int pageWidth,
            int pageHeight) {
        mResolvedSizes = resolvedSizes;
        mUnresolvedSizes = unresolvedSizes;

        mTotalSize = totalSize;
        mPageWidth = pageWidth;
        mPageHeight = pageHeight;
    }

    public void setHasEncounteredPositionResolutionGap(boolean hasGap) {
        mHasEncounteredPositionResolutionGap = hasGap;
    }

    public void setCurrentPosition(int position) {
        mCurrentPosition = position;
    }

    public void reset() {
        mResolvedSizes = null;
        mUnresolvedSizes = null;
        mHasEncounteredPositionResolutionGap = false;
        mCurrentPosition = 0;
    }

    public int resolveSize(SizeInfo sizeInfo, boolean isHorizontal) {
//        if (sizeInfo.elementId != null) {
//            PageResolver.ResolveUnitImplementation unit = mResolvedSizes.find(sizeInfo.elementId, isHorizontal);
//
//            if (unit != null) {
//                return unit.getSize();
//            }
//        }

        View view = mView.findViewById(sizeInfo.elementId);

        if (sizeInfo instanceof Element) {
            if (view != null && view.getVisibility() == View.GONE) {
                return 0;
            }
        }
        else if (sizeInfo instanceof Space) {
            int visibilityElement = ((Space) sizeInfo).visibilityElement;

            if (visibilityElement != 0) {
                view = mView.findViewById(visibilityElement);

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
            case SizeInfo.METRIC_MATCH_PARENT:
                return sizeInfo.measureStaticSize(mTotalSize, mMetrics, mPageSizeProvider.getPgUnitSize());
            case SizeInfo.METRIC_ELEMENT_RATIO:
                ResolveUnit ratioUnit = mResolvedSizes.find(sizeInfo.relatedElementId, isHorizontal);

                if (ratioUnit != null) {
                    return (int) (sizeInfo.size * ratioUnit.getSize());
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_ALIGN:
                ResolveUnit resolveUnit = mResolvedSizes.find(sizeInfo.relatedElementId, isHorizontal);

                if (!mHasEncounteredPositionResolutionGap && resolveUnit != null && resolveUnit.isPositionSet()) {
                    int targetPosition = resolveUnit.getStart();

                    return targetPosition > mCurrentPosition ? targetPosition - mCurrentPosition : 0;
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_WRAP:
                if (sizeInfo.elementId == 0) {
                    return 0;
                }

                ResolveUnit thisDirection = mResolvedSizes.find(sizeInfo.elementId, isHorizontal);

                if (thisDirection != null) {
                    return thisDirection.getSize();
                }

                thisDirection = mUnresolvedSizes.find(sizeInfo.elementId, isHorizontal);

                if (thisDirection == null) {
                    throw new RuntimeException("Referenced element(" + sizeInfo.elementId + ") not found in unresovled units.");
                }

                thisDirection.setWrapping(true);

                ResolveUnit otherDirection = mResolvedSizes.find(sizeInfo.elementId, !isHorizontal);

                if (otherDirection == null) {
                    otherDirection = mUnresolvedSizes.find(sizeInfo.elementId, !isHorizontal);

                    if (otherDirection == null) {
                        throw new RuntimeException("Other direction not found for referenced view(" + sizeInfo.elementId + ").");
                    }
                }

                if (otherDirection.isSizeSet()) {
                    int minSelf = 0;
                    int maxSelf = -1;

                    if (sizeInfo instanceof Space) {
                        Space space = (Space) sizeInfo;

                        if (space.min != null) {
                            minSelf = resolveSize(space.min, isHorizontal);
                        }

                        if (space.max != null) {
                            maxSelf = resolveSize(space.max, isHorizontal);
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

                    if (sizeInfo instanceof Space) {
                        Space space = (Space) sizeInfo;

                        if (space.min != null) {
                            minSelf = resolveSize(space.min, isHorizontal);
                        }

                        if (space.max != null) {
                            maxSelf = resolveSize(space.max, isHorizontal);
                        }
                    }

                    int minOther = 0;
                    int maxOther = -1;

                    if (otherDirection.getSizeInfo() != null && otherDirection.getSizeInfo() instanceof Space) {
                        Space space = (Space) otherDirection.getSizeInfo();

                        if (space.min != null) {
                            minOther = resolveSize(space.min, !isHorizontal);
                        }

                        if (space.max != null) {
                            maxOther = resolveSize(space.max, !isHorizontal);
                        }
                    }

                    if (maxSelf == -1) {
                        maxSelf = mTotalSize;
                    }

                    if (isHorizontal) {
                        if (maxOther == -1) {
                            maxOther = mPageHeight;
                        }

                        measureView(view, minSelf, maxSelf, minOther, maxOther);

                        if (view.getMeasuredHeight() != UNRESOLVABLE_SIZE) {
                            otherDirection.setSize(view.getMeasuredHeight());

                            mUnresolvedSizes.take(otherDirection);
                            mResolvedSizes.add(otherDirection);
                        }

                        return view.getMeasuredWidth();
                    }

                    if (maxOther == -1) {
                        maxOther = mPageWidth;
                    }

                    measureView(view, minOther, maxOther, minSelf, maxSelf);

                    if (view.getMeasuredWidth() != UNRESOLVABLE_SIZE) {
                        otherDirection.setSize(view.getMeasuredWidth());

                        mUnresolvedSizes.take(otherDirection);
                        mResolvedSizes.add(otherDirection);
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
