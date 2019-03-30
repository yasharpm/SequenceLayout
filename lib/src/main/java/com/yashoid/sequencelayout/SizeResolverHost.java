package com.yashoid.sequencelayout;

import android.view.View;

public interface SizeResolverHost extends PageSizeProvider {

    View findViewById(int id);

    ResolutionBox getResolvedUnits();
    ResolutionBox getUnresolvedUnits();

    int getResolvingWidth();
    int getResolvingHeight();

}
