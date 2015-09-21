package me.denley.preferencebinder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

import me.denley.preferencebinder.internal.PreferenceBinderProcessor;

public final class PreferenceBinder {
    private PreferenceBinder(){
        throw new AssertionError("Instances are not allowed");
    }

    /** DO NOT USE: Exposed for generated code. */
    public interface Binder<T> {
        void bind(Context context, T target, SharedPreferences prefs);
        void unbind(T target);
    }

    static final Map<Class<?>, Binder<Object>> BINDERS = new LinkedHashMap<Class<?>, Binder<Object>>();
    static final Binder<Object> NOP_BINDER = new Binder<Object>() {
        @Override public void bind(Context context, Object target, SharedPreferences prefs) { }
        @Override public void unbind(Object target) {}
    };


    /**
     * Bind annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target activity for field binding.
     */
    public static void bind(Activity target) {
        bind(target, target);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Service}.
     *
     * @param target Target Service for field binding.
     */
    public static void bind(Service target) {
        bind(target, target);
    }

    /**
     * Bind annotated fields and methods in the specified {@link View}.
     *
     * @param target Target view for field binding.
     */
    public static void bind(View target) {
        bind(target.getContext(), target);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Dialog}.
     *
     * @param target Target dialog for field binding.
     */
    public static void bind(Dialog target) {
        bind(target.getContext(), target);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Fragment}.
     *
     * @param target Target fragment for field binding.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void bind(Fragment target) {
        final Context context = target.getActivity();

        if(context==null) {
            throw new IllegalStateException("Fragment must be attached to an Activity before binding");
        }

        bind(context, target);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Object}.
     *
     * @param context The Context to use to load {@link SharedPreferences} values.
     * @param target Target for field binding.
     */
    public static void bind(Context context, Object target){
        bind(context, target, PreferenceManager.getDefaultSharedPreferences(context));
    }

    /**
     * Bind annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target for field binding.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void bind(Activity target, String prefsFileName) {
        bind(target, target, prefsFileName);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Service}.
     *
     * @param target Target Service for field binding.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void bind(Service target, String prefsFileName) {
        bind(target, target);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target for field binding.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void bind(View target, String prefsFileName) {
        bind(target.getContext(), target, prefsFileName);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Activity}.
     *
     * @param target Target for field binding.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void bind(Dialog target, String prefsFileName) {
        bind(target.getContext(), target, prefsFileName);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Fragment}.
     *
     * @param target Target for field binding.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void bind(Fragment target, String prefsFileName) {
        final Context context = target.getActivity();

        if(context==null) {
            throw new IllegalStateException("Fragment must be attached to an Activity before binding");
        }

        bind(context, target, prefsFileName);
    }

    /**
     * Bind annotated fields and methods in the specified {@link Object}.
     *
     * @param context The Context to use to load {@link SharedPreferences} values.
     * @param target Target for field binding.
     * @param prefsFileName The name of the {@link android.content.SharedPreferences} file to use.
     */
    public static void bind(Context context, Object target, String prefsFileName) {
        bind(context, target, context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE));
    }

    /**
     * Stops receiving preference value changes on the given target.
     *
     * @param target Target for field binding.
     */
    public static void unbind(Object target){
        Class<?> targetClass = target.getClass();

        Binder<Object> binder = BINDERS.get(targetClass);
        if (binder != null) {
            binder.unbind(target);
        }
    }

    /**
     * Bind annotated fields and methods in the specified {@link Object}.
     *
     * This method should only be used for unit-testing purposes (by providing a
     * mocked {@link SharedPreferences} object).
     *
     * @param context The Context to use to load {@link SharedPreferences} values.
     * @param target Target for field binding.
     * @param prefs The SharedPreferences object from which to load preference values.
     */
    public static void bind(Context context, Object target, SharedPreferences prefs) {
        Class<?> targetClass = target.getClass();

        try{
            Binder<Object> binder = findBinderForClass(targetClass);
            if (binder != null) {
                binder.bind(context, target, prefs);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to bind preferences for " + target, e);
        }
    }

    private static Binder<Object> findBinderForClass(Class<?> cls) throws IllegalAccessException, InstantiationException {
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
            binder = (Binder<Object>) binderClass.newInstance();
        } catch (ClassNotFoundException e) {
            binder = findBinderForClass(cls.getSuperclass());
        }
        BINDERS.put(cls, binder);
        return binder;
    }

}
