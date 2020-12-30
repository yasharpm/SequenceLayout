package com.yashoid.sequencelayout.sample;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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
    }

}
