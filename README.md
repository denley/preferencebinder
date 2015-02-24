[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-PreferenceInjector-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1569)

# PreferenceInjector
A SharedPreferences injection library for Android. Using annotation processing, this library makes it easy to load SharedPreferences values and listen for changes.


How to Use
-------

#### Injecting Preference Values
Use the `@InjectPreference` annotation to retrieve and inject preference values.

It can be used on any field, like so:
```
@InjectPreference("my_preference_key")
String valueOfPreference;
```

or on any method, like so:
```
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
```
@OnPreferenceChange("my_preference_key")
String valueOfPreference;
```

or on any method, like so:
```
@OnPreferenceChange("my_preference_key")
void valueChanged(String valueOfPreference) {
    // do something with the value
    ...
}
```

Typically, you might use `@InjectPreference` and `OnPreferenceChange` together, like so:
```
@InjectPreference("my_preference_key")
@OnPreferenceChange("my_preference_key")
void updateForValue(String valueOfPreference) {
    // do something with the value
    ...
}
```


Be sure to match the field types and method parameter types with the type of value stored for the preference key. This can't be checked at compile time, and may cause runtime exceptions if a different type of value is stored into the `SharedPreferences` file.

#### Triggering Injection and Listeners
To trigger injection, and start listening for changes to preference values, you must call the following method in your target `Activity`, `Fragment`, `View` or `Dialog` (a typical place for this is in an `onCreate`, `onCreateView` or `onFinishInflate` method):
```
PreferenceInjector.inject(this);
```

If your target class in not one of these types, then you must also provide a `Context` with the target, like so:
```
PreferenceInjector.inject(context, this);
```

Be sure to cancel your listeners when you no longer want updates (e.g. in your `Activity`'s `onDestroy` method). You only need to do this if you have any `@OnPreferenceChange` annotations.
```
PreferenceInjector.stopListening(this);
```

To use a non-default `SharedPreferences` file, you can specify the name of the file when injecting, like so:
```
PreferenceInjector.inject(this, "prefs_file_name");
```

#### Default Values
To specify default values for preference keys, use the `@PreferenceDefault` annotation on static field containing the default value, like so:
```
@PreferenceDefault("my_preference_key")
static String MY_PREFERENCE_DEFAULT = "Unknown";

@InjectPreference("my_preference_key")
void updateForValue(String valueOfPreference) {
    // do something with the value
    ...
}
```

In the above example, the injector will call `updateForValue(MY_PREFERENCE_DEFAULT)` if no value is set for `"my_preference_key"` at injection time. For fields and methods annotated with `@OnPreferenceChange`, the default value will be passed whenever the value for the given key is removed.

Build Configuration
--------

Add the following line to the gradle dependencies for your module.
```
compile 'me.denley.preferenceinjector:PreferenceInjector:2.1.0'
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