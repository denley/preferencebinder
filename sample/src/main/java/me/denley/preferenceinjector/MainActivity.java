package me.denley.preferenceinjector;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.SeekBar;


public class MainActivity extends Activity {

    private static final long PREFERENCE_CHANGE_INTERVAL_MS = 1000;

    CheckBox booleanPreferenceDisplay;
    SeekBar integerPreferenceDisplay;

    @InjectPreference(value = "boolean_pref_key", autoUpdate = true)
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
        PreferenceInjector.inject(this);
        booleanPreferenceDisplay.setChecked(booleanPrefValue);

        startHandlerOnBackgroundThread();
    }

    @OnPreferenceChange(value = "integer_pref_key", initialize = true)
    void onNewValue(int newValue){
        integerPreferenceDisplay.setProgress(newValue);
        booleanPreferenceDisplay.setChecked(booleanPrefValue);
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
        handler.postDelayed(externalPreferenceChanger, PREFERENCE_CHANGE_INTERVAL_MS);
    }

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
        if(preferenceChangeLooper != null) {
            preferenceChangeLooper.quit();
        }
        super.onDestroy();
    }

}
