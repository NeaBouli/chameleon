# 🦎 CHAMELEON — LOGBUCH
_Zuletzt aktualisiert: 2026-04-15 03:05 UTC_
_Aktiver Step: S-01 → S-02_
_Build Status: 🟢 GREEN (assembleDebug + assembleRelease)_

---

## 📊 STATUS ÜBERSICHT

| Modul               | Status       | Tests  | Letzte Änderung |
|---------------------|--------------|--------|-----------------|
| :stealthx-crypto    | ✅ Compiles   | 3/11 JVM* | 2026-04-15 |
| :stealthx-ifr       | ✅ Compiles   | —      | 2026-04-15      |
| :security           | ✅ Compiles   | —      | 2026-04-15      |
| :core               | ✅ Compiles   | —      | 2026-04-15      |
| :data               | ✅ Compiles   | —      | 2026-04-15      |
| :domain             | ✅ Compiles   | —      | 2026-04-15      |
| :features:overlay   | ✅ Compiles   | —      | 2026-04-15      |
| :features:messenger | ✅ Compiles   | —      | 2026-04-15      |
| :features:privatezone | ✅ Compiles | —      | 2026-04-15      |
| :features:geofencing | ✅ Compiles  | —      | 2026-04-15      |
| :features:decoy     | ✅ Compiles   | —      | 2026-04-15      |
| :presentation       | ✅ Compiles   | —      | 2026-04-15      |
| :shared             | ✅ Compiles   | —      | 2026-04-15      |
| :app                | ✅ Compiles   | —      | 2026-04-15      |

*Tests: 8/11 JVM-Tests scheitern weil lazysodium-android keine native lib im JVM-Testrunner laden kann. Braucht `lazysodium-java` als testImpl oder Emulator-Tests.

---

## ✅ ERLEDIGT

### [INIT] Projekt-Scaffolding
- [x] Vollständige Gradle Multi-Module Struktur (13 Module)
- [x] libs.versions.toml mit allen Versionen
- [x] ChameleonCrypto.kt, DoubleRatchet.kt, SodiumInitializer.kt
- [x] KeystoreManager.kt, SecureMemoryWipe.kt
- [x] TierGate.kt, RuleEngine.kt, Models.kt
- [x] ICryptoBridge.aidl, ProcessTextResult.kt, CryptoService.kt
- [x] ChameleonAccessibilityService.kt, BootReceiver.kt
- [x] IFRConstants.kt
- [x] CI/CD, Detekt, ProGuard

### [S-00] Repo-Init & Ökosystem-Analyse
- [x] 2026-04-15 02:00 — Git init + remote (github.com/NeaBouli/chameleon)
- [x] 2026-04-15 02:05 — Gradle Wrapper 8.9
- [x] 2026-04-15 02:15 — SecureCall Analyse → docs/SECURECALL_ANALYSIS.md
- [x] 2026-04-15 02:20 — Erster Commit + Push: `chore: initial project scaffold`

### [S-01] Gradle Build Fix
- [x] 2026-04-15 02:25 — Compose Compiler Plugin (kotlin-compose) in allen UI-Modulen
- [x] 2026-04-15 02:30 — gradle.properties (android.useAndroidX=true)
- [x] 2026-04-15 02:35 — local.properties (Android SDK Pfad)
- [x] 2026-04-15 02:40 — :stealthx-crypto JVM→Android Library (lazysodium-android braucht Android)
- [x] 2026-04-15 02:40 — :domain JVM→Android Library (dep auf :stealthx-crypto)
- [x] 2026-04-15 02:42 — SodiumInitializer von :security nach :stealthx-crypto verschoben (Constraint fix)
- [x] 2026-04-15 02:45 — jvmTarget=17 in allen 13 Modulen
- [x] 2026-04-15 02:48 — WalletConnect deps vorübergehend deaktiviert (Maven Repo fehlt → S-06)
- [x] 2026-04-15 02:50 — KeystoreManager StrongBox via Reflection (API 28 vs minSdk 26)
- [x] 2026-04-15 02:52 — ProcessTextResult Parcelable + CryptoService + BootReceiver
- [x] 2026-04-15 02:55 — Launcher Icons (adaptive), Theme, strings.xml
- [x] 2026-04-15 02:57 — ProGuard: web3j transitive deps (tuweni, groovy) dontwarn
- [x] 2026-04-15 02:58 — META-INF packaging excludes
- [x] 2026-04-15 02:59 — Timber dependency hinzugefügt
- [x] 2026-04-15 03:01 — **BUILD SUCCESSFUL** (assembleDebug + assembleRelease)

---

## 🔨 IN ARBEIT

### [S-02] Security Layer
_Nächster Schritt_

---

