package com.yashoid.sequencelayout;

import android.view.View;

import java.util.List;

public interface SizeResolverHost {

    float getPgSize();

    float getPgUnitSize();

    float getScreenDensity();

    float getScreenScaledDensity();

    View findViewById(int id);

    List<Span> getResolvedSpans();
    List<Span> getUnresolvedSpans();

    int getResolvingWidth();
    int getResolvingHeight();

}
