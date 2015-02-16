package me.denley.preferenceinjector.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Denley on 15/02/2015.
 */
public class PrefValueInjector {
    private final Map<String, PrefInjection> prefKeyMap = new LinkedHashMap<>();
    private final String classPackage;
    private final String className;
    private final String targetClass;
    private String parentInjector;

    PrefValueInjector(String classPackage, String className, String targetClass) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetClass = targetClass;
    }

    void addBinding(String key, PrefBinding binding) {
        getOrCreatePrefInjection(key).addPrefBinding(binding);
    }

    PrefInjection getPrefInjection(String key) {
        return prefKeyMap.get(key);
    }

    void setParentInjector(String parentInjector) {
        this.parentInjector = parentInjector;
    }

    private PrefInjection getOrCreatePrefInjection(String key) {
        PrefInjection viewId = prefKeyMap.get(key);
        if (viewId == null) {
            viewId = new PrefInjection(key);
            prefKeyMap.put(key, viewId);
        }
        return viewId;
    }

    String getFqcn() {
        return classPackage + "." + className;
    }

    String brewJava() {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code from Preference Injector. Do not modify!\n");
        builder.append("package ").append(classPackage).append(";\n\n");

        builder.append("import android.view.View;\n");
        builder.append("import android.content.Context;\n");
        builder.append("import android.content.SharedPreferences;\n");
        builder.append("import java.util.Map;\n");
        if (parentInjector == null) {
            builder.append("import me.denley.preferenceinjector.PreferenceInjector.Injector;\n");
        }
        builder.append('\n');

        builder.append("public class ").append(className);
        builder.append("<T extends ").append(targetClass).append(">");

        if (parentInjector != null) {
            builder.append(" extends ").append(parentInjector).append("<T>");
        } else {
            builder.append(" implements Injector<T>");
        }
        builder.append(" {\n");

        emitInject(builder);

        builder.append("}\n");
        return builder.toString();
    }

    private void emitInject(StringBuilder builder) {
        builder.append("  @Override ")
                .append("public void inject(final Context context, final T target, SharedPreferences source) {\n");

        // Emit a call to the superclass injector, if any.
        if (parentInjector != null) {
            builder.append("    super.inject(context, target, source);\n\n");
        }

        builder.append("    final Map<String, ?> prefsMap = source.getAll();\n");
        // Local variable in which all values will be temporarily stored.
        builder.append("    Object value;\n");

        // Loop over each view injection and emit it.
        for (PrefInjection injection : prefKeyMap.values()) {
            emitPrefInjection(builder, injection);
        }

        builder.append("  }\n");
    }

    private void emitPrefInjection(StringBuilder builder, PrefInjection injection) {
        // Example: value = prefsMap.get("this_key");
        builder.append("    value = prefsMap.get(\"")
                .append(injection.getKey())
                .append("\");\n");

        emitPrefBindings(builder, injection);
    }

    private void emitPrefBindings(StringBuilder builder, PrefInjection injection) {
        Collection<PrefBinding> bindings = injection.getPrefBindings();
        if (bindings.isEmpty()) {
            return;
        }

        // Example: target.targetVariable = (String) value;
        for (PrefBinding binding : bindings) {
            builder.append("    target.")
                    .append(binding.getName())
                    .append(" = (")
                    .append(binding.getType())
                    .append(") value;\n");
        }
    }

}
