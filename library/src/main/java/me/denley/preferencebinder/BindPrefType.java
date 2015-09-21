package me.denley.preferencebinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Bind a field or method to a structured object stored in SharedPreferences
 */
@Retention(CLASS) @Target({FIELD, METHOD})
public @interface BindPrefType {

    /**
     * Class types to bind and respond to changes in. These classes should be annotated with {@link PrefType}.
     * Can be empty to infer the type from the field or method parameter
     */
    Class[] value() default {};

    /** Whether or not to initialize this field/method when binding occurs */
    boolean init() default true;

    /** Whether or not to update this field or call this method again when the preference value changes */
    boolean listen() default true;

}
