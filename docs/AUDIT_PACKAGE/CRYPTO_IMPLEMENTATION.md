# Chameleon — Cryptographic Implementation

## Overview

All cryptographic operations are implemented in the `:stealthx-crypto` module via [lazysodium-android 5.1.0](https://github.com/terl/lazysodium-android), which wraps libsodium.

**No other crypto library is used. No AES-GCM. No BouncyCastle.**

## Algorithms

| Algorithm | Purpose | Library | Parameters |
|-----------|---------|---------|------------|
| XChaCha20-Poly1305 | Symmetric AEAD encryption | lazysodium | 32B key, 24B nonce, 16B tag |
| X25519 | Diffie-Hellman key exchange | lazysodium | 32B public, 32B private |
| Ed25519 | Digital signatures | lazysodium | 32B public, 64B private |
| Argon2id | Password-based KDF | lazysodium | 64MB memory, 3 iterations |
| HKDF-SHA256 | Key derivation (Double Ratchet) | javax.crypto.Mac | RFC 5869 |
| HMAC-SHA256 | IFR cache tamper detection | Android Keystore | Hardware-backed key |

## XChaCha20-Poly1305 Details

- **Wire Format:** `[ciphertext + 16B auth tag]` (nonce stored separately)
- **Nonce:** 24 bytes, always fresh via `sodium.randomBytesBuf(24)` — never reused
- **AAD:** Package name of source app (context binding)
- **Padding:** Plaintext padded to 256-byte boundary before encryption (prevents size analysis)
- **Implementation:** `ChameleonCrypto.encrypt()` / `ChameleonCrypto.decrypt()`

## Double Ratchet Protocol

Based on [Signal's Double Ratchet specification](https://signal.org/docs/specifications/doubleratchet/).

- **DH Ratchet:** X25519 key pairs, rotated on each message exchange
- **KDF Root Key:** HKDF-SHA256 with info `"ChameleonRatchetRoot_v1"`
- **KDF Chain Key:** HKDF-SHA256 with info `"ChameleonRatchetChain_v1"`
- **KDF Message Key:** HKDF-SHA256 with info `"ChameleonRatchetMessage_v1"`
- **Forward Secrecy:** Old chain keys wiped immediately via `ChameleonCrypto.wipeBytes()`
- **Max Skipped Messages:** 100 (prevents DoS via large counter jumps)

## Argon2id Parameters

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Memory | 65,536 KB (64 MB) | Resists GPU/ASIC attacks |
| Iterations | 3 | Balances security vs. mobile UX |
| Parallelism | 4 | Utilizes multi-core ARM SoCs |
| Salt | 32 bytes (SecureRandom) | Unique per derivation |
| Output | 32 bytes (256-bit key) | Standard key size |

## Key Storage

- **Android Keystore:** Hardware-backed (StrongBox or TEE fallback)
- **Per-use authentication:** `setUserAuthenticationValidityDurationSeconds(-1)`
- **Database key:** AES-256-GCM wrapped in Keystore, used as SQLCipher passphrase
- **HMAC key:** Hardware Keystore, used for IFR cache tamper detection

## Memory Wiping

- All key material wiped after use via `Arrays.fill(data, 0)`
- `ChameleonCrypto.wipeBytes()` and `ChameleonCrypto.wipeChars()` called on every key/password
- `SecureMemoryWipe.secureDelete()` uses DoD 5220.22-M 3-pass file wipe

## Code Locations

| Component | File |
|-----------|------|
| AEAD Encrypt/Decrypt | `stealthx-crypto/src/.../ChameleonCrypto.kt` |
| HKDF-SHA256 | `stealthx-crypto/src/.../ChameleonCrypto.kt:hkdf()` |
| Double Ratchet | `stealthx-crypto/src/.../DoubleRatchet.kt` |
| Sodium Init | `stealthx-crypto/src/.../SodiumInitializer.kt` |
| Keystore Manager | `security/src/.../KeystoreManager.kt` |
| Memory Wipe | `security/src/.../SecureMemoryWipe.kt` |
