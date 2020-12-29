package com.yashoid.sequencelayout;

public interface SizeResolverHost {

    float getPgSize();

    float getPgUnitSize();

    float getScreenDensity();

    float getScreenScaledDensity();

    int measureElementWidth(String id, int height, int min, int max);

    int measureElementHeight(String id, int width, int min, int max);

    void measureElement(String id, int minWidth, int maxWidth, int minHeight, int maxHeight, int[] measuredSize);

    Span findResolvedSpan(String id, boolean horizontal);
    Span findUnresolvedSpan(String id, boolean horizontal);

    void onSpanResolved(Span span);

    int getResolvingWidth();
    int getResolvingHeight();

}
