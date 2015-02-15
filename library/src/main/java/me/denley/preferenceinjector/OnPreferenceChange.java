package me.denley.preferenceinjector;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Register a method to be called when a preference value changes.
 */
@Retention(CLASS) @Target(METHOD)
public @interface OnPreferenceChange {

    /** SharedPreferences key to listen for changes to its value. */
    String value();

    /**
     * Whether or not this method should be called when the listener is first initialized
     */
    boolean initialize() default false;

}
