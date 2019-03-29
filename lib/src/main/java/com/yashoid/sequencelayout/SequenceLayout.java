package com.yashoid.sequencelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.yashoid.sequencelayout.temp.PageResolver;
import com.yashoid.sequencelayout.temp.SequenceReader;
import com.yashoid.sequencelayout.temp.Sequence;

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
            try {
                XmlResourceParser parser = getResources().getXml(sequencesResId);
                List<Sequence> sequences = new SequenceReader(context).readSequences(parser);

                for (Sequence sequence: sequences) {
                    mPageResolver.onSequenceAdded(sequence);
                }
            } catch (Exception e) {
                throw new RuntimeException("Bad sequence file format.", e);
            }
        }

        a.recycle();
    }

    public float getPgSize() {
        return mPgSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        mPageResolver.reset();
        mPageResolver.startResolution(widthSize, heightSize, widthMode == MeasureSpec.UNSPECIFIED, heightMode == MeasureSpec.UNSPECIFIED);

        setMeasuredDimension(mPageResolver.getResolvedWidth(), mPageResolver.getResolvedHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mPageResolver.layoutViews();
    }

}
