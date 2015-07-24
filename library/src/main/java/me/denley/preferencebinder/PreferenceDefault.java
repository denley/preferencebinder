package me.denley.preferencebinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Assign a static field to be used as a default value for a preference.
 */
@Retention(CLASS) @Target(FIELD)
public @interface PreferenceDefault {

    /** SharedPreferences key to listen for changes to its value. */
    String value();

}
