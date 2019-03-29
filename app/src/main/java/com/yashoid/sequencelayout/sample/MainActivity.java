package com.yashoid.sequencelayout.sample;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Factory merger 168
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getResources().getLayout(R.layout.activity_main);

//        getLayoutInflater().setFactory2();
    }

    private class MyInflator extends LayoutInflater {

        protected MyInflator(Context context) {
            super(context);
        }

        @Override
        public LayoutInflater cloneInContext(Context newContext) {
            return null;
        }

    }
}
