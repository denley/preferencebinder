package me.denley.preferencebinder;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

import me.denley.preferencebinder.internal.PrefTypeProcessor;
import me.denley.preferencebinder.internal.PreferenceBinderProcessor;

/** DO NOT USE: Exposed for generated code. */
@SuppressWarnings("unused")
public class BinderUtils {

    /** DO NOT USE: Exposed for generated code. */
    public interface Binder<T> {
        void bind(Context context, T target, SharedPreferences prefs);
        void unbind(T target);
    }

    /** DO NOT USE: Exposed for generated code. */
    public interface TypeBinder<T> {
        void save(T target, SharedPreferences.Editor editor);
        void load(T target, SharedPreferences prefs);
        boolean containsKey(String key);
    }

    private static final Map<Class<?>, Binder<Object>> BINDERS = new LinkedHashMap<Class<?>, Binder<Object>>();
    private static final Binder<Object> NOP_BINDER = new Binder<Object>() {
        @Override public void bind(Context context, Object target, SharedPreferences prefs) { }
        @Override public void unbind(Object target) {}
    };

    private static final Map<Class<?>, TypeBinder<Object>> TYPE_BINDERS = new LinkedHashMap<Class<?>, TypeBinder<Object>>();


    static Binder<Object> findBinderForClass(Class<?> cls) throws IllegalAccessException, InstantiationException {
        Binder<Object> binder = BINDERS.get(cls);
        if (binder != null) {
            return binder;
        }
        String clsName = cls.getName();
        if (clsName.startsWith(PreferenceBinderProcessor.ANDROID_PREFIX) || clsName.startsWith(PreferenceBinderProcessor.JAVA_PREFIX)) {
            return NOP_BINDER;
        }
        try {
            Class<?> binderClass = Class.forName(clsName + PreferenceBinderProcessor.SUFFIX);
            //noinspection unchecked
            binder = (Binder<Object>) binderClass.newInstance();
        } catch (ClassNotFoundException e) {
            binder = findBinderForClass(cls.getSuperclass());
        }
        BINDERS.put(cls, binder);
        return binder;
    }

    /** DO NOT USE: Exposed for generated code. */
    public static TypeBinder<Object> findTypeBinderForClass(Class<?> cls) {
        try {
            return findTypeBinderForClass(cls, cls);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find binder for "+cls.getName()+". Did you annotate it with @PrefType?");
        }
    }

    private static TypeBinder<Object> findTypeBinderForClass(Class<?> cls, Class<?> originalClass) throws IllegalAccessException, InstantiationException {
        TypeBinder<Object> binder = TYPE_BINDERS.get(cls);
        if (binder != null) {
            return binder;
        }

        String clsName = cls.getName();
        if (clsName.startsWith(PreferenceBinderProcessor.ANDROID_PREFIX) || clsName.startsWith(PreferenceBinderProcessor.JAVA_PREFIX)) {
            throw new RuntimeException("You must annotate " + originalClass.getName() + " with @PrefType in order to save it to SharedPreferences");
        }
        try {
            Class<?> binderClass = Class.forName(clsName + PrefTypeProcessor.SUFFIX);
            //noinspection unchecked
            binder = (TypeBinder<Object>) binderClass.newInstance();
        } catch (ClassNotFoundException e) {
            binder = findTypeBinderForClass(cls.getSuperclass(), originalClass);
        }
        TYPE_BINDERS.put(cls, binder);
        return binder;
    }

}
