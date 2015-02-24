package me.denley.preferenceinjector.sample;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;

import java.util.Set;

import me.denley.preferenceinjector.InjectPreference;
import me.denley.preferenceinjector.OnPreferenceChange;
import me.denley.preferenceinjector.PreferenceDefault;
import me.denley.preferenceinjector.PreferenceInjector;
import me.denley.preferenceinjector.R;

/**
 * Created by Denley on 25/02/2015.
 */
public class SampleFragment extends Fragment {

    private static final long PREFERENCE_CHANGE_INTERVAL_MS = 500;
    private static final long PREFERENCE_CHANGE_INITIAL_WAIT_MS = 3000;

    @PreferenceDefault("integer_pref_key")
    static final int INTEGER_PREF_DEFAULT = 50;

    CheckBox booleanPreferenceDisplay;
    SeekBar integerPreferenceDisplay;

    @InjectPreference("boolean_pref_key")
    @OnPreferenceChange("boolean_pref_key")
    boolean booleanPrefValue;

    @InjectPreference("string_pref_key")
    String stringPrefValue;

    @InjectPreference("string_set_pref_key")
    Set<String> stringSetPrefValue;

    Looper preferenceChangeLooper;
    Handler handler;

    Runnable externalPreferenceChanger = new Runnable(){
        public void run(){
            changePreferenceValues();
            handler.postDelayed(externalPreferenceChanger, PREFERENCE_CHANGE_INTERVAL_MS);
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_main, container, false);
        booleanPreferenceDisplay = (CheckBox) view.findViewById(R.id.pref_boolean);
        integerPreferenceDisplay = (SeekBar) view.findViewById(R.id.pref_integer);
        PreferenceInjector.inject(this);

        startHandlerOnBackgroundThread();
        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();

        PreferenceInjector.stopListening(this);

        if(preferenceChangeLooper != null) {
            preferenceChangeLooper.quit();
        }
        super.onDestroy();
    }

    @InjectPreference("integer_pref_key")
    @OnPreferenceChange("integer_pref_key")
    void onNewValue(int newValue){
        integerPreferenceDisplay.setProgress(newValue);
    }

    @InjectPreference("boolean_pref_key")
    @OnPreferenceChange("boolean_pref_key")
    void onNewValue2(boolean newValue){
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
        handler.postDelayed(externalPreferenceChanger, PREFERENCE_CHANGE_INITIAL_WAIT_MS);
    }

    private void changePreferenceValues(){
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean booleanPrefValue = !prefs.getBoolean("boolean_pref_key", false);
        final int integerPrefValue = (prefs.getInt("integer_pref_key", 0) + 1) % 100;

        prefs.edit()
                .putBoolean("boolean_pref_key", booleanPrefValue)
                .putInt("integer_pref_key", integerPrefValue)
                .apply();
    }

}
