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
- Install apps that are not already on the device, unless an admin hands out a code for one
- Access purchase history, reviews, or any Google account features beyond fetching update metadata

## Sending an app to someone

The point of this app is that the person using it cannot install whatever they like. When they
genuinely need something new, an admin can unlock exactly one app with a short code, without
touching their phone.

**The person receiving the app:**

1. Opens App Updater and taps the tag icon in the toolbar (**Redeem a code**)
2. Types the 8-character code, e.g. `K7QM-2X4B`
3. Sees which app the code unlocks, and taps **Install**

Once installed, the app is updated like any other. The code cannot be used for anything else — a
code is bound to one package.

**The admin, once per device:**

1. On the Redeem screen, tap the tag icon 7 times to reveal the admin prompt
2. Paste a [fine-grained personal access token](https://github.com/settings/personal-access-tokens)
   scoped to this repository with **Contents: read and write**

The token is stored in the app's own private preferences and is never shared with the recipient's
device.

**Handing out a code:** open the admin screen, tap **+**, search for the app (or paste its package
name), pick how long the code should last, and send the code however you like. Codes can be
revoked from the same screen.

### How it works

Codes are validated against [`grants.json`](grants.json) in this repository. Issuing a code writes
a grant to that file through the GitHub contents API; the recipient's app reads it back
anonymously, so the recipient's device needs no credentials of its own.

The file is public, so it stores only the SHA-256 hash of each code — nobody can mine it for
working codes. Expiry and revocation are what put a code out of use.

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
