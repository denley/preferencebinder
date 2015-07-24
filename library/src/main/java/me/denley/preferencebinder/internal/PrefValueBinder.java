package me.denley.preferencebinder.internal;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class PrefValueBinder {
    private static final String INDENT = "    ";
    private static final String INDENT_2 = "        ";
    private static final String INDENT_3 = "            ";
    private static final String INDENT_4 = "                ";

    private final Map<String, PrefBinding> prefKeyMap = new LinkedHashMap<>();
    private final Map<String, String> defaultFieldMap = new LinkedHashMap<>();
    private final Map<String, String> defaultTypeMap = new LinkedHashMap<>();
    private final String classPackage;
    private final String className;
    private final String parentClassName;
    private final String targetClass;
    private String parentBinder;

    private boolean hasListenerBindings;

    PrefValueBinder(String classPackage, String className, String parentClassName, String targetClass) {
        this.classPackage = classPackage;
        this.className = className;
        this.parentClassName = parentClassName;
        this.targetClass = targetClass;
    }

    void addBinding(String key, Binding binding) {
        getOrCreatePrefBinding(key, binding.getType()).addBinding(binding);
    }

    void addDefault(String key, String defaultFieldName, String type) {
        if(defaultFieldMap.containsKey(key)){
            throw new IllegalArgumentException("Default value set more than once for \""+key+"\" in "+getFqcn());
        }

        defaultFieldMap.put(key, defaultFieldName);
        defaultTypeMap.put(key, type);
    }

    void setParentBinder(String parentBinder) {
        this.parentBinder = parentBinder;
    }

    private PrefBinding getOrCreatePrefBinding(String key, String typeDef) {
        final String defaultTypeDef = defaultTypeMap.get(key);
        if(defaultTypeDef!=null && typeDef!=null && !defaultTypeDef.equals(typeDef)){
            throw new IllegalArgumentException("Default value type ("+defaultTypeDef
                    +") does not match binding type ("+typeDef+") for \"" + key+"\"");
        }

        PrefBinding binding = prefKeyMap.get(key);
        if (binding == null) {
            binding = new PrefBinding(key, getType(key, typeDef), defaultFieldMap.get(key));
            prefKeyMap.put(key, binding);
        }else if(binding.getType()==null) {
            binding.setType(getType(key, typeDef));
        }else if(typeDef!=null && !binding.getType().getFieldTypeDef().equals(typeDef)) {
            throw new IllegalArgumentException("Inconsistent type definitions for key " + key);
        }

        return binding;
    }

    String getFqcn() {
        return classPackage + "." + className;
    }

    String brewJava() {
        hasListenerBindings = hasListenerBindings();

        StringBuilder builder = new StringBuilder();

        builder.append("// Generated code from Preference Binder. Do not modify!\n");
        builder.append("package ").append(classPackage).append(";\n\n");
        emitImports(builder);
        emitClassDefinition(builder);
        emitMemberVariables(builder);
        emitBindMethod(builder);
        emitStopListeningMethod(builder);
        emitInitializationMethod(builder);
        emitListenerMethod(builder);
        builder.append("}\n");

        return builder.toString();
    }

    private boolean hasListenerBindings(){
        for (PrefBinding bindion : prefKeyMap.values()) {
            if(!collectListenerBindings(bindion).isEmpty()) {
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
        if (parentBinder == null) {
            builder.append("import me.denley.preferencebinder.PreferenceBinder.Binder;\n");
        }
        builder.append('\n');
    }

    private void emitClassDefinition(StringBuilder builder) {
        builder.append("public class ").append(className);
        builder.append("<T extends ").append(targetClass).append(">");

        if (parentBinder != null) {
            builder.append(" extends ").append(parentBinder).append("<T>");
        } else {
            builder.append(" implements Binder<T>");
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

    private void emitBindMethod(StringBuilder builder){
        builder.append(INDENT)
                .append("@Override public void bind")
                .append("(Context context, final T target, SharedPreferences prefs) {\n");

        // Emit a call to the superclass binder, if any.
        if (parentBinder != null) {
            builder.append(INDENT_2).append("super.bind(context, target, prefs);\n\n");
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
        for (PrefBinding bindion : prefKeyMap.values()) {
            emitInitializationIfNecessary(builder, bindion);
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void emitInitializationIfNecessary(StringBuilder builder, PrefBinding bindion) {
        Collection<InitBinding> initializationBindings = collectInitializationBindings(bindion);
        if (!initializationBindings.isEmpty()) {
            emitInitialization(builder, bindion, initializationBindings);
        }
    }

    private void emitInitialization(StringBuilder builder, PrefBinding bindion, Collection<InitBinding> initializationBindings){
        builder.append(INDENT_2)
                .append("if (prefs.contains(\"")
                .append(bindion.getKey())
                .append("\")) {\n");

        if(bindion.getType()!=null) {
            builder.append(INDENT_3);
            emitInitialValueLoad(builder, bindion);
        }

        emitInitializationSetters(builder, bindion.getKey(), initializationBindings);
        builder.append(INDENT_2).append("}");
        emitDefaultInitialization(builder, bindion, initializationBindings);
        builder.append("\n\n");
    }

    private void emitDefaultInitialization(StringBuilder builder, PrefBinding bindion, Collection<InitBinding> initializationBindings){
        final String defaultFieldName = bindion.getDefaultStaticField();
        if(defaultFieldName!=null) {
            builder.append(" else {\n");
            emitInitializationSetters(builder, parentClassName + "." + defaultFieldName, initializationBindings);
            builder.append(INDENT_2).append("}");
        }
    }

    private Collection<InitBinding> collectInitializationBindings(PrefBinding bindion){
        Collection<InitBinding> initializationBindings = new LinkedHashSet<>();
        for (Binding binding : bindion.getBindings()) {
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

    private void emitInitialValueLoad(StringBuilder builder, PrefBinding bindion){
        builder.append(bindion.getType().getFieldTypeDef())
                .append(" ")
                .append(bindion.getKey())
                .append(" = prefs.")
                .append(bindion.getType().getSharedPrefsMethodName())
                .append("(\"")
                .append(bindion.getKey())
                .append("\", ")
                .append(bindion.getType().getDefaultValue())
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
            emitListenerBindions(builder);
            builder.append("\n")
                    .append(INDENT)
                    .append("};\n\n");
        }
    }

    private void emitListenerBindions(StringBuilder builder){
        boolean hasStartedIfBlock = false;

        for (PrefBinding bindion : prefKeyMap.values()) {
            Collection<PrefListenerBinding> bindings = collectListenerBindings(bindion);

            if(!bindings.isEmpty()) {
                if (hasStartedIfBlock) {
                    builder.append(" else ");
                } else {
                    builder.append(INDENT_2);
                    hasStartedIfBlock = true;
                }
                emitListenerBindionIfBlock(builder, bindion, bindings);
            }
        }
    }

    private void emitStopListeningMethod(StringBuilder builder){
        builder.append(INDENT)
                .append("@Override public void stopListening(T target) {\n");

        // Emit a call to the superclass, if any.
        if (parentBinder != null) {
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

    private void emitListenerBindionIfBlock(StringBuilder builder, PrefBinding bindion, Collection<PrefListenerBinding> bindings){
        builder.append("if (key.equals(\"")
                .append(bindion.getKey())
                .append("\")) {\n");
        emitListenerBindion(builder, bindion, bindings);
        builder.append(INDENT_2).append("}");
    }

    private void emitListenerBindion(StringBuilder builder, PrefBinding bindion, Collection<PrefListenerBinding> bindings){
        builder.append(INDENT_3)
                .append("if (prefs.contains(\"")
                .append(bindion.getKey())
                .append("\")) {\n");

        if(bindion.getType()!=null) {
            builder.append(INDENT_4);
            emitInitialValueLoad(builder, bindion);
        }

        emitListenerBindings(builder, bindion.getKey(), bindings);
        builder.append(INDENT_3).append("}");
        emitListenerDefaultAssignment(builder, bindion, bindings);
        builder.append("\n");
        emitEmptyValueListenerBindings(builder, bindings);
    }

    private void emitListenerDefaultAssignment(StringBuilder builder, PrefBinding bindion, Collection<PrefListenerBinding> bindings){
        final String defaultFieldName = bindion.getDefaultStaticField();
        if(defaultFieldName!=null) {
            builder.append(" else {\n");
            emitListenerBindings(builder, parentClassName+"."+defaultFieldName, bindings);
            builder.append(INDENT_3).append("}");
        }
    }

    private void emitListenerBindings(StringBuilder builder, String assignment, Collection<PrefListenerBinding> bindings) {
        // Update fields before method calls
        for (PrefListenerBinding binding : bindings) {
            if(binding.getBindingType() == ElementType.FIELD) {
                builder.append(INDENT_4);
                emitFieldUpdate(builder, assignment, binding);
            }
        }
        for (PrefListenerBinding binding : bindings) {
            if(binding.getBindingType() == ElementType.METHOD && binding.getType()!=null) {
                builder.append(INDENT_4);
                emitMethodCall(builder, assignment, binding);
            }
        }
    }

    private void emitEmptyValueListenerBindings(StringBuilder builder, Collection<PrefListenerBinding> bindings) {
        for (PrefListenerBinding binding : bindings) {
            if(binding.getBindingType() == ElementType.METHOD && binding.getType()==null) {
                builder.append(INDENT_3);
                emitMethodCall(builder, null, binding);
            }
        }
    }

    private Collection<PrefListenerBinding> collectListenerBindings(PrefBinding bindion){
        Collection<PrefListenerBinding> prefListenerBindings = new LinkedHashSet<>();
        for (Binding binding : bindion.getBindings()) {
            if(binding instanceof PrefListenerBinding){
                prefListenerBindings.add((PrefListenerBinding)binding);
            }
        }
        return prefListenerBindings;
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
