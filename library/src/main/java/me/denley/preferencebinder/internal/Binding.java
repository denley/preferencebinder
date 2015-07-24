package me.denley.preferencebinder.internal;

import java.lang.annotation.ElementType;

/** A single field or method binding. */
class Binding {

    private final String name;
    private final String type;
    private final ElementType elementType;
    private final WidgetBindingType bindingType;

    public Binding(String name, String type, ElementType elementType, WidgetBindingType bindingType) {
        this.name = name;
        this.type = type;
        this.elementType = elementType;
        this.bindingType = bindingType;
    }

    /** The name of the field or method */
    String getName(){
        return name;
    }

    /** The fully qualified object type of the field or method parameter (or primative) */
    String getType(){
        return type;
    }

    /** Either ElementType.FIELD or ElementType.METHOD */
    ElementType getBindingType(){
        return elementType;
    }

    WidgetBindingType getWidgetBindingType() {
        return bindingType;
    }

}
