# Testing

Current source: v1.99, build 13. Private test build; no store release.

Before this source-only preparation, build 13 passed instrumentation checks on disposable Android 13/API 33 and Android 16/API 36 emulators. The checks cover corpus consistency, rotation, sizing, widget/notification synchronisation, backup, storage reporting and sampled animation paths. Assertion counts include repeated data checks and are not independent test scenarios.

```sh
./gradlew assembleDebug assembleDebugAndroidTest lint
./gradlew connectedDebugAndroidTest
```

Run `SmokeTests` only on a disposable emulator: it changes preferences and creates test widgets. The optional `-PstorageProbe` instrumentation runner performs read-only own-app storage measurements instead.

The helper scripts in `scripts/` operate an emulator over ADB; choose an emulator serial explicitly with `ANDROID_SERIAL`.

No physical-phone frame-rate claim is made. Lock-screen hosting and notification presentation vary by manufacturer. The Samsung storage-reporting discrepancy is unresolved; matching emulator tests did not reproduce it. Do not treat a successful build or emulator test as public-release clearance.

Quote sourcing tests verify data structure and citation presence, not legal permission to redistribute. See QUOTE-RIGHTS.md.
