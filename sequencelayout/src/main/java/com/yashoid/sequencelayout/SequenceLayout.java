package com.yashoid.sequencelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SequenceLayout extends ViewGroup implements Environment {

    private static final String METRIC_REFERENCE = "@";

    private IPageResolver mPageResolver;

    private float mPageWidth;
    private float mPageHeight;

    private int mPgAffectiveWidth;

    private Map<String, Integer> mViewIdMap = new Hashtable<>();

    public SequenceLayout(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public SequenceLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public SequenceLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SequenceLayout, defStyleAttr, 0);

        mPageWidth = a.getFloat(R.styleable.SequenceLayout_pageWidth, 0);
        mPageHeight = a.getFloat(R.styleable.SequenceLayout_pageHeight, 0);

        mPageResolver = new PageResolver(this);

        int sequencesResId = a.getResourceId(R.styleable.SequenceLayout_sequences, 0);

        if (sequencesResId != 0) {
            addSequences(sequencesResId);
        }

        a.recycle();

        mPgAffectiveWidth = getResources().getDisplayMetrics().widthPixels;
    }

    IPageResolver getPageResolver() {
        return mPageResolver;
    }

    void setPageResolver(IPageResolver pageResolver) {
        mPageResolver = pageResolver;

        requestLayout();
    }

    @Override
    public float getScreenDensity() {
        return getResources().getDisplayMetrics().density;
    }

    @Override
    public float getScreenScaledDensity() {
        return getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    public float getPageWidth() {
        return mPageWidth;
    }

    @Override
    public float getPageHeight() {
        return mPageHeight;
    }

    @Override
    public boolean readSizeInfo(String src, SizeInfo dst) {
        if (src.startsWith(METRIC_REFERENCE)) {
            dst.metric = SizeInfo.METRIC_PX;
            int resId = Integer.parseInt(src.substring(METRIC_REFERENCE.length()));
            dst.size = getResources().getDimension(resId);

            return true;
        }

        return false;
    }

    @Override
    public int getElementCount() {
        return getChildCount();
    }

    public View findViewById(String id) {
        if (id == null) {
            return null;
        }

        Integer viewId = mViewIdMap.get(id);

        if (viewId == null) {
            viewId = resolveViewId(id, getContext());

            if (viewId == 0) {
                return null;
            }

            mViewIdMap.put(id, viewId);
        }

        return findViewById(viewId);
    }

    @Override
    public boolean isElementVisible(String id) {
        View view = findViewById(id);

        return view == null || view.getVisibility() != GONE;
    }

    @Override
    public int measureElementWidth(String id, int height, int min, int max) {
        View view = findViewById(id);

        if (view == null) {
            return min;
        }

        int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        view.measure(widthSpec, heightSpec);

        return view.getMeasuredWidth();
    }

    @Override
    public int measureElementHeight(String id, int width, int min, int max) {
        View view = findViewById(id);

        if (view == null) {
            return min;
        }

        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        view.measure(widthSpec, heightSpec);

        return view.getMeasuredHeight();
    }

    @Override
    public void measureElement(String id, int minWidth, int maxWidth, int minHeight, int maxHeight, int[] measuredSize) {
        View view = findViewById(id);

        if (view == null) {
            measuredSize[0] = minWidth;
            measuredSize[1] = minHeight;
            return;
        }

        int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        view.measure(widthSpec, heightSpec);

        measuredSize[0] = view.getMeasuredWidth();
        measuredSize[1] = view.getMeasuredHeight();
    }

    public void setPageSize(float width, float height) {
        mPageWidth = width;
        mPageHeight = height;

        requestLayout();
    }

    public void setPageWidth(float width) {
        mPageWidth = width;

        requestLayout();
    }

    public void setPageHeight(float height) {
        mPageHeight = height;

        requestLayout();
    }

    public List<Sequence> readSequences(int sequencesResId) {
        try {
            XmlResourceParser parser = getResources().getXml(sequencesResId);

            return new SequenceReader(this).readSequences(parser);
        } catch (Exception e) {
            String resourceName = getResources().getResourceName(sequencesResId);

            throw new RuntimeException("Bad sequence file '" + resourceName + "'.", e);
        }
    }

    public List<Sequence> addSequences(int sequencesResId) {
        List<Sequence> sequences = readSequences(sequencesResId);

        addSequences(sequences);

        return sequences;
    }

    public void addSequences(List<Sequence> sequences) {
        for (Sequence sequence: sequences) {
            mPageResolver.onSequenceAdded(sequence);
        }

        requestLayout();
    }

    public void addSequence(Sequence sequence) {
        mPageResolver.onSequenceAdded(sequence);

        requestLayout();
    }

    public void removeSequence(Sequence sequence) {
        mPageResolver.onSequenceRemoved(sequence);

        requestLayout();
    }

    public void removeSequences(List<Sequence> sequences) {
        for (Sequence sequence: sequences) {
            mPageResolver.onSequenceRemoved(sequence);
        }

        requestLayout();
    }

    public Sequence findSequenceById(String id) {
        return mPageResolver.findSequenceById(id);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        DisplayMetrics dm = getResources().getDisplayMetrics();

        widthSize = getSize(widthMode, widthSize, dm.widthPixels);
        heightSize = getSize(heightMode, heightSize, dm.heightPixels);

        mPgAffectiveWidth = widthSize;

        mPageResolver.startResolution(widthSize, heightSize, widthMode == MeasureSpec.UNSPECIFIED, heightMode == MeasureSpec.UNSPECIFIED);

        setMeasuredDimension(mPageResolver.getResolvedWidth(), mPageResolver.getResolvedHeight());
    }

    private int getSize(int mode, int size, int maxSize) {
        switch (mode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.AT_MOST:
                return size;
            case MeasureSpec.UNSPECIFIED:
            default:
                return size > 0 ? size : maxSize;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mPgAffectiveWidth = getWidth();

        mPageResolver.positionViews();
    }

    @Override
    public void positionElement(String id, int left, int top, int right, int bottom) {
        View view = findViewById(id);

        if (view == null) {
            return;
        }

        view.measure(
                MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY)
        );

        view.layout(left, top, right, bottom);
    }

    public SequenceLayoutAnimatorCreator createLayoutAnimation() {
        if (mPageResolver instanceof SequenceLayoutAnimatorCreator) {
            throw new IllegalStateException("Can not animate the layout while it is in animation.");
        }

        return new SequenceLayoutAnimatorCreator(this);
    }

    private static int resolveViewId(String idName, Context context) {
        if (idName.startsWith("@id/")) {
            // Everything is fine.
        }
        else if (idName.startsWith("@+id/")) {
            idName = idName.replace("+", "");
        }
        else {
            idName = "@id/" + idName;
        }

        return context.getResources().getIdentifier(idName, null, context.getPackageName());
    }

}
