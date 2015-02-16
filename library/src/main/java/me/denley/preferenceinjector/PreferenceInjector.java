package me.denley.preferenceinjector;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

import me.denley.preferenceinjector.internal.PreferenceInjectorProcessor;

import static me.denley.preferenceinjector.internal.PreferenceInjectorProcessor.ANDROID_PREFIX;
import static me.denley.preferenceinjector.internal.PreferenceInjectorProcessor.JAVA_PREFIX;

/**
 * Created by Denley on 15/02/2015.
 */
public final class PreferenceInjector {
    private PreferenceInjector(){
        throw new AssertionError("Instances are not allowed");
    }

    /** DO NOT USE: Exposed for generated code. */
    public interface Injector<T> {
        void inject(Context context, T target, SharedPreferences prefs);
    }

    static final Map<Class<?>, Injector<Object>> INJECTORS = new LinkedHashMap<Class<?>, Injector<Object>>();
    static final Injector<Object> NOP_INJECTOR = new Injector<Object>() {
        @Override public void inject(Context context, Object target, SharedPreferences prefs) { }
    };


    /**
     * Inject annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target activity for field injection.
     */
    public static void inject(Activity target) {
        inject(target, target);
    }

    /**
     * Inject annotated fields and methods in the specified {@link View}.
     *
     * @param target Target view for field injection.
     */
    public static void inject(View target) {
        inject(target.getContext(), target);
    }

    /**
     * Inject annotated fields and methods in the specified {@link Dialog}.
     *
     * @param target Target dialog for field injection.
     */
    public static void inject(Dialog target) {
        inject(target.getContext(), target);
    }

    /**
     * Inject annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target for field injection.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void inject(Activity target, String prefsFileName) {
        inject(target, target, prefsFileName);
    }

    /**
     * Inject annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target for field injection.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void inject(View target, String prefsFileName) {
        inject(target.getContext(), target, prefsFileName);
    }

    /**
     * Inject annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target for field injection.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void inject(Dialog target, String prefsFileName) {
        inject(target.getContext(), target, prefsFileName);
    }

    private static void inject(Context context, Object target, String prefsFileName){
        inject(context, target, context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE));
    }

    private static void inject(Context context, Object target){
        inject(context, target, PreferenceManager.getDefaultSharedPreferences(context));
    }

    private static void inject(Context context, Object target, SharedPreferences prefs){
        Class<?> targetClass = target.getClass();

        try{
            Injector<Object> injector = findInjectorForClass(targetClass);
            if (injector != null) {
                injector.inject(context, target, prefs);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to inject preferences for " + target, e);
        }
    }

    private static Injector<Object> findInjectorForClass(Class<?> cls) throws IllegalAccessException, InstantiationException {
        Injector<Object> injector = INJECTORS.get(cls);
        if (injector != null) {
            return injector;
        }
        String clsName = cls.getName();
        if (clsName.startsWith(ANDROID_PREFIX) || clsName.startsWith(JAVA_PREFIX)) {
            return NOP_INJECTOR;
        }
        try {
            Class<?> injectorClass = Class.forName(clsName + PreferenceInjectorProcessor.SUFFIX);
            injector = (Injector<Object>) injectorClass.newInstance();
        } catch (ClassNotFoundException e) {
            injector = findInjectorForClass(cls.getSuperclass());
        }
        INJECTORS.put(cls, injector);
        return injector;
    }

}
