package me.denley.preferenceinjector.internal;

import java.lang.annotation.ElementType;

/** A field or method view injection binding. */
interface Binding {
    /** A description of the binding in human readable form (e.g., "field 'foo'"). */
    String getDescription();

    String getName();

    String getType();

    ElementType getBindingType();

}
