# SecureCall → Chameleon — Architektur-Referenz
_Analysiert: 2026-04-15 | Quelle: ~/Desktop/stealth_

> Nur Patterns und Konzepte — kein kopierter Code.
> SecureCall = Rust/JNI, Chameleon = Kotlin/lazysodium.

---

## 1. Crypto-Stack Patterns

### XChaCha20-Poly1305 AEAD
- **Wire Format:** `[24-byte nonce] [ciphertext] [16-byte auth tag]`
- **Key:** 256-bit, automatisch zeroized nach Verwendung
- **Nonce:** Zufällig pro Frame (keine Counter)
- **Replay Detection:** RFC 6479 Sliding-Window Bitmap (64 Nonces)
- **Chameleon-Adaption:** lazysodium `crypto_aead_xchacha20poly1305_ietf_encrypt()` liefert gleiches Format

### X25519 Key Exchange
- Raw Keypair Format: `[private (32B) | public (32B)]` = 64 bytes
- Low-Order-Point Check: Reject all-zero shared secrets
- Automatic Zeroize on Drop
- **Chameleon-Adaption:** lazysodium `cryptoScalarMult()` + manueller Zeroize via `SecureMemoryWipe`

### Key Derivation (HKDF-SHA256)
- Shared Secret → HKDF Expand → 32-byte Session Key
- Info-String kontextspezifisch: `"SecureCall-AEAD-Key-v1"`
- Deterministisch: same input = same key
- Kotlin Fallback: `HkdfSha256.kt` in `ghostnet/crypto/`
- **Chameleon:** lazysodium hat kein HKDF — Argon2id als KDF ODER eigene HKDF-Impl in `:stealthx-crypto`

### Kein Double Ratchet in SecureCall
- Statische Sessions (ein Key pro Call, nie persistiert)
- Re-Keying nur bei Call-Restart
- **Chameleon-Entscheidung:** Double Ratchet NÖTIG für Messenger-Feature (langlebige Sessions)

### Mandatory Native Crypto
- Pattern: `if (!CoreCrypto.isNativeAvailable()) throw SecurityException()`
- Fail-safe: Crash statt Fallback auf schwache Crypto
- **Chameleon:** Gleicher Ansatz — `SodiumInitializer.ensureInit()` in Application.onCreate()

---

## 2. Tier-System (3-Layer)

### Architektur:
```
Layer 1: Compile-Time   → BuildConfig.FLAVOR (free/pro/premium)
Layer 2: Activated Tier  → SharedPreferences (IFR/Manual/Activation Code)
Layer 3: Runtime         → max(Layer1, Layer2)
```

### FeatureProvider Interface
```kotlin
interface FeatureProvider {
    val tier: String
    val maxCallDurationMinutes: Int
    val rootDetectionBlocks: Boolean
    val screenCaptureDetection: Boolean
    // ... 18+ flags
}
```

### FeatureProviderRegistry (Singleton)
- Volatile `FeatureProvider`-Instanz → Thread-safe Reads
- Initialisiert bei App-Start durch `TierManager.applyTier()`
- Kein DI-Framework (kein Hilt/Dagger) — bewusste Entscheidung für Performance
- **Chameleon:** Hilt verwenden (Multi-Modul Projekt profitiert von DI), TierGate.kt zentralisiert

### Feature Flags per Tier (SecureCall)
| Feature | Free | Pro | Premium |
|---------|------|-----|---------|
| Max Call Duration | 15 min | unlimited | unlimited |
| Device Attestation | false | true | true |
| Root Detection Blocks | false | true | true |
| Screen Capture Detection | false | false | true |
| Debugger Detection | false | false | true |
| Emulator Detection | false | false | true |

---

## 3. IFR-Integration

### IfrLockManager Pattern
- Stateless Verifikation (alles in SharedPreferences)
- Storage Keys: `ifr_wallet_address`, `ifr_tier`, `ifr_locked_amount`, `ifr_last_verified`, `ifr_verification_method`

### Zwei Verifikations-Methoden
| | Manual | WalletConnect/SIWE |
|---|---|---|
| Expiry | 30 Tage | Permanent |
| Re-Verify | Jedes Mal | Alle 24h (Balance) |
| Geräte | 1 Wallet/Gerät | Unbegrenzt |
| Beweis | Keine Signatur | ECDSA Signatur |

