# MultiWiFi Android

MultiWiFi is an Android network diagnostics app that reports networks Android exposes to the app and provides a foundation for future multipath aggregation.

IMPORTANT: Detecting multiple networks does not combine their bandwidth. True aggregation generally requires multiple usable network paths, a compatible client/server protocol, and often a user-authorized VPN tunnel. This initial milestone is a real network-status dashboard, not a bandwidth-aggregation engine.

## Current features

- Kotlin + Jetpack Compose dashboard.
- Monitors available internet-capable networks through Android Connectivity APIs.
- Displays transport type, validation status, metered status, and estimated bandwidth when provided by Android.
- Reports aggregation readiness without claiming that multiple networks are bonded.
- Uses network callbacks rather than polling.
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
- [ ] Real per-network latency and throughput tests against a controlled endpoint
- [ ] Aggregation protocol design and threat model
- [ ] Multipath test-stream prototype
- [ ] Optional VPN tunnel after protocol testing
- [ ] Physical-device testing
- [ ] Store package-format and policy verification

## Security and privacy

The initial app does not request Wi-Fi passwords, create a VPN, or route traffic through a server. It does not claim to measure throughput. Do not add full-device traffic routing without explicit consent, a reviewed protocol, and a clear privacy policy.

Android references: https://developer.android.com/develop/connectivity/network-ops/reading-network-state and https://developer.android.com/reference/android/net/ConnectivityManager

## License

Apache-2.0. See LICENSE.
