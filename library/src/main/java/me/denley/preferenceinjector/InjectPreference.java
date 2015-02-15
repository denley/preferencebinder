package me.denley.preferenceinjector;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Bind a field to the preference value with the specified key.
 */
@Retention(CLASS) @Target(FIELD)
public @interface InjectPreference {

    /** SharedPreferences key for the preference value to be found */
    String value();

    /**
     * Whether or not to automatically update the field's value when the preference value changes
     */
    boolean autoUpdate() default false;

}
