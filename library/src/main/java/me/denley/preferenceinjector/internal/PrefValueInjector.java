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

    void addBinding(String key, Binding binding) {
        getOrCreatePrefInjection(key).addBinding(binding);
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
            builder.append(" implements Injector<T>, SharedPreferences.OnSharedPreferenceChangeListener");
        }
        builder.append(" {\n");

        emitInject(builder);

        builder.append("}\n");
        return builder.toString();
    }

    private void emitInject(StringBuilder builder) {
        builder.append("  T target;\n");
        builder.append("  SharedPreferences prefs;\n\n");

        builder.append("  @Override public void inject(final Context context, final T target, SharedPreferences source) {\n");

        // Emit a call to the superclass injector, if any.
        if (parentInjector != null) {
            builder.append("    super.inject(context, target, source);\n\n");
        }

        builder.append("    this.target = target;\n");
        builder.append("    this.prefs = source;\n\n");

        builder.append("    final Map<String, ?> prefsMap = prefs.getAll();\n");
        // Local variable in which all values will be temporarily stored.
        builder.append("    Object value;\n\n");

        // Loop over each injection and emit it.
        for (PrefInjection injection : prefKeyMap.values()) {
            emitPrefInjection(builder, injection);
            builder.append("\n");
        }

        builder.append("    prefs.registerOnSharedPreferenceChangeListener(this);\n");
        builder.append("  }\n\n");

        builder.append("  @Override public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {\n");

        // Loop over each injection and emit it.
        for (PrefInjection injection : prefKeyMap.values()) {
            emitMethodBindings(builder, injection);
        }

        builder.append("\n  }\n\n");

        builder.append("  @Override public void stopListening() {\n")
                .append("    prefs.unregisterOnSharedPreferenceChangeListener(this);\n")
                .append("  }\n\n");
    }

    private void emitPrefInjection(StringBuilder builder, PrefInjection injection) {
        // Example: value = prefsMap.get("this_key");
        builder.append("    value = prefsMap.get(\"")
                .append(injection.getKey())
                .append("\");\n");

        emitPrefBindings(builder, injection);
    }

    private void emitPrefBindings(StringBuilder builder, PrefInjection injection) {
        Collection<Binding> bindings = injection.getBindings();
        if (bindings.isEmpty()) {
            return;
        }

        // Example: target.targetVariable = (String) value;
        for (Binding binding : bindings) {
            if(binding instanceof PrefBinding) {
                builder.append("    target.")
                        .append(binding.getName())
                        .append(" = (")
                        .append(binding.getType())
                        .append(") value;\n");
            }
        }
    }

    private void emitMethodBindings(StringBuilder builder, PrefInjection injection) {
        Collection<Binding> bindings = injection.getBindings();
        if (bindings.isEmpty()) {
            return;
        }

        boolean hasStartedIfBlock = false;

        // Example: target.targetMethod( (String) value);
        for (Binding binding : bindings) {
            if(binding instanceof MethodBinding) {

                if(hasStartedIfBlock) {
                    builder.append(" else ");
                } else{
                    builder.append("    ");
                    hasStartedIfBlock = true;
                }

                emitMethodBinding(builder, injection, binding);
            }
        }
    }

    private void emitMethodBinding(StringBuilder builder, PrefInjection injection, Binding binding){
        builder.append("if (key.equalsIgnoreCase(\"")
                .append(injection.getKey())
                .append("\")) {\n");

        builder.append("      final Map<String, ?> prefsMap = prefs.getAll();\n");
        builder.append("      Object value = prefsMap.get(\"")
                .append(injection.getKey())
                .append("\");\n");

        builder.append("      target.")
                .append(binding.getName())
                .append("( (")
                .append(binding.getType())
                .append(") value);\n");

        builder.append("    }");
    }

}
