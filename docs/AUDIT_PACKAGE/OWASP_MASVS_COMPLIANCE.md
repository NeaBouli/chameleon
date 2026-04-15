# Chameleon — OWASP MASVS L2 Compliance Matrix

_Assessment date: 2026-04-15_
_Target: MASVS Level 2 (Defense-in-Depth)_

## MSTG-STORAGE — Data Storage

| ID | Requirement | Status | Evidence |
|----|-------------|--------|----------|
| STORAGE-1 | No sensitive data in plaintext SharedPreferences | ✅ PASS | `EncryptedSharedPreferences` (AES256-SIV/GCM) via MasterKey |
| STORAGE-2 | No sensitive data in backups | ✅ PASS | `allowBackup=false`, `fullBackupContent=false`, `data_extraction_rules.xml` excludes all domains |
| STORAGE-3 | No sensitive data in logs | ✅ PASS | No `Log.d`/`println` in crypto/security modules. Timber debug tree only in debug builds. |
| STORAGE-4 | No sensitive data shared with third parties | ✅ PASS | No analytics, no telemetry, no crash reporting SDK |
| STORAGE-5 | Keyboard cache disabled for sensitive inputs | ⚠️ PARTIAL | `android:inputType="textPassword"` on PIN fields. General text fields not restricted. |
| STORAGE-6 | No sensitive data in clipboard | ✅ PASS | `ClipboardCleaner` auto-clears after 60 seconds |
| STORAGE-7 | No sensitive data exposed via IPC | ✅ PASS | AIDL services `exported=false`, `ProcessTextResult` carries no raw keys |

## MSTG-CRYPTO — Cryptography

| ID | Requirement | Status | Evidence |
|----|-------------|--------|----------|
| CRYPTO-1 | Strong algorithms only | ✅ PASS | XChaCha20-Poly1305, Argon2id, X25519, Ed25519. No MD5/SHA1/DES/3DES/RC4. |
| CRYPTO-2 | Cryptographic random only | ✅ PASS | `sodium.randomBytesBuf()` (libsodium CSPRNG) and `SecureRandom`. No `java.util.Random()`. |
| CRYPTO-3 | No hardcoded key material | ✅ PASS | All keys from Keystore or derived via Argon2id. Contract addresses are public constants, not secrets. |
| CRYPTO-4 | No deprecated algorithms | ✅ PASS | AES-GCM only inside Keystore for key wrapping (hardware-enforced). |
| CRYPTO-5 | Key management | ✅ PASS | Android Keystore with per-use auth (-1), keys wiped after use |

## MSTG-NETWORK — Network Communication

| ID | Requirement | Status | Evidence |
|----|-------------|--------|----------|
| NETWORK-1 | No cleartext traffic | ✅ PASS | `usesCleartextTraffic=false` in manifest |
| NETWORK-2 | Certificate validation | ⚠️ N/A | No direct HTTPS calls from Chameleon. web3j uses system trust store. |
| NETWORK-3 | Certificate pinning | ⚠️ N/A | Only eth_call to public RPC endpoints. No proprietary API to pin. |

## MSTG-PLATFORM — Platform Interaction

| ID | Requirement | Status | Evidence |
|----|-------------|--------|----------|
| PLATFORM-1 | Minimum permissions | ⚠️ PARTIAL | Location + Camera are runtime-requested and tier-gated. Could be split into install variants. |
| PLATFORM-2 | IPC interfaces secured | ✅ PASS | AIDL services `exported=false`, `BIND_ACCESSIBILITY_SERVICE` system permission |
| PLATFORM-3 | No JavaScript in WebViews | ✅ PASS | No WebViews used |
| PLATFORM-4 | No deep link injection | ✅ PASS | No exported deep link receivers |

## MSTG-CODE — Code Quality

| ID | Requirement | Status | Evidence |
|----|-------------|--------|----------|
| CODE-1 | Code obfuscation | ✅ PASS | R8/ProGuard enabled in release, `-keep` rules for crypto classes |
| CODE-2 | No debug code in release | ✅ PASS | `isDebuggable=false` in release buildType, no debug logs |
| CODE-3 | Debug detection | ⚠️ PARTIAL | No active debugger detection. ELITE tier could add this via `DecoyProfileEngine`. |
| CODE-4 | Anti-tampering | ⚠️ PARTIAL | No signature verification at runtime. Planned for S-10. |

## Summary

| Category | Pass | Partial | Fail |
|----------|------|---------|------|
| STORAGE | 6 | 1 | 0 |
| CRYPTO | 5 | 0 | 0 |
| NETWORK | 1 | 2 | 0 |
| PLATFORM | 3 | 1 | 0 |
| CODE | 2 | 2 | 0 |
| **Total** | **17** | **6** | **0** |

**Overall: MASVS L2 substantially compliant. 6 partial items documented with mitigations.**
