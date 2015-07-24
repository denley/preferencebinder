package me.denley.preferencebinder.sample;

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

import me.denley.preferencebinder.BindPref;
import me.denley.preferencebinder.PreferenceBinder;
import me.denley.preferencebinder.R;

public class SampleFragment extends Fragment {

    private static final long PREFERENCE_CHANGE_INTERVAL_MS = 500;
    private static final long PREFERENCE_CHANGE_INITIAL_WAIT_MS = 3000;

    CheckBox booleanPreferenceDisplay;
    SeekBar integerPreferenceDisplay;

    @BindPref("boolean_pref_key")
    boolean booleanPrefValue;

    @BindPref(value = "string_pref_key", listen = false)
    String stringPrefValue;

    @BindPref(value = "string_set_pref_key", listen = false)
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
        PreferenceBinder.bind(this);

        startHandlerOnBackgroundThread();
        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();

        PreferenceBinder.unbind(this);

        if(preferenceChangeLooper != null) {
            preferenceChangeLooper.quit();
        }
        super.onDestroy();
    }

    @BindPref("integer_pref_key")
    void onNewValue(int newValue){
        integerPreferenceDisplay.setProgress(newValue);
    }

    @BindPref("boolean_pref_key")
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
