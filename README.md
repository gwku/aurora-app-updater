# App Updater

A fork of [Aurora Store](https://gitlab.com/AuroraOSS/AuroraStore) stripped down to a single purpose: **keeping installed apps up to date** on Android devices that have no access to the Google Play Store.

Designed for restricted or managed Android devices where sideloading is permitted but the Play Store is unavailable or disabled.

## What it does

- Checks Google Play for available updates to apps already installed on the device
- Downloads and installs updates silently in the background
- Shows a simple list of pending updates with an "Update All" button
- Updates itself when a new release is published

## What it does not do

- Browse, search or discover new apps
- Install apps that are not already on the device
- Access purchase history, reviews, or any Google account features beyond fetching update metadata

## Authentication

Sign in with a Google account or use an anonymous token. Anonymous mode is sufficient for fetching updates.

## Downloads

Get the latest signed APK from [Releases](../../releases).

## Building

Requires JDK 21.

```bash
./gradlew assembleVanillaRelease
```

## License

GPL-3.0 — see [LICENSE](LICENSE). Based on Aurora Store by Rahul Kumar Patel.
