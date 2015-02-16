package me.denley.preferenceinjector.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by Denley on 16/02/2015.
 */
public class PrefInjection {
    private final String key;
    private final PrefType type;
    private final Set<Binding> bindings = new LinkedHashSet<>();

    PrefInjection(String key, PrefType type) {
        this.key = key;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public PrefType getType() {
        return type;
    }

    public Collection<Binding> getBindings() {
        return bindings;
    }

    public void addBinding(Binding binding) {
        bindings.add(binding);
    }

}
