package com.yashoid.sequencelayout;

import org.xmlpull.v1.XmlPullParser;

public class Span extends SizeInfo implements Comparable<Span> {

    private static final String ID = "id";
    private static final String SIZE = "size";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String VISIBLE_IF = "visibleIf";

    public boolean isHorizontal;

    public SizeInfo min = null;
    public SizeInfo max = null;

    public String visibilityElement = null;

    private boolean visible;

    private int resolvedSize = -1;
    private int start = -1;
    private int end = -1;

    Span(XmlPullParser parser, boolean isHorizontal, Environment environment) {
        this.isHorizontal = isHorizontal;

        final int attrCount = parser.getAttributeCount();

        boolean hasSize = false;

        for (int i = 0; i < attrCount; i++) {
            String name = parser.getAttributeName(i);

            switch (name) {
                case ID:
                    id = parser.getAttributeValue(i);
                    continue;
                case SIZE:
                    hasSize = true;
                    readSizeInfo(parser.getAttributeValue(i), this, environment);
                    continue;
                case MIN:
                    min = new SizeInfo();
                    readSizeInfo(parser.getAttributeValue(i), min, environment);

                    if (min.metric == METRIC_WEIGHT) {
                        throw new SequenceSyntaxException("Line " + parser.getLineNumber() +
                                ": Weighted size is not allowed as min size.");
                    }

                    continue;
                case MAX:
                    max = new SizeInfo();
                    readSizeInfo(parser.getAttributeValue(i), max, environment);

                    if (max.metric == METRIC_WEIGHT) {
                        throw new SequenceSyntaxException("Line " + parser.getLineNumber() +
                                ": Weighted size is not allowed as max size.");
                    }

                    continue;
                case VISIBLE_IF:
                    visibilityElement = parser.getAttributeValue(i);
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
            min.id = id;
        }

        if (max != null) {
            max.id = id;
        }
    }

    public Span() {

    }

    boolean isVisible() {
        return visible;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
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

        if (id != null && span.id != null) {
            return span.id.equals(id) && span.isHorizontal == isHorizontal;
        }
        else {
            return this == obj;
        }
    }

    @Override
    public int compareTo(Span that) {
        if (this.isHorizontal != that.isHorizontal) {
            return this.isHorizontal ? 1 : -1;
        }

        if (this.id != null) {
            if (that.id != null) {
                return this.id.compareTo(that.id);
            }

            return 1;
        }
        else if (that.id != null) {
            return -1;
        }

        return this.hashCode() - that.hashCode();
    }

}
