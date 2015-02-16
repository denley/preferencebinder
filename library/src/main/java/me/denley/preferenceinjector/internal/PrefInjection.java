package me.denley.preferenceinjector.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by Denley on 16/02/2015.
 */
public class PrefInjection {
    private final String key;
    private final Set<PrefBinding> bindings = new LinkedHashSet<>();

    PrefInjection(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public Collection<PrefBinding> getPrefBindings() {
        return bindings;
    }

    public void addPrefBinding(PrefBinding viewBinding) {
        bindings.add(viewBinding);
    }

}
