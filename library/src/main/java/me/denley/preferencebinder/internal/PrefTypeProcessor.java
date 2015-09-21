package me.denley.preferencebinder.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import me.denley.preferencebinder.PrefType;

import static javax.tools.Diagnostic.Kind.ERROR;

public class PrefTypeProcessor {

    public static final String SUFFIX = "$$PreferenceTypeBinder";

    private Map<TypeElement, PrefTypeBinderClassFactory> targetClassMap;
    private Set<String> targetClassNames;

    private final ProcessingEnvironment processingEnvironment;
    private final RoundEnvironment env;

    public PrefTypeProcessor(final ProcessingEnvironment processingEnvironment, final RoundEnvironment env) {
        this.processingEnvironment = processingEnvironment;
        this.env = env;
    }

    public void process() {
        targetClassMap = new HashMap<TypeElement, PrefTypeBinderClassFactory>();
        targetClassNames = new LinkedHashSet<>();

        parseAnnotations();
        findAndSetParentBinders();
        writeFiles();
    }

    private void parseAnnotations() {
        for(Element prefTypeElement : env.getElementsAnnotatedWith(PrefType.class)) {
            parsePrefTypeOrFail(prefTypeElement);
        }
    }

    private void parsePrefTypeOrFail(Element annotatedElement) {
        try {
            parsePrefType(annotatedElement);
        } catch (Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));
            error(annotatedElement, "Unable to create Preference Type Binder for @PrefType annotation\n%s", stackTrace);
        }
    }

    private void parsePrefType(Element annotatedElement) {
        if (!verifyClassModifiers(annotatedElement)) {
            return;
        }

        // Assemble information on the binding point.
        final TypeElement enclosingElement = (TypeElement) annotatedElement;
        final String name = annotatedElement.getSimpleName().toString();

        final String classPackage = processingEnvironment.getElementUtils().getPackageOf(enclosingElement).getQualifiedName().toString();
        final String parentClassName = PreferenceBinderProcessor.getClassName(enclosingElement, classPackage);
        final String className = parentClassName + SUFFIX;

        PrefTypeBinderClassFactory factory = new PrefTypeBinderClassFactory(classPackage, className, name);
        targetClassMap.put(enclosingElement, factory);

        for(Element classContainedElement : enclosingElement.getEnclosedElements()) {
            if(isFieldElement(classContainedElement)) {
                verifyFieldModifiers(classContainedElement);
                factory.addBinding(new PrefTypeBinderClassFactory.PrefTypeFieldBinding(processingEnvironment, classContainedElement));
            }
        }

        // Add the type-erased version to the valid binding targets set.
        targetClassNames.add(enclosingElement.toString());
    }

    private boolean verifyClassModifiers(Element element) {
        if(element.getKind() != ElementKind.CLASS) {
            error(element, "@PrefType annotations may only be used on classes.");
            return false;
        }
        return true;
    }

    private boolean isFieldElement(Element element) {
        return element.getKind() == ElementKind.FIELD
                && !element.getModifiers().contains(Modifier.STATIC);
    }

    private void verifyFieldModifiers(Element element) {
        final Set<Modifier> modifiers = element.getModifiers();
        if(modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.PROTECTED)) {
            error(element, "@PrefType fields must not have private or protected visibility");
        } else if (modifiers.contains(Modifier.FINAL)) {
            error(element, "@PrefType fields must not be final");
        }
    }

    private void writeFiles() {
        for (Map.Entry<TypeElement, PrefTypeBinderClassFactory> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            PrefTypeBinderClassFactory binder = entry.getValue();

            try {
                JavaFileObject jfo = processingEnvironment.getFiler().createSourceFile(binder.getFqcn(), typeElement);
                Writer writer = jfo.openWriter();
                writer.write(binder.brewJava());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(typeElement, "Unable to write type binder for type %s: %s", typeElement, e.getMessage());
            }
        }
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnvironment.getMessager().printMessage(ERROR, message, element);
    }

    private void findAndSetParentBinders(){
        for (Map.Entry<TypeElement, PrefTypeBinderClassFactory> entry : targetClassMap.entrySet()) {
            findAndSetParentBinder(entry);
        }
    }

    private void findAndSetParentBinder(Map.Entry<TypeElement, PrefTypeBinderClassFactory> entry) {
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
        return packageName + "." + PreferenceBinderProcessor.getClassName(parentTypeElement, packageName);
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

    private String getPackageName(TypeElement type) {
        return processingEnvironment.getElementUtils().getPackageOf(type).getQualifiedName().toString();
    }

}
