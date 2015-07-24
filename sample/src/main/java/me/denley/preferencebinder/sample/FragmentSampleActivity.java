package me.denley.preferencebinder.sample;

import android.app.Activity;
import android.os.Bundle;


public class FragmentSampleActivity extends Activity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new SampleFragment())
                .commit();
    }
}
