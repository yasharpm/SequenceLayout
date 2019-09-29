package com.yashoid.sequencelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import java.util.List;

public class SequenceLayout extends ViewGroup {

    private PageResolver mPageResolver;

    private float mPgSize;

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

        mPgSize = a.getFloat(R.styleable.SequenceLayout_pgSize, 0);

        mPageResolver = new PageResolver(this);

        int sequencesResId = a.getResourceId(R.styleable.SequenceLayout_sequences, 0);

        if (sequencesResId != 0) {
            addSequences(sequencesResId);
        }

        a.recycle();
    }

    public float getPgSize() {
        return mPgSize;
    }

    public float resolveSize(float size) {
        if (mPgSize == 0) {
            return size;
        }

        return size * getWidth() / mPgSize;
    }

    public List<Sequence> addSequences(int sequencesResId) {
        try {
            XmlResourceParser parser = getResources().getXml(sequencesResId);
            List<Sequence> sequences = new SequenceReader(getContext()).readSequences(parser);

            addSequences(sequences);

            return sequences;
        } catch (Exception e) {
            throw new RuntimeException("Bad sequence file.", e);
        }
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
        mPageResolver.layoutViews();
    }

}
