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

    private final Map<String, PrefInjection> prefKeyMap = new LinkedHashMap<>();
    private final Map<String, String> defaultFieldMap = new LinkedHashMap<>();
    private final Map<String, String> defaultTypeMap = new LinkedHashMap<>();
    private final String classPackage;
    private final String className;
    private final String parentClassName;
    private final String targetClass;
    private String parentInjector;

    private boolean hasListenerBindings;

    PrefValueInjector(String classPackage, String className, String parentClassName, String targetClass) {
        this.classPackage = classPackage;
        this.className = className;
        this.parentClassName = parentClassName;
        this.targetClass = targetClass;
    }

    void addBinding(String key, Binding binding) {
        getOrCreatePrefInjection(key, binding.getType()).addBinding(binding);
    }

    void addDefault(String key, String defaultFieldName, String type) {
        if(defaultFieldMap.containsKey(key)){
            throw new IllegalArgumentException("Default value set more than once for \""+key+"\" in "+getFqcn());
        }

        defaultFieldMap.put(key, defaultFieldName);
        defaultTypeMap.put(key, type);
    }

    void setParentInjector(String parentInjector) {
        this.parentInjector = parentInjector;
    }

    private PrefInjection getOrCreatePrefInjection(String key, String typeDef) {
        final String defaultTypeDef = defaultTypeMap.get(key);
        if(defaultTypeDef!=null && typeDef!=null && !defaultTypeDef.equals(typeDef)){
            throw new IllegalArgumentException("Default value type ("+defaultTypeDef
                    +") does not match injection type ("+typeDef+") for \"" + key+"\"");
        }

        PrefInjection injection = prefKeyMap.get(key);
        if (injection == null) {
            injection = new PrefInjection(key, getType(key, typeDef), defaultFieldMap.get(key));
            prefKeyMap.put(key, injection);
        }else if(injection.getType()==null) {
            injection.setType(getType(key, typeDef));
        }else if(typeDef!=null && !injection.getType().getFieldTypeDef().equals(typeDef)) {
            throw new IllegalArgumentException("Inconsistent type definitions for key " + key);
        }

        return injection;
    }

    String getFqcn() {
        return classPackage + "." + className;
    }

    String brewJava() {
        hasListenerBindings = hasListenerBindings();

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

    private boolean hasListenerBindings(){
        for (PrefInjection injection : prefKeyMap.values()) {
            if(!collectListenerBindings(injection).isEmpty()) {
                return true;
            }
        }

        return false;
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
        if(hasListenerBindings) {
            builder.append(INDENT).append("private HashMap<T, SharedPreferences> prefsMap = new HashMap<T, SharedPreferences>();\n");
            builder.append(INDENT).append("private HashMap<T, OnSharedPreferenceChangeListener> listenerMap = new HashMap<T, OnSharedPreferenceChangeListener>();\n");
        }
        builder.append("\n");
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
        builder.append(INDENT_2).append("initializeTarget(target, prefs);\n");

        if(hasListenerBindings) {
            builder.append("\n");
            emitListener(builder);
            builder.append(INDENT_2).append("prefsMap.put(target, prefs);\n");
            builder.append(INDENT_2).append("listenerMap.put(target, listener);\n");
            builder.append(INDENT_2).append("prefs.registerOnSharedPreferenceChangeListener(listener);\n");
        }

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

        if(injection.getType()!=null) {
            builder.append(INDENT_3);
            emitInitialValueLoad(builder, injection);
        }

        emitInitializationSetters(builder, injection.getKey(), initializationBindings);
        builder.append(INDENT_2).append("}");
        emitDefaultInitialization(builder, injection, initializationBindings);
        builder.append("\n\n");
    }

    private void emitDefaultInitialization(StringBuilder builder, PrefInjection injection, Collection<InitBinding> initializationBindings){
        final String defaultFieldName = injection.getDefaultStaticField();
        if(defaultFieldName!=null) {
            builder.append(" else {\n");
            emitInitializationSetters(builder, parentClassName + "." + defaultFieldName, initializationBindings);
            builder.append(INDENT_2).append("}");
        }
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

    private void emitInitializationSetters(StringBuilder builder, String assignment, Collection<InitBinding> initializationBindings) {
        // Update fields before making method calls
        for (InitBinding binding : initializationBindings) {
            if(binding.getBindingType()== ElementType.FIELD) {
                builder.append(INDENT_3);
                emitFieldUpdate(builder,assignment,  binding);
            }
        }
        for (InitBinding binding : initializationBindings) {
            if(binding.getBindingType()== ElementType.METHOD) {
                builder.append(INDENT_3);
                emitMethodCall(builder, assignment, binding);
            }
        }
    }

    private void emitInitialValueLoad(StringBuilder builder, PrefInjection injection){
        builder.append(injection.getType().getFieldTypeDef())
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
        if(hasListenerBindings) {
            builder.append(INDENT)
                    .append("private void updateTarget(T target, SharedPreferences prefs, String key) {\n");
            emitListenerInjections(builder);
            builder.append("\n")
                    .append(INDENT)
                    .append("};\n\n");
        }
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

        if(hasListenerBindings) {
            builder.append(INDENT_2)
                    .append("SharedPreferences prefs = prefsMap.remove(target);\n")
                    .append(INDENT_2)
                    .append("OnSharedPreferenceChangeListener listener = listenerMap.remove(target);\n")
                    .append(INDENT_2)
                    .append("if (prefs!=null && listener!=null) {\n")
                    .append(INDENT_3)
                    .append("prefs.unregisterOnSharedPreferenceChangeListener(listener);\n")
                    .append(INDENT_2)
                    .append("}\n");
        }

        builder.append(INDENT).append("}\n\n");
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

        if(injection.getType()!=null) {
            builder.append(INDENT_4);
            emitInitialValueLoad(builder, injection);
        }

        emitListenerBindings(builder, injection.getKey(), bindings);
        builder.append(INDENT_3).append("}");
        emitListenerDefaultAssignment(builder, injection, bindings);
        builder.append("\n");
        emitEmptyValueListenerBindings(builder, bindings);
    }

    private void emitListenerDefaultAssignment(StringBuilder builder, PrefInjection injection, Collection<ListenerBinding> bindings){
        final String defaultFieldName = injection.getDefaultStaticField();
        if(defaultFieldName!=null) {
            builder.append(" else {\n");
            emitListenerBindings(builder, parentClassName+"."+defaultFieldName, bindings);
            builder.append(INDENT_3).append("}");
        }
    }

    private void emitListenerBindings(StringBuilder builder, String assignment, Collection<ListenerBinding> bindings) {
        // Update fields before method calls
        for (ListenerBinding binding : bindings) {
            if(binding.getBindingType() == ElementType.FIELD) {
                builder.append(INDENT_4);
                emitFieldUpdate(builder, assignment, binding);
            }
        }
        for (ListenerBinding binding : bindings) {
            if(binding.getBindingType() == ElementType.METHOD && binding.getType()!=null) {
                builder.append(INDENT_4);
                emitMethodCall(builder, assignment, binding);
            }
        }
    }

    private void emitEmptyValueListenerBindings(StringBuilder builder, Collection<ListenerBinding> bindings) {
        for (ListenerBinding binding : bindings) {
            if(binding.getBindingType() == ElementType.METHOD && binding.getType()==null) {
                builder.append(INDENT_3);
                emitMethodCall(builder, null, binding);
            }
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

    private void emitFieldUpdate(StringBuilder builder, String assignment, Binding binding){
        builder.append("target.")
                .append(binding.getName())
                .append(" = ")
                .append(assignment)
                .append(";\n");
    }

    private void emitMethodCall(StringBuilder builder, String assignment, Binding binding){
        builder.append("target.")
                .append(binding.getName())
                .append("(");
        if(binding.getType()!=null) {
            builder.append(assignment);
        }
        builder.append(");\n");
    }

    private PrefType getType(String key, String typeDef){
        if(typeDef==null){
            return null;
        }

        for(PrefType type:PrefType.values()){
            if(type.getFieldTypeDef().equals(typeDef)){
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid preference value type ("
                +typeDef+") for key "+key);
    }

}
