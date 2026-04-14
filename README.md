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
| S-00 | 🔨 Active | Repo init, SecureCall analysis |
| S-01 | ⏳ Pending | Gradle modules + CI/CD |
| S-02 | ⏳ Pending | Security Layer (Keystore + Argon2id) |
| S-03 | ⏳ Pending | AccessibilityService (AIDL) |
| S-04 | ⏳ Pending | Data Layer (Room + SQLCipher) |
| S-05 | ⏳ Pending | Domain Layer (XChaCha20 + Double Ratchet) |
| S-06 | ⏳ Pending | IFR Module (WalletConnect) |
| S-07 | ⏳ Pending | Compose UI |
| S-08 | ⏳ Pending | Feature Layer |
| S-09 | ⏳ Pending | Security Hardening + Audit |
| S-10 | ⏳ Pending | F-Droid + Play Store Release |

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

*Chameleon — Privatsphäre, die sich anpasst.*  
*Part of the [StealthX Platform](https://stealthx.tech) | [SecureCall](https://stealthx.tech) | [IFR Token](https://ifrunit.tech)*
