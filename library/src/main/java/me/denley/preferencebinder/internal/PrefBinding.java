package me.denley.preferencebinder.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class PrefBinding {
    private final String key;
    private PrefType type;
    private final String defaultStaticField;
    private final Set<Binding> bindings = new LinkedHashSet<>();

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

    public Collection<Binding> getBindings() {
        return bindings;
    }

    public void addBinding(Binding binding) {
        bindings.add(binding);
    }

}
