package me.denley.preferencebinder.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.SeekBar;

import me.denley.preferencebinder.BindPref;
import me.denley.preferencebinder.PreferenceBinder;
import me.denley.preferencebinder.R;
import me.denley.preferencebinder.internal.WidgetBindingType;


public class MainActivity extends Activity {

    private static final long PREFERENCE_CHANGE_INTERVAL_MS = 500;
    private static final long PREFERENCE_CHANGE_INITIAL_WAIT_MS = 3000;


    @BindPref(value = "boolean_pref_key", bindTo = WidgetBindingType.CHECKED)
    CheckBox booleanPreferenceDisplay;

    @BindPref(value = "integer_pref_key", bindTo = WidgetBindingType.SEEKBAR_PROGRESS)
    SeekBar integerPreferenceDisplay;

    @BindPref(value = "boolean_pref_key", listen = true)
    boolean booleanPrefValue;

    Looper preferenceChangeLooper;
    Handler handler;

    Runnable externalPreferenceChanger = new Runnable(){
        public void run(){
            changePreferenceValues();
            handler.postDelayed(externalPreferenceChanger, PREFERENCE_CHANGE_INTERVAL_MS);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        booleanPreferenceDisplay = (CheckBox) findViewById(R.id.pref_boolean);
        integerPreferenceDisplay = (SeekBar) findViewById(R.id.pref_integer);
        PreferenceBinder.bind(this);
        startHandlerOnBackgroundThread();
    }

    @BindPref(value = {"integer_pref_key", "boolean_pref_key"}, init = false)
    void onNewValue3(){

    }

    private void startHandlerOnBackgroundThread(){
        new Thread(){
            public void run(){
                startHandler();
                Looper.loop();
            }
        }.start();
    }

    private void startHandler(){
        Looper.prepare();
        preferenceChangeLooper = Looper.myLooper();
        handler = new Handler(preferenceChangeLooper);
        handler.postDelayed(externalPreferenceChanger, PREFERENCE_CHANGE_INITIAL_WAIT_MS);
    }

    @SuppressLint("CommitPrefEdits")
    private void changePreferenceValues(){
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean booleanPrefValue = !prefs.getBoolean("boolean_pref_key", false);
        final int integerPrefValue = (prefs.getInt("integer_pref_key", 0) + 1) % 100;

        prefs.edit()
                .putBoolean("boolean_pref_key", booleanPrefValue)
                .putInt("integer_pref_key", integerPrefValue)
                .commit();
    }

    @Override protected void onDestroy() {
        PreferenceBinder.unbind(this);
        handler.removeCallbacks(externalPreferenceChanger);
        if(preferenceChangeLooper != null) {
            preferenceChangeLooper.quit();
        }
        super.onDestroy();
    }

}
