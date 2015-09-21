# Change Log
All notable changes to this project will be documented in this file.

## 3.1.0 - Unreleased
### Fixed
- `PreferenceBinder.bind` and `PreferenceBinder.unbind` statement check now looks inside blocks/scopes (e.g. `if` blocks).
- Added `PreferenceBinder.bind(Context context, Object target, SharedPreferences prefs)` to allow testing with mocked `SharedPreferences` files.

## 3.0.2 - 2015-08-03
### Removed
- Removed compiler check for `PreferenceBinder.bind` and `PreferenceBinder.unbind` method calls pending testing and stability improvement to the feature

## 3.0.1 - 2015-08-02
### Fixed
- Fixed NullPointerException when using this library in classes with abstract methods.
- Updated bind/unbind statement checker to look in scopes inside methods.

## 3.0.0 - 2015-07-25
### Changed
- New annotation `@BindPref` replaces both `@InjectPreference` and `@OnPreferenceChanged`. The `listen` flag now defaults to true
- All instances of "inject", "injector", and "injection" have been replaced with "bind", "binding", and "binding".
- `@PreferenceDefault` annotated fields now apply globally (application-wide) rather than just for their containing class.

### Added
- `@BindPref` annotation may now be used on certain widget types (fields), to automatically bind the preference value to a widget's method. See README.md for details.
- Compiler now checks for `PreferenceBinder.bind` and `PreferenceBinder.unbind` (when necessary) method calls in classes with `@BindPref` annotations.

## 2.2.1 - 2015-03-28
### Fixed
- Set minSdk to 4, to prevent unnecessary permissions being added by default (see [this reference](https://code.google.com/p/android/issues/detail?id=4101) for explanation)

## 2.2.0 - 2015-03-01
### Added
- Optional "listen" flag for `@InjectPreference` annotation as shorthand for also adding `@OnPreferenceChange` annotation to the same element
- `OnPreferenceChange` annotations on methods can now contain multiple keys (if the method has zero parameters)

### Changed
- Method calls now always occur after field updates for any given preference key (for both initialization and updates)
- Annotated methods can now contain zero parameters, to disregard the preference value.

## 2.1.1 - 2015-02-25
### Added
- `PreferenceInjector.bind(Fragment target)` method. The `Context` can be inferred from the `Fragment`.
- `PreferenceInjector.bind(Service target)` method. The `Context` can be inferred from the `Service`.

## 2.1.0 - 2015-02-19
### Added
- `@PreferenceDefault` annotation, to specify injection values when none exists in the `SharedPreferences` file

### Fixed
- `String` and `Set<String>` are now properly recognized as valid types

## 2.0.0 - 2015-02-19
### Changed
- Removed `initialize` flag from `@OnPreferenceChange` annotation, `@InjectPreference` can now be applied to methods instead
- Removed `autoUpdate` flag from `@InjectPreference` annotation, `@OnPreferenceChange` can now be applied to fields instead
- `PreferenceInjector.unbind` no longer needs to be called if no `@OnPreferenceChange` annotations exist

## 1.0 - 2015-02-18
### Added
- @InjectPreference and code generation for it
- @OnPreferenceChange and code generation for it