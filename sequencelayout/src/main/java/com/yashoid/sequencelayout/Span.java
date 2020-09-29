package com.yashoid.sequencelayout;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

public class Span extends SizeInfo {

    private static final String ID = "id";
    private static final String SIZE = "size";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String VISIBLE_IF = "visibleIf";

    public boolean isHorizontal;

    public SizeInfo min = null;
    public SizeInfo max = null;

    public int visibilityElement = 0;

    private int resolvedSize = -1;
    private int start = -1;
    private int end = -1;

    Span(XmlPullParser parser, boolean isHorizontal, Context context) {
        this.isHorizontal = isHorizontal;

        final int attrCount = parser.getAttributeCount();

        boolean hasSize = false;

        for (int i = 0; i < attrCount; i++) {
            String name = parser.getAttributeName(i);

            switch (name) {
                case ID:
                    viewId = resolveViewId(parser.getAttributeValue(i), context);
                    continue;
                case SIZE:
                    hasSize = true;
                    readSizeInfo(parser.getAttributeValue(i), this, context);
                    continue;
                case MIN:
                    min = new SizeInfo();
                    readSizeInfo(parser.getAttributeValue(i), min, context);

                    if (min.metric == METRIC_WEIGHT) {
                        throw new SequenceSyntaxException("Line " + parser.getLineNumber() +
                                ": Weighted size is not allowed as min size.");
                    }

                    continue;
                case MAX:
                    max = new SizeInfo();
                    readSizeInfo(parser.getAttributeValue(i), max, context);

                    if (max.metric == METRIC_WEIGHT) {
                        throw new SequenceSyntaxException("Line " + parser.getLineNumber() +
                                ": Weighted size is not allowed as max size.");
                    }

                    continue;
                case VISIBLE_IF:
                    visibilityElement = resolveViewId(parser.getAttributeValue(i), context);
                    continue;
                default:
                    throw new SequenceSyntaxException("Line " + parser.getLineNumber() + ": " +
                            "Invalid " + SequenceReader.SPAN + " attribute '" + name + "'. " +
                            "Valid attributes are " + ID + ", " + SIZE + ", " + MIN + ", " +
                            MAX + " and " + VISIBLE_IF + ".");
            }
        }

        if (!hasSize) {
            throw new SequenceSyntaxException("Line " + parser.getLineNumber() +
                    ": Span has no size");
        }

        if (min != null) {
            min.viewId = viewId;
        }

        if (max != null) {
            max.viewId = viewId;
        }
    }

    public Span() {

    }

    void setResolvedSize(int resolvedSize) {
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

    void setStart(int start) {
        this.start = start;
    }

    void setEnd(int end) {
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

        if (viewId != 0 && span.viewId != 0) {
            return span.viewId == viewId && span.isHorizontal == isHorizontal;
        }
        else {
            return this == obj;
        }
    }

}
