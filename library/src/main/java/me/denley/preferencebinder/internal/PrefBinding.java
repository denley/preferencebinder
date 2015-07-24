package me.denley.preferencebinder.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class PrefBinding {
    private final String key;
    private PrefType type;
    private final String defaultStaticField;
    private final Set<Binding> initBindings = new LinkedHashSet<>();
    private final Set<Binding> listenerBindings = new LinkedHashSet<>();

    PrefBinding(String key, PrefType type, String defaultField) {
        this.key = key;
        this.type = type;
        this.defaultStaticField = defaultField;
    }

    public String getKey() {
        return key;
    }

    public PrefType getType() {
        return type;
    }

    public void setType(PrefType type) {
        this.type = type;
    }

    public String getDefaultStaticField() {
        return defaultStaticField;
    }

    public Collection<Binding> getInitBindings() {
        return initBindings;
    }

    public Collection<Binding> getListenerBindings() {
        return listenerBindings;
    }

    public void addInitBinding(Binding binding) {
        initBindings.add(binding);
    }

    public void addListenerBinding(Binding binding) {
        listenerBindings.add(binding);
    }

}
