package me.denley.preferencebinder.internal;

public enum WidgetBindingType {

    ASSIGN(
            null,
            "%s = %s",
            null,
            null
    ),
    ACTIVATED(
            "boolean",
            "%s.setActivated(%s)",
            null,
            null
    ),
    ENABLED(
            "boolean",
            "%s.setEnabled(%s)",
            null,
            null
    ),
    SELECTED(
            "boolean",
            "%s.setSelected(%s)",
            null,
            null
    ),
    VISIBILITY(
            "boolean",
            "%s.setVisibility(%s ? android.view.View.VISIBLE : android.view.View.GONE)",
            null,
            null
    ),
    CHECKED(
            "boolean",

            "%s.setChecked(%s)",

            "%s.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {\n" +
            "            @Override public void onCheckedChanged(final android.widget.CompoundButton buttonView, final boolean isChecked) {\n" +
            "                prefs.edit().putBoolean(%s, isChecked).apply();\n" +
            "            }\n" +
            "        })",

            "%s.setOnCheckedChangeListener(null)"
    ),
    TEXT(
            "java.lang.String",
            "%s.setText(%s)",
            null,
            null
    ),
    PROGRESS(
            "int",
            "%s.setProgress(%s)",
            null,
            null
    ),
    SEEKBAR_PROGRESS(
            "int",

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
            "int",
            "%s.setMax(%s)",
            null,
            null
    );

    public final String prefType;
    public final String bindingCall;
    public final String listenerCall;
    public final String listenerUnbind;

    WidgetBindingType(String prefType, String bindingCall, String listenerCall, String listenerUnbind) {
        this.prefType = prefType;
        this.bindingCall = bindingCall;
        this.listenerCall = listenerCall;
        this.listenerUnbind = listenerUnbind;
    }

}
