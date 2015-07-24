package me.denley.preferencebinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import me.denley.preferencebinder.internal.WidgetBindingType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Bind a field to the preference value with the specified key.
 */
@Retention(CLASS) @Target({FIELD, METHOD})
public @interface BindPref {

    /** SharedPreferences key for the preference value to be found */
    String[] value();

    /** Whether or not to initialize this field/method when binding occurs */
    boolean init() default true;

    /** Whether or not to update this field or call this method again when the preference value changes */
    boolean listen() default true;

    /** The method to use to bind to a widget */
    WidgetBindingType bindTo() default WidgetBindingType.ASSIGN;

}
