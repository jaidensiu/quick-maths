# Quick Maths

Icons from [Flaticon by Freepik](https://www.flaticon.com/)

## TODO

- Migrate flavor source sets to Gradle modules: `:core:game`, `:car`, `:car:stub`
  - Wire via `automotiveImplementation(":car")` / `mobileImplementation(":car:stub")`
  - Extract `GearMonitor` interface into `:core:game`
  - Move `useLibrary("android.car")` into `:car`
- Organize submodules flavors (automotive, debug, mobile, mobileRelease, etc.)
- Add analytics
- Add pre-commit hook for fmt/lint
