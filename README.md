# Marginalia

A small offline Android app for maxims and aphorisms, with a resizable home-screen widget.

Inspired by [Adagia](https://readadagia.com). Marginalia is an independent project, **not an official Adagia app**, and is not affiliated with or endorsed by Adagia.

## Status

**Private test build: v1.99, build 13.** There is no store release. This repository contains source code and the Gradle wrapper, not APKs, demo media or release assets. Emulator checks are not evidence of physical-phone performance; no physical-device frame-rate or compatibility guarantee is made.

The working anthology contains 1,643 passages attributed to 95 thinkers, with a 316-passage selected-maxims subset. These are Marginalia's own current data counts. **The full anthology is not cleared for public redistribution.** Modern works and translations need further review; see [QUOTE-RIGHTS.md](QUOTE-RIGHTS.md). Keep this repository private until that review and owner approval are complete.

## What it does

- Browse and search the local anthology, follow source links, and save favourites.
- Rotate the shared widget/notification passage every 12 or 24 hours; optional equal exposure across thinkers and a 24-hour hold.
- Resize the home-screen widget; lettering adapts to available space.
- Choose Paper or Ink, translucent frosted/liquid-style widget backgrounds, and serif, sans or outlined subtitle lettering.
- Tap the widget for an author-themed upward emoji burst and recoil. Nietzsche uses the camel, lion and child. The author line opens the source.
- Use a native lock-screen widget where the phone exposes a compatible picker, or opt into a notification card over the existing wallpaper.

Glass styling uses translucent highlights and edges, not live wallpaper blur/refraction. Android controls lock-screen widget availability and notification expansion; long passages may require expansion. Background refresh can be delayed by Android power management. The app does not replace wallpaper.

## Build locally

Open this folder in Android Studio. The project uses Java 17 source compatibility, Android Gradle Plugin 9.4.1, the checked-in Gradle wrapper, compile SDK 37 and target SDK 36; minimum SDK is 26. Use an Android Studio/JDK combination supported by that Gradle/AGP version and install the requested SDK.

Android Studio can create your machine-specific `local.properties`. Then run:

```sh
./gradlew assembleDebug
```

A release build without a local signing script is unsigned. Signing keys, passwords and `signing/` are intentionally excluded; supply your own signing setup if you need to distribute your own build. Do not commit credentials or generated APK/AAB files.

## Privacy and permissions

No account, ads, analytics or Android internet permission. Source links open an external browser. Favourites and settings stay on the device. Optional quotation notifications require permission on supported Android versions. Lock-screen-only notification mode additionally requires opt-in Notification Access; the app does not read other apps' notification content. Backups are user-initiated local exports.

## Verification and limitations

See [TESTING.md](TESTING.md). Android 13 and 16 emulator checks have been run; physical-device verification is incomplete. A Samsung app-size reporting discrepancy remains unresolved and was also observed with a separate tiny control app. This repository is not a claim that the issue is fixed.

## Licence and third-party material

Original Marginalia code is available under the [MIT licence](LICENSE), selected with the owner's authorisation. **This licence does not cover quoted passages, translations or third-party material.** Their own rights and terms apply; see [QUOTE-RIGHTS.md](QUOTE-RIGHTS.md).

The native liquid-glass edge treatment incorporates an adaptation from [dashersw/liquid-glass-js](https://github.com/dashersw/liquid-glass-js); its MIT notice is retained in `app/src/main/assets/liquid-glass-license.txt`. Source/edition information and the Project Gutenberg notice are retained in the app assets. Their presence does not clear every passage in the anthology.
