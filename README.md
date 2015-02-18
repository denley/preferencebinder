# PreferenceInjector
A SharedPreferences injection library for Android. Using annotation processing, this library makes it easy to load SharedPreferences values and listen for changes.


How to Use
-------

Use the `@InjectPreference` annotation on a field in any class of your app. The preference value for the specified key will be loaded into the field (be sure to match the types properly, this is not checked at compile time).

```
@InjectPreference("my_preference_key")
String valueOfPreference;
```

Use the `@OnPreferenceChange` annotation on a method in any class of your app. The method will be called whenever the preference value changes for the specified key, providing the new value associated with the key as the method parameter (be sure to match the types properly, this is not checked at compile time).

```
@OnPreferenceChange("my_preference_key")
void valueChanged(String valueOfPreference) {
    // do something with the value
    ...
}
```

You can then call the following method from anywhere in your target class, to inject the preferences and begin listening for changes (a likely place for this is in an `Activity`'s `onCreate` method):
```
PreferenceInjector.init(this); // For certain class types, you may have to add a Context argument too
```

Be sure to cancel your listeners when you no longer want updates (e.g. in your `Activity`'s `onDestroy` method):
```
PreferenceInjector.stopListening(this);
```


You can also specify for fields to be updated automatically when the preference value changes:
```
@InjectPreference(value = "my_preference_key", autoUpdate = true)
```

Similarly, you can specify that methods should be called with the initial value of the preference:
```
@OnPreferenceChange(value = "my_preference_key", initialize = true)
```


How to Include in Your Project
--------

Add the following line to the gradle dependencies for your module.
```
compile 'me.denley.preferenceinjector:PreferenceInjector:1.0'
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