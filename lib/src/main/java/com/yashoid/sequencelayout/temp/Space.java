package com.yashoid.sequencelayout.temp;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

public class Space extends SizeInfo {

    private static final String SIZE = "size";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String VISIBILITY_ELEMENT = "visibilityElement";

    public SizeInfo min = null;
    public SizeInfo max = null;

    public int visibilityElement = 0;

    Space(XmlPullParser parser, Context context) {
        final int attrCount = parser.getAttributeCount();

        for (int i = 0; i < attrCount; i++) {
            String name = parser.getAttributeName(i);

            switch (name) {
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
                    readAttribute(name, parser.getAttributeValue(i), context);
                    continue;
            }
        }
    }

    protected void readAttribute(String name, String value, Context context) {

    }

}
