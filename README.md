# MultiWiFi Android

MultiWiFi is an Android network diagnostics app that reports networks Android exposes to the app and provides a foundation for future multipath aggregation.

IMPORTANT: Detecting multiple networks does not combine their bandwidth. True aggregation generally requires multiple usable network paths, a compatible client/server protocol, and often a user-authorized VPN tunnel. This initial milestone is a real network-status dashboard, not a bandwidth-aggregation engine.

## Current features

- Kotlin + Jetpack Compose dashboard.
- Monitors available internet-capable networks through Android Connectivity APIs.
- Displays transport type, validation status, metered status, and estimated bandwidth when provided by Android.
- Reports aggregation readiness without claiming that multiple networks are bonded.
- Uses network callbacks rather than polling.
- Nearby Wi-Fi list with signal strength and security type, and a "Connect" button that asks Android to join the network you pick (Android 10+). Android shows its own approval prompt. Apps cannot read the phone's saved-network list, so networks are chosen from the nearby scan. Whether two Wi-Fi networks can stay connected together depends on the phone; if not, Android may switch away from the current Wi-Fi.
- Per-network tests, run only when you tap "Test this path" or "Test all paths": TCP connect latency (5 samples, median and jitter) and a single-stream download of about 2 MB. Every socket is bound to the network being tested, and DNS is resolved through that network.
- GitHub Actions build workflow.

## Requirements

- Android Studio with JDK 17.
- Android SDK Platform 35.
- Gradle 8.9 or compatible.
- Android 8.0 (API 26) or later.

## Build

Open this repository in Android Studio and allow Gradle sync to finish. Alternatively, with Gradle 8.9 installed and Android SDK configured, run:

    gradle assembleDebug

The debug APK should be at app/build/outputs/apk/debug/app-debug.apk.

## Roadmap

- [x] Initial Compose dashboard and network monitoring
- [x] CI build workflow
- [x] Per-network latency and throughput tests (currently against a public speed-test endpoint)
- [ ] Switch the tests to an endpoint you control (`ProbeConfig.HOST`)
- [ ] Aggregation protocol design and threat model
- [ ] Multipath test-stream prototype
- [ ] Optional VPN tunnel after protocol testing
- [ ] Physical-device testing
- [ ] Store package-format and policy verification

## Security and privacy

Wi-Fi passwords you type to join a network go only to Android's connection request and are not stored. The app asks for location permission only because Android requires it to list nearby networks. The app does not read other apps' Wi-Fi passwords, create a VPN, or route traffic through a server. Network tests send requests to the host in `ProbeConfig.HOST` only when you tap a test button, and the download test uses about 2 MB of data on the tested network, so check the metered label first. Results are single-stream estimates, not a full speed test. Do not add full-device traffic routing without explicit consent, a reviewed protocol, and a clear privacy policy.

Android references: https://developer.android.com/develop/connectivity/network-ops/reading-network-state and https://developer.android.com/reference/android/net/ConnectivityManager

## License

MIT. See LICENSE.
