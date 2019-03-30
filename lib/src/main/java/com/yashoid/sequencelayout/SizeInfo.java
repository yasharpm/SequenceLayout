package com.yashoid.sequencelayout;

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;

public class SizeInfo {

    public static final int SIZE_WEIGHTED = -1;

    // Static metrics
    public static final int METRIC_PX = 0;
    public static final int METRIC_MM = 1;
    public static final int METRIC_PG = 2;
    public static final int METRIC_RATIO = 3;

    // Element related metrics
    public static final int METRIC_ELEMENT_RATIO = 5;
    public static final int METRIC_ALIGN = 6;

    // Wrapping metric
    public static final int METRIC_WRAP = 7;

    // Weighted metric
    public static final int METRIC_WEIGHT = 8;

    private static final String M_WEIGHT = "w";
    private static final String M_RATIO = "%";
    private static final String M_PX = "px";
    private static final String M_MM = "mm";
    private static final String M_PG = "pg";
    private static final String M_WRAP = "wrap";
    private static final String M_ELEMENT_RATIO = "%";
    private static final String M_ALIGN = "align@";

    private static final float MM_PER_INCH = 25.4f;
    private static final float MM_TO_PX_RATIO = 160 / MM_PER_INCH ;

    public static void readSizeInfo(String size, SizeInfo sizeInfo, Context context) {
        if (TextUtils.isEmpty(size)) {
            throw new RuntimeException("Size is empty");
        }

        sizeInfo.encoded = size;

        if (size.endsWith(M_WRAP)) {
            sizeInfo.metric = METRIC_WRAP;
        }
        else if (size.endsWith(M_WEIGHT)) {
            sizeInfo.metric = METRIC_WEIGHT;
            sizeInfo.size = readFloat(size, M_WEIGHT);
        }
        else if (size.endsWith(M_RATIO)) {
            sizeInfo.metric = METRIC_RATIO;
            sizeInfo.size = readFloat(size, M_RATIO) / 100f;
        }
        else if (size.endsWith(M_PX)) {
            sizeInfo.metric = METRIC_PX;
            sizeInfo.size = readFloat(size, M_PX);
        }
        else if (size.endsWith(M_MM)) {
            sizeInfo.metric = METRIC_MM;
            sizeInfo.size = readFloat(size, M_MM);
        }
        else if (size.endsWith(M_PG)) {
            sizeInfo.metric = METRIC_PG;
            sizeInfo.size = readFloat(size, M_PG);
        }
        else if (size.contains(M_ELEMENT_RATIO)) {
            sizeInfo.metric = METRIC_ELEMENT_RATIO;

            int ratioPosition = size.indexOf(M_ELEMENT_RATIO);

            String rawId = size.substring(ratioPosition + M_ELEMENT_RATIO.length());
            sizeInfo.relatedElementId = resolveViewId(rawId, context);
            sizeInfo.size = Float.parseFloat(size.substring(0, ratioPosition)) / 100f;
        }
        else if (size.startsWith(M_ALIGN)) {
            sizeInfo.metric = METRIC_ALIGN;
            sizeInfo.relatedElementId = resolveViewId(size.substring(M_ALIGN.length()), context);
        }
        else {
            throw new RuntimeException("Unrecognized size info.");
        }
    }

    private static float readFloat(String size, String suffix) {
        return Float.parseFloat(size.substring(0, size.length() - suffix.length()));
    }

    public int elementId = 0;

    public float size;
    public int metric = METRIC_WRAP;
    public int relatedElementId = 0;

    private String encoded;

    public SizeInfo() {

    }

    public SizeInfo(int metric, float size) {
        this.metric = metric;
        this.size = size;
    }

    public boolean isStatic() {
        return metric == METRIC_PG || metric == METRIC_PX || metric == METRIC_RATIO || metric == METRIC_MM;
    }

    public boolean isElementRelated() {
        return metric == METRIC_ELEMENT_RATIO || metric == METRIC_ALIGN;
    }

    public int measureStaticSize(int totalSize, PageSizeProvider pageSizeProvider) {
        switch (metric) {
            case METRIC_PX:
                return (int) size;
            case METRIC_MM:
                return (int) (size * pageSizeProvider.getScreenDensity() * MM_TO_PX_RATIO);
            case METRIC_PG:
                return (int) (size * pageSizeProvider.getPgUnitSize());
            case METRIC_RATIO:
                return (int) (size * totalSize);
        }

        throw new RuntimeException("Metric value '" + metric + "' is not static.");
    }

    @Override
    public String toString() {
        return encoded;
    }

    public static int resolveViewId(String idName, Context context) {
        if (!idName.contains("/")) {
            idName = "@id/" + idName;
        }
        else {
            idName = idName.replace("+", "");
        }

        return context.getResources().getIdentifier(idName, null, context.getPackageName());
    }

}
