# FocusGram DMs 1.0.0 preview — validation

Date: October 9, 2026. Connected account independently verified through GitHub CLI and the GitHub connector: `rogerhunter00-afk`.

## Upstream and source

- Fork: https://github.com/rogerhunter00-afk/FocusGram
- Parent: https://github.com/Ujwal223/FocusGram
- Inspected upstream checkout: `c9ec258696a377f6163600edd6f570d6f4571574`.
- Latest upstream release inspected: v2.1.0, June 13, 2026, including its APK metadata and release notes. The upstream APK was not repackaged or redistributed as the DM app.
- Root licence inspected: GNU Affero General Public License v3.0. Original source and attribution retained. This fork adds an independent native Android DM app in `dm-app/`; only that app is packaged in the published APK.
- The fork's inherited Flutter release workflow is not used. This release was built locally with the documented dependency-free Android SDK build script.

## Passed

- JDK 17 compilation against Android SDK 35; release-mode D8 bytecode conversion; Android resource packaging.
- 24 Java URL-routing cases: inbox, DM thread/new message, login/verification, blocked feed/Reels/Explore/Stories/posts/profiles, external URLs, deceptive hosts, unsupported schemes and malformed URLs.
- Headless Chromium fixture checks: hidden distracting navigation, preserved inbox/search/attachment/image/audio/message controls, DM SPA navigation, redirect on Reels/Stories navigation, dynamically inserted links, allowed login route.
- Installed both the release APK and separate instrumentation test APK on a hardware-accelerated Android 10 (API 29) x86_64 emulator.
- Instrumentation fixture checks in real Android WebView: app launch, JavaScript injection, hiding Home/Reels links, preserving chat controls, DM SPA navigation, Android Back returning from a thread to the inbox, disabled file access, disabled third-party cookies, disabled Android backup. The Back test exposed a stale native-URL timing issue after SPA navigation; the release handles Back using the active document's URL, and the regression check passed.
- Live unauthenticated smoke test reached Instagram's own cookie-consent and login screens. Declined optional cookies. After force-stopping and restarting the app, the cookie choice persisted and Instagram's login screen returned.
- APK signing verification: release RSA 2048 certificate; APK Signature Scheme v2 and v3 verified; ZIP alignment verified. Android installed it successfully.
- Source whitespace check passed. Private key/password remain outside the repository; no Instagram credentials were supplied, collected or published.

Public signing certificate SHA-256:

`96d5270f71181a6e070dfbe98ae4d359bddb54aa742ce140c3db99e4738600ac`

## Not verified / limitations

- No real Instagram account was used. Successful sign-in, login session retention after authentication, sending/receiving DMs, photo uploads, voice recording/playback and encrypted chats still require a test on a current phone with the user's own account. The fixture tests preserve their controls and test routing; they do not prove the live service offers or accepts these features.
- The available emulator has Android WebView 74.0.3729.185. It rendered login but Instagram eventually displayed a browser-version warning. Update Android System WebView/Chrome on the phone. Native/UI fixture tests passed; modern live-account compatibility is still unverified.
- Upstream notifications were inspected: its local notification helper is not an authenticated background DM push subscription. The new app has no supported Instagram background push channel; notifications and calls are explicitly unsupported. No polling or undocumented API was added.
- Multi-step two-factor/checkpoint flows, keyboard variations, attachment providers, microphone denial/retry, all locales and every possible Instagram markup variant have not been exercised with a live account/device.
- AGPL source/build instructions are public at the tagged release. No promise is made that a separately built APK will have the same byte hash or signing certificate; signing with your own key produces a different certificate.

## Reproduce

See `README.md` for local build requirements and signing environment variables. `python dm-app/build.py` runs the Java policy checks and builds/verifies the APK. For browser fixture checks, install Playwright, then run `node dm-app/test/browser-test.cjs`; optionally provide `CHROME_PATH` to an installed browser.

For fresh-emulator instrumentation checks only (no real account), build with `python dm-app/build.py --instrumentation`, install `dm-app/build/focusgram-dms.apk` and `dm-app/build/smoke-test.apk`, then run:

```
adb shell am instrument -w uk.rogerhunter.focusgram.dm.test/.SmokeTest
```

The instrumentation test uses synthetic chat markup and never logs in. Do not install the test APK on a phone that you use for personal messaging.
