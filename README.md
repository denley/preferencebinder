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
To trigger injection, and start listening for changes to preference values, you must call the following method in your target class (a typical place for this is in an `Activity`'s `onCreate` method):
```
// In certain class types, you may have to add a Context argument too
PreferenceInjector.init(this);
```

Be sure to cancel your listeners when you no longer want updates (e.g. in your `Activity`'s `onDestroy` method). You only need to do this if you have any `@OnPreferenceChange` annotations.
```
PreferenceInjector.stopListening(this);
```


Build Configuration
--------

Add the following line to the gradle dependencies for your module.
```
compile 'me.denley.preferenceinjector:PreferenceInjector:2.0.0'
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