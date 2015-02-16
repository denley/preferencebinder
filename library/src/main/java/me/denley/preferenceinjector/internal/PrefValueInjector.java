package me.denley.preferenceinjector.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        getOrCreatePrefInjection(key, binding.getType()).addBinding(binding);
    }

    PrefInjection getPrefInjection(String key) {
        return prefKeyMap.get(key);
    }

    void setParentInjector(String parentInjector) {
        this.parentInjector = parentInjector;
    }

    private PrefInjection getOrCreatePrefInjection(String key, String typeDef) {
        PrefInjection viewId = prefKeyMap.get(key);
        if (viewId == null) {
            viewId = new PrefInjection(key, getType(key, typeDef));
            prefKeyMap.put(key, viewId);
        }else {
            if(!viewId.getType().getFieldTypeDef().equals(typeDef)){
                throw new IllegalArgumentException("Inconsistent type definitions for key "+key);
            }
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

        // Loop over each injection and emit it.
        for (PrefInjection injection : prefKeyMap.values()) {
            emitInitialization(builder, injection);
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

    private void emitInitialization(StringBuilder builder, PrefInjection injection) {
        Collection<Binding> initializationBindings = collectInitializationBindings(injection);
        if (!initializationBindings.isEmpty()) {
            emitInitialValueLoad(builder, injection);
            emitInitializationSetters(builder, injection.getKey(), initializationBindings);
        }
    }

    private Collection<Binding> collectInitializationBindings(PrefInjection injection){
        Collection<Binding> initializationBindings = new LinkedHashSet<>();
        for (Binding binding : injection.getBindings()) {
            if(isInitializationBinding(binding)){
                initializationBindings.add(binding);
            }
        }
        return initializationBindings;
    }

    private void emitInitializationSetters(StringBuilder builder, String key, Collection<Binding> initializationBindings) {
        for (Binding binding : initializationBindings) {
            emitInitializationSetter(builder, key, binding);
        }
        builder.append("\n");
    }

    private void emitInitializationSetter(StringBuilder builder, String key, Binding binding){
        builder.append(INDENT_2);
        if(binding instanceof MethodBinding) {
            emitMethodCall(builder, key, binding);
        } else {
            emitFieldUpdate(builder,key,  binding);
        }
    }

    private boolean isInitializationBinding(Binding binding){
        if(binding instanceof PrefBinding) {
            return true;
        }else if(binding instanceof MethodBinding) {
            if(((MethodBinding) binding).isInitialize()){
                return true;
            }
        }

        return false;
    }

    private void emitInitialValueLoad(StringBuilder builder, PrefInjection injection){
        // Example: value = prefsMap.get("this_key");
        builder.append(INDENT_2)
                .append(injection.getType().getFieldTypeDef())
                .append(" ")
                .append(injection.getKey())
                .append(" = prefs.")
                .append(injection.getType().getSharedPrefsMethodName())
                .append("(\"")
                .append(injection.getKey())
                .append("\", ")
                .append(injection.getType().getDefaultValue())
                .append(");\n");
    }

    private void emitListenerInjections(StringBuilder builder){
        boolean hasStartedIfBlock = false;

        for (PrefInjection injection : prefKeyMap.values()) {
            Collection<Binding> bindings = collectListenerBindings(injection);

            if(!bindings.isEmpty()) {
                if (hasStartedIfBlock) {
                    builder.append(" else ");
                } else {
                    builder.append(INDENT_2);
                    hasStartedIfBlock = true;
                }
                emitListenerInjection(builder, injection, bindings);
            }
        }
    }

    private void emitListenerInjection(StringBuilder builder, PrefInjection injection, Collection<Binding> bindings){
        builder.append("if (key.equals(\"")
                .append(injection.getKey())
                .append("\")) {\n");

        builder.append(INDENT_3)
                .append(injection.getType().getFieldTypeDef())
                .append(" ")
                .append(injection.getKey())
                .append(" = prefs.")
                .append(injection.getType().getSharedPrefsMethodName())
                .append("(\"")
                .append(injection.getKey())
                .append("\", ")
                .append(injection.getType().getDefaultValue())
                .append(");\n");

        emitListenerBindings(builder, injection.getKey(), bindings);
        builder.append(INDENT_2).append("}");
    }

    private void emitListenerBindings(StringBuilder builder, String key, Collection<Binding> bindings) {
        for (Binding binding : bindings) {
            emitListenerBinding(builder, key, binding);
        }
    }

    private void emitListenerBinding(StringBuilder builder, String key, Binding binding) {
        builder.append(INDENT_3);
        if(binding instanceof PrefBinding) {
            emitFieldUpdate(builder, key, binding);
        } else {
            emitMethodCall(builder, key, binding);
        }
    }

    private Collection<Binding> collectListenerBindings(PrefInjection injection){
        Collection<Binding> listenerBindings = new LinkedHashSet<>();
        for (Binding binding : injection.getBindings()) {
            if(isListenerBinding(binding)){
                listenerBindings.add(binding);
            }
        }
        return listenerBindings;
    }

    private boolean isListenerBinding(Binding binding){
        if(binding instanceof MethodBinding) {
            return true;
        } else if (binding instanceof PrefBinding) {
            PrefBinding asPrefBinding = (PrefBinding)binding;
            if(asPrefBinding.isAutoUpdate()){
                return true;
            }
        }
        return false;
    }

    private void emitFieldUpdate(StringBuilder builder, String key, Binding binding){
        // Example: target.targetVariable = (String) value;
        builder.append("target.")
                .append(binding.getName())
                .append(" = ")
                .append(key)
                .append(";\n");
    }

    private void emitMethodCall(StringBuilder builder, String key, Binding binding){
        // Example: target.targetMethod( (String) value);
        builder.append("target.")
                .append(binding.getName())
                .append("(")
                .append(key)
                .append(");\n");
    }

    private PrefType getType(String key, String typeDef){
        for(PrefType type:PrefType.values()){
            if(type.getFieldTypeDef().equals(typeDef)){
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid preference value type ("
                +typeDef+") for key "+key);
    }

}
