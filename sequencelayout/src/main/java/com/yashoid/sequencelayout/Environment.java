package com.yashoid.sequencelayout;

public interface Environment {

    float getScreenDensity();
    float getScreenScaledDensity();

    float getPgSize();

    boolean readSizeInfo(String src, SizeInfo dst);

    int getElementCount();

    /**
     *
     * @param id
     * @return false if id is not null and the element exists and is not visible. Otherwise true.
     */
    boolean isElementVisible(String id);

    int measureElementWidth(String id, int height, int min, int max);

    int measureElementHeight(String id, int width, int min, int max);

    void measureElement(String id, int minWidth, int maxWidth, int minHeight, int maxHeight, int[] measuredSize);

    void positionElement(String id, int left, int top, int right, int bottom);

}
