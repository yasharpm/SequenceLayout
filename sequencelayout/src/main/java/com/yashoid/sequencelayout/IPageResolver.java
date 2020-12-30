package com.yashoid.sequencelayout;

interface IPageResolver {

    void setEnvironment(Environment environment);

    void onSequenceAdded(Sequence sequence);

    void onSequenceRemoved(Sequence sequence);

    Sequence findSequenceById(String id);

    void startResolution(int pageWidth, int pageHeight, boolean horizontalWrapping, boolean verticalWrapping);

    int getResolvedWidth();
    int getResolvedHeight();

    void positionViews();

}
