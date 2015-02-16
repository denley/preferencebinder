package me.denley.preferenceinjector.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
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
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import me.denley.preferenceinjector.InjectPreference;
import me.denley.preferenceinjector.OnPreferenceChange;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

public class PreferenceInjectorProcessor extends AbstractProcessor {

    public static final String SUFFIX = "$$SharedPreferenceInjector";
    public static final String ANDROID_PREFIX = "android.";
    public static final String JAVA_PREFIX = "java.";



    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    private Map<TypeElement, PrefValueInjector> targetClassMap;
    private Set<String> erasedTargetNames;

    @Override public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        filer = env.getFiler();
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportTypes = new LinkedHashSet<String>();
        supportTypes.add(InjectPreference.class.getCanonicalName());
        supportTypes.add(OnPreferenceChange.class.getCanonicalName());
        return supportTypes;
    }

    @Override public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        targetClassMap = new LinkedHashMap<>();
        erasedTargetNames = new LinkedHashSet<>();

        Map<TypeElement, PrefValueInjector> targetClassMap = findAndParseAnnotations(env);

        for (Map.Entry<TypeElement, PrefValueInjector> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            PrefValueInjector injector = entry.getValue();

            try {
                JavaFileObject jfo = filer.createSourceFile(injector.getFqcn(), typeElement);
                Writer writer = jfo.openWriter();
                writer.write(injector.brewJava());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(typeElement, "Unable to write injector for type %s: %s", typeElement, e.getMessage());
            }
        }

        return true;
    }

    private Map<TypeElement, PrefValueInjector> findAndParseAnnotations(RoundEnvironment env) {
        findAndParseInjectPreferenceAnnotations(env);
        findAndParseOnPreferenceChangeAnnotations(env);
        findAndSetParentInjectors();
        return targetClassMap;
    }

    private void findAndParseInjectPreferenceAnnotations(RoundEnvironment env){
        final Set<? extends Element> injectPreferenceAnnotations = env.getElementsAnnotatedWith(InjectPreference.class);
        parseInjectPreferenceAnnotations(injectPreferenceAnnotations);
    }

    private void parseInjectPreferenceAnnotations(Set<? extends Element> injectPreferenceAnnotations) {
        for (Element element : injectPreferenceAnnotations) {
            parseInjectPreferenceOrFail(element);
        }
    }

    private void parseInjectPreferenceOrFail(Element annotatedElement) {
        try {
            parseInjectPreference(annotatedElement);
        } catch (Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));

            error(annotatedElement, "Unable to generate preference injector for @InjectPreference.\n\n%s", stackTrace);
        }
    }

    private void parseInjectPreference(Element annotatedElement) {
        if (injectPreferenceAnnotationHasError(annotatedElement)) {
            return;
        }

        // Assemble information on the injection point.
        TypeElement enclosingElement = (TypeElement) annotatedElement.getEnclosingElement();
        final InjectPreference annotation = annotatedElement.getAnnotation(InjectPreference.class);
        boolean autoUpdate = annotation.autoUpdate();
        String preferenceKey = annotation.value();
        String name = annotatedElement.getSimpleName().toString();
        String type = annotatedElement.asType().toString();

        PrefValueInjector injector = getOrCreateTargetClass(targetClassMap, enclosingElement);
        PrefBinding binding = new PrefBinding(name, type, autoUpdate);
        injector.addBinding(preferenceKey, binding);

        // Add the type-erased version to the valid injection targets set.
        erasedTargetNames.add(enclosingElement.toString());
    }

    private PrefValueInjector getOrCreateTargetClass(Map<TypeElement, PrefValueInjector> targetClassMap,
                                                TypeElement enclosingElement) {
        PrefValueInjector injector = targetClassMap.get(enclosingElement);
        if (injector == null) {
            String targetType = enclosingElement.getQualifiedName().toString();
            String classPackage = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, classPackage) + SUFFIX;

            injector = new PrefValueInjector(classPackage, className, targetType);
            targetClassMap.put(enclosingElement, injector);
        }
        return injector;
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private boolean injectPreferenceAnnotationHasError(Element element){
        return isInaccessibleViaGeneratedCode(InjectPreference.class, "fields", element)
                || isBindingInWrongPackage(InjectPreference.class, element);
    }

    private void findAndParseOnPreferenceChangeAnnotations(RoundEnvironment env) {
        final Set<? extends Element> injectPreferenceAnnotations = env.getElementsAnnotatedWith(OnPreferenceChange.class);
        parseOnPreferenceChangeAnnotations(injectPreferenceAnnotations);
    }

    private void parseOnPreferenceChangeAnnotations(Set<? extends Element> injectPreferenceAnnotations) {
        for (Element element : injectPreferenceAnnotations) {
            parseOnPreferenceChangeAnnotationOrFail(element);
        }
    }

    private void parseOnPreferenceChangeAnnotationOrFail(Element annotatedElement) {
        try {
            parseOnPreferenceChangeAnnotation(annotatedElement);
        } catch (Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));
            error(annotatedElement, "Unable to generate preference injector for @OnPreferenceChange.\n\n%s", stackTrace);
        }
    }

    private void parseOnPreferenceChangeAnnotation(Element annotatedElement) {
        if (onPreferenceChangeAnnotationHasError(annotatedElement)) {
            return;
        }

        // This should be guarded by the annotation's @Target but it's worth a check for safe casting.
        if (!(annotatedElement instanceof ExecutableElement) || annotatedElement.getKind() != METHOD) {
            throw new IllegalStateException(
                    String.format("@%s annotation must be on a method.", annotatedElement.getSimpleName()));
        }

        // Assemble information on the injection point.
        ExecutableElement executableElement = (ExecutableElement) annotatedElement;
        TypeElement enclosingElement = (TypeElement) annotatedElement.getEnclosingElement();
        OnPreferenceChange annotation = annotatedElement.getAnnotation(OnPreferenceChange.class);
        String preferenceKey = annotation.value();
        boolean initialize = annotation.initialize();
        String name = annotatedElement.getSimpleName().toString();
        List<? extends VariableElement> params = executableElement.getParameters();

        if(params.size() != 1){
            error(annotatedElement,
                    "Methods annotated with @OnPreferenceChange must have a single parameter. (%s.%s)",
                    enclosingElement.getQualifiedName(),
                    name);
            return;
        }

        PrefValueInjector injector = getOrCreateTargetClass(targetClassMap, enclosingElement);
        String paramType = params.get(0).asType().toString();
        MethodBinding binding = new MethodBinding(name, paramType, initialize);
        injector.addBinding(preferenceKey, binding);

        // Add the type-erased version to the valid injection targets set.
        erasedTargetNames.add(enclosingElement.toString());
    }

    private boolean onPreferenceChangeAnnotationHasError(Element element){
        return isInaccessibleViaGeneratedCode(OnPreferenceChange.class, "methods", element)
                || isBindingInWrongPackage(OnPreferenceChange.class, element);
    }

    private void findAndSetParentInjectors(){
        for (Map.Entry<TypeElement, PrefValueInjector> entry : targetClassMap.entrySet()) {
            findAndSetParentInjector(entry);
        }
    }

    private void findAndSetParentInjector(Map.Entry<TypeElement, PrefValueInjector> entry) {
        String parentClassFqcn = findParentFqcn(entry.getKey(), erasedTargetNames);
        if (parentClassFqcn != null) {
            entry.getValue().setParentInjector(parentClassFqcn + SUFFIX);
        }
    }

    /** Finds the parent injector type in the supplied set, if any. */
    private String findParentFqcn(TypeElement typeElement, Set<String> parents) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (parents.contains(typeElement.toString())) {
                String packageName = getPackageName(typeElement);
                return packageName + "." + getClassName(typeElement, packageName);
            }
        }
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
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
