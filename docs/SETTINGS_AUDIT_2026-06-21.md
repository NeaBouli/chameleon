# Chameleon Settings Audit - 2026-06-21

Scope: full Settings and Access/Upgrade wiring pass for the Android app.

## Findings fixed

- External website links in Settings and Upgrade now use guarded `ACTION_VIEW` handling and show a toast instead of crashing when no browser handler exists.
- Activation success copy now says `Unlocked` instead of `Unaccess`.
- Added a real `Background Contact Listener` setting.
- The new setting is backed by encrypted preferences and immediately starts/stops `ContactListenerService`.
- `ChameleonApplication` only auto-starts the foreground listener when the setting is enabled.
- `BootReceiver` respects the setting and no longer restarts the contact listener after reboot when disabled.
- `ContactListenerService` checks the setting on create/start and during reconnect, stops foreground mode when disabled, and closes the WebSocket via `ContactExchangeManager.stopListening()`.
- `ContactExchangeManager` now exposes `stopListening()` and clears pending frames/identified state.
- Version bumped to `versionCode=6`, `versionName=0.1.5-alpha`.

## Verified settings

- Overlay Encryption routes to `OverlayScreen`; overlay enabled state and whitelist are backed by `SettingsViewModel` and encrypted preferences.
- Automation Rules and Private Zone remain Pro-gated by `TierGatedContent`.
- Decoy Profile, Geofencing and Multi-Decoy Profiles remain Elite-gated by `TierGatedContent`.
- Geofence restore still uses `tierGate.getTier()` after boot, independent of the contact-listener setting.
- Buy Lifetime Access and User Manual links return HTTP 200.
- Activation Code calls the activation backend and persists Pro/Elite through `AccessTierRepository`.
- IFR/wallet code guard passes for app source.

## Builds and installs

- `./gradlew --no-daemon --max-workers=1 verifyNoAppIfrWalletCode`
- `./gradlew --no-daemon --max-workers=1 app:assembleRelease app:bundleRelease`
- `./gradlew --no-daemon --max-workers=1 app:assembleInternalRelease`

Installed Internal APK:

- S7 `ce10160adc00152604`: `chameleon24.app` versionCode `6`, versionName `0.1.5-alpha`
- Tab S4 `ce12182c68644439037e`: `chameleon24.app` versionCode `6`, versionName `0.1.5-alpha`
- S10 was not connected.

Desktop artifacts:

- `/Users/gio/Desktop/Chameleon-LATEST.aab` SHA256 `94853cdbcf13f7ef4fe87d5c591e1fd43de0af96b7701532ac7d2cd898fb92f8`
- `/Users/gio/Desktop/Chameleon-Release-LATEST.apk` SHA256 `57b5d557cdde7955e138711b58ecb405899c95ba9c60d32b96b3485f6c4b8218`
- `/Users/gio/Desktop/Chameleon-Internal-LATEST.apk` SHA256 `4792222890d3e30217053cf03e5d24e6a2e48c4a54047cdf749828673134ee87`
