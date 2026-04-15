# Chameleon — Dependency Audit

_Assessed: 2026-04-15_

## Critical Dependencies

| Library | Version | License | Purpose | CVE Status |
|---------|---------|---------|---------|------------|
| lazysodium-android | 5.1.0 | MPL-2.0 | libsodium wrapper (XChaCha20, X25519, Argon2id) | No known CVEs |
| JNA | 5.14.0 | Apache-2.0 / LGPL-2.1 | JNI bridge for lazysodium | No known CVEs |
| SQLCipher | 4.5.4 | BSD-3 | Database encryption | No known CVEs |
| web3j-core | 4.12.0 | Apache-2.0 | Ethereum RPC (eth_call only) | No known CVEs |

## Android Dependencies

| Library | Version | License | Purpose |
|---------|---------|---------|---------|
| Kotlin | 2.0.21 | Apache-2.0 | Language |
| AGP | 8.7.0 | Apache-2.0 | Build system |
| Compose BOM | 2024.11.00 | Apache-2.0 | UI framework |
| Hilt | 2.52 | Apache-2.0 | Dependency injection |
| Room | 2.6.1 | Apache-2.0 | Database ORM |
| AndroidX Security Crypto | 1.1.0-alpha06 | Apache-2.0 | EncryptedSharedPreferences |
| AndroidX Biometric | 1.2.0-alpha05 | Apache-2.0 | Biometric authentication |
| Coroutines | 1.9.0 | Apache-2.0 | Async programming |
| Navigation Compose | 2.8.4 | Apache-2.0 | Screen navigation |
| Timber | 5.0.1 | Apache-2.0 | Logging (debug only) |

## Testing Dependencies

| Library | Version | License |
|---------|---------|---------|
| JUnit 5 | 5.11.3 | EPL-2.0 |
| MockK | 1.13.13 | Apache-2.0 |
| Robolectric | 4.13 | MIT |
| Coroutines Test | 1.9.0 | Apache-2.0 |

## Supply Chain Notes

- All dependencies from Maven Central or Google Maven
- No JitPack dependencies in production (only attempted for WalletConnect, currently disabled)
- `gradle/libs.versions.toml` is the single source of truth for all versions
- Dependabot integration recommended for automated CVE monitoring
