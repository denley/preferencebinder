package me.denley.preferenceinjector.internal;

import java.lang.annotation.ElementType;
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
    private static final String INDENT_4 = "                ";
    private static final String INDENT_5 = "                    ";
    private static final String INDENT_6 = "                        ";

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
        emitImports(builder);
        emitClassDefinition(builder);
        emitMemberVariables(builder);
        emitInjectMethod(builder);
        emitStopListeningMethod(builder);
        emitInitializationMethod(builder);
        emitListenerMethod(builder);
        builder.append("}\n");

        return builder.toString();
    }

    private void emitImports(StringBuilder builder){
        builder.append("import android.view.View;\n");
        builder.append("import android.content.Context;\n");
        builder.append("import android.content.SharedPreferences;\n");
        builder.append("import android.content.SharedPreferences.OnSharedPreferenceChangeListener;\n");
        builder.append("import java.util.HashMap;\n");
        if (parentInjector == null) {
            builder.append("import me.denley.preferenceinjector.PreferenceInjector.Injector;\n");
        }
        builder.append('\n');
    }

    private void emitClassDefinition(StringBuilder builder) {
        builder.append("public class ").append(className);
        builder.append("<T extends ").append(targetClass).append(">");

        if (parentInjector != null) {
            builder.append(" extends ").append(parentInjector).append("<T>");
        } else {
            builder.append(" implements Injector<T>");
        }
        builder.append(" {\n");
    }

    private void emitMemberVariables(StringBuilder builder) {
        builder.append(INDENT).append("private HashMap<T, SharedPreferences> prefsMap = new HashMap<T, SharedPreferences>();\n");
        builder.append(INDENT).append("private HashMap<T, OnSharedPreferenceChangeListener> listenerMap = new HashMap<T, OnSharedPreferenceChangeListener>();\n\n");
    }

    private void emitInjectMethod(StringBuilder builder){
        builder.append(INDENT)
                .append("@Override public void inject")
                .append("(Context context, final T target, SharedPreferences prefs) {\n");

        // Emit a call to the superclass injector, if any.
        if (parentInjector != null) {
            builder.append(INDENT_2).append("super.inject(context, target, prefs);\n\n");
        }

        // Loop over each initialization and emit it.
        builder.append(INDENT_2).append("initializeTarget(target, prefs);\n\n");

        emitListener(builder);

        builder.append(INDENT_2).append("prefsMap.put(target, prefs);\n");
        builder.append(INDENT_2).append("listenerMap.put(target, listener);\n");
        builder.append(INDENT_2).append("prefs.registerOnSharedPreferenceChangeListener(listener);\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void emitInitializationMethod(StringBuilder builder){
        builder.append(INDENT).append("private void initializeTarget(T target, SharedPreferences prefs) {\n");
        for (PrefInjection injection : prefKeyMap.values()) {
            emitInitializationIfNecessary(builder, injection);
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void emitInitializationIfNecessary(StringBuilder builder, PrefInjection injection) {
        Collection<InitBinding> initializationBindings = collectInitializationBindings(injection);
        if (!initializationBindings.isEmpty()) {
            emitInitialization(builder, injection, initializationBindings);
        }
    }

    private void emitInitialization(StringBuilder builder, PrefInjection injection, Collection<InitBinding> initializationBindings){
        builder.append(INDENT_2)
                .append("if (prefs.contains(\"")
                .append(injection.getKey())
                .append("\")) {\n");
        emitInitialValueLoad(builder, injection);
        emitInitializationSetters(builder, injection.getKey(), initializationBindings);
        builder.append(INDENT_2).append("}\n\n");
    }

    private Collection<InitBinding> collectInitializationBindings(PrefInjection injection){
        Collection<InitBinding> initializationBindings = new LinkedHashSet<>();
        for (Binding binding : injection.getBindings()) {
            if(binding instanceof InitBinding){
                initializationBindings.add((InitBinding)binding);
            }
        }
        return initializationBindings;
    }

    private void emitInitializationSetters(StringBuilder builder, String key, Collection<InitBinding> initializationBindings) {
        for (InitBinding binding : initializationBindings) {
            emitInitializationSetter(builder, key, binding);
        }
    }

    private void emitInitializationSetter(StringBuilder builder, String key, InitBinding binding){
        builder.append(INDENT_3);
        if(binding.getBindingType()== ElementType.METHOD) {
            emitMethodCall(builder, key, binding);
        } else {
            emitFieldUpdate(builder,key,  binding);
        }
    }

    private void emitInitialValueLoad(StringBuilder builder, PrefInjection injection){
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
    }

    private void emitListener(StringBuilder builder) {
        builder.append(INDENT_2)
                .append("OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {\n")
                .append(INDENT_3)
                .append("@Override public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {\n")
                .append(INDENT_4)
                .append("updateTarget(target, prefs, key);\n")
                .append(INDENT_3)
                .append("}\n")
                .append(INDENT_2)
                .append("};\n\n");
    }

    private void emitListenerMethod(StringBuilder builder) {
        builder.append(INDENT)
                .append("private void updateTarget(T target, SharedPreferences prefs, String key) {\n");
        emitListenerInjections(builder);
        builder.append("\n")
                .append(INDENT)
                .append("};\n\n");
    }

    private void emitListenerInjections(StringBuilder builder){
        boolean hasStartedIfBlock = false;

        for (PrefInjection injection : prefKeyMap.values()) {
            Collection<ListenerBinding> bindings = collectListenerBindings(injection);

            if(!bindings.isEmpty()) {
                if (hasStartedIfBlock) {
                    builder.append(" else ");
                } else {
                    builder.append(INDENT_2);
                    hasStartedIfBlock = true;
                }
                emitListenerInjectionIfBlock(builder, injection, bindings);
            }
        }
    }

    private void emitStopListeningMethod(StringBuilder builder){
        builder.append(INDENT)
                .append("@Override public void stopListening(T target) {\n");

        // Emit a call to the superclass, if any.
        if (parentInjector != null) {
            builder.append(INDENT_2).append("super.stopListening(target);\n\n");
        }

        builder.append(INDENT_2)
                .append("SharedPreferences prefs = prefsMap.remove(target);\n")
                .append(INDENT_2)
                .append("OnSharedPreferenceChangeListener listener = listenerMap.remove(target);\n")
                .append(INDENT_2)
                .append("if (prefs!=null && listener!=null) {\n")
                .append(INDENT_3)
                .append("prefs.unregisterOnSharedPreferenceChangeListener(listener);\n")
                .append(INDENT_2)
                .append("}\n")
                .append(INDENT).append("}\n\n");
    }

    private void emitListenerInjectionIfBlock(StringBuilder builder, PrefInjection injection, Collection<ListenerBinding> bindings){
        builder.append("if (key.equals(\"")
                .append(injection.getKey())
                .append("\")) {\n");
        emitListenerInjection(builder, injection, bindings);
        builder.append(INDENT_2).append("}");
    }

    private void emitListenerInjection(StringBuilder builder, PrefInjection injection, Collection<ListenerBinding> bindings){
        builder.append(INDENT_3)
                .append("if (prefs.contains(\"")
                .append(injection.getKey())
                .append("\")) {\n");

        builder.append(INDENT_4)
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
        builder.append(INDENT_3).append("}\n");
    }

    private void emitListenerBindings(StringBuilder builder, String key, Collection<ListenerBinding> bindings) {
        for (ListenerBinding binding : bindings) {
            emitListenerBinding(builder, key, binding);
        }
    }

    private void emitListenerBinding(StringBuilder builder, String key, ListenerBinding binding) {
        builder.append(INDENT_4);
        if(binding.getBindingType() == ElementType.FIELD) {
            emitFieldUpdate(builder, key, binding);
        } else {
            emitMethodCall(builder, key, binding);
        }
    }

    private Collection<ListenerBinding> collectListenerBindings(PrefInjection injection){
        Collection<ListenerBinding> listenerBindings = new LinkedHashSet<>();
        for (Binding binding : injection.getBindings()) {
            if(binding instanceof ListenerBinding){
                listenerBindings.add((ListenerBinding)binding);
            }
        }
        return listenerBindings;
    }

    private void emitFieldUpdate(StringBuilder builder, String key, Binding binding){
        builder.append("target.")
                .append(binding.getName())
                .append(" = ")
                .append(key)
                .append(";\n");
    }

    private void emitMethodCall(StringBuilder builder, String key, Binding binding){
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
