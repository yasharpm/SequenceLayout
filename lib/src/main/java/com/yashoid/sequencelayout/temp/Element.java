package com.yashoid.sequencelayout.temp;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

public class Element extends Space {

    private static final String ID = "id";

    public Element(XmlPullParser parser, Context context) {
        super(parser, context);

        if (min != null) {
            min.elementId = elementId;
        }

        if (max != null) {
            max.elementId = elementId;
        }
    }

    @Override
    protected void readAttribute(String name, String value, Context context) {
        super.readAttribute(name, value, context);

        switch (name) {
            case ID:
                elementId = resolveViewId(value, context);
                break;
        }
    }

}
