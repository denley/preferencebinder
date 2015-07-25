package me.denley.preferencebinder.internal;

public enum WidgetBindingType {

    ASSIGN(
            null,
            "%s = %s",
            null,
            null
    ),
    ACTIVATED(
            PrefType.BOOLEAN,
            "%s.setActivated(%s)",
            null,
            null
    ),
    ENABLED(
            PrefType.BOOLEAN,
            "%s.setEnabled(%s)",
            null,
            null
    ),
    SELECTED(
            PrefType.BOOLEAN,
            "%s.setSelected(%s)",
            null,
            null
    ),
    VISIBILITY(
            PrefType.BOOLEAN,
            "%s.setVisibility(%s ? android.view.View.VISIBLE : android.view.View.GONE)",
            null,
            null
    ),
    CHECKED(
            PrefType.BOOLEAN,

            "%s.setChecked(%s)",

            "%s.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {\n" +
            "            @Override public void onCheckedChanged(final android.widget.CompoundButton buttonView, final boolean isChecked) {\n" +
            "                prefs.edit().putBoolean(%s, isChecked).apply();\n" +
            "            }\n" +
            "        })",

            "%s.setOnCheckedChangeListener(null)"
    ),
    TEXT(
            PrefType.STRING,
            "%s.setText(%s)",
            null,
            null
    ),
    PROGRESS(
            PrefType.INTEGER,
            "%s.setProgress(%s)",
            null,
            null
    ),
    SEEKBAR_PROGRESS(
            PrefType.INTEGER,

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
            PrefType.INTEGER,
            "%s.setMax(%s)",
            null,
            null
    );

    public final PrefType prefType;
    public final String bindingCall;
    public final String listenerCall;
    public final String listenerUnbind;

    WidgetBindingType(PrefType prefType, String bindingCall, String listenerCall, String listenerUnbind) {
        this.prefType = prefType;
        this.bindingCall = bindingCall;
        this.listenerCall = listenerCall;
        this.listenerUnbind = listenerUnbind;
    }

}
