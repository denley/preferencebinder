package me.denley.preferencebinder.sample;

import me.denley.preferencebinder.PreferenceDefault;

public class PreferenceDefaults {

    @PreferenceDefault("integer_pref_key")
    public static final int INTEGER_PREF_DEFAULT = 50;

    @PreferenceDefault("is_verified")
    public static final boolean IS_VERIFIED_DEFAULT = false;

}
