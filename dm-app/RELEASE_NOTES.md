# FocusGram DMs 1.0.0 — preview

A signed Android app that starts at Instagram's message inbox and blocks Feed, Reels, Explore, Stories, posts and profiles. No ads or app analytics. Android 7.0 or later with an up-to-date Android System WebView is required.

**Download [focusgram-dms.apk](https://github.com/rogerhunter00-afk/FocusGram/releases/download/dm-v1.0.0/focusgram-dms.apk) on your Android phone.**

## Install and use

1. Open the download link on your phone and open the downloaded APK.
2. If Android asks, allow your browser to install unknown apps, then tap **Install**. You can switch that permission off afterwards.
3. Open **FocusGram DMs** and sign in on Instagram's own page using your Instagram username/password. Complete any verification Instagram requests. Update Android System WebView/Chrome if Instagram says the browser is unsupported.
4. Use **Inbox** to return to your messages, **Reload** if a page gets stuck, and **Help → Sign out** to clear the session from this phone.

## Before relying on it

**This is a preview. Background message notifications and calls are not supported.** Keep official Instagram if reliable alerts are essential.

The app preserves the website's photo/video picker and requests microphone permission for voice recording when Instagram offers it. Photos, voice messages, encrypted chats and successful sign-in have **not been verified with a real account**. Instagram's web support can differ between accounts. Shared posts/Reels stay blocked. Login with Facebook in another browser will not transfer its session into this app.

Instagram login/session data stays in the app's private storage on the phone, with Android backup disabled. No credentials were used during development. External links ask before opening in another app.

## Checks completed

- Release APK built, signed, signature/alignment verified, and installed in an Android 10 emulator.
- All 24 Java routing tests passed; Chromium fixture tests passed.
- Real Android WebView fixture tests passed for script injection, navigation hiding, preserved chat controls, DM navigation and storage policy.
- Instagram's public login screen rendered and cookie choice persisted across a full app restart. The emulator's old WebView showed a browser-version warning; actual login/messages remain unverified.

See [the complete test report](https://github.com/rogerhunter00-afk/FocusGram/blob/dm-v1.0.0/dm-app/TEST_REPORT.md) and [source/build instructions](https://github.com/rogerhunter00-afk/FocusGram/tree/dm-v1.0.0/dm-app). APK SHA-256 is included in `SHA256SUMS.txt`.

Fork of FocusGram by Ujwal Chapagain, distributed under AGPL-3.0. Independent app, not affiliated with Meta/Instagram. The source archives attached automatically by GitHub contain the complete corresponding source at this tag.
