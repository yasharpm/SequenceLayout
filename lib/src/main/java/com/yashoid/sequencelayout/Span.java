package com.yashoid.sequencelayout;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

public class Span extends SizeInfo {

    private static final String ID = "id";
    private static final String SIZE = "size";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String VISIBILITY_ELEMENT = "visibilityElement";

    public SizeInfo min = null;
    public SizeInfo max = null;

    public int visibilityElement = 0;

    Span(XmlPullParser parser, Context context) {
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

}
