package com.yashoid.sequencelayout;

public class SizeResolver {

    public static final int UNRESOLVABLE_SIZE = -1;

    private static final int[] sSizeHelper = new int[2];

    private SizeResolverHost mHost;

    SizeResolver() {

    }

    void setHost(SizeResolverHost host) {
        mHost = host;
    }

    public int resolveSize(SizeInfo sizeInfo, boolean isHorizontal, int totalSize) {
        if (sizeInfo instanceof Span) {
            if (!((Span) sizeInfo).isVisible()) {
                return 0;
            }
        }

        switch (sizeInfo.metric) {
            case SizeInfo.METRIC_PX:
            case SizeInfo.METRIC_MM:
            case SizeInfo.METRIC_PG:
            case SizeInfo.METRIC_DP:
            case SizeInfo.METRIC_SP:
                return sizeInfo.measureStaticSize(totalSize, mHost);
            case SizeInfo.METRIC_RATIO:
                return totalSize == -1 ? UNRESOLVABLE_SIZE : sizeInfo.measureStaticSize(totalSize, mHost);
            case SizeInfo.METRIC_VIEW_RATIO:
                Span relatedSpan = mHost.findResolvedSpan(sizeInfo.relatedElementId, isHorizontal);

                if (relatedSpan != null) {
                    return (int) (sizeInfo.size * relatedSpan.getResolvedSize());
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_MAX:
                int max = 0;

                for (SizeInfo si: sizeInfo.relations) {
                    int size = resolveSize(si, isHorizontal, totalSize);

                    if (size == UNRESOLVABLE_SIZE) {
                        return UNRESOLVABLE_SIZE;
                    }

                    max = Math.max(max, size);
                }

                return max;
            case SizeInfo.METRIC_WRAP:
                if (sizeInfo.id == null) {
                    return 0;
                }

                Span thisDirection = mHost.findResolvedSpan(sizeInfo.id, isHorizontal);

                if (thisDirection != null) {
                    return thisDirection.getResolvedSize();
                }

                thisDirection = mHost.findUnresolvedSpan(sizeInfo.id, isHorizontal);

                if (thisDirection == null) {
                    throw new RuntimeException("Referenced element(" + sizeInfo.id + ") not found in unresovled units.");
                }

                Span otherDirection = mHost.findResolvedSpan(sizeInfo.id, !isHorizontal);

                if (otherDirection == null) {
                    otherDirection = mHost.findUnresolvedSpan(sizeInfo.id, !isHorizontal);

                    if (otherDirection == null) {
                        throw new RuntimeException("Other direction not found for referenced view(" + sizeInfo.id + ").");
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
                        return mHost.measureElementWidth(sizeInfo.id, otherDirection.getResolvedSize(), minSelf, maxSelf);
                    }

                    return mHost.measureElementHeight(sizeInfo.id, otherDirection.getResolvedSize(), minSelf, maxSelf);
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

                    if (otherDirection.min != null) {
                        minOther = resolveSize(otherDirection.min, !isHorizontal, totalSize);
                    }

                    if (otherDirection.max != null) {
                        maxOther = resolveSize(otherDirection.max, !isHorizontal, totalSize);
                    }

                    if (maxSelf == -1) {
                        maxSelf = totalSize;
                    }

                    if (isHorizontal) {
                        if (maxOther == -1) {
                            maxOther = mHost.getResolvingHeight();
                        }

                        mHost.measureElement(sizeInfo.id, minSelf, maxSelf, minOther, maxOther, sSizeHelper);

                        if (sSizeHelper[1] != UNRESOLVABLE_SIZE) {
                            otherDirection.setResolvedSize(sSizeHelper[1]);

                            mHost.onSpanResolved(otherDirection);
                        }

                        return sSizeHelper[0];
                    }

                    if (maxOther == -1) {
                        maxOther = mHost.getResolvingWidth();
                    }

                    mHost.measureElement(sizeInfo.id, minOther, maxOther, minSelf, maxSelf, sSizeHelper);

                    if (sSizeHelper[0] != UNRESOLVABLE_SIZE) {
                        otherDirection.setResolvedSize(sSizeHelper[0]);

                        mHost.onSpanResolved(otherDirection);
                    }

                    return sSizeHelper[1];
                }

                return UNRESOLVABLE_SIZE;
            case SizeInfo.METRIC_WEIGHT:
                return UNRESOLVABLE_SIZE;
            default:
                throw new RuntimeException("Unsupported metric for size info(" + sizeInfo + ").");
        }
    }

}
