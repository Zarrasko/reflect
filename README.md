# Reflect

A daily journaling prompt tool for the Light Phone III, built on the
[Light SDK](https://github.com/lightphone/light-sdk).

Shows a new prompt each day. Tap the shuffle icon (or shake the phone) for a
different one, or browse the full list from the list icon.

This repo is a standalone extraction of the `examples/reflect` module from
the official Light SDK repo, along with the minimum SDK pieces
(`sdk/client`, `sdk/ui`, `sdk/shared`, and the build plugin) needed for it to
build on its own. It won't automatically pick up updates from the official
SDK — for the source of truth, official examples, and full documentation,
see [lightphone/light-sdk](https://github.com/lightphone/light-sdk).

## Building

```bash
./gradlew :app:assembleDebug
```

## Installing on a Light Phone III

There's no app-store install flow for community tools yet, so this has to
be sideloaded:

1. Enable Developer Options + USB debugging on your Light Phone III
2. Install `adb` on your computer (part of Android platform-tools)
3. Connect your phone via USB
4. `./gradlew :app:installDebug`, or `adb install app-debug.apk` if you
   already have a built APK
5. Launch "Reflect" from your phone's app list

Or just grab a prebuilt APK from this repo's
[Releases](../../releases) page and `adb install` it directly.
