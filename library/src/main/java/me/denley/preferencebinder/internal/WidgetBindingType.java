package me.denley.preferencebinder.internal;

public enum WidgetBindingType {

    ASSIGN(
            null,
            "%s = %s",
            null,
            null
    ),
    ACTIVATED(
            PreferenceType.BOOLEAN,
            "%s.setActivated(%s)",
            null,
            null
    ),
    ENABLED(
            PreferenceType.BOOLEAN,
            "%s.setEnabled(%s)",
            null,
            null
    ),
    SELECTED(
            PreferenceType.BOOLEAN,
            "%s.setSelected(%s)",
            null,
            null
    ),
    VISIBILITY(
            PreferenceType.BOOLEAN,
            "%s.setVisibility(%s ? android.view.View.VISIBLE : android.view.View.GONE)",
            null,
            null
    ),
    CHECKED(
            PreferenceType.BOOLEAN,

            "%s.setChecked(%s)",

            "%s.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {\n" +
            "            @Override public void onCheckedChanged(final android.widget.CompoundButton buttonView, final boolean isChecked) {\n" +
            "                prefs.edit().putBoolean(%s, isChecked).apply();\n" +
            "            }\n" +
            "        })",

            "%s.setOnCheckedChangeListener(null)"
    ),
    TEXT(
            PreferenceType.STRING,
            "%s.setText(%s)",
            null,
            null
    ),
    PROGRESS(
            PreferenceType.INTEGER,
            "%s.setProgress(%s)",
            null,
            null
    ),
    SEEKBAR_PROGRESS(
            PreferenceType.INTEGER,

            "%s.setProgress(%s)",

            "%s.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {\n" +
                    "            @Override public void onProgressChanged(final android.widget.SeekBar seekBar, final int progress, final boolean fromUser) {\n" +
                    "                prefs.edit().putInt(%s, progress).apply();\n" +
                    "            }\n" +
                    "            @Override public void onStartTrackingTouch(final android.widget.SeekBar seekBar) { }\n" +
                    "            @Override public void onStopTrackingTouch(final android.widget.SeekBar seekBar) { }\n" +
                    "        })",

            "%s.setOnSeekBarChangeListener(null)"
    ),
    MAX_PROGRESS(
            PreferenceType.INTEGER,
            "%s.setMax(%s)",
            null,
            null
    );

    public final PreferenceType preferenceType;
    public final String bindingCall;
    public final String listenerCall;
    public final String listenerUnbind;

    WidgetBindingType(PreferenceType preferenceType, String bindingCall, String listenerCall, String listenerUnbind) {
        this.preferenceType = preferenceType;
        this.bindingCall = bindingCall;
        this.listenerCall = listenerCall;
        this.listenerUnbind = listenerUnbind;
    }

}
