package com.yashoid.sequencelayout;

import android.view.View;

import java.util.List;

public class SizeResolver {

    public static final int UNRESOLVABLE_SIZE = -1;

    private SizeResolverHost mHost;

    public SizeResolver() {

    }

    public void setHost(SizeResolverHost host) {
        mHost = host;
    }

    public int resolveSize(SizeInfo sizeInfo, boolean isHorizontal, int totalSize) {
        final List<Span> resolvedSpans = mHost.getResolvedSpans();
        final List<Span> unresolvedSpans = mHost.getUnresolvedSpans();

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
                return sizeInfo.measureStaticSize(totalSize, mHost);
            case SizeInfo.METRIC_ELEMENT_RATIO:
                Span relatedSpan = SpanUtil.find(sizeInfo.relatedElementId, isHorizontal, resolvedSpans);

                if (relatedSpan != null) {
                    return (int) (sizeInfo.size * relatedSpan.getResolvedSize());
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_WRAP:
                if (sizeInfo.elementId == 0) {
                    return 0;
                }

                Span thisDirection = SpanUtil.find(sizeInfo.elementId, isHorizontal, resolvedSpans);

                if (thisDirection != null) {
                    return thisDirection.getResolvedSize();
                }

                thisDirection = SpanUtil.find(sizeInfo.elementId, isHorizontal, unresolvedSpans);

                if (thisDirection == null) {
                    throw new RuntimeException("Referenced element(" + sizeInfo.elementId + ") not found in unresovled units.");
                }

                Span otherDirection = SpanUtil.find(sizeInfo.elementId, !isHorizontal, resolvedSpans);

                if (otherDirection == null) {
                    otherDirection = SpanUtil.find(sizeInfo.elementId, !isHorizontal, unresolvedSpans);

                    if (otherDirection == null) {
                        throw new RuntimeException("Other direction not found for referenced view(" + sizeInfo.elementId + ").");
                    }
                }

                if (otherDirection.isSizeResolved()) {
                    int minSelf = 0;
                    int maxSelf = -1;

                    if (sizeInfo instanceof Span) {
                        Span span = (Span) sizeInfo;

                        if (span.min != null) {
                            minSelf = resolveSize(span.min, isHorizontal, totalSize);
                        }

                        if (span.max != null) {
                            maxSelf = resolveSize(span.max, isHorizontal, totalSize);
                        }
                    }

                    maxSelf = maxSelf == -1 ? totalSize : Math.min(maxSelf, totalSize);

                    if (isHorizontal) {
                        measureViewWidth(view, otherDirection.getResolvedSize(), minSelf, maxSelf);

                        return view.getMeasuredWidth();
                    }

                    measureViewHeight(view, otherDirection.getResolvedSize(), minSelf, maxSelf);

                    return view.getMeasuredHeight();
                }

                if (otherDirection.metric == SizeInfo.METRIC_WRAP) {
                    int minSelf = 0;
                    int maxSelf = -1;

                    if (sizeInfo instanceof Span) {
                        Span span = (Span) sizeInfo;

                        if (span.min != null) {
                            minSelf = resolveSize(span.min, isHorizontal, totalSize);
                        }

                        if (span.max != null) {
                            maxSelf = resolveSize(span.max, isHorizontal, totalSize);
                        }
                    }

                    int minOther = 0;
                    int maxOther = -1;

                    if (otherDirection != null) {
                        Span span = otherDirection;

                        if (span.min != null) {
                            minOther = resolveSize(span.min, !isHorizontal, totalSize);
                        }

                        if (span.max != null) {
                            maxOther = resolveSize(span.max, !isHorizontal, totalSize);
                        }
                    }

                    if (maxSelf == -1) {
                        maxSelf = totalSize;
                    }

                    if (isHorizontal) {
                        if (maxOther == -1) {
                            maxOther = mHost.getResolvingHeight();
                        }

                        measureView(view, minSelf, maxSelf, minOther, maxOther);

                        if (view.getMeasuredHeight() != UNRESOLVABLE_SIZE) {
                            otherDirection.setResolvedSize(view.getMeasuredHeight());

                            unresolvedSpans.remove(otherDirection);
                            resolvedSpans.add(otherDirection);
                        }

                        return view.getMeasuredWidth();
                    }

                    if (maxOther == -1) {
                        maxOther = mHost.getResolvingWidth();
                    }

                    measureView(view, minOther, maxOther, minSelf, maxSelf);

                    if (view.getMeasuredWidth() != UNRESOLVABLE_SIZE) {
                        otherDirection.setResolvedSize(view.getMeasuredWidth());

                        unresolvedSpans.remove(otherDirection);
                        resolvedSpans.add(otherDirection);
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
