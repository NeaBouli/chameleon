# Chameleon — Full-Scope Security & Crypto Audit

- **Series:** Collateral Web3 Open Audits (sixth engagement: StealthX clients)
- **Date:** 2026-09-19 (recon 2026-09-15, resumed and verified 2026-09-19)
- **Target:** NeaBouli/chameleon @ `d48e37989125392b902b7298d6fd0af472f84d6d`
- **Scope:** full Kotlin tree (app, core, data, domain, features, presentation, security, stealthx-crypto, stealthx-access), build/CI, overlay/messenger fail-closed wiring, crypto implementation, crypto delta vs securechat's fork
- **Method:** deep-recon agent + lead verification of every finding at exact file:line; read-only; no secrets read; no builds
- **Prior baseline:** BRIDGE.md 2026-07-12 internal audit ("not sell-ready", overlay+messenger fail-closed, 334 tests green) — re-checked below
- **Register:** CHA-01 … CHA-13 (this report) — **0 Critical / 0 High / 5 Medium / 7 Low / 1 Info**

---

## Executive summary

Chameleon's headline state is honestly disabled and **genuinely fail-closed** — verified in depth: the overlay is triple-gated (preferences migration forces it off, the AccessibilityService is **not declared in any manifest** so the OS can never enable it, and `notifySecurityLevel()` has zero callers); the cross-device messenger is `check(false)`-gated with a static UI notice. The entitlement stack is strict (same verifier as SecureChat, plus the stricter key-pair validation that repo lacks). The real findings are one layer down: an unchecked X25519 low-order path in the shared crypto (latent), relay-pushed contacts silently saved as "verified", the padding-length leak, and a default-on background listener that phones home an unauthenticated identity.

## Severity table

| ID | Severity | Title |
|----|----------|-------|
| CHA-01 | Medium (latent High) | X25519 low-order/identity output unchecked in `computeSharedSecret` |
| CHA-02 | Medium | Relay-pushed contacts saved silently as `isVerified=true` — TOFU self-signature, no freshness, no user confirmation |
| CHA-03 | Medium | Padding defeated by cleartext `paddedLength` (wire, relay JSON, Room) |
| CHA-04 | Medium | Default-on persistent listener phones home; relay identity unauthenticated; sxId in URL query |
| CHA-05 | Low | Attestation verifier claims exceed implementation (no chain/challenge validation) |
| CHA-06 | Low | Safety-number encoding bug: `and 55` bias instead of 00–99 |
| CHA-07 | Low | Decoy PIN has no attempt throttling |
| CHA-08 | Low (latent) | Proximity transports unauthenticated + unbounded `readBytes()`; BT pairing logic dead |
| CHA-09 | Medium | Two divergent ratchets: spec DoubleRatchet is dead code; wired ad-hoc ratchet wipes skipped keys |
| CHA-10 | Low | Release builds unminified; entitlement public key absent-by-default (verify release injection) |
| CHA-11 | Low | `CLAUDE_CODE_START.md` overclaims/stale (nonexistent module, hardcoded IFR thresholds, direct-to-main) |
| CHA-12 | Info | TLS pins — verified live (all 3 match); leaf-pin rotation note |
| CHA-13 | Low | Exported BROWSABLE `stealthx://add` deep link auto-navigates unlocked app to AddContact (STX-31 cross-ref) |

---

## CHA-01 — Medium (latent High) — X25519 low-order/identity output unchecked

**Evidence (lead-verified):** `stealthx-crypto/.../ChameleonCrypto.kt:216-220` — `computeSharedSecret` discards `cryptoScalarMult`'s boolean; libsodium returns false on all-zero shared secret (low-order peer key). A malicious contact can force a predictable session key. Feeds `DoubleRatchet.kdfRootKey` and `ChatSessionRepository.createSession/withReceiveChain` (`:117,144`). Currently unreachable (messenger fail-closed) — hence Medium, latent High. The same defect exists in securechat's fork (SCT-09).

**Recommendation:** `check(sodium.cryptoScalarMult(...))` or switch to `cryptoKx*`; fix in **both** forks (and delete one fork — see CHA-report 2 on the crypto fork divergence).

## CHA-02 — Medium — Relay-pushed contacts saved silently as `isVerified=true`

**Evidence (lead-verified):** `data/.../exchange/ContactExchangeManager.kt:170-223` — the bundle signature is verified **against the bundle's own key** (TOFU self-signed: proves possession, not identity); no user confirmation (contrast the QR/deep-link path requiring a manual tap, `AddContactScreen.kt:196-198`); no `createdAt` freshness bound → replay of captured frames; the relay (or anyone emitting CONTACT_EXCHANGE after an unauthenticated IDENTIFY, CHA-04) can insert arbitrary contacts; attacker-controlled `h` handle is stored unvalidated as display name (`:190,215`) — homograph/impersonation bait; the entity is persisted with `isVerified = true` (`:218`).

