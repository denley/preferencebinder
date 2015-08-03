package me.denley.preferencebinder.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

import me.denley.preferencebinder.BindPref;
import me.denley.preferencebinder.PreferenceDefault;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

public class PreferenceBinderProcessor extends AbstractProcessor {

    public static final String SUFFIX = "$$SharedPreferenceBinder";
    public static final String ANDROID_PREFIX = "android.";
    public static final String JAVA_PREFIX = "java.";


    private Elements elementUtils;
    private Filer filer;

    private Map<TypeElement, BinderClassFactory> targetClassMap;
    private Set<String> targetClassNames;

    @Override public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
        filer = env.getFiler();
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportTypes = new LinkedHashSet<String>();
        supportTypes.add(BindPref.class.getCanonicalName());
        supportTypes.add(PreferenceDefault.class.getCanonicalName());
        return supportTypes;
    }

    @Override public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        targetClassMap = new LinkedHashMap<>();
        targetClassNames = new LinkedHashSet<>();
        BinderClassFactory.clearDefaults();

        Map<TypeElement, BinderClassFactory> targetClassMap = findAndParseAnnotations(env);

        for (Map.Entry<TypeElement, BinderClassFactory> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            BinderClassFactory binder = entry.getValue();

            try {
                JavaFileObject jfo = filer.createSourceFile(binder.getFqcn(), typeElement);
                Writer writer = jfo.openWriter();
                writer.write(binder.brewJava());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(typeElement, "Unable to write binder for type %s: %s", typeElement, e.getMessage());
            }
        }

        return true;
    }

    private Map<TypeElement, BinderClassFactory> findAndParseAnnotations(RoundEnvironment env) {
        findAndParseDefaultFieldNames(env);
        findAndParseBindPreferenceAnnotations(env);
        findAndSetParentBinders();
        return targetClassMap;
    }

    private void findAndParseDefaultFieldNames(RoundEnvironment env) {
        Set<? extends Element> defaultFieldNameAnnotations = env.getElementsAnnotatedWith(PreferenceDefault.class);
        parseDefaultFieldsNames(defaultFieldNameAnnotations);
    }

    private void parseDefaultFieldsNames(Set<? extends Element> defaultFieldNameAnnotations){
        for (Element element : defaultFieldNameAnnotations) {
            parseDefaultFieldNameOrFail(element);
        }
    }

    private void parseDefaultFieldNameOrFail(Element annotatedElement) {
        try {
            parseDefaultFieldName(annotatedElement);
        } catch (Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));

            error(annotatedElement, "Unable to load default preference value from @PreferenceDefault.\n\n%s", stackTrace);
        }
    }

    private void parseDefaultFieldName(Element annotatedElement) {
        if (isAccessibleAndStatic(PreferenceDefault.class, annotatedElement)) {
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
            error(annotatedElement,
                    "Only fields can be annotate with @PreferenceDefault (%s.%s)",
                    enclosingElement.getQualifiedName(),
                    name);
            return;
        }

        BinderClassFactory.addDefault(preferenceKey, name, type, enclosingElement);
    }

    private void findAndParseBindPreferenceAnnotations(RoundEnvironment env){
        final Set<? extends Element> bindPreferenceAnnotations = env.getElementsAnnotatedWith(BindPref.class);
        parseBindPreferenceAnnotations(bindPreferenceAnnotations);
    }

    private void parseBindPreferenceAnnotations(Set<? extends Element> bindPreferenceAnnotations) {
        for (Element element : bindPreferenceAnnotations) {
            parseBindPreferenceOrFail(element);
        }
    }

    private void parseBindPreferenceOrFail(Element annotatedElement) {
        try {
            parseBindPreference(annotatedElement);
        } catch (Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));

            error(annotatedElement, "Unable to generate preference binder for @BindPreference.\n\n%s", stackTrace);
        }
    }

    private void parseBindPreference(Element annotatedElement) {
        if (bindPreferenceAnnotationHasError(annotatedElement)) {
            return;
        }

        // Assemble information on the binding point.
        final TypeElement enclosingElement = (TypeElement) annotatedElement.getEnclosingElement();
        final BindPref annotation = annotatedElement.getAnnotation(BindPref.class);
        final String[] preferenceKeys = annotation.value();
        final String name = annotatedElement.getSimpleName().toString();

        final boolean isField = annotatedElement.getKind().isField();
        final ElementType elementType = isField?ElementType.FIELD:ElementType.METHOD;
        String type;

        if(!annotation.init() && !annotation.listen()) {
            error(annotatedElement, "@BindPref binding has no effect (it should either initialize or listen)", enclosingElement.getQualifiedName(), name);
            return;
        } else if(preferenceKeys.length == 0) {
            error(annotatedElement, "Missing preference key(s) for @BindPref annotation", enclosingElement.getQualifiedName(), name);
            return;
        } else if(isField){
            if(preferenceKeys.length > 1) {
                error(annotatedElement, "Multiple preference keys are only allowed for @BindPref method annotations (not fields)", enclosingElement.getQualifiedName(), name);
                return;
            }

            if(annotation.bindTo().prefType == null) {
                type = annotatedElement.asType().toString();
            } else {
                type = annotation.bindTo().prefType.getFieldTypeDef();
            }
        }else {
            // Assemble information on the binding point.
            ExecutableElement executableElement = (ExecutableElement) annotatedElement;
            List<? extends VariableElement> params = executableElement.getParameters();

            if(annotation.bindTo() != WidgetBindingType.ASSIGN) {
                error(annotatedElement, "@BindPref method annotations should not use the \"bindTo\" property", enclosingElement.getQualifiedName(), name);
                return;
            } else if(preferenceKeys.length > 1) {
                if(params.size() > 0) {
                    error(annotatedElement, "@BindPref method annotations with multiple preference keys can not have method parameters", enclosingElement.getQualifiedName(), name);
                    return;
                }
                type = null;
            }else if(params.size() != 1) {
                error(annotatedElement,
                        "Methods annotated with @BindPref must have a single parameter. (%s.%s)",
                        enclosingElement.getQualifiedName(),
                        name);
                return;
            }else {
                type = params.get(0).asType().toString();
            }
        }

        BinderClassFactory binder = getOrCreateTargetClass(enclosingElement);
        Binding binding = new Binding(name, type, elementType, annotation.bindTo());

        for(String preferenceKey : preferenceKeys) {
            if(annotation.init()) {
                binder.addInitBinding(preferenceKey, binding);
            }
            if (annotation.listen()) {
                binder.addListenerBinding(preferenceKey, binding);
            }
        }

        // Add the type-erased version to the valid binding targets set.
        targetClassNames.add(enclosingElement.toString());
    }

    private BinderClassFactory getOrCreateTargetClass(TypeElement enclosingElement) {
        BinderClassFactory binder = targetClassMap.get(enclosingElement);
        if (binder == null) {
            String targetType = enclosingElement.getQualifiedName().toString();
            String classPackage = getPackageName(enclosingElement);
            String parentClassName = getClassName(enclosingElement, classPackage);
            String className = parentClassName + SUFFIX;

            binder = new BinderClassFactory(classPackage, className, targetType);
            targetClassMap.put(enclosingElement, binder);
        }
        return binder;
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private boolean bindPreferenceAnnotationHasError(Element element){
        return isInaccessibleViaGeneratedCode(BindPref.class, element)
                || isBindingInWrongPackage(BindPref.class, element);
    }

    private void findAndSetParentBinders(){
        for (Map.Entry<TypeElement, BinderClassFactory> entry : targetClassMap.entrySet()) {
            findAndSetParentBinder(entry);
        }
    }

    private void findAndSetParentBinder(Map.Entry<TypeElement, BinderClassFactory> entry) {
        String parentClassClassName = findParentClassName(entry.getKey());
        if (parentClassClassName != null) {
            entry.getValue().setParentBinder(parentClassClassName + SUFFIX);
        }
    }

    /** Finds the parent binder type in the supplied set, if any. */
    private String findParentClassName(TypeElement typeElement) {
        final TypeElement parentTypeElement = findParentClass(typeElement);
        if(parentTypeElement == null) {
            return null;
        }

        String packageName = getPackageName(parentTypeElement);
        return packageName + "." + getClassName(parentTypeElement, packageName);
    }

    private TypeElement findParentClass(TypeElement typeElement) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (targetClassNames.contains(typeElement.toString())) {
                return typeElement;
            }
        }
    }

    private boolean isAccessibleAndStatic(Class<? extends Annotation> annotationClass, Element element){
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (!modifiers.contains(PUBLIC) || !modifiers.contains(STATIC)) {
            error(element, "@%s annotated elements must have public and static modifiers. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s annotated elements may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s annotated elements may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s annotated elements must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s annotated elements may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s annotated elements may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith(ANDROID_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith(JAVA_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

}
