package com.yashoid.sequencelayout.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.yashoid.sequencelayout.SequenceLayoutAnimator;
import com.yashoid.sequencelayout.SequenceLayoutAnimatorCreator;
import com.yashoid.sequencelayout.Sequence;
import com.yashoid.sequencelayout.SequenceLayout;

import java.util.List;

public class AnimationActivity extends AppCompatActivity {

    public static Intent getIntent(Context context) {
        Intent intent = new Intent(context, AnimationActivity.class);

        return intent;
    }

    private SequenceLayout mLayout;

    private View mBoxA;
    private View mBoxB;

    private List<Sequence> mSequencesA;
    private List<Sequence> mSequencesB;

    private boolean mIsA = true;

    private SequenceLayoutAnimator mAnimator = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.animation_1);

        mLayout = findViewById(R.id.layout);

        mBoxA = findViewById(R.id.box_a);
        mBoxB = findViewById(R.id.box_b);

        mLayout.removeView(mBoxB);

        mSequencesA = mLayout.readSequences(R.xml.sequences_animation1_a);
        mSequencesB = mLayout.readSequences(R.xml.sequences_animation1_b);

        mLayout.addSequences(mSequencesA);
    }

    public void animate(View v) {
        if (mAnimator != null && mAnimator.isRunning()) {
            return;
        }

        SequenceLayoutAnimatorCreator animatorCreator = mLayout.createLayoutAnimation();

        if (mIsA) {
            animatorCreator
                    .addSequences(mSequencesB)
                    .removeSequences(mSequencesA)
                    .addView(mBoxB)
                    .removeView(mBoxA);
        }
        else {
            animatorCreator
                    .addSequences(mSequencesA)
                    .removeSequences(mSequencesB)
                    .addView(mBoxA)
                    .removeView(mBoxB);
        }

        mAnimator = animatorCreator.create();

        mAnimator.setDuration(1_200);
        mAnimator.setInterpolator(new OvershootInterpolator());
        mAnimator.start();

        mIsA = !mIsA;
    }

}
