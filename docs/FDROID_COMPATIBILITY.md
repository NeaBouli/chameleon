# Chameleon — F-Droid Compatibility Assessment

## FOSS Compliance

| Dependency | License | F-Droid Compatible | Notes |
|-----------|---------|-------------------|-------|
| lazysodium-android | MPL-2.0 | Yes | FOSS-compatible |
| JNA | Apache-2.0 / LGPL-2.1 | Yes | FOSS-compatible |
| SQLCipher | BSD-3 | Yes | FOSS-compatible |
| web3j-core | Apache-2.0 | Yes | FOSS-compatible |
| Jetpack Compose | Apache-2.0 | Yes | Google, but FOSS |
| Hilt/Dagger | Apache-2.0 | Yes | FOSS-compatible |
| Room | Apache-2.0 | Yes | FOSS-compatible |
| Timber | Apache-2.0 | Yes | FOSS-compatible |
| zxing | Apache-2.0 | Yes | FOSS-compatible |
| Play Services Location | Proprietary | **No** | F-Droid flavor must exclude |

## F-Droid Flavor Strategy

The F-Droid build flavor excludes:
- `play-services-location` — replaced with Android framework LocationManager
- No FCM (not used anyway — no push notifications)
- No proprietary Google libraries

## Anti-Features

| Anti-Feature | Applies | Reason |
|-------------|---------|--------|
| NonFreeNet | Yes | Optional IFR verification requires Ethereum RPC (user-initiated) |
| NonFreeDep | No | All dependencies are FOSS (F-Droid flavor excludes Play Services) |
| Tracking | No | No analytics, telemetry, or crash reporting |
| Ads | No | No advertising |
| NonFreeAdd | No | No proprietary add-ons |

## Reproducible Build

The F-Droid metadata includes build instructions for reproducible builds.
A Dockerfile is provided for build environment consistency.
