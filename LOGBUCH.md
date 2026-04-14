# 🦎 CHAMELEON — LOGBUCH
_Zuletzt aktualisiert: 2026-04-15 02:15 UTC_
_Aktiver Step: S-00 (Abschluss)_
_Build Status: 🟡 INITIALISIERUNG_

---

## 📊 STATUS ÜBERSICHT

| Modul               | Status       | Tests  | Letzte Änderung |
|---------------------|--------------|--------|-----------------|
| :stealthx-crypto    | 🔨 Scaffolded | 0/9   | Init            |
| :stealthx-ifr       | ⏳ Pending    | —      | —               |
| :security           | 🔨 Scaffolded | —      | Init            |
| :core               | ⏳ Pending    | —      | —               |
| :data               | ⏳ Pending    | —      | —               |
| :domain             | 🔨 Scaffolded | —      | Init            |
| :features:overlay   | ⏳ Pending    | —      | —               |
| :features:messenger | ⏳ Pending    | —      | —               |
| :features:privatezone | ⏳ Pending  | —      | —               |
| :features:geofencing | ⏳ Pending   | —      | —               |
| :features:decoy     | ⏳ Pending    | —      | —               |
| :presentation       | ⏳ Pending    | —      | —               |
| :shared             | 🔨 Scaffolded | —      | Init            |
| :app                | 🔨 Scaffolded | —      | Init            |

---

## ✅ ERLEDIGT

### [INIT] Projekt-Scaffolding
- [x] Vollständige Gradle Multi-Module Struktur (13 Module)
- [x] libs.versions.toml mit allen Versionen
- [x] settings.gradle.kts + Root build.gradle.kts
- [x] app/build.gradle.kts + ProGuard Rules
- [x] ChameleonApplication.kt (SodiumInitializer.ensureInit() in onCreate)
- [x] AndroidManifest.xml (kein Internet by default)
- [x] SodiumInitializer.kt — libsodium Init Singleton
- [x] ChameleonCrypto.kt — XChaCha20-Poly1305, X25519, Ed25519, Argon2id
- [x] DoubleRatchet.kt — Double Ratchet Skeleton (HKDF TODO in S-05)
- [x] ChameleonCryptoTest.kt — 10 Unit Tests
- [x] Models.kt — EncryptedPayload, RatchetMessage, IfrTier, SecurityLevel etc.
- [x] TierGate.kt — Interface (Impl in S-06)
- [x] RuleEngine.kt — Conflict Resolution (Fail Secure)
- [x] LOGBUCH.md, CLAUDE_CODE_START.md, .gitignore, LICENSE, README.md

### [S-00] Repo-Init & Ökosystem-Analyse
- [x] 2026-04-15 02:00 — Git initialisiert, Remote gesetzt (github.com/NeaBouli/chameleon)
- [x] 2026-04-15 02:05 — Gradle Wrapper 8.9 generiert und verifiziert
- [x] 2026-04-15 02:15 — SecureCall Repo analysiert → docs/SECURECALL_ANALYSIS.md
- [ ] Erster Commit + Push: `chore: initial project scaffold`

---

## 🔨 IN ARBEIT

### [S-00] Abschluss
- [ ] `git add . && git commit && git push origin main`
- [ ] `./gradlew build` — Fehler dokumentieren
- [ ] Build-Fehler beheben

---

## ⏳ TODO — NÄCHSTE SCHRITTE

### S-01 — Gradle Build Fix + CI:
- [ ] Build-Fehler beheben (Compose, kapt, Dependencies)
- [ ] Alle 13 Module kompilierbar machen
- [ ] CI: lint + test + build-debug
- [ ] Detekt Konfiguration prüfen

### S-02 — Security Layer:
- [ ] KeystoreManager.kt (StrongBox + TEE Fallback)
- [ ] HardwareAttestationVerifier.kt
- [ ] Argon2KeyDerivation.kt (via lazysodium)
- [ ] SecureMemoryWipe.kt (DoD 3-Pass)
- [ ] Unit Tests

### S-03 bis S-10 — siehe CLAUDE_CODE_START.md

---

## 🔴 BUGS & KRITISCHE PROBLEME

_Keine bekannten Bugs — Projekt gerade initialisiert_

---

## ⚠️ WARNUNGEN & HINWEISE

- [HINWEIS-001] DoubleRatchet.kt enthält `// TODO: HKDF` Platzhalter → in S-05 auflösen
- [HINWEIS-002] SodiumInitializer muss AUCH im `:crypto` Prozess aufgerufen werden
- [HINWEIS-003] lazysodium Version 5.1.0 — NICHT 5.0.x (JNA Bug)
- [HINWEIS-004] StrongBox nur auf physischen Geräten mit Titan M
- [HINWEIS-005] IFR Cache HMAC: bei Mismatch immer FREE Tier — niemals höher
- [ERINNERUNG-001] android:allowBackup="false" — nie ändern
- [ERINNERUNG-002] Kein Crypto-Code außerhalb :stealthx-crypto
- [ERINNERUNG-003] Kein Tier-Check außerhalb TierGate.kt

---

## 📋 ARCHITEKTUR CONSTRAINTS (UNVERÄNDERLICH)

```
1.  Crypto:          XChaCha20-Poly1305 + X25519 + Double Ratchet + Argon2id
2.  Bibliothek:      lazysodium-android 5.1.0 ONLY
3.  :domain          → darf NIEMALS :data importieren
4.  :security        → darf NIEMALS andere eigene Module importieren
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
| :stealthx-crypto | 9 def. | ⏳ Init | —            |

---

## 📝 COMMIT LOG

| Datum | Hash | Message | Tests |
|-------|------|---------|-------|
| — | — | — | — |

---

## ❓ OFFENE FRAGEN

_Keine offenen Fragen_

---

## 🚫 BLOCKERS

_Keine Blocker_

---

_Ende des Logbuchs_
