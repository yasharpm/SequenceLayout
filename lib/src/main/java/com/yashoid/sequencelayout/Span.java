package com.yashoid.sequencelayout;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

public class Span extends SizeInfo {

    private static final String ID = "id";
    private static final String SIZE = "size";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String VISIBILITY_ELEMENT = "visibilityElement";

    public boolean isHorizontal;

    public SizeInfo min = null;
    public SizeInfo max = null;

    public int visibilityElement = 0;

    public int resolvedSize = -1;
    public int start = -1;
    public int end = -1;

    Span(XmlPullParser parser, boolean isHorizontal, Context context) {
        this.isHorizontal = isHorizontal;

        final int attrCount = parser.getAttributeCount();

        for (int i = 0; i < attrCount; i++) {
            String name = parser.getAttributeName(i);

            switch (name) {
                case ID:
                    elementId = resolveViewId(parser.getAttributeValue(i), context);
                    continue;
                case SIZE:
                    readSizeInfo(parser.getAttributeValue(i), this, context);
                    continue;
                case MIN:
                    min = new SizeInfo();
                    readSizeInfo(parser.getAttributeValue(i), min, context);

                    if (min.metric == METRIC_WEIGHT) {
                        throw new IllegalArgumentException("Weighted size is illegal.");
                    }

                    continue;
                case MAX:
                    max = new SizeInfo();
                    readSizeInfo(parser.getAttributeValue(i), max, context);

                    if (max.metric == METRIC_WEIGHT) {
                        throw new IllegalArgumentException("Weighted size is illegal.");
                    }

                    continue;
                case VISIBILITY_ELEMENT:
                    visibilityElement = resolveViewId(parser.getAttributeValue(i), context);
                    continue;
                default:
                    continue;
            }
        }

        if (min != null) {
            min.elementId = elementId;
        }

        if (max != null) {
            max.elementId = elementId;
        }
    }

    public Span() {

    }

    public void setResolvedSize(int resolvedSize) {
        this.resolvedSize = resolvedSize;
    }

    public int getResolvedSize() {
        return resolvedSize;
    }
    public boolean isSizeResolved() {
        return resolvedSize != -1;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean isPositionSet() {
        return start != -1 && end != -1;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Span)) {
            return false;
        }

        Span span = (Span) obj;

        if (elementId != 0 && span.elementId != 0) {
            return span.elementId == elementId && span.isHorizontal == isHorizontal;
        }
        else {
            return this == obj;
        }
    }

}
