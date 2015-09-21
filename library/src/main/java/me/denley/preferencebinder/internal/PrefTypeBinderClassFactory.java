package me.denley.preferencebinder.internal;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import me.denley.preferencebinder.PrefKey;

public class PrefTypeBinderClassFactory {

    private static final String INDENT = "    ";
    private static final String INDENT_2 = "        ";
    private static final String INDENT_3 = "            ";
    private static final String INDENT_4 = "                ";

    public static class PrefTypeFieldBinding {

        final String name;
        final PreferenceType type;
        final String key;

        public PrefTypeFieldBinding(final ProcessingEnvironment processingEnvironment, final Element fieldElement) {
            this.name = fieldElement.getSimpleName().toString();
            this.type = PreferenceType.getType(fieldElement.asType().toString());

            final String className = fieldElement.getEnclosingElement().getSimpleName().toString();
            final PrefKey prefKey = fieldElement.getAnnotation(PrefKey.class);
            key = prefKey == null ? className + "_" + name : prefKey.value();

            final String defaultType = PrefDefaultManager.getDefaultType(key);
            if(defaultType != null && PreferenceType.getType(defaultType) != type) {
                processingEnvironment.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Field type for " + className + "#" + name
                                + " does not match the @PreferenceDefault type defined for the key \"" + key + '"',
                        fieldElement);
            }


        }

    }

    private final String classPackage, className;
    private final String targetClassName;

    final List<PrefTypeFieldBinding> bindings = new LinkedList<PrefTypeFieldBinding>();
    private String parentBinder = null;

    public PrefTypeBinderClassFactory(final String classPackage, final String className, final String targetClassName) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetClassName = targetClassName;
    }

    void addBinding(PrefTypeFieldBinding binding) {
        bindings.add(binding);
    }

    void setParentBinder(final String parent) {
        parentBinder = parent;
    }

    public CharSequence getFqcn() {
        return classPackage + "." + className;
    }

    public String brewJava() {
        final StringBuilder builder = new StringBuilder();

        builder.append("package ").append(classPackage).append(";\n\n");
        builder.append("import android.content.SharedPreferences;\n");
        builder.append("import me.denley.preferencebinder.BinderUtils.TypeBinder;\n");
        builder.append("\n");
        builder.append("public class ")
                .append(targetClassName)
                .append(PrefTypeProcessor.SUFFIX)
                .append("<T extends ").append(targetClassName).append("> ");

        if(parentBinder == null) {
            builder.append("implements TypeBinder<T> {\n\n");
        } else {
            builder.append("extends ").append(parentBinder).append("<T> {\n\n");
        }

        writeLoadMethod(builder);
        writeSaveMethod(builder);
        writeContainsKeyMethod(builder);

        builder.append("}\n");
        return builder.toString();
    }

    private void writeSaveMethod(final StringBuilder builder) {
        builder.append(INDENT).append("public void save(T target, SharedPreferences.Editor editor) {\n");

        if(parentBinder != null) {
            builder.append(INDENT_2).append("super.save(target, editor);\n\n");
        }

        builder.append(INDENT_2).append("editor");
        for(PrefTypeFieldBinding binding : bindings) {
            final String key = '"' + binding.key + '"';

            builder.append(".")
                    .append(binding.type.getSetterMethod())
                    .append("(")
                    .append(key)
                    .append(", target.")
                    .append(binding.name)
                    .append(")\n")
                    .append(INDENT_4);
        }
        builder.append(";\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeLoadMethod(final StringBuilder builder) {
        builder.append(INDENT).append("public void load(T target, SharedPreferences prefs) {\n");

        if(parentBinder != null) {
            builder.append(INDENT_2).append("super.load(target, prefs);\n\n");
        }

        for(PrefTypeFieldBinding binding : bindings) {
            final String keyString = '"' + binding.key + '"';

            builder.append(INDENT_2)
                    .append("if(prefs.contains(")
                    .append(keyString)
                    .append(")) {\n");

            builder.append(INDENT_3)
                    .append("target.")
                    .append(binding.name)
                    .append(" = prefs.")
                    .append(binding.type.getSharedPrefsGetterMethod())
                    .append("(")
                    .append(keyString)
                    .append(", ")
                    .append(binding.type.getDefaultValue())
                    .append(");\n");

            builder.append(INDENT_2).append("}");

            final String defaultValue = PrefDefaultManager.getDefault(binding.key);
            if(defaultValue != null) {
                builder.append("else {\n")
                        .append(INDENT_3)
                        .append("target.")
                        .append(binding.name)
                        .append(" = ")
                        .append(defaultValue)
                        .append(";\n")
                        .append(INDENT_2)
                        .append("}");
            }

            builder.append("\n");
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void writeContainsKeyMethod(final StringBuilder builder) {
        builder.append(INDENT).append("public boolean containsKey(String key) {\n");

        for(PrefTypeFieldBinding binding : bindings) {
            final String key = '"' + binding.key + '"';

            builder.append(INDENT_2)
                    .append("if(")
                    .append(key)
                    .append(".equals(key)) {\n")
                    .append(INDENT_3)
                    .append("return true;\n")
                    .append(INDENT_2)
                    .append("}\n");
        }

        if(parentBinder != null) {
            builder.append(INDENT_2).append("return super.containsKey(key);\n");
        } else {
            builder.append(INDENT_2).append("return false;\n");
        }

        builder.append(INDENT).append("}\n\n");
    }

}
