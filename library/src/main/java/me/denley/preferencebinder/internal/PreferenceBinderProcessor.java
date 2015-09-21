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
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

import me.denley.preferencebinder.BindPref;
import me.denley.preferencebinder.BindPrefType;
import me.denley.preferencebinder.PrefType;
import me.denley.preferencebinder.PreferenceDefault;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
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
        supportTypes.add(BindPrefType.class.getCanonicalName());
        supportTypes.add(PreferenceDefault.class.getCanonicalName());
        supportTypes.add(PrefType.class.getCanonicalName());
        return supportTypes;
    }

    @Override public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        targetClassMap = new LinkedHashMap<>();
        targetClassNames = new LinkedHashSet<>();
        PrefDefaultManager.clearDefaults();

        PrefDefaultManager.findAndParseDefaultFieldNames(processingEnv, env);

        final PrefTypeProcessor prefTypeProcessor = new PrefTypeProcessor(processingEnv, env);
        prefTypeProcessor.process();

        findAndParseBindPreferenceAnnotations(env);
        findAndParseBindPrefTypeAnnotations(env);
        findAndSetParentBinders();

        writeFiles();
        return true;
    }

    private void writeFiles() {
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
        if (bindPreferenceAnnotationHasError(annotatedElement, BindPref.class)) {
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

            if(annotation.bindTo().preferenceType == null) {
                type = annotatedElement.asType().toString();
            } else {
                type = annotation.bindTo().preferenceType.getFieldTypeDef();
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

    private void findAndParseBindPrefTypeAnnotations(RoundEnvironment env){
        final Set<? extends Element> bindPreferenceAnnotations = env.getElementsAnnotatedWith(BindPrefType.class);
        parseBindPrefTypeAnnotations(bindPreferenceAnnotations);
    }

    private void parseBindPrefTypeAnnotations(final Set<? extends Element> bindPreferenceAnnotations) {
        for(Element element : bindPreferenceAnnotations) {
            parseBindPrefTypeOrFail(element);
        }
    }

    private void parseBindPrefTypeOrFail(Element annotatedElement) {
        try {
            parseBindPrefType(annotatedElement);
        } catch (Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));

            error(annotatedElement, "Unable to generate preference binder for @BindPrefType.\n\n%s", stackTrace);
        }
    }

    private void parseBindPrefType(Element annotatedElement) {
        if (bindPreferenceAnnotationHasError(annotatedElement, BindPrefType.class)) {
            return;
        }

        // Assemble information on the binding point.
        final TypeElement enclosingElement = (TypeElement) annotatedElement.getEnclosingElement();
        final BindPrefType annotation = annotatedElement.getAnnotation(BindPrefType.class);
        final List<? extends TypeMirror> classes;
        try {
            annotation.value(); // this should throw
            throw new RuntimeException("Unable to get class types from annotation");
        } catch(MirroredTypesException mte) {
            classes =  mte.getTypeMirrors();
        }

        final String name = annotatedElement.getSimpleName().toString();

        final boolean isField = annotatedElement.getKind().isField();
        final ElementType elementType = isField ? ElementType.FIELD : ElementType.METHOD;
        String type;

        if(!annotation.init() && !annotation.listen()) {
            error(annotatedElement, "@BindPrefType binding has no effect (it should either initialize or listen)", enclosingElement.getQualifiedName(), name);
            return;
        } else if(isField){
            if(classes.size() > 1) {
                error(annotatedElement, "Multiple class types are only allowed for @BindPrefType method annotations (not fields)", enclosingElement.getQualifiedName(), name);
                return;
            }

            type = annotatedElement.asType().toString();
            if (classes.size() > 0) {
                if(!classes.get(0).toString().equals(type)) {
                    error(annotatedElement, "Specified type does not match field type", enclosingElement.getQualifiedName(), name);
                    return;
                }
            }
        }else {
            // Assemble information on the binding point.
            ExecutableElement executableElement = (ExecutableElement) annotatedElement;
            List<? extends VariableElement> params = executableElement.getParameters();

            if(classes.size() > 1) {
                if(params.size() > 0) {
                    error(annotatedElement, "@BindPrefType method annotations with multiple class types can not have method parameters", enclosingElement.getQualifiedName(), name);
                    return;
                }
                type = null;
            } else {
                type = params.get(0).asType().toString();
                if (classes.size() > 0) {
                    if(!classes.get(0).toString().equals(type)) {
                        error(annotatedElement, "Specified type does not match method parameter type", enclosingElement.getQualifiedName(), name);
                        return;
                    }
                }
            }
        }

        BinderClassFactory binder = getOrCreateTargetClass(enclosingElement);
        Binding binding = new Binding(name, type, elementType, WidgetBindingType.ASSIGN);

        if(classes.isEmpty()) {
            if (annotation.init()) {
                binder.addInitTypeBinding(type, binding);
            }
            if (annotation.listen()) {
                binder.addListenerTypeBinding(type, binding);
            }
        } else{
            for (TypeMirror classType : classes) {
                if (annotation.init()) {
                    binder.addInitTypeBinding(classType.toString(), binding);
                }
                if (annotation.listen()) {
                    binder.addListenerTypeBinding(classType.toString(), binding);
                }
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

    static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private boolean bindPreferenceAnnotationHasError(Element element, Class<? extends Annotation> annotationClass){
        return isInaccessibleViaGeneratedCode(annotationClass, element)
                || isBindingInWrongPackage(annotationClass, element);
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
