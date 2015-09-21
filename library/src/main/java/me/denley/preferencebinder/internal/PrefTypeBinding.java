package me.denley.preferencebinder.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.type.TypeMirror;

public class PrefTypeBinding {
    private final Set<Binding> initBindings = new LinkedHashSet<>();
    private final Set<Binding> listenerBindings = new LinkedHashSet<>();

    private final String classType;

    PrefTypeBinding(String classType) {
        this.classType = classType;
    }

    void addInitBinding(Binding binding) {
        initBindings.add(binding);
    }

    void addListenerBinding(Binding binding) {
        listenerBindings.add(binding);
    }

    Collection<Binding> getInitBindings() {
        return initBindings;
    }

    Collection<Binding> getListenerBindings() {
        return listenerBindings;
    }

    String getClassName() {
        return classType;
    }

    String getVariableName() {
        final String type = getClassName();
        return type.substring(type.lastIndexOf(".") + 1).toLowerCase();
    }

}
