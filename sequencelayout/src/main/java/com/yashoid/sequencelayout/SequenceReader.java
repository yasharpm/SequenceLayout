package com.yashoid.sequencelayout;

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

    static final String SPAN = "Span";

    private Context mContext;

    public SequenceReader(Context context) {
        mContext = context;
    }

    public List<Sequence> readSequences(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<Sequence> sequences = new ArrayList<>();

        parser.next();
        parser.next();

        try {
            parser.require(XmlPullParser.START_TAG, NS, SEQUENCES);
        } catch (XmlPullParserException e) {
            throw new SequenceSyntaxException("Sequences xml file must have a '" + SEQUENCES +
                    "' tag as root.");
        }

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
                // skip(parser);
                throw new SequenceSyntaxException("Line " + parser.getLineNumber() + ": Under " +
                        SEQUENCES + " tag only " + SEQUENCE_HORIZONTAL + " and " +
                        SEQUENCE_VERTICAL + " are allowed.");
            }
        }

        return sequences;
    }

    private Sequence readSequence(XmlPullParser parser, boolean horizontal) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, horizontal ? SEQUENCE_HORIZONTAL : SEQUENCE_VERTICAL);

        String id = null;
        String start = Sequence.SEQUENCE_EDGE_START + "@";
        String end = Sequence.SEQUENCE_EDGE_END + "@";

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
            else {
                throw new SequenceSyntaxException("Line " + parser.getLineNumber() +
                        ": Unrecognized attribute '" + attrName + "'. Expected either " +
                        SEQUENCE_ID + ", " + SEQUENCE_START + " or " + SEQUENCE_END + ".");
            }
        }

        Sequence sequence = null;

        try {
            sequence = new Sequence(id, horizontal, start, end, mContext);

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String name = parser.getName();

                if (SPAN.equals(name)) {
                    Span span = new Span(parser, horizontal, mContext);

                    sequence.addSpan(span);
                }
                else {
                    throw new SequenceSyntaxException("Line " + parser.getLineNumber() + ": " +
                            "Only " + SPAN + " tag is expected inside a sequence.");
                }

                skip(parser);
            }
        } catch (IllegalArgumentException e) {
            throw new SequenceSyntaxException("Exception at line " + parser.getLineNumber(), e);
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
