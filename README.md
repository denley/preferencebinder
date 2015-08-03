[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-PreferenceInjector-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1569)

# PreferenceBinder
A SharedPreferences binding library for Android. Using annotation processing, this library makes it easy to load SharedPreferences values and listen for changes.


How to Use
-------

#### Basic Usage
Use the `@BindPref` annotation to retrieve, initialize, and listen to changes in ("bind") preference values.

```java
public class MainActivity extends Activity {

    @BindPref("my_preference_key") String valueOfPreference;

    @BindPref("my_preference_key") void initializeForPreferenceValue(String valueOfPreference) {
        // do something with the value
        ...
    }

    @Override public void onCreate(Bundle inState) {
        PreferenceBinder.bind(this);
    }

    @Override public void onDestroy() {
        PreferenceBinder.unbind(this);
    }

}
```

As soon as `PreferenceBinder.bind()` is called, preference values are loaded from your default SharedPreferences file and assigned to the annotated fields, and passed as the parameter to annotated methods.
Whenever the preference value changes, annotated fields are re-assigned, and annotated methods are called again with the new preference value.

Be sure to match the field types and method parameter types with the type of value stored for the preference key. This is not checked at compile time, and may cause runtime exceptions.

To use a non-default `SharedPreferences` file, you can specify the name of the file, like so:
```java
PreferenceBinder.bind(this, "prefs_file_name");
```

#### Advanced Usage

You may specify more than one preference key when annotating methods with `@BindPref`.
In this case, the method will be called when the value for any one of the specified keys changes. For example:
```java
@BindPref({"show_full_names", "use_small_icons"})
void refreshList() {
    adapter.notifyDataSetChanged();
}
```
Method bindings with more than one preference key do not supply the new value of the preference. But if used in combination with field bindings,
the method will always be called after the new preference values have been assigned to any annotated fields so that they can be used inside the method call.
<br/><br/>
If you only want to initialize your preference values (and not bother listening for changes), you can do so with the `listen` flag. Altenatively, you can disable initialization with the `init` flag.
```java
@BindPref(value = "use_small_icons", listen = false)
void initUseSmallIcons(boolean useSmallIcons) {
    // Do something with the value
    // ...
}

@BindPref(value = "show_full_names", init = false)
void onShowFullNamesChanged(boolean showFullNames) {
    // Do something with the value
    // ...
}
```

#### Default Values
To specify default values for preference keys, use the `@PreferenceDefault` annotation on static field containing the default value, like so:
```java
@PreferenceDefault("my_preference_key") public static String MY_PREFERENCE_DEFAULT = "Unknown";

@BindPref("my_preference_key") void updateForValue(String valueOfPreference) {
    // do something with the value
    // ...
}
```

In the above example, `PreferenceBinder` will call `updateForValue(MY_PREFERENCE_DEFAULT)` if no value is set for `"my_preference_key"` on initialization, or if the value for the given key is removed (with "listening" enabled).

Default values apply to your entire application, so you don't need to specify them in each class. You might find it convenient to assign them all in a single utility class.

#### Widget Binding
Preference values can also be bound directly into some standard Android widgets.

For example, in the following code will automatically load the preference value for the key "sensitivity" and apply it to the `SeekBar` through its `setProgress` method.
```java
@BindPref(value = "sensitivity", bindTo = WidgetBindingType.SEEKBAR_PROGRESS)
SeekBar sensitivity;
```
In addition to loading the preference value into the widget, `PreferenceBinder` will also listen for changes to the `SeekBar`'s progress value (from user input) and save the new value back into your SharedPreferences file for the given preference key!

The following table outlines the widget binding types that are currently supported. If you would like to see a binding type included in this library, please post an issue for the feature request.

"bindTo" type | Widget type | Method called | Saves user changes?
-------- | -------- | -------- | --------
ASSIGN (default) | - | = | no
ACTIVATED | View | setActivated | no
ENABLED | View | setEnabled | no
SELECTED | View | setSelected | no
VISIBILITY | View | setVisibility (View.VISIBLE or View.GONE) | no
CHECKED | CompoundButton | setChecked | yes
TEXT | TextView | setText | no
SEEKBAR_PROGRESS | SeekBar | setProgress | yes
PROGRESS | ProgressBar | setProgress | no
MAX_PROGRESS | ProgressBar | setMax | no

Build Configuration
--------

Add the following line to the gradle dependencies for your module.
```groovy
compile 'me.denley.preferenceinjector:PreferenceInjector:3.0.2'
```

If you are using any other annotation processors in your application (e.g. Dagger, ButterKnife, etc.) then you will also need to add the following to your module's build.gradle file:
```groovy
android {
    packagingOptions {
        exclude 'META-INF/services/javax.annotation.processing.Processor'
    }
}
```

ProGuard
--------

When using ProGuard, you need to specify that generated classes should be kept, and that annotated fields and methods should not be renamed. To achieve these criteria, the following lines can be added to your ProGuard configuration:

```
-keep class me.denley.preferencebinder.** { *; }
-dontwarn me.denley.preferencebinder.internal.**
-keep class **$$SharedPreferenceBinder { *; }

-keepclasseswithmembernames class * {
    @me.denley.preferencebinder.* <fields>;
}

-keepclasseswithmembernames class * {
    @me.denley.preferencebinder.* <methods>;
}
```


License
-------

    Copyright 2015 Denley Bihari

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Annotation processor structure adapted from [Butter Knife](https://github.com/JakeWharton/butterknife) (Copyright 2013 Jake Wharton).
