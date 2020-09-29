package com.yashoid.sequencelayout;

import java.util.List;

class SpanUtil {

    private static final Span span = new Span();

    static Span find(int targetId, boolean horizontal, List<Span> spanList) {
        span.viewId = targetId;
        span.isHorizontal = horizontal;

        int index = spanList.indexOf(span);

        return index < 0 ? null : spanList.get(index);
    }

}
