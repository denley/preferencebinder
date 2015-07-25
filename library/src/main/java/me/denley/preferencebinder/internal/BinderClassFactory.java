package me.denley.preferencebinder.internal;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;

public class BinderClassFactory {
    private static final String INDENT = "    ";
    private static final String INDENT_2 = "        ";
    private static final String INDENT_3 = "            ";
    private static final String INDENT_4 = "                ";

    private final static Map<String, String> defaultFieldMap = new HashMap<>();
    private final static Map<String, String> defaultTypeMap = new HashMap<>();

    static void clearDefaults() {
        defaultFieldMap.clear();
        defaultTypeMap.clear();
    }

    static void addDefault(String key, String defaultFieldName, String type, TypeElement enclosingElement) {
        if(defaultFieldMap.containsKey(key)){
            throw new IllegalArgumentException("Default value set more than once for \""+key);
        }

        final String qualifiedFieldCall = enclosingElement.getQualifiedName() + "." + defaultFieldName;

        defaultFieldMap.put(key, qualifiedFieldCall);
        defaultTypeMap.put(key, type);
    }



    private final Map<String, PrefBinding> prefKeyMap = new LinkedHashMap<>();
    private final String classPackage;
    private final String className;
    private final String targetClass;
    private String parentBinder;

    private boolean hasListenerBindings;

