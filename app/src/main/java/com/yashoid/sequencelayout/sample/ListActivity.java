package com.yashoid.sequencelayout.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String ITEMS[] = {
            "Here",
            "Is",
            "A",
            "Sample",
            "Of",
            "What",
            "A",
            "List",
            "Of",
            "Sequence layout",
            "Items",
            "Look",
            "Like"
    };

    public static Intent getIntent(Context context) {
        Intent intent = new Intent(context, ListActivity.class);

        return intent;
    }

    private ListView mList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mList = findViewById(R.id.list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list, R.id.text_1);
        adapter.addAll(ITEMS);

        mList.setAdapter(adapter);
        mList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((ArrayAdapter) mList.getAdapter()).add(mList.getItemAtPosition(position));
    }

}
