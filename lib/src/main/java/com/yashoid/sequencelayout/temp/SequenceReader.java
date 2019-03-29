package com.yashoid.sequencelayout.temp;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SequenceReader {

    private static final String NS = null;

    private static final String SEQUENCES = "Sequences";

    private static final String SEQUENCE_HORIZONTAL = "Horizontal";
    private static final String SEQUENCE_VERTICAL = "Vertical";
    private static final String SEQUENCE_ID = "id";
    private static final String SEQUENCE_START = "start";
    private static final String SEQUENCE_END = "end";

    private static final String SPACE = "Space";
    private static final String ELEMENT = "Element";

    private Context mContext;

    public SequenceReader(Context context) {
        mContext = context;
    }

    public List<Sequence> readSequences(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<Sequence> sequences = new ArrayList<>();

        parser.next();
        parser.next();

        parser.require(XmlPullParser.START_TAG, NS, SEQUENCES);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (SEQUENCE_HORIZONTAL.equals(name)) {
                sequences.add(readSequence(parser, true));
            }
            else if (SEQUENCE_VERTICAL.equals(name)) {
                sequences.add(readSequence(parser, false));
            }
            else {
                skip(parser);
            }
        }

        return sequences;
    }

    private Sequence readSequence(XmlPullParser parser, boolean horizontal) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, horizontal ? SEQUENCE_HORIZONTAL : SEQUENCE_VERTICAL);

        String id = null;
        String start = "start@";
        String end = "end@";

        int attrCount = parser.getAttributeCount();

        for (int i = 0; i < attrCount; i++) {
            String attrName = parser.getAttributeName(i);

            if (SEQUENCE_ID.equals(attrName)) {
                id = parser.getAttributeValue(i);
            }
            else if (SEQUENCE_START.equals(attrName)) {
                start = parser.getAttributeValue(i);
            }
            else if (SEQUENCE_END.equals(attrName)) {
                end = parser.getAttributeValue(i);
            }
        }

        Sequence sequence = new Sequence(id, horizontal, start, end, mContext);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (SPACE.equals(name)) {
                Space space = new Space(parser, mContext);

                sequence.addSizeInfo(space);
            }
            else if (ELEMENT.equals(name)) {
                Element element = new Element(parser, mContext);

                sequence.addSizeInfo(element);
            }

            skip(parser);
        }

        return sequence;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;

        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