### SIWE Flow (Sign-In with Ethereum)
```
1. GET /siwe/challenge?deviceId=xxx → {nonce, message}
2. MetaMask Deep Link: metamask://dapp/stealthx.tech/siwe.html?nonce=...
3. User signiert → kopiert Signatur in App
4. POST /siwe/verify {walletAddress, signature, nonce, deviceId}
5. Backend: ethers.verifyMessage() → Adresse recovered
6. Backend: ifrToken.balanceOf(wallet) → Threshold Check
7. Response: {success, tier, lockedAmount, walletBound}
```

### Thresholds
| | SecureCall | Chameleon |
|---|---|---|
| Pro | 1,000 IFR (9 dec) | 2,000 IFR (9 dec) |
| Premium/Elite | 5,000 IFR (9 dec) | 6,000 IFR (9 dec) |

### WebSocket IFR Verification
```
Client → Server: {type: "VERIFY_IFR_LOCK", walletAddress: "0x..."}
Server → Client: {type: "IFR_LOCK_RESULT", success: true, tier: "PRO", lockedAmount: "2000"}
Timeout: 15 Sekunden via Handler.postDelayed
```

### Error Codes
```
invalid_address    → Nicht 0x + 40 hex
signature_invalid  → SIWE Signatur ungültig
wallet_bound       → Bereits an anderes Gerät
insufficient       → Balance unter Threshold
balance_check_failed → RPC nicht erreichbar
challenge_expired  → Nonce Timeout (5 min)
```

### WalletConnect SDK → Entfernt
- Reown SDK hatte Relay 403 Bug (reown-kotlin issue #240)
- Ersetzt durch manuelle SIWE via Deep Links + In-App Browser
- **Chameleon:** WalletConnect v2 SDK (2.7.0) evaluieren, Fallback auf Deep Links vorbereiten

---

## 4. Android Architecture Patterns

### Kein DI Framework
- Singletons: `object GhostNetCryptoManager`, `object TierManager`
- Service Binding via Android Framework
- **Chameleon:** Hilt verwenden — Multi-Modul profitiert stark von DI

### WebSocket Service (Bound Service)
- `LocalBinder` Pattern für IPC
- Volatile Connection State Flags
- Callback-basiert: `statusCallbackOnline`, `errorCallback`, `_onIncomingCall`

### Security Monitor
- `SecureModeMonitor.kt` + `SecureModeAdvanced.kt`
- Erkennung: Root, Debugger, Emulator, Screen Capture
- Verhalten tierabhängig (Premium: terminate, Pro: block, Free: warn)

---

## 5. Entscheidungen für Chameleon

### Übernehmen ✅
- Wire Format XChaCha20 (24+cipher+16)
- Zeroize-Pattern für Schlüssel
- Fail-safe Crypto (crash statt fallback)
- IfrLockManager SharedPreferences Approach
- 24h Re-Verifikation + 30-Tage Manual Expiry
- Feature Flags per Tier
- Security Monitor Pattern (Root/Debugger/Emulator)

### Anpassen 🔄
- Hilt statt Manual Singletons (Multi-Modul)
- Double Ratchet für Messenger (SecureCall hat keinen)
- Argon2id statt HKDF (lazysodium-native)
- TierGate.kt statt FeatureProvider (zentralisierter)
- WalletConnect v2 SDK evaluieren statt nur Deep Links

### Vermeiden ❌
- BouncyCastle
- AES-GCM
- Tier-Checks im UI-Code (nur via TierGate)
- Crypto-Code außerhalb `:stealthx-crypto`
- Hardcodierte TURN Credentials

---

## 6. Relevante Dateien (Referenz)

| Bereich | Pfad |
|---------|------|
| Crypto AEAD | `stealth/core_crypto/src/aead/mod.rs` |
| X25519 | `stealth/core_crypto/src/identity/mod.rs` |
| KDF | `stealth/core_crypto/src/session/mod.rs` |
| JNI Bridge | `stealth/core_crypto/src/ffi/mod.rs` |
| Kotlin Crypto | `stealth/client_android/.../ghostnet/crypto/` |
| FeatureProvider | `stealth/client_android/.../config/FeatureProvider.kt` |
| TierManager | `stealth/client_android/.../config/TierManager.kt` |
| IfrLockManager | `stealth/client_android/.../config/IfrLockManager.kt` |
| WalletConnect | `stealth/client_android/.../wallet/WalletConnectManager.kt` |
| WebSocket | `stealth/client_android/.../net/WebSocketService.kt` |
| Backend IFR | `stealth/backend/signaling/src/server.js` |
| Security Design | `stealth/docs/SECURITY_DESIGN.md` |

---

_Dieses Dokument ist eine Referenz, kein Implementierungsplan._
_Implementierung folgt den Steps in LOGBUCH.md._
