# Community Audits — Collateral Web3 Open Audits

External audit of Chameleon, commissioned by the repository owner and published
with his explicit authorization (public series like IFR/Ekklesia/Stealth/
Prometheus/TrueRepublic/SecureChat). **Report-only**: no audited code changed;
live checks anonymous GET only; no secrets read. Companion audit of the sibling
app: NeaBouli/securechat (same series).

- **Audit date:** 2026-09-19 (recon 2026-09-15, resumed/verified 2026-09-19)
- **Baseline:** `main` @ `d48e37989125392b902b7298d6fd0af472f84d6d`
- **Register:** CHA-01 … CHA-26 — **0 Critical / 1 High / 10 Medium / 13 Low / 2 Informational**
- **Prior baseline:** internal readiness audit 2026-07-12 (BRIDGE.md, "not sell-ready") — re-checked: overlay/messenger fail-closed state **verified stronger than documented** (triple gate); details in the full-scope report
- **Finding tracker:** umbrella issue (see issue list) with one checkbox per finding

## Reports

| # | Report | Register | Severity (C/H/M/L/I) | SHA-256 |
|---|--------|----------|----------------------|---------|
| 1 | [Full-scope security & crypto](cha-full-scope-audit-2026-09-19.md) | CHA-01…13 | 0/0/5/7/1 | `2e1a57218a50d8f2b6ac91e2f8d17f37b92f4338593c9298ea635eccbaede659` |
| 2 | [Surfaces, content & AI-readiness](cha-surfaces-content-audit-2026-09-19.md) | CHA-14…26 | 0/1/5/6/1 | `593186cd56f19dc28e4ebd367b6b7d6da997082b390368b7102d13b2b5e8adaf` |

## Headline findings

- **CHA-14 (High):** app-generated invite URLs carry sxId + full key bundle +
  optional handle into the GA4-tracked, unvalidated `?link=` page on
  stealthx.tech (`data/.../StealthXIdentity.kt:79-82`; platform side = STX-29/31).
- **CHA-01 (Medium, latent High):** X25519 low-order/identity output unchecked
  in `computeSharedSecret` — a malicious contact could force a predictable
  session key (`stealthx-crypto/.../ChameleonCrypto.kt:216-220`; same defect in
  securechat's fork, SCT-09).
- **CHA-02 (Medium):** relay-pushed contacts saved silently as `isVerified=true`
  — TOFU self-signature, no freshness bound, no user confirmation
  (`ContactExchangeManager.kt:170-223`).
- **CHA-04 (Medium):** background listener defaults ON and phones home an
  unauthenticated identity; privacy.html doesn't disclose the default
  (`AppPreferences.kt:101-103`, `ContactExchangeManager.kt:94-97`).
- **CHA-09 (Medium):** two divergent ratchets — spec DoubleRatchet is dead code,
  the wired ad-hoc ratchet wipes skipped message keys (out-of-order = loss).
- **CHA-15/16/17/18/19 (Medium):** "0 Data Collected" vs own privacy docs; wiki
  hub invites the AccessibilityService the manual refuses; LOGBUCH documents
  removed wallet code + 2,000/6,000 IFR tier thresholds; 129 GPL-3.0 files vs
  proprietary LICENSE + "open-source" mislabel; security contact fragmentation.

## Verified strengths (selection)

Overlay genuinely fail-closed at **three independent layers** (prefs migration,
manifest absence, zero activation callers); messenger fail-closed with static
UI notice; entitlement stack strict and device-bound; SQLCipher +
EncryptedSharedPreferences; minimal exported surface; crypto hygiene in active
paths (fresh nonces, AAD, wiping, constant-time compares); **TLS pins verified
live against the current chain 2026-09-19**; zero trackers on the site;
chameleon's crypto fork is the *stricter* variant (key-pair validation the
securechat fork lacks).
