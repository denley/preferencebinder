# Change Log
All notable changes to this project will be documented in this file.

## 2.2.0 - 2015-03-01
### Added
- Optional "listen" flag for `@InjectPreference` annotation as shorthand for also adding `@OnPreferenceChange` annotation to the same element
- `OnPreferenceChange` annotations on methods can now contain multiple keys (if the method has zero parameters)

### Changed
- Method calls now always occur after field updates for any given preference key (for both initialization and updates)
- Annotated methods can now contain zero parameters, to disregard the preference value.

## 2.1.1 - 2015-02-25
### Added
- `PreferenceInjector.inject(Fragment target)` method. The `Context` can be inferred from the `Fragment`.
- `PreferenceInjector.inject(Service target)` method. The `Context` can be inferred from the `Service`.

## 2.1.0 - 2015-02-19
### Added
- `@PreferenceDefault` annotation, to specify injection values when none exists in the `SharedPreferences` file

### Fixed
- `String` and `Set<String>` are now properly recognized as valid types

## 2.0.0 - 2015-02-19
### Changed
- Removed `initialize` flag from `@OnPreferenceChange` annotation, `@InjectPreference` can now be applied to methods instead
- Removed `autoUpdate` flag from `@InjectPreference` annotation, `@OnPreferenceChange` can now be applied to fields instead
- `PreferenceInjector.stopListening` no longer needs to be called if no `@OnPreferenceChange` annotations exist

## 1.0 - 2015-02-18
### Added
- @InjectPreference and code generation for it
- @OnPreferenceChange and code generation for it