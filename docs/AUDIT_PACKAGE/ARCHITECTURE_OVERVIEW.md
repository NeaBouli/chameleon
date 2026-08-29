# Chameleon — Architecture Overview

## Layer Architecture

```
┌─────────────────────────────────────────────────────┐
│  :app — Entry point, Hilt DI graph, MainActivity    │
├─────────────────────────────────────────────────────┤
│  :presentation — Compose UI, Navigation, ViewModels │
├──────────┬──────────┬──────────┬──────────┬─────────┤
│ :overlay │:messenger│:privatez │:geofence │ :decoy  │
│  FREE    │  PRO     │  PRO     │  ELITE   │ ELITE   │
├──────────┴──────────┴──────────┴──────────┴─────────┤
│  :domain — EncryptionEngine, TierGate, RuleEngine   │
├─────────────┬───────────────────┬───────────────────┤
│ :stealthx-  │    :security      │    :core          │
│   crypto    │   Keystore, Wipe  │  A11y, AIDL, IPC  │
├─────────────┤                   ├───────────────────┤
│ :stealthx-  │    :data          │                   │
│   access    │  Room+SQLCipher   │                   │
├─────────────┴───────────────────┴───────────────────┤
│  :shared — Models, Extensions (zero dependencies)   │
└─────────────────────────────────────────────────────┘
```

## Dependency Rules (Enforced)

| Module | May depend on | NEVER depends on |
|--------|--------------|-----------------|
| :shared | nothing | anything |
| :stealthx-crypto | :shared | :security, :data, :domain |
| :security | :shared | :stealthx-crypto, :data |
| :domain | :stealthx-crypto, :shared | :data (interfaces only) |
| :core | :stealthx-crypto, :security, :shared | :data |
| :data | :domain, :stealthx-crypto, :security, :shared | :core |
| :stealthx-access | :shared | network, wallet and payment code |
| :features:* | :domain, :data, :shared | :stealthx-crypto |
| :presentation | :domain, :features:*, :shared | :data, :core |
| :app | all | — |

## AIDL Process Isolation

The `CryptoService` runs in `android:process=":crypto"` — a separate Android process. The `AccessibilityService` communicates exclusively via AIDL (`ICryptoBridge`).

**AccessibilityService responsibilities:**
- Read text from accessibility events
- Send text to CryptoService via AIDL
- Inject processed text back

**CryptoService responsibilities:**
- Initialize libsodium (own JNI context per process)
- Evaluate security rules
- Encrypt/decrypt text
- Return `ProcessTextResult` via AIDL

## Paid Access Gating

All feature access control flows through `TierGate` in `:domain`:

```
TierGatedContent (Composable) -> TierGate.getTier() -> AccessTierRepository -> verified server-signed activation cache
```

No `if(isPro)` or `if(isElite)` anywhere in feature code.

The Android app has no wallet connector, IFR balance lookup or token-derived access tier.
Any IFR holder discount is handled in the browser before purchase and results only in the
same server-signed activation credential used by ordinary checkout.
