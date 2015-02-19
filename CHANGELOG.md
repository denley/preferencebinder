# Change Log
All notable changes to this project will be documented in this file.

## Unreleased
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