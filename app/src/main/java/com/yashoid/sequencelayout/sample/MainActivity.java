package com.yashoid.sequencelayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sample1(View v) {
        startSampleActivity(R.layout.sample_1);
    }

    public void sample2(View v) {
        startSampleActivity(R.layout.sample_2);
    }

    public void sample3(View v) {
        startSampleActivity(R.layout.sample_3);
    }

    public void sample4(View v) {
        startActivity(ListActivity.getIntent(this));
    }

    private void startSampleActivity(int layoutResId) {
        startActivity(SampleActivity.getIntent(this, layoutResId));
    }

}