**Recommendation:** user-confirm dialog for relay-received contacts, replay window on `createdAt`, `isVerified=false` until out-of-band verification.

## CHA-03 — Medium — Padding defeated by cleartext `paddedLength`

**Evidence:** `EncryptedPayload.paddedLength = plaintext.size` (`ChameleonCrypto.kt:85`) is serialized in the clear into the overlay wire string (`CryptoService.kt:102`, `OverlayEngine.kt:93`), relay JSON (`ServerRelayTransport.kt:127`) and Room (`MessageEntity`). Any observer who can see the ciphertext sees the exact plaintext length — the 256-byte padding (`ChameleonCrypto.kt:324-327`) buys nothing. Same class as SCT-05 (securechat's wire envelope), different paths. Either drop padding honestly or stop transmitting/storing the exact length.

## CHA-04 — Medium — Default-on persistent listener phones home; unauthenticated relay identity

**Evidence (lead-verified):** `backgroundListenerEnabled` **defaults `true`** (`AppPreferences.kt:101-103`); `MainActivity.onResume` (`:92-94`) and `BootReceiver.kt:37-39` start a foreground-service WebSocket that sends `IDENTIFY {sxId}` (`ContactExchangeManager.kt:94-97`) — the server learns identity/IP/uptime with no first-run consent; `privacy.html:43` conditions collection on "when the contact listener is enabled" **without disclosing the default-on**. No proof-of-possession on IDENTIFY → anyone can claim any sxId and receive its relayed exchanges/messages (client-side instance of STX-01). Additionally the sxId rides in the URL query at `ServerRelayTransport.kt:76` (log/proxy exposure).

**Recommendation:** default the listener to off (or ask at first run); sign IDENTIFY with the Ed25519 key; move sxId out of the query string.

## CHA-05 — Low — Attestation claims exceed implementation

`HardwareAttestationVerifier.kt:36` documents "Root CA (Google root — pinned)"; code only DER-walks cert[0]'s extension for the security-level enum (`:167-209`) — no chain validation, no challenge comparison, no verifiedBootState/rootOfTrust parsing. Failure degrades to PROTECTED (safe direction); dead code in production. Fix or document honestly.

## CHA-06 — Low — Safety-number encoding bug

`MessengerEngine.kt:92-94` — `it.toInt() and 0xFF % 100` evaluates as `it.toInt() and (0xFF % 100)` = `and 55` → digits biased to 00–55, not uniform 00–99. Reduces visual-verification entropy; engine currently unreachable for messaging.

## CHA-07 — Low — Decoy PIN has no attempt throttling

Policy is ≥4 digits, digits-only (`DecoySetupViewModel.kt:82-94`); `DecoyAuthViewModel.submitPin` has no lockout/backoff (`:39-74`). Argon2id 64MB raises per-try cost, but a 4-digit space is exhaustible against a coerced/unattended device. Positive: both hashes always computed (`DecoyProfileEngine.kt:82-83`); hashes/salts in EncryptedSharedPreferences.

## CHA-08 — Low (latent) — Proximity transports unauthenticated + unbounded reads

`BluetoothTransport.kt:115` and `WifiDirectTransport.kt:150` do `inputStream.readBytes()` with no size cap (memory DoS) and accept attacker-chosen `from` fields; BT matching `it.name == recipientId` (`:62-64`) can never match an `sx_` ID (dead pairing logic); WifiDirect opens TCP :8742 on the LAN (`:100-108`). All blocked today by the repository gate — must be fixed before any re-enable.

## CHA-09 — Medium — Two divergent ratchets

The spec-shaped `DoubleRatchet` (`stealthx-crypto/DoubleRatchet.kt`, **byte-identical** to securechat's) is used only by its own test; the wired messenger uses the incompatible ad-hoc ratchet in `ChatSessionRepository`, which additionally **wipes skipped message keys instead of storing them** (`:82-88`) → out-of-order delivery un-decryptable by design; even the green `DoubleRatchetTest` does not cover the production path. (SecureChat's version of this is worse because it also makes public claims — SCT-01/04; Chameleon's messenger is fail-closed, so the exposure is latent.)

## CHA-10 — Low — Release unminified; entitlement key absent-by-default

`app/build.gradle.kts:112-113` `isMinifyEnabled=false, isShrinkResources=false` — ships the dead overlay/messenger code and eases reversing. `data/build.gradle.kts:12-18` defaults `ENTITLEMENT_PUBLIC_KEY_BASE64` to `""` (activation then fails closed as `entitlement_not_configured`) — the release pipeline must inject the real key; verify the published APK carries it.

## CHA-11 — Low — `CLAUDE_CODE_START.md` overclaims/stale

Documents a nonexistent `:stealthx-ifr` module, hardcoded IFR thresholds (2,000/6,000) contradicting current "no token threshold" copy, an outdated "no INTERNET except IFR" constraint, and direct-to-main push instructions. Public contract addresses only — no secrets — but a bad instruction file for agents.

## CHA-12 — Info — TLS pins verified live

`StealthXApiTls.kt:13-15` pins three SPKI hashes for `api.stealthx.tech` (OkHttp `CertificatePinner`, no expiry). **Lead live check 2026-09-19: all three match the live chain exactly** (leaf `1e85xNSE…`, intermediates `nWN7PSep…`/`fk6IOKit…`). Rotation note: when the leaf renews, the leaf pin breaks closed and the intermediate pins carry — consider dropping the leaf pin or tracking its rotation in the release checklist.

## CHA-13 — Low — Exported deep link auto-navigation (STX-31 cross-ref)

The app generates `https://stealthx.tech/invite/?app=chameleon&link=…` (`StealthXIdentity.kt:79-82`). App-side consumption is sound (full Ed25519 verification + sxId↔key binding + manual tap, `AddContactViewModel.kt:99-118`). But the deep link is BROWSABLE+exported (`AndroidManifest.xml:78-83`) and a pending URI auto-navigates the unlocked app to AddContact (`StealthXNavGraph.kt:78-85`) — phishing-UX vector, mitigated by mandatory confirmation. The platform-side `?link=` validation hole remains STX-31 (stealth repo).

---

## Prior-baseline re-check (BRIDGE.md 2026-07-12)

| Baseline claim | Verdict |
|---|---|
| Overlay fail-closed in client | **HOLDS — stronger than documented (triple gate):** prefs migration forces off (`AppPreferences.kt:73-80`); AccessibilityService **not declared in any manifest** (orphaned config at `core/.../accessibility_service_config.xml`); `notifySecurityLevel()` has zero callers; `CryptoService.processText` passthrough at securityLevel 0; UI static "unavailable" |
| Device-local random overlay key (blocker) | **STILL PRESENT** (`CryptoService.kt:79-93`: random 32B, Keystore-wrapped, never shared) |
| No verified security-level activation path | **HOLDS** (attestation verifier referenced only by its own test) |
| Messenger session derivation incompatible | **HOLDS, sharper** (fresh ephemeral vs peer static on both sides → send/receive chains diverge; fail-closed via `check(crossDeviceMessengerEnabled)` + static UI notice) |
| Transports not started | **PARTIALLY CHANGED** — messenger transports never start, but `ContactListenerService` runs a default-on persistent WS (CHA-04) |
| Entitlement startup + 7-day refresh | **VERIFIED** (`ChameleonApplication.kt:58-73`; #45 adds timeout + single-completion + retry classification) |
| Onboarding persistence before navigation | **VERIFIED** (`SetupScreen.kt:64-66`) |
| Gradle guard rejects client-side paid unlock | **EXISTS, CI-wired, string-pattern-only** (bypassable by term splitting; `verifyNoAppIfrWalletCode` omits the `security/` module — CI tripwire, not a control) |
| 334 tests green | NOT RE-VERIFIED (read-only; 192 `@Test` annotations counted — parameterized runs plausibly inflate to 334) |

## stealthx-crypto fork delta (vs securechat)

- `DoubleRatchet.kt` byte-identical; `security/` module byte-identical (md5); `SodiumInitializer.kt` comments/order only.
- `ChameleonCrypto.kt`: **chameleon is the newer/stricter variant** — adds `isValidX25519KeyPair`/`isValidSigningKeyPair` with constant-time `MessageDigest.isEqual` (`:197-208,235-246`), consumed by `StealthXIdentity`'s classifier. Nothing missing relative to securechat.
- `EntitlementTokenVerifier.kt`: functionally identical except the product namespace (`chameleon_` vs `securechat_`).
- Build: chameleon `compileSdk=35` (securechat 36); lazysodium **5.1.0** here vs 5.2.0 in securechat; no transitive-dependency exclusions here.
- **Meta-finding for report 2:** the "shared" module is two diverging forks — fixes (like the CHA-01 check or SCT-02's binding) land in one repo and not the other.

## Verified strengths

- Overlay genuinely fail-closed at **three independent layers** (prefs migration, manifest absence, zero security-level callers) — no in-app or intent-driven re-enable path found.
- Entitlement: strict parser, claim whitelist, aud/sub device binding, 31-day cap; activation+refresh verify before persisting; release-grade tier cache HMAC-SHA256 over Keystore key, fails to FREE; dev tier overrides gated off in all release flavors + `verifyNoReleaseTierOverrides` in CI.
- Storage: SQLCipher DB with Keystore-wrapped random passphrase; identity private keys in EncryptedSharedPreferences with integrity classifier and safe recovery-wipe ordering; `allowBackup=false` + full-exclusion rules.
- Exported surface minimal: only MainActivity exported (deep link + NFC); services/receivers `exported="false"`; debug receivers confined to the debug source set.
- Crypto hygiene in active paths: fresh nonces, AAD binding, key wiping, constant-time key-pair validation, Argon2id for PINs, dual-hash PIN compare; sodium init fails closed on Android.
- No committed secrets/keystores (git ls-files verified empty for key patterns).
