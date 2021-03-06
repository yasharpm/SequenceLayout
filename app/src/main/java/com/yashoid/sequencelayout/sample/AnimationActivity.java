package com.yashoid.sequencelayout.sample;

import android.animation.Animator;
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
import com.yashoid.sequencelayout.SwipeAnimator;

import java.util.List;

public class AnimationActivity extends AppCompatActivity implements Animator.AnimatorListener {

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

    private SequenceLayoutAnimator mNextAnimator = null;

    private SwipeAnimator mSwipeAnimator = null;

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

        mSwipeAnimator = new SwipeAnimator(this);
        mSwipeAnimator.setFullDistance(360 * getResources().getDisplayMetrics().density);

        mLayout.setOnTouchListener(mSwipeAnimator);

        prepareNextAnimator();
    }

    private void prepareNextAnimator() {
        SequenceLayoutAnimatorCreator animatorCreator = mLayout.createLayoutAnimation();

        if (mIsA) {
            animatorCreator
                    .addSequences(mSequencesB)
                    .removeSequences(mSequencesA)
                    .addView(mBoxB)
                    .removeView(mBoxA);

            mSwipeAnimator.setActiveDirection(90);
        }
        else {
            animatorCreator
                    .addSequences(mSequencesA)
                    .removeSequences(mSequencesB)
                    .addView(mBoxA)
                    .removeView(mBoxB);

            mSwipeAnimator.setActiveDirection(-90);
        }

        mNextAnimator = animatorCreator.create();

        mNextAnimator.setDuration(1_000);
        mNextAnimator.setInterpolator(new OvershootInterpolator());

        mSwipeAnimator.setAnimator(mNextAnimator);

        mNextAnimator.addListener(this);
    }

    public void animate(View v) {
        if (mNextAnimator.isRunning() || mSwipeAnimator.getState() != SwipeAnimator.STATE_IDLE) {
            return;
        }

        mNextAnimator.start();
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mIsA = !mIsA;

        prepareNextAnimator();
    }

    @Override
    public void onAnimationStart(Animator animation) { }

    @Override
    public void onAnimationCancel(Animator animation) { }

    @Override
    public void onAnimationRepeat(Animator animation) { }

}
