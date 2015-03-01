package me.denley.preferenceinjector.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by Denley on 16/02/2015.
 */
public class PrefInjection {
    private final String key;
    private PrefType type;
    private final String defaultStaticField;
    private final Set<Binding> bindings = new LinkedHashSet<>();

    PrefInjection(String key, PrefType type, String defaultField) {
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
