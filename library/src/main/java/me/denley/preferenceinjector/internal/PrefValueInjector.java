package me.denley.preferenceinjector.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Denley on 15/02/2015.
 */
public class PrefValueInjector {
    private static final String INDENT = "    ";
    private static final String INDENT_2 = "        ";
    private static final String INDENT_3 = "            ";

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
        builder.append(INDENT).append("T target;\n");
        builder.append(INDENT).append("SharedPreferences prefs;\n\n");

        builder.append(INDENT)
                .append("@Override public void inject(final Context context, final T target, SharedPreferences source) {\n");

        // Emit a call to the superclass injector, if any.
        if (parentInjector != null) {
            builder.append(INDENT_2)
                    .append("super.inject(context, target, source);\n\n");
        }

        builder.append(INDENT_2).append("this.target = target;\n");
        builder.append(INDENT_2).append("this.prefs = source;\n\n");

        builder.append(INDENT_2).append("final Map<String, ?> prefsMap = prefs.getAll();\n");
        // Local variable in which all values will be temporarily stored.
        builder.append(INDENT_2).append("Object value;\n\n");

        // Loop over each injection and emit it.
        for (PrefInjection injection : prefKeyMap.values()) {
            emitInitialBindings(builder, injection);
            builder.append("\n");
        }

        builder.append(INDENT_2).append("prefs.registerOnSharedPreferenceChangeListener(this);\n");
        builder.append(INDENT).append("}\n\n");

        builder.append(INDENT).append("@Override public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {\n");

        emitListenerInjections(builder);

        builder.append("\n")
                .append(INDENT)
                .append("}\n\n");

        builder.append(INDENT)
                .append("@Override public void stopListening() {\n")
                .append(INDENT_2)
                .append("prefs.unregisterOnSharedPreferenceChangeListener(this);\n")
                .append(INDENT).append("}\n\n");
    }

    private void emitInitialBindings(StringBuilder builder, PrefInjection injection) {
        Collection<Binding> bindings = injection.getBindings();
        if (bindings.isEmpty()) {
            return;
        }

        emitInitialValueLoad(builder, injection);

        for (Binding binding : bindings) {
            if(binding instanceof PrefBinding) {
                builder.append(INDENT_2);
                emitFieldUpdate(builder, binding);
            }else if(binding instanceof MethodBinding) {
                MethodBinding asMethodBinding = (MethodBinding) binding;
                if(asMethodBinding.isInitialize()){
                    builder.append(INDENT_2);
                    emitMethodCall(builder, binding);
                }
            }
        }
    }

    private void emitInitialValueLoad(StringBuilder builder, PrefInjection injection){
        // Example: value = prefsMap.get("this_key");
        builder.append(INDENT_2)
                .append("value = prefsMap.get(\"")
                .append(injection.getKey())
                .append("\");\n");
    }

    private void emitListenerInjections(StringBuilder builder){
        boolean hasStartedIfBlock = false;

        for (PrefInjection injection : prefKeyMap.values()) {
            if(hasStartedIfBlock) {
                builder.append(" else ");
            } else{
                builder.append(INDENT_2);
                hasStartedIfBlock = true;
            }

            builder.append("if (key.equals(\"")
                    .append(injection.getKey())
                    .append("\")) {\n");
            builder.append(INDENT_3)
                    .append("final Map<String, ?> prefsMap = prefs.getAll();\n");
            builder.append(INDENT_3)
                    .append("Object value = prefsMap.get(\"").append(injection.getKey()).append("\");\n");
            emitListenerBindings(builder, injection);
            builder.append(INDENT_2).append("}");
        }
    }

    private void emitListenerBindings(StringBuilder builder, PrefInjection injection) {
        Collection<Binding> bindings = injection.getBindings();

        for (Binding binding : bindings) {
            if(binding instanceof MethodBinding) {
                builder.append(INDENT_3);
                emitMethodCall(builder, binding);
            } else if (binding instanceof PrefBinding) {
                PrefBinding asPrefBinding = (PrefBinding)binding;
                if(asPrefBinding.isAutoUpdate()){
                    builder.append(INDENT_3);
                    emitFieldUpdate(builder, binding);
                }
            }
        }
    }

    private void emitFieldUpdate(StringBuilder builder, Binding binding){
        // Example: target.targetVariable = (String) value;
        builder.append("target.")
                .append(binding.getName())
                .append(" = (")
                .append(binding.getType())
                .append(") value;\n");
    }

    private void emitMethodCall(StringBuilder builder, Binding binding){
        // Example: target.targetMethod( (String) value);
        builder.append("target.")
                .append(binding.getName())
                .append("( (")
                .append(binding.getType())
                .append(") value);\n");
    }

}
