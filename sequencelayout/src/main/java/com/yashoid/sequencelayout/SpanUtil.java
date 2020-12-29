package com.yashoid.sequencelayout;

import java.util.Collections;
import java.util.List;

class SpanUtil {

    private static final Span span = new Span();

    static Span find(String targetId, boolean horizontal, List<Span> spanList) {
        span.id = targetId;
        span.isHorizontal = horizontal;

        if (targetId.equals("button_sample1") && horizontal) {
            span.metric = 3;
        }

        int index = Collections.binarySearch(spanList, span);

        if (index < 0 && spanList.size() > 0) {
            span.metric = 4;
        }

        return index < 0 ? null : spanList.get(index);
    }

    static void add(Span span, List<Span> spanList) {
        int index = Collections.binarySearch(spanList, span);

        if (index < 0) {
            index = -index - 1;
        }

        spanList.add(index, span);
    }

}
