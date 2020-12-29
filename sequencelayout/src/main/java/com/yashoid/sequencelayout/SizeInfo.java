package com.yashoid.sequencelayout;

public class SizeInfo {

    public static final int SIZE_WEIGHTED = -1;

    // Static metrics
    public static final int METRIC_DP = 0;
    public static final int METRIC_SP = 1;
    public static final int METRIC_PX = 2;
    public static final int METRIC_MM = 3;
    public static final int METRIC_PW = 4;
    public static final int METRIC_PH = 5;
    public static final int METRIC_RATIO = 6;

    // Element related metrics
    public static final int METRIC_VIEW_RATIO = 7;
    public static final int METRIC_ALIGN = 8;
    public static final int METRIC_MAX = 9;

    // Wrapping metric
    public static final int METRIC_WRAP = 10;

    // Weighted metric
    public static final int METRIC_WEIGHT = 11;

    private static final String M_DP = "dp";
    private static final String M_DP_2 = "dip";
    private static final String M_SP = "sp";
    private static final String M_WEIGHT = "w";
    private static final String M_RATIO = "%";
    private static final String M_PX = "px";
    private static final String M_MM = "mm";
    private static final String M_PW = "pw";
    private static final String M_PH = "ph";
    private static final String M_WRAP = "wrap";
    private static final String M_VIEW_RATIO = "%";
    private static final String M_ALIGN = "align@";
    private static final String M_MAX = "@MAX";

    private static final float MM_PER_INCH = 25.4f;
    private static final float MM_TO_PX_RATIO = 160 / MM_PER_INCH ;

    public static void readSizeInfo(String size, SizeInfo sizeInfo, Environment environment) {
        if (size == null || size.length() == 0) {
            throw new IllegalArgumentException("Size is empty");
        }

        sizeInfo.encoded = size;

        if (size.startsWith(M_MAX)) {
            sizeInfo.metric = METRIC_MAX;

            String[] rawSizeInfoArr = size.substring(M_MAX.length() + 1, size.length() - 1).split(",");

            sizeInfo.relations = new SizeInfo[rawSizeInfoArr.length];

            for (int i = 0; i < rawSizeInfoArr.length; i++) {
                sizeInfo.relations[i] = new SizeInfo();

                try {
                    readSizeInfo(rawSizeInfoArr[i], sizeInfo.relations[i], environment);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid max syntax. Separate each size " +
                            "with a comma and no extra spaces.", e);
                }
            }
        }
        else if (size.endsWith(M_WRAP)) {
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
        else if (size.endsWith(M_PW)) {
            sizeInfo.metric = METRIC_PW;
            sizeInfo.size = readFloat(size, M_PW);
        }
        else if (size.endsWith(M_PH)) {
            sizeInfo.metric = METRIC_PH;
            sizeInfo.size = readFloat(size, M_PH);
        }
        else if (size.endsWith(M_DP)) {
            sizeInfo.metric = METRIC_PX;
            sizeInfo.size = readFloat(size, M_DP);
        }
        else if (size.endsWith(M_DP_2)) {
            sizeInfo.metric = METRIC_DP;
            sizeInfo.size = readFloat(size, M_DP_2);
        }
        else if (size.endsWith(M_SP)) {
            sizeInfo.metric = METRIC_SP;
            sizeInfo.size = readFloat(size, M_SP);
        }
        else if (size.contains(M_VIEW_RATIO)) {
            sizeInfo.metric = METRIC_VIEW_RATIO;

            int ratioPosition = size.indexOf(M_VIEW_RATIO);

            sizeInfo.relatedElementId = size.substring(ratioPosition + M_VIEW_RATIO.length());
            sizeInfo.size = Float.parseFloat(size.substring(0, ratioPosition)) / 100f;
        }
        else if (size.startsWith(M_ALIGN)) {
            sizeInfo.metric = METRIC_ALIGN;
            sizeInfo.relatedElementId = size.substring(M_ALIGN.length());
        }
        else if (environment.readSizeInfo(size, sizeInfo)) {
            // All is fine.
        }
        else {
            throw new IllegalArgumentException("Unrecognized size info '" + size + "'.");
        }
    }

    private static float readFloat(String size, String suffix) {
        try {
            return Float.parseFloat(size.substring(0, size.length() - suffix.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected a float value followed by " + suffix + ".",
                    e);
        }
    }

    public String id = null;

    public float size;
    public int metric = METRIC_WRAP;
    public String relatedElementId = null;
    public SizeInfo[] relations = null;

    private String encoded;

    public SizeInfo() {

    }

    public SizeInfo(int metric, float size) {
        this.metric = metric;
        this.size = size;
    }

    public boolean isStatic() {
        return metric == METRIC_DP || metric == METRIC_SP || metric == METRIC_PW ||
                metric == METRIC_PH || metric == METRIC_PX || metric == METRIC_RATIO ||
                metric == METRIC_MM;
    }

    public boolean isElementRelated() {
        return metric == METRIC_VIEW_RATIO || metric == METRIC_ALIGN || metric == METRIC_MAX;
    }

    public int measureStaticSize(int totalSize, SizeResolverHost sizeResolverHost) {
        switch (metric) {
            case METRIC_PX:
                return (int) size;
            case METRIC_MM:
                return (int) (size * sizeResolverHost.getScreenDensity() * MM_TO_PX_RATIO);
            case METRIC_PW:
                return (int) (size * sizeResolverHost.getPageWidthUnitSize());
            case METRIC_PH:
                return (int) (size * sizeResolverHost.getPageHeightUnitSize());
            case METRIC_RATIO:
                return (int) (size * totalSize);
            case METRIC_DP:
                return (int) (size * sizeResolverHost.getScreenDensity());
            case METRIC_SP:
                return (int) (size * sizeResolverHost.getScreenScaledDensity());
        }

        throw new RuntimeException("Metric value '" + metric + "' is not static.");
    }

    @Override
    public String toString() {
        return encoded;
    }

}