    BinderClassFactory(String classPackage, String className, String targetClass) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetClass = targetClass;
    }

    void addInitBinding(String key, Binding binding) {
        getOrCreatePrefBinding(key, binding.getType()).addInitBinding(binding);
    }

    void addListenerBinding(String key, Binding binding) {
        getOrCreatePrefBinding(key, binding.getType()).addListenerBinding(binding);
    }

    void setParentBinder(String parentBinder) {
        this.parentBinder = parentBinder;
    }

    String getParentBinder() {
        return parentBinder;
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

    boolean hasListenerBindings(){
        for (PrefBinding binding : prefKeyMap.values()) {
            if(!binding.getListenerBindings().isEmpty()) {
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
        builder.append(INDENT).append("private void initializeTarget(T target, final SharedPreferences prefs) {\n");
        for (PrefBinding binding : prefKeyMap.values()) {
            emitInitializationIfNecessary(builder, binding);
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void emitInitializationIfNecessary(StringBuilder builder, PrefBinding bindings) {
        Collection<Binding> initializationBindings = bindings.getInitBindings();
        if (!initializationBindings.isEmpty()) {
            emitInitialization(builder, bindings, initializationBindings);
        }

        // Bind all widget listeners
        for(Binding binding : bindings.getListenerBindings()) {
            final String bindFormat = binding.getWidgetBindingType().listenerCall;
            if(bindFormat != null) {
                builder.append(INDENT_2)
                        .append(String.format(bindFormat, "target." + binding.getName(), '"' + bindings.getKey() + '"'))
                        .append(";\n");
            }
        }

        builder.append("\n");
    }

    private void emitInitialization(StringBuilder builder, PrefBinding binding, Collection<Binding> initializationBindings){
        builder.append(INDENT_2)
                .append("if (prefs.contains(\"")
                .append(binding.getKey())
                .append("\")) {\n");

        if(binding.getType()!=null) {
            builder.append(INDENT_3);
            emitInitialValueLoad(builder, binding);
        }

        emitInitializationSetters(builder, binding.getKey(), initializationBindings);
        builder.append(INDENT_2).append("}");
        emitDefaultInitialization(builder, binding, initializationBindings);
        builder.append("\n");
    }

    private void emitDefaultInitialization(StringBuilder builder, PrefBinding binding, Collection<Binding> initializationBindings){
        final String defaultFieldName = binding.getDefaultStaticField();
        if(defaultFieldName!=null) {
            builder.append(" else {\n");
            emitInitializationSetters(builder, defaultFieldName, initializationBindings);
            builder.append(INDENT_2).append("}");
        }
    }

    private void emitInitializationSetters(StringBuilder builder, String assignment, Collection<Binding> initializationBindings) {
        // Update fields before making method calls
        for (Binding binding : initializationBindings) {
            if(binding.getBindingType() == ElementType.FIELD) {
                builder.append(INDENT_3);
                emitFieldUpdate(builder, assignment, binding);
            }
        }
        for (Binding binding : initializationBindings) {
            if(binding.getBindingType() == ElementType.METHOD) {
                builder.append(INDENT_3);
                emitMethodCall(builder, assignment, binding);
            }
        }
    }

    private void emitInitialValueLoad(StringBuilder builder, PrefBinding binding){
        builder.append(binding.getType().getFieldTypeDef())
                .append(" ")
                .append(binding.getKey())
                .append(" = prefs.")
                .append(binding.getType().getSharedPrefsMethodName())
                .append("(\"")
                .append(binding.getKey())
                .append("\", ")
                .append(binding.getType().getDefaultValue())
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
            emitListenerbindings(builder);
            builder.append("\n")
                    .append(INDENT)
                    .append("};\n\n");
        }
    }

    private void emitListenerbindings(StringBuilder builder){
        boolean hasStartedIfBlock = false;

        for (PrefBinding binding : prefKeyMap.values()) {
            Collection<Binding> bindings = binding.getListenerBindings();

            if(!bindings.isEmpty()) {
                if (hasStartedIfBlock) {
                    builder.append(" else ");
                } else {
                    builder.append(INDENT_2);
                    hasStartedIfBlock = true;
                }
                emitListenerbindingIfBlock(builder, binding, bindings);
            }
        }
    }

    private void emitStopListeningMethod(StringBuilder builder){
        builder.append(INDENT)
                .append("@Override public void unbind(T target) {\n");

        // Emit a call to the superclass, if any.
        if (parentBinder != null) {
            builder.append(INDENT_2).append("super.unbind(target);\n\n");
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

            // Un-bind all widget listeners
            for (PrefBinding prefKeyBinding : prefKeyMap.values()) {
                for(Binding binding : prefKeyBinding.getListenerBindings()) {
                    final String unbindFormat = binding.getWidgetBindingType().listenerUnbind;
                    if(unbindFormat != null) {
                        builder.append(INDENT_2)
                                .append(String.format(unbindFormat, "target." + binding.getName()))
                                .append(";\n");
                    }
                }
            }
        }

        builder.append(INDENT).append("}\n\n");
    }

    private void emitListenerbindingIfBlock(StringBuilder builder, PrefBinding binding, Collection<Binding> bindings){
        builder.append("if (key.equals(\"")
                .append(binding.getKey())
                .append("\")) {\n");
        emitListenerBinding(builder, binding, bindings);
        builder.append(INDENT_2).append("}");
    }

    private void emitListenerBinding(StringBuilder builder, PrefBinding binding, Collection<Binding> bindings) {
        // Don't bother with the if block if there's nothing to put inside it
        if(hasNonEmptyBinding(bindings)) {
            builder.append(INDENT_3)
                    .append("if (prefs.contains(\"")
                    .append(binding.getKey())
                    .append("\")) {\n");

            if (binding.getType() != null) {
                builder.append(INDENT_4);
                emitInitialValueLoad(builder, binding);
            }

            emitListenerBindings(builder, binding.getKey(), bindings);
            builder.append(INDENT_3).append("}");
            emitListenerDefaultAssignment(builder, binding, bindings);
            builder.append("\n");
        }
        emitEmptyValueListenerBindings(builder, bindings);
    }

    private boolean hasNonEmptyBinding(Collection<Binding> bindings) {
        for(Binding binding : bindings) {
            if(binding.getBindingType() != ElementType.METHOD || binding.getType() != null) {
                return true;
            }
        }
        return false;
    }

    private void emitListenerDefaultAssignment(StringBuilder builder, PrefBinding binding, Collection<Binding> bindings){
        final String defaultFieldName = binding.getDefaultStaticField();
        if(defaultFieldName!=null) {
            builder.append(" else {\n");
            emitListenerBindings(builder, defaultFieldName, bindings);
            builder.append(INDENT_3).append("}");
        }
    }

    private void emitListenerBindings(StringBuilder builder, String assignment, Collection<Binding> bindings) {
        // Update fields before method calls
        for (Binding binding : bindings) {
            if(binding.getBindingType() == ElementType.FIELD) {
                builder.append(INDENT_4);
                emitFieldUpdate(builder, assignment, binding);
            }
        }
        for (Binding binding : bindings) {
            if(binding.getBindingType() == ElementType.METHOD && binding.getType() != null) {
                builder.append(INDENT_4);
                emitMethodCall(builder, assignment, binding);
            }
        }
    }

    private void emitEmptyValueListenerBindings(StringBuilder builder, Collection<Binding> bindings) {
        for (Binding binding : bindings) {
            if(binding.getBindingType() == ElementType.METHOD && binding.getType()==null) {
                builder.append(INDENT_3);
                emitMethodCall(builder, null, binding);
            }
        }
    }

    private void emitFieldUpdate(StringBuilder builder, String assignment, Binding binding){
        final String bindingFormat = binding.getWidgetBindingType().bindingCall;
        final String targetName = "target." + binding.getName();

        builder.append(String.format(bindingFormat, targetName, assignment)).append(";\n");
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
