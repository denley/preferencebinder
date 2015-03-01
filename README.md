[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-PreferenceInjector-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1569)

# PreferenceInjector
A SharedPreferences "injection" library for Android. Using annotation processing, this library makes it easy to load SharedPreferences values and listen for changes.


How to Use
-------

#### Loading Preference Values
Use the `@InjectPreference` annotation to retrieve and initialize ("inject") preference values.

It can be used on any field, like so:
```java
@InjectPreference("my_preference_key")
String valueOfPreference;
```

or on any method, like so:
```java
@InjectPreference("my_preference_key")
void initializeForPreferenceValue(String valueOfPreference) {
    // do something with the value
    ...
}
```

Be sure to match the field types and method parameter types with the type of value stored for the preference key. This is not checked at compile time, and may cause runtime exceptions.

#### Listening for Changes
Use the `@OnPreferenceChange` annotation to listen for changes to preference values.

It can be used on any field, like so:
```java
@OnPreferenceChange("my_preference_key")
String valueOfPreference;
```

or on any method, like so (the method parameter is optional):
```java
@OnPreferenceChange("my_preference_key")
void valueChanged(String valueOfPreference) {
    // do something with the value
    ...
}
```

Be sure to match the field types and method parameter types with the type of value stored for the preference key. This can't be checked at compile time, and may cause runtime exceptions if a different type of value is stored into the `SharedPreferences` file.
<br/><br/>
Typically you might want to use `@InjectPreference` and `OnPreferenceChange` together, to both initialize and respond to changes. Instead of adding both annotations, you can simply use the "listen" flag in the `@InjectPreference` annotation, like so:
```java
@InjectPreference(value = "my_preference_key", listen = true)
void setValue(String valueOfPreference) {
    // do something with the value
    ...
}
```

When annotating a method with `OnPreferenceChange`, you may specify more than one key. In this case, the method will be called when any value for one of the specified keys changes. For example:
```java
@OnPreferenceChange({"show_full_names", "use_small_icons"})
void refreshList() {
    adapter.notifyDataSetChanged();
}
```

#### Triggering Initialization and Listeners
To bind your preference values and start listening for changes, you must call the following method in your target (a typical place for this is in an `onCreate`, `onCreateView` or `onFinishInflate` method):
```java
PreferenceInjector.inject(this);
```

The method above works if your target is a subclass of `Activity`, `Fragment`, `View`, `Service` or `Dialog`. If your target is not one of these types, then you must also provide a `Context` too, like so:
```java
PreferenceInjector.inject(context, this);
```

Be sure to cancel your listeners when you no longer want updates (e.g. in your `Activity`'s `onDestroy` method). You only need to do this if you have any `@OnPreferenceChange` annotations.
```java
PreferenceInjector.stopListening(this);
```

To use a non-default `SharedPreferences` file, you can specify the name of the file when initializing, like so:
```java
PreferenceInjector.inject(this, "prefs_file_name");
```

#### Default Values
To specify default values for preference keys, use the `@PreferenceDefault` annotation on static field containing the default value, like so:
```java
@PreferenceDefault("my_preference_key")
static String MY_PREFERENCE_DEFAULT = "Unknown";

@InjectPreference("my_preference_key")
void updateForValue(String valueOfPreference) {
    // do something with the value
    // ...
}
```

In the above example, `PreferenceInjector` will call `updateForValue(MY_PREFERENCE_DEFAULT)` if no value is set for `"my_preference_key"` on initialization. For fields and methods annotated with `@OnPreferenceChange`, the default value will be passed whenever the value for the given key is removed.

Build Configuration
--------

Add the following line to the gradle dependencies for your module.
```groovy
compile 'me.denley.preferenceinjector:PreferenceInjector:2.2.0'
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
