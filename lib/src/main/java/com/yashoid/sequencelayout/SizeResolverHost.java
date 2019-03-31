package com.yashoid.sequencelayout;

import android.view.View;

import java.util.List;

public interface SizeResolverHost extends PageSizeProvider {

    View findViewById(int id);

    List<Span> getResolvedUnits();
    List<Span> getUnresolvedUnits();

    int getResolvingWidth();
    int getResolvingHeight();

}
