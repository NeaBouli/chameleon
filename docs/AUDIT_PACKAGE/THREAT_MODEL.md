# Chameleon — Threat Model

## Protected Assets

1. **User's plaintext messages** in third-party apps (WhatsApp, Telegram, etc.)
2. **Encryption keys** (X25519, symmetric session keys)
3. **Contact key bundles** (identity verification)
4. **IFR token balance and tier status** (economic privacy)
5. **Security rules and geofence locations** (behavioral patterns)
6. **Private Zone files** (photos, documents)

## Threat Actors

| Actor | Capability | Mitigation |
|-------|-----------|------------|
| **Casual observer** | Screen viewing | FLAG_SECURE, overlay encryption |
| **Device thief** | Physical access, unlocked | Biometric auth, per-use Keystore, SQLCipher |
| **Device thief** | Physical access, locked | Hardware Keystore (TEE/StrongBox), encrypted DB |
| **Malicious app** | Same device, different process | AIDL isolation, process=":crypto", exported=false |
| **Network attacker** | Man-in-the-middle | No cleartext traffic, no internet permission by default |
| **Cloud backup** | Google Drive access | allowBackup=false, data_extraction_rules excludes all |
| **Forensic analyst** | Full disk image | SQLCipher, SecureMemoryWipe, no plaintext on disk |
| **Coercion** | Forced unlock | Decoy profile (ELITE), plausible deniability |

## What Chameleon Does NOT Protect Against

- **Root/jailbreak:** A rooted device can read process memory. Chameleon warns but cannot prevent.
- **Hardware implants:** Keyloggers or modified firmware bypass all software protections.
- **Metadata:** Chameleon encrypts content, not metadata (who, when, which app).
- **Accessibility Service abuse:** The service has broad read access — the whitelist is the control.
- **Key compromise:** If a session key is extracted from memory during use, that session is compromised.
- **Side-channel attacks:** Timing attacks on Argon2id or XChaCha20 are not mitigated in software.
- **Social engineering:** No technical defense against users sharing their PIN or keys.

## Trust Boundaries

```
┌─────────────────────────────────────────────┐
│  AccessibilityService (untrusted context)   │
│  - Reads text, injects text                 │
│  - NO crypto, NO rules, NO keys             │
└──────────────────┬──────────────────────────┘
                   │ AIDL IPC
┌──────────────────▼──────────────────────────┐
│  CryptoService (process=":crypto")          │
│  - Isolated Android process                 │
│  - ALL crypto decisions here                │
│  - SodiumInitializer in own JNI context     │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Android Keystore (TEE / StrongBox)         │
│  - Private keys never leave hardware        │
│  - Per-use biometric authentication         │
└─────────────────────────────────────────────┘
```