## ⏳ TODO — NÄCHSTE SCHRITTE

### S-02 — Security Layer:
- [ ] Unit Tests mit lazysodium-java für JVM oder Robolectric
- [ ] HardwareAttestationVerifier.kt
- [ ] Argon2KeyDerivation.kt Wrapper
- [ ] SecureMemoryWipe Integration Tests

### S-03 — Core Layer:
- [ ] AIDL processText Implementierung in CryptoService
- [ ] accessibility_service_config.xml Package Whitelist verfeinern

### S-04 bis S-10 — siehe CLAUDE_CODE_START.md

---

## 🔴 BUGS & KRITISCHE PROBLEME

### [BUG-001] — OFFEN
**Datum:** 2026-04-15
**Modul:** :stealthx-crypto
**Beschreibung:** 8/11 Unit Tests scheitern im JVM-Testrunner
**Ursache:** lazysodium-android kann native lib nicht im JVM laden
**Fix:** lazysodium-java als testImplementation ODER instrumented Tests
**Status:** 🟡 LOW — Build ist grün, Tests brauchen Emulator/lazysodium-java

### [BUG-002] — DEFERRED (S-06)
**Datum:** 2026-04-15
**Modul:** :stealthx-ifr
**Beschreibung:** WalletConnect v2 SDK nicht auflösbar
**Ursache:** Reown Maven Repository nicht konfiguriert
**Fix:** Maven repo URL für WalletConnect SDK finden und in settings.gradle.kts hinzufügen
**Status:** 🟡 DEFERRED → S-06

---

## ⚠️ WARNUNGEN & HINWEISE

- [HINWEIS-001] DoubleRatchet.kt enthält `// TODO: HKDF` Platzhalter → in S-05 auflösen
- [HINWEIS-002] SodiumInitializer muss AUCH im `:crypto` Prozess aufgerufen werden ✅ (CryptoService.onCreate)
- [HINWEIS-003] lazysodium Version 5.1.0 — NICHT 5.0.x (JNA Bug)
- [HINWEIS-004] StrongBox nur auf physischen Geräten mit Titan M
- [HINWEIS-005] IFR Cache HMAC: bei Mismatch immer FREE Tier — niemals höher
- [HINWEIS-006] :stealthx-crypto und :domain sind jetzt Android Libraries (nicht JVM)
- [ERINNERUNG-001] android:allowBackup="false" — nie ändern
- [ERINNERUNG-002] Kein Crypto-Code außerhalb :stealthx-crypto
- [ERINNERUNG-003] Kein Tier-Check außerhalb TierGate.kt

---

## 📋 ARCHITEKTUR CONSTRAINTS (UNVERÄNDERLICH)

```
1.  Crypto:          XChaCha20-Poly1305 + X25519 + Double Ratchet + Argon2id
2.  Bibliothek:      lazysodium-android 5.1.0 ONLY
3.  :domain          → darf NIEMALS :data importieren
4.  :security        → darf NIEMALS andere eigene Module importieren (nur :shared)
5.  Krypto-Code      → NUR in :stealthx-crypto
6.  Tier-Logik       → NUR in TierGate.kt (:domain)
7.  Internet         → Nur für IFR-Verifikation (opt-in)
8.  Backup           → android:allowBackup="false"
9.  Screenshots      → FLAG_SECURE auf allen Activities
10. Lizenz           → GPL-3.0, Header in jeder Kotlin-Datei
```

---

## 🔑 IFR CONTRACT ADRESSEN (HARDCODIERT)

```
IFRLock:          0x769928aBDfc949D0718d8766a1C2d7dBb63954Eb
BuilderRegistry:  0xdfe6636DA47F8949330697e1dC5391267CEf0EE3
IFR Token:        0x77e99917Eca8539c62F509ED1193ac36580A6e7B
Chain ID:         1 (Ethereum Mainnet)
Decimals:         9
PRO Threshold:    2_000_000_000_000 (2.000 IFR)
ELITE Threshold:  6_000_000_000_000 (6.000 IFR)
```

---

## 🧪 TEST-PROTOKOLL

| Modul            | Tests  | Status | Letzter Lauf |
|------------------|--------|--------|--------------|
| :stealthx-crypto | 3/11   | 🟡 JVM | 2026-04-15   |

---

## 📝 COMMIT LOG

| Datum | Hash | Message | Tests |
|-------|------|---------|-------|
| 2026-04-15 | 979722f | chore: initial project scaffold | — |
| 2026-04-15 | TBD | fix: gradle build — all 13 modules compile | 3/11 |

---

## ❓ OFFENE FRAGEN

_Keine offenen Fragen_

---

## 🚫 BLOCKERS

_Keine Blocker_

---

_Ende des Logbuchs_
