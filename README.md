# 🦎 Chameleon

**Context-Aware Privacy OS for Android**

*A product of the [StealthX Platform](https://stealthx.tech)*

[![GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%2026+-green.svg)](https://developer.android.com)
[![Crypto](https://img.shields.io/badge/Crypto-XChaCha20--Poly1305-purple.svg)](docs/CRYPTO_PROTOCOL_SPEC.md)
[![Status](https://img.shields.io/badge/Status-In%20Development-orange.svg)](LOGBUCH.md)

---

## What is Chameleon?

Chameleon encrypts your communications automatically — based on **where you are**, **which app you use**, and **who might be watching**.

Unlike Signal or WhatsApp which are standalone messengers, Chameleon is a **privacy layer** that works *on top* of any app. It intercepts text via Android's AccessibilityService, encrypts it with XChaCha20-Poly1305, and injects the ciphertext back — transparently.

**Free tier:** Overlay encryption for any app.  
**Pro tier (IFR Lock ≥ 2,000):** Private Zone + local Messenger mode.  
**Elite tier (IFR Lock ≥ 6,000):** Geofencing + Decoy Profile + Ghost Mode.

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
:stealthx-ifr           IFR Token verification (WalletConnect, web3j LITE)
:security               Android Keystore, Attestation, SecureWipe
:core                   AccessibilityService (AIDL isolated), Overlay
:data                   Room + SQLCipher, EncryptedSharedPrefs, SecureFile
:domain                 EncryptionEngine, RuleEngine, TierGate, KeyManager
:features:overlay       Free — text overlay encryption
:features:messenger     Pro — local encrypted messenger (no server)
:features:privatezone   Pro — encrypted file storage
:features:geofencing    Elite — location-based rule triggers
:features:decoy         Elite — decoy profile system
:presentation           Jetpack Compose UI, StealthX Design System
:shared                 Data models, utilities (no dependencies)
```

**Dependency rule:** `:domain` never imports `:data`. `:security` imports nothing. Crypto only in `:stealthx-crypto`.

---

## IFR Token Integration

Chameleon is integrated with the [IFR Token](https://ifrunit.tech) ecosystem.  
Lock IFR tokens once for lifetime tier access — no subscriptions.

| IFR Lock | Tier | Features |
|----------|------|----------|
| 0 IFR | Free | Overlay encryption, 5 rules |
| ≥ 2,000 IFR | Pro | + Private Zone, + Messenger |
| ≥ 6,000 IFR | Elite | + Geofencing, + Decoy Profile |

IFR Lock contract: `0x769928aBDfc949D0718d8766a1C2d7dBb63954Eb` (Ethereum Mainnet)

---

## Development Status

See [LOGBUCH.md](LOGBUCH.md) for the live development log.

| Phase | Status | Description |
|-------|--------|-------------|
| S-00 | ✅ Done | Repo init, SecureCall analysis |
| S-01 | ✅ Done | Gradle modules (13), CI/CD, all compile |
| S-02 | ✅ Done | Security Layer (Keystore, Attestation, Argon2id) |
| S-03 | ✅ Done | AccessibilityService (AIDL, CryptoService :crypto process) |
| S-04 | ✅ Done | Data Layer (Room + SQLCipher + IFR HMAC Cache) |
| S-05 | ✅ Done | Domain Layer (XChaCha20, Double Ratchet HKDF, TierGate, RuleEngine) |
| S-06 | ✅ Done | IFR Module (WalletConnect, IFRLockVerifier, TierActivator) |
| S-07 | ✅ Done | Compose UI (StealthX Design System, Navigation, Screens) |
| S-08 | ✅ Done | Feature Layer (Overlay, Messenger, PrivateZone, Geofencing, Decoy) |
| S-09 | ✅ Done | Security Hardening + OWASP MASVS L2 Audit |
| S-10 | ⏳ Pending | F-Droid + Play Store Release |

---

## Screenshots

> Screenshots will be added after S-08 (Feature Layer) completion.
> FLAG_SECURE is active — screenshots must be taken with developer override.

---

## Security Audit

Chameleon targets **OWASP MASVS Level 2** compliance. The full audit package is in [`docs/AUDIT_PACKAGE/`](docs/AUDIT_PACKAGE/).

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

1. **No internet permission** — except optional, user-initiated IFR verification
2. **Private keys never leave hardware Keystore** (StrongBox or TEE)
3. **Fail Secure** — on error, encrypt. Never decrypt.

---

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).  
100% open source. Fork it, audit it, contribute.

---

*Chameleon — Privacy that adapts.*
*Part of the [StealthX Platform](https://stealthx.tech) | [SecureCall](https://stealthx.tech) | [IFR Token](https://ifrunit.tech)*
