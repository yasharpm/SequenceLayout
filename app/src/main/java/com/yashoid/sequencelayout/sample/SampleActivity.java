package com.yashoid.sequencelayout.sample;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.yashoid.sequencelayout.SequenceLayout;

public class SampleActivity extends AppCompatActivity {

    private static final String EXTRA_LAYOUT_RES_ID = "layoutResId";

    public static Intent getIntent(Context context, int layoutResId) {
        Intent intent = new Intent(context, SampleActivity.class);

        intent.putExtra(EXTRA_LAYOUT_RES_ID, layoutResId);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        int layoutResId = intent.getIntExtra(EXTRA_LAYOUT_RES_ID, 0);

        setContentView(layoutResId);

        SequenceLayout layout = null;

        // TODO Loop break
        // TODO Animation

        // This is for the future. Too much interpolator?
//        layout.animate()
//                .addSequences(R.xml.sequences_a).interpolator(new AccelerateInterpolator())
//                .removeSequences(badSequences).interpolator(new DecelerateInterpolator())
//                .addViews(R.layout.new_views).fadingIn()
//                .removeViews(mViewA, mViewB).fadingOut()
//                .duration(200)
//                .interpolator(new LinearInterpolator())
//                .next()
//                .addSequences(R.xml.sequences_b)
//                .removeSequences(R.xml.sequences_a)
//                .duration(100)
//                .start();
    }

}
