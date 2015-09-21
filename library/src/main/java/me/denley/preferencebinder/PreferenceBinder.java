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

@SuppressWarnings("unused")
public final class PreferenceBinder {

    private PreferenceBinder(){
        throw new AssertionError("Instances are not allowed");
    }

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

        try{
            BinderUtils.Binder<Object> binder = BinderUtils.findBinderForClass(targetClass);
            if (binder != null) {
                binder.unbind(target);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to bind preferences for " + target, e);
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
            BinderUtils.Binder<Object> binder = BinderUtils.findBinderForClass(targetClass);
            if (binder != null) {
                binder.bind(context, target, prefs);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to bind preferences for " + target, e);
        }
    }

    /**
     * Saves an object to a SharedPreferences file. The object must be of a class
     * annotated with {@link PrefType}.
     *
     * @param context The Context to use to save {@link SharedPreferences} values.
     * @param prefObject The object to save.
     */
    public static void save(Context context, Object prefObject) {
        save(prefObject, PreferenceManager.getDefaultSharedPreferences(context));
    }

    /**
     * Saves an object to a SharedPreferences file. The object must be of a class
     * annotated with {@link PrefType}.
     *
     * @param context The Context to use to save {@link SharedPreferences} values.
     * @param prefObject The object to save.
     * @param prefsFileName The name of the {@link SharedPreferences} file to save to.
     */
    public static void save(Context context, Object prefObject, String prefsFileName) {
        save(prefObject, context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE));
    }

    /**
     * Saves an object to a SharedPreferences file. The object must be of a class
     * annotated with {@link PrefType}.
     *
     * This method should only be used for unit-testing purposes (by providing a
     * mocked {@link SharedPreferences} object).
     *
     * @param prefObject The object to save.
     * @param prefs The SharedPreferences object from which to load preference values.
     */
    public static void save(Object prefObject, SharedPreferences prefs) {
        Class<?> prefTypeClass = prefObject.getClass();

        try{
            BinderUtils.TypeBinder<Object> binder = BinderUtils.findTypeBinderForClass(prefTypeClass);
            if (binder != null) {
                final SharedPreferences.Editor editor = prefs.edit();

                binder.save(prefObject, editor);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    editor.apply();
                } else {
                    editor.commit();
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to save target object " + prefObject, e);
        }
    }

    /**
     * Loads an object from a SharedPreferences file. The object must be of a class
     * annotated with {@link PrefType}.
     *
     * @param context The Context from which to load the default SharedPreferences file.
     * @param type The class type of the object to load
     */
    public static <T> T load(Context context, Class<T> type) {
        return load(PreferenceManager.getDefaultSharedPreferences(context), type);
    }

    /**
     * Loads an object from a SharedPreferences file. The object must be of a class
     * annotated with {@link PrefType}.
     *
     * @param context The Context from which to load the SharedPreferences file.
     * @param prefsFileName The name of the preferences file to use to load the object.
     * @param type The class type of the object to load
     */
    public static <T> T load(Context context, String prefsFileName, Class<T> type) {
        return load(context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE), type);
    }

    /**
     * Loads an object from a SharedPreferences file. The object must be of a class
     * annotated with {@link PrefType}.
     *
     * This method should only be used for unit-testing purposes (by providing a
     * mocked {@link SharedPreferences} object).
     *
     * @param prefs The SharedPreferences object from which to load preference values.
     * @param type The class type of the object to load
     */
    public static <T> T load(SharedPreferences prefs, Class<T> type) {
        @SuppressWarnings("unchecked")
        final BinderUtils.TypeBinder<T> binder = (BinderUtils.TypeBinder<T>) BinderUtils.findTypeBinderForClass(type);

        try {
            final T target = type.newInstance();
            binder.load(target, prefs);
            return target;
        } catch (InstantiationException e) {
            throw new RuntimeException("Missing default constructor for " + type);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate " + type);
        }
    }


}
