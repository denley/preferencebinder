package me.denley.preferencebinder.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import me.denley.preferencebinder.PreferenceDefault;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

public class PrefDefaultManager {

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

    static String getDefault(String key) {
        return defaultFieldMap.get(key);
    }

    static String getDefaultType(String key) {
        return defaultTypeMap.get(key);
    }

    static void findAndParseDefaultFieldNames(ProcessingEnvironment processingEnv, RoundEnvironment env) {
        Set<? extends Element> defaultFieldNameAnnotations = env.getElementsAnnotatedWith(PreferenceDefault.class);
        parseDefaultFieldsNames(processingEnv, defaultFieldNameAnnotations);
    }

    private static void parseDefaultFieldsNames(ProcessingEnvironment processingEnv, Set<? extends Element> defaultFieldNameAnnotations){
        for (Element element : defaultFieldNameAnnotations) {
            parseDefaultFieldNameOrFail(processingEnv, element);
        }
    }

    private static void parseDefaultFieldNameOrFail(ProcessingEnvironment processingEnv, Element annotatedElement) {
        try {
            parseDefaultFieldName(processingEnv, annotatedElement);
        } catch (Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));

            error(processingEnv, annotatedElement,
                    "Unable to load default preference value from @PreferenceDefault.\n\n%s",
                    stackTrace);
        }
    }

    private static void parseDefaultFieldName(ProcessingEnvironment processingEnv, Element annotatedElement) {
        if (isAccessibleAndStatic(processingEnv, PreferenceDefault.class, annotatedElement)) {
            return;
        }

        // Assemble information on the binding point.
        TypeElement enclosingElement = (TypeElement) annotatedElement.getEnclosingElement();
        final PreferenceDefault annotation = annotatedElement.getAnnotation(PreferenceDefault.class);
        String preferenceKey = annotation.value();
        String name = annotatedElement.getSimpleName().toString();
        String type = annotatedElement.asType().toString();

        // Check that the target is a field
        if(!annotatedElement.getKind().isField()){
            error(processingEnv, annotatedElement,
                    "Only fields can be annotate with @PreferenceDefault (%s.%s)",
                    enclosingElement.getQualifiedName(),
                    name);
            return;
        }

        PrefDefaultManager.addDefault(preferenceKey, name, type, enclosingElement);
    }

    private static boolean isAccessibleAndStatic(ProcessingEnvironment processingEnv, Class<? extends Annotation> annotationClass, Element element){
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (!modifiers.contains(PUBLIC) || !modifiers.contains(STATIC)) {
            error(processingEnv, element,
                    "@%s annotated elements must have public and static modifiers. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(processingEnv, enclosingElement,
                    "@%s annotated elements may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(processingEnv, enclosingElement,
                    "@%s annotated elements may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private static void error(ProcessingEnvironment processingEnv, Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

}
