# FocusGram DMs

A small, independent Android app that opens Instagram's website directly at the DM inbox. This is the app distributed by this fork. The original Flutter source is retained for provenance and is not bundled into the DM APK.

Android 7.0 or later, an up-to-date Android System WebView, and an internet connection are required.

## Install

1. On your Android phone, open this fork's Releases page and download `focusgram-dms.apk`.
2. Open the downloaded file. If Android asks, allow your browser to install unknown apps, then tap Install. You can turn that permission off afterwards.
3. Open **FocusGram DMs**. Sign in on Instagram's own page with your Instagram username and password, completing any verification Instagram requests.
4. Use **Inbox** to return to your chats and **Reload** if a page gets stuck. Android Back returns from a conversation to the inbox, then exits.

The application ID differs from upstream, so both apps can be installed. Only install APKs from this repository's releases. Updates require the same signing certificate.

## What this version does

- Starts at `/direct/inbox/`; allows DM and Instagram account/verification routes.
- Redirects feed, Reels, Explore, Stories, posts and profiles to the inbox; hides their links including during SPA navigation.
- Preserves Instagram's DM web UI, including its search, photo/video attachment picker and media playback.
- Offers microphone permission only when Instagram asks for audio capture from a DM screen. No blanket camera or storage permission.
- Stores first-party cookies and website storage in the app's private phone storage; no cloud backup, analytics, ads, server or credential logging.
- Asks before opening external HTTP(S), mail or phone links. Blocks `javascript:`, `file:`, `content:`, `intent:` and other unsupported navigation schemes.
- Provides Help and an explicit local sign-out that clears cookies, website storage and cache.

## Limitations — please read before relying on it

**Background DM notifications and calls are not supported.** Upstream's local reminder notification service is not a reliable Instagram push channel. This dependency-free Android WebView app has no supported background Instagram push subscription, polling or private API. Keep official Instagram installed if dependable new-message alerts matter. We deliberately do not request notification permission or pretend local reminders are incoming messages.

Photos, voice-message recording/playback, encrypted chats and account verification depend on what Instagram exposes to your particular account in its mobile website. They have not been verified with a logged-in Instagram account unless the release notes explicitly say so. Native calls, disappearing media and native-only features may be absent. Login with Facebook in an external browser cannot transfer that browser's session to this app; use an Instagram username/password. Password-manager integration depends on the phone/WebView. Instagram may require login again or reject embedded browsers.

Instagram's HTML and routing can change. Link/route guards are tested, but every live navigation/control/localisation has not been verified. This is a focused client, not an account-level content restriction. Shared posts/Reels are blocked rather than opened inside the app. External links can lead to distracting content in another app after confirmation.

## Build from source

Requirements: JDK 17, Python 3, Android SDK platform 35 and build-tools 35.0.0. No Flutter, Gradle, third-party Java libraries or NDK are used by this APK.

Set `JAVA_HOME`, `ANDROID_HOME`, `DM_KEYSTORE` (outside the checkout) and `DM_STORE_PASSWORD` in your local environment. Create your own key with alias `focusgram-dms`, then run `python dm-app/build.py`. The script compiles the app, runs route tests, creates a release DEX, packages resources, aligns the APK, signs it and verifies the certificate/signature. Output: `dm-app/build/focusgram-dms.apk`. Never commit signing material or passwords. Independently built APKs will have a different certificate from published releases.

## Source and licence

Fork of [Ujwal223/FocusGram](https://github.com/Ujwal223/FocusGram), inspected at upstream v2.1.0 (latest published release June 13, 2026). Copyright © 2025–2026 Ujwal Chapagain. This fork's DM app changes were made October 9, 2026. Distributed under the same [AGPL-3.0 licence](../LICENSE). Complete corresponding app source, assets and build script are in this directory; original upstream notices/source are retained. Not affiliated with or endorsed by Meta or Instagram.
