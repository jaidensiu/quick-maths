# Quick Maths

Icons from [Flaticon by Freepik](https://www.flaticon.com/)

## Layout

- `android/`: Android app module (Gradle project at the repo root)
- `ios/`: iOS app, see [ios/README.md](ios/README.md)
- `shared/audio/`: C++ SFX mixer used by both apps: Android drives it from Oboe (`android/src/main/cpp`), iOS from an `AVAudioSourceNode` through the C header `QuickMathsMixer.h`
  - Test: `cd shared && c++ -std=c++17 -Iaudio audio/Mixer.cpp MixerTest.cpp -o /tmp/mixer_test && /tmp/mixer_test`

## TODO

- Migrate flavor source sets to Gradle modules: `:core:game`, `:car`, `:car:stub`
  - Wire via `automotiveImplementation(":car")` / `mobileImplementation(":car:stub")`
  - Extract `GearMonitor` interface into `:core:game`
  - Move `useLibrary("android.car")` into `:car`
