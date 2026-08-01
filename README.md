# 🦎 Chameleon

**Context-Aware Privacy OS for Android**

*A product of the [StealthX Platform](https://stealthx.tech)*

[![Source Available](https://img.shields.io/badge/License-Source--Available-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%2026+-green.svg)](https://developer.android.com)
[![Crypto](https://img.shields.io/badge/Crypto-XChaCha20--Poly1305-purple.svg)](docs/CRYPTO_PROTOCOL_SPEC.md)
[![Status](https://img.shields.io/badge/Status-In%20Development-orange.svg)](LOGBUCH.md)

---

## What is Chameleon?

Chameleon is an Android privacy research client under active development. Its local encrypted storage, rule, geofencing, and decoy components are being tested independently.

The cross-device overlay and messenger are currently disabled. Authenticated peer pairing, compatible session establishment, transport identity, accessibility-loop prevention, and physical two-device tests are required before those capabilities can be released or sold.

---

## Security Architecture

```
XChaCha20-Poly1305    Symmetric encryption (24-byte nonce, no IV reuse risk)
X25519 ECDH           Key exchange (ephemeral, per-session)
Double Ratchet        Forward secrecy for Messenger mode
Argon2id              Password-based key derivation (64MB memory, 3 iterations)
Ed25519               Key bundle signing for contact verification
Android Keystore      Hardware-backed private key storage (StrongBox/TEE)
```

**Single crypto library:** [lazysodium-android](https://github.com/terl/lazysodium-android)  
**No AES-GCM. No BouncyCastle. No custom crypto.**

---

## Module Structure

```
:app                    Entry point, Hilt DI graph
:stealthx-crypto        THE ONLY crypto module (XChaCha20, X25519, DR, Argon2id)
:stealthx-ifr           Legacy/internal IFR helpers, not part of the public Android wallet flow
:security               Android Keystore, Attestation, SecureWipe
:core                   AccessibilityService (AIDL isolated), Overlay
:data                   Room + SQLCipher, EncryptedSharedPrefs, SecureFile
:domain                 EncryptionEngine, RuleEngine, TierGate, KeyManager
:features:overlay       Disabled pending cross-device pairing verification
:features:messenger     Disabled pending session/transport verification
:features:privatezone   Pro — encrypted file storage
:features:geofencing    Elite — location-based rule triggers
:features:decoy         Elite — decoy profile system
:presentation           Jetpack Compose UI, StealthX Design System
:shared                 Data models, utilities (no dependencies)
```

**Dependency rule:** `:domain` never imports `:data`. `:security` imports nothing. Crypto only in `:stealthx-crypto`.

---

## Paid Access

New paid access is launch-gated. The Android client accepts only server-signed, device-bound entitlements and does not persist paid access from local payment callbacks. Availability will be announced through VLABS after product, payment, refund-revocation, and two-device verification gates pass.

---

## Development Status

See [LOGBUCH.md](LOGBUCH.md) for the live development log.

| Phase | Status | Description |
|-------|--------|-------------|
| S-00 | ✅ Done | Repo init, SecureCall analysis |
| S-01 | ✅ Done | Gradle modules (13), CI/CD, all compile |
| S-02 | ✅ Done | Security Layer (Keystore, Attestation, Argon2id) |
| S-03 | ✅ Done | AccessibilityService (AIDL, CryptoService :crypto process) |
| S-04 | ✅ Done | Data Layer (Room + SQLCipher + local integrity cache) |
| S-05 | ✅ Done | Domain Layer (XChaCha20, Double Ratchet HKDF, TierGate, RuleEngine) |
| S-06 | ✅ Done | IFR web discount model and activation-code unlock path |
| S-07 | ✅ Done | Compose UI (StealthX Design System, Navigation, Screens) |
| S-08 | In progress | Feature Layer; overlay and messenger remain disabled |
| S-09 | In progress | Internal hardening; no external MASVS certification claimed |
| S-10 | Blocked | Distribution pending product verification and license decision |

---

## Download

**Public Alpha: v0.1.10-alpha** — Android 16 evaluation build; external security audit remains pending.

- F-Droid: not eligible under the current source-available license
- Play Store: closed testing submitted; public listing is not live
- GitHub Releases: [download the latest signed APK](https://github.com/NeaBouli/chameleon/releases/download/v0.1.10-alpha-chameleon/Chameleon-LATEST.apk)
- Source: `git clone https://github.com/NeaBouli/chameleon.git`

---

## Security Audit

Chameleon uses MASVS as an internal review framework. It has not received an external MASVS certification. The working audit package is in [`docs/AUDIT_PACKAGE/`](docs/AUDIT_PACKAGE/).

| Document | Contents |
|----------|----------|
| [Crypto Implementation](docs/AUDIT_PACKAGE/CRYPTO_IMPLEMENTATION.md) | Algorithms, parameters, key management |
| [Threat Model](docs/AUDIT_PACKAGE/THREAT_MODEL.md) | Assets, threat actors, trust boundaries |
| [Architecture Overview](docs/AUDIT_PACKAGE/ARCHITECTURE_OVERVIEW.md) | Module layers, AIDL isolation |
| [OWASP MASVS Compliance](docs/AUDIT_PACKAGE/OWASP_MASVS_COMPLIANCE.md) | Point-by-point compliance matrix |
| [Known Limitations](docs/AUDIT_PACKAGE/KNOWN_LIMITATIONS.md) | What Chameleon does NOT protect against |
| [Dependency Audit](docs/AUDIT_PACKAGE/DEPENDENCY_AUDIT.md) | All libraries, versions, CVE status |

**Recommended external auditor:** [Trail of Bits](https://www.trailofbits.com/) — experienced with XChaCha20 + Double Ratchet stacks (audited SimpleX Chat).

**No release without external audit** — this is an architectural principle.

---

## Three Rules Never Broken

1. **Network access is explicit** — the manifest includes internet and nearby-device capabilities for planned transports and entitlement renewal
2. **Private keys never leave hardware Keystore** (StrongBox or TEE)
3. **Fail Secure** — on error, encrypt. Never decrypt.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for code review process and architecture rules.

Security-critical changes (crypto, keystore, AIDL) require additional review.

---

## License

StealthX Source-Available License. This repository is source-available, not open source.

You may read and inspect the source code for transparency and security review.
You may not copy, modify, build, run, distribute, rebrand, host, or use Chameleon
or official StealthX services without prior written permission from Vendetta Labs.
See [LICENSE](LICENSE).

---

*Chameleon — Privacy that adapts.*
*Part of the [StealthX Platform](https://stealthx.tech) | [SecureCall](https://stealthx.tech) | [IFR Token](https://ifrunit.tech)*
