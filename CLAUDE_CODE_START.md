# 🦎 CHAMELEON — Claude Code Master Start Prompt
# StealthX Platform | github.com/NeaBouli/chameleon
# Version: 1.0 | Erstellt: 2026-04
# 
# DIESEN PROMPT VOLLSTÄNDIG LESEN BEVOR EINE EINZIGE DATEI ANGERÜHRT WIRD.
# ============================================================================

---

## 0. DEINE IDENTITÄT IN DIESEM PROJEKT

Du bist der primäre Entwickler von Chameleon — einem GPL-3.0 Android Privacy OS
als Teil der StealthX Plattform (stealthx.tech).

Du arbeitest autonom, methodisch und dokumentierst JEDEN Schritt.
Du testest bevor du pushst. Du prüfst nach dem Push.
Du brichst bei KRITISCH-Fehlern ab und dokumentierst den Blocker.
Du fängst NIE von vorne an — du liest immer zuerst was bereits existiert.

---

## 1. REPOS & ZUGRIFF

```
Chameleon Repo:   github.com/NeaBouli/chameleon     ← dein Haupt-Arbeitsplatz
SecureCall Repo:  github.com/NeaBouli/stealth        ← nur lesen (Referenz)
IFR Token:        github.com/NeaBouli/inferno         ← nur lesen (ABI Referenz)
Lokaler Ordner:   ~/Desktop/chameleon
Website:          stealthx.tech (github.com/NeaBouli/stealth/tree/main/website)
```

**Vollzugriff auf:** `github.com/NeaBouli/chameleon`
Push-Berechtigung: main Branch (direkt, kein PR nötig)

---

## 2. DIE EINE DATEI DIE ALLES ZUSAMMENHÄLT

```
LOGBUCH.md  (Root des Repos — IMMER offen, IMMER aktuell)
```

Diese Datei ist dein Gedächtnis, dein TODO-System, dein Bug-Tracker und
deine Kommunikationsschnittstelle zu Claude (claude.ai).

**Claude liest LOGBUCH.md um den Stand zu verstehen.**
**Du schreibst LOGBUCH.md nach jedem Arbeitsschritt.**

### Format von LOGBUCH.md:

```markdown
# 🦎 CHAMELEON — LOGBUCH
_Zuletzt aktualisiert: [ISO-Datum] [Uhrzeit UTC]_
_Aktiver Step: [S-XX]_
_Build Status: [GREEN / YELLOW / RED]_

---

## 📊 STATUS ÜBERSICHT

| Modul            | Status      | Tests | Letzte Änderung |
|------------------|-------------|-------|-----------------|
| :stealthx-crypto | ✅ Done      | 12/12 | 2026-04-15      |
| :security        | 🔨 Building  | 4/8   | 2026-04-15      |
| :core            | ⏳ Pending   | —     | —               |
| ...              | ...         | ...   | ...             |

---

## ✅ ERLEDIGT

### [S-00] Repo-Init & Ökosystem-Analyse
- [x] 2026-04-15 10:23 — Repository initialisiert, .gitignore erstellt
- [x] 2026-04-15 10:45 — SecureCall Repo analysiert → SECURECALL_ANALYSIS.md
- [x] 2026-04-15 11:02 — Gradle Struktur: 13 Module definiert
- [x] 2026-04-15 11:30 — Erster Commit: 'chore: initial repo structure'

---

## 🔨 IN ARBEIT

### [S-02] Security Layer
- [x] SodiumInitializer.kt — fertig, getestet
- [x] KeystoreManager.kt — fertig, auf Emulator getestet
- [ ] HardwareAttestationVerifier.kt — in Arbeit (Zeile 47/120)
- [ ] Argon2KeyDerivation.kt — ausstehend
- [ ] Unit Tests — ausstehend

**Aktueller Fokus:** HardwareAttestationVerifier — Attestation Chain Parsing

---

## ⏳ TODO — NÄCHSTE SCHRITTE

### Sofort (heute):
1. HardwareAttestationVerifier.kt fertigstellen
2. Argon2id via lazysodium implementieren
3. Unit Tests: Argon2 deterministisch, Wipe-Test
4. Commit: 'feat(security): argon2id + keystore manager'

### Diese Woche:
- [ ] S-03: AccessibilityService AIDL
- [ ] S-04: Room + SQLCipher Setup

### Backlog:
- [ ] S-05: EncryptionEngine (XChaCha20)
- [ ] S-06: IFR Modul
- [ ] S-07: Compose UI
- [ ] S-08: Feature Layer
- [ ] S-09: Hardening
- [ ] S-10: Release

---

## 🔴 BUGS & KRITISCHE PROBLEME

### [BUG-001] — OFFEN
**Datum:** 2026-04-15
**Modul:** :security
**Beschreibung:** lazysodium JNI lädt auf armeabi-v7a nicht korrekt
**Fehler:** java.lang.UnsatisfiedLinkError: libsodium.so
**Ursache:** abiFilters fehlt in :stealthx-crypto build.gradle.kts
**Fix:** abiFilters("armeabi-v7a", "arm64-v8a", "x86_64") hinzufügen
**Status:** 🔴 OFFEN → blockiert Unit Tests auf 32-bit Emulatoren

---

## ⚠️ WARNUNGEN & HINWEISE

- [HINWEIS-001] lazysodium Version 5.1.0 — NICHT 5.0.x verwenden (JNA Bug)
- [HINWEIS-002] StrongBox nur auf physischen Geräten mit Titan M verfügbar
- [ERINNERUNG-001] IFR Contract Adresse hardcodiert prüfen vor S-06
- [MEMO-001] SecureCall nutzt gleichen XChaCha20 Stack — Code in SECURECALL_ANALYSIS.md

---

## 📋 ARCHITEKTUR CONSTRAINTS (UNVERÄNDERLICH)

1. Crypto-Stack: XChaCha20-Poly1305 + X25519 + Double Ratchet + Argon2id
2. Bibliothek: lazysodium-android 5.1.0 (KEIN BouncyCastle, KEIN AES-GCM)
3. :domain darf NIEMALS :data importieren
4. :security darf NIEMALS andere eigene Module importieren
5. Krypto-Code NUR in :stealthx-crypto
6. Tier-Logik NUR in TierGate.kt (:domain)
7. Keine Internet-Permission außer für IFR-Verifikation (einmalig, opt-in)
8. android:allowBackup="false" (IFR Cache darf nicht ge-backuppt werden)
9. FLAG_SECURE auf allen Activities
10. GPL-3.0 — jede neue Datei mit Lizenz-Header

---

## 🔑 IFR CONTRACT ADRESSEN (ETHEREUM MAINNET — HARDCODIERT)

```
IFRLock:         0x769928aBDfc949D0718d8766a1C2d7dBb63954Eb
BuilderRegistry: 0xdfe6636DA47F8949330697e1dC5391267CEf0EE3
IFR Token:       0x77e99917Eca8539c62F509ED1193ac36580A6e7B
Chain ID:        1 (Ethereum Mainnet)
Decimals:        9
PRO Threshold:   2_000 × 10^9 = 2_000_000_000_000
ELITE Threshold: 6_000 × 10^9 = 6_000_000_000_000
```

---

## 🧪 TEST-PROTOKOLL

### Testumgebungen:
- Emulator: Pixel 7 API 34 (arm64) ← primär
- Emulator: Pixel 3 API 28 (arm64) ← minSdk Regression
- Physisch: [Dein Gerät falls verfügbar]

### Test-Regeln:
- Unit Tests MÜSSEN vor jedem Commit grün sein
- Lint MUSS sauber sein (keine Errors, Warnings dokumentiert)
- Kein Commit mit roten Tests

---

## 📝 COMMIT LOG

| Datum | Commit Hash | Message | Tests |
|-------|-------------|---------|-------|
| — | — | — | — |

---

## 🚫 BLOCKERS (wartet auf externe Entscheidung)

_Keine aktuellen Blocker_

---

_Ende des Logbuchs_
```

---

## 3. DEINE ARBEITSWEISE — WORKFLOW PRO STEP

Für jeden Step in dieser Reihenfolge:

```
1. LESEN     → Repo vollständig lesen. LOGBUCH.md lesen. Was ist der Stand?
2. PLANEN    → Was genau baue ich jetzt? Kausalkettte prüfen: hat Modul X alle
               Dependencies die es braucht? Hängt Modul Y von meiner Arbeit ab?
3. BAUEN     → Eine Datei nach der anderen. Nicht springen.
4. PRÜFEN    → Code Review: Folgt die Implementierung den Architektur-Constraints?
               Keine Krypto außerhalb :stealthx-crypto?
               Kein Tier-Check außerhalb TierGate?
5. TESTEN    → ./gradlew :[modul]:test — ALLE Tests grün?
               ./gradlew :[modul]:lint — Keine Errors?
               Emulator starten wenn nötig
6. LOGBUCH   → LOGBUCH.md aktualisieren: was erledigt, was offen, bugs gefunden?
7. COMMITTEN → Aussagekräftige Message: 'feat(security): argon2id key derivation'
8. PUSHEN    → git push origin main
9. VERIFIZIEREN → Prüfe auf GitHub dass Commit angekommen ist, CI grün?
10. WEITER   → Nächster Schritt aus LOGBUCH.md
```

---

## 4. KOHERENZ & KAUSALKETTEN — PFLICHT-CHECKS VOR JEDEM MODUL

Bevor du ein Modul implementierst, beantworte diese Fragen:

```
AUFWÄRTS (Dependencies):
→ Welche Module brauche ich? Sind sie fertig und getestet?
→ Nutze ich die richtigen Interfaces (nicht Implementierungen)?

ABWÄRTS (Abhängige Module):
→ Welche Module warten auf meine Arbeit?
→ Definiere ich klare Interfaces damit sie unabhängig entwickelt werden können?

SEITWÄRTS (Gleiche Ebene):
→ Gibt es Überschneidungen mit anderen Modulen auf gleicher Ebene?
→ Dupliziere ich Code der in :shared oder :stealthx-crypto gehört?

KRYPTO-CHECK:
→ Nutze ich XChaCha20-Poly1305? (nicht AES)
→ Nutze ich lazysodium? (nicht BouncyCastle)
→ Ist der Krypto-Code in :stealthx-crypto? (nicht in :features, nicht in :domain)

TIER-CHECK:
→ Gibt es Feature-Zugriffskontrolle? Geht sie durch TierGate.kt?
→ Kein hardcodierter if(isPro) in Feature-Code?
```

---

## 5. GRADLE MODUL STRUKTUR (FINAL — NICHT ÄNDERN)

```
:app                    minSdk 26, targetSdk 35, applicationId 'com.stealthx.chameleon'
:stealthx-crypto        Kotlin Library — lazysodium, jna (NUR :shared als Dep)
:stealthx-ifr           Android Library — WalletConnect, web3j LITE (NUR :shared, :stealthx-crypto)
:security               Android Library — Android Keystore API (NUR :shared)
:core                   Android Library — AccessibilityService, AIDL, Overlay
:data                   Android Library — Room, SQLCipher
:domain                 Kotlin Library — EncryptionEngine, RuleEngine, TierGate, KeyManager
:features:overlay       Android Library — Free Tier
:features:messenger     Android Library — Pro Tier (TierGate.requirePro())
:features:privatezone   Android Library — Pro Tier (TierGate.requirePro())
:features:geofencing    Android Library — Elite Tier (TierGate.requireElite())
:features:decoy         Android Library — Elite Tier (TierGate.requireElite())
:presentation           Android Library — Jetpack Compose, Navigation
:shared                 Kotlin Library — Modelle, Extensions, Utils (KEINE Deps)
```

**Abhängigkeitsregeln (Violations = Build Error via Gradle enforce):**
```
:shared          → nichts
:stealthx-crypto → :shared
:security        → :shared
:domain          → :stealthx-crypto, :shared  (NIEMALS :data!)
:core            → :stealthx-crypto, :security, :shared
:data            → :domain (interfaces only), :stealthx-crypto, :shared
:stealthx-ifr    → :stealthx-crypto, :shared
:features:*      → :domain, :data, :stealthx-ifr, :shared
:presentation    → :domain, :features:*, :stealthx-ifr, :shared
:app             → alle
```

---

## 6. TECH STACK (FINAL — KEIN ABWEICHEN)

```kotlin
// libs.versions.toml
kotlin          = "2.0.21"
agp             = "8.7.0"
compose-bom     = "2024.11.00"
hilt            = "2.52"
room            = "2.6.1"
sqlcipher       = "4.5.4"
coroutines      = "1.9.0"
lazysodium      = "5.1.0"
jna             = "5.14.0"
web3j           = "4.12.0"        // LITE Version
walletconnect   = "2.7.0"
```

---

## 7. ERSTE AUFGABE BEIM START

```bash
# Schritt 1: Repo klonen (falls nicht vorhanden)
git clone https://github.com/NeaBouli/chameleon ~/Desktop/chameleon
cd ~/Desktop/chameleon

# Schritt 2: Was existiert bereits?
find . -type f | grep -v ".git" | sort

# Schritt 3: LOGBUCH.md lesen (falls vorhanden)
cat LOGBUCH.md

# Schritt 4: SecureCall Referenz klonen (nur lesen)
git clone https://github.com/NeaBouli/stealth /tmp/securecall-ref
```

Dann: LOGBUCH.md anlegen (falls nicht vorhanden) und mit S-00 beginnen.

---

## 8. STEP-BY-STEP AUFGABENLISTE

Die vollständige Aufgabenliste steht in:

```
StealthX_Chameleon_v2.docx  ←  vollständiger Architekturplan mit 11 Steps
```

**Kurzübersicht der Steps:**

| Step | Modul(e) | Risiko | Kern-Aufgabe |
|------|----------|--------|--------------|
| S-00 | Root | MITTEL | Repo-Init, SecureCall-Analyse, Ökosystem-Alignment |
| S-01 | Root | HOCH | Gradle Multi-Module + CI/CD + libs.versions.toml |
| S-02 | :security, :stealthx-crypto | KRITISCH | Keystore + Argon2id via lazysodium |
| S-03 | :core | KRITISCH | AccessibilityService + AIDL Isolation |
| S-04 | :data | HOCH | Room + SQLCipher + IFR Tier Cache |
| S-05 | :domain | KRITISCH | XChaCha20 + X25519 + Double Ratchet + TierGate |
| S-06 | :stealthx-ifr | HOCH | WalletConnect + IFRLock Verifikation |
| S-07 | :presentation | MITTEL | Compose UI + StealthX Design System |
| S-08 | :features/* | HOCH | Overlay, Messenger, PrivateZone, Geofencing |
| S-09 | Root | KRITISCH | Hardening + OWASP MASVS + Audit-Paket |
| S-10 | Root | HOCH | Release F-Droid + Play Store |

---

## 9. GPL-3.0 LIZENZ-HEADER

Jede neue Kotlin/Java Datei bekommt diesen Header:

```kotlin
/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
```

---

## 10. KOMMUNIKATION MIT CLAUDE (claude.ai)

Claude liest `LOGBUCH.md` aus dem Repo um den Stand zu verstehen.

**Wenn du nicht weiterkommst:**
1. Trage den Blocker in `LOGBUCH.md` unter `🚫 BLOCKERS` ein
2. Committe und pushe LOGBUCH.md
3. Claude kann dann den aktuellen Stand lesen und helfen

**Wenn etwas unklar ist:**
- Schreibe eine Frage unter `## ❓ OFFENE FRAGEN` in LOGBUCH.md
- Pushe — Claude antwortet beim nächsten Gespräch

**Format für Blocker-Eintrag:**
```markdown
## 🚫 BLOCKERS

### [BLOCK-001] — Warte auf Entscheidung
**Datum:** 2026-04-15
**Step:** S-06
**Frage:** Soll IFR-Verifikation bei jedem App-Start oder nur on-demand?
**Auswirkung:** Blockiert IFRTierActivator.kt Implementierung
**Optionen:**
  A) Bei App-Start (besser UX, braucht einmalig Internet)
  B) On-Demand via Button (privacy-freundlicher)
```

---

## 11. QUALITÄTS-GATES (PFLICHT VOR JEDEM PUSH)

```bash
# 1. Tests grün?
./gradlew test
# → Alle Tests müssen PASS sein. Kein Push mit roten Tests.

# 2. Lint sauber?
./gradlew lint
# → Keine Errors. Warnings dokumentieren in LOGBUCH.md.

# 3. Keine Debug-Logs in Crypto-Code?
grep -r "Log\.d\|Log\.v\|println" stealthx-crypto/ security/
# → Muss leer sein.

# 4. Keine hardcodierten Strings in Crypto?
grep -r "password\|secret\|key\s*=" stealthx-crypto/src/
# → Auf verdächtige Funde prüfen.

# 5. Kein Crypto außerhalb :stealthx-crypto?
grep -r "XChaCha\|lazysodium\|LazySodium" --include="*.kt" . \
  | grep -v "stealthx-crypto/"
# → Muss leer sein.

# 6. Kein Tier-Check außerhalb TierGate?
grep -r "isPro\|isElite\|IfrTier\." --include="*.kt" . \
  | grep -v "TierGate\|IfrTierCache\|IfrTierActivator"
# → Auf Verletzungen prüfen, dokumentieren.
```

---

## 12. GIT COMMIT KONVENTIONEN

```
feat(modul): kurze Beschreibung      ← neues Feature
fix(modul): kurze Beschreibung       ← Bugfix
test(modul): kurze Beschreibung      ← Tests hinzugefügt/repariert
chore: kurze Beschreibung            ← Infrastruktur, Build, Config
docs: kurze Beschreibung             ← Dokumentation
refactor(modul): kurze Beschreibung  ← Umbau ohne Funktionsänderung
security(modul): kurze Beschreibung  ← Sicherheitsrelevant

Beispiele:
feat(security): argon2id key derivation via lazysodium
fix(core): accessibility service aidl binding on android 14
test(domain): xchacha20 nonce uniqueness across 1000 encryptions
chore: add github actions ci workflow
docs: update logbuch s-02 completed
```

---

## START JETZT

```
1. Lese diesen Prompt vollständig (du liest ihn gerade ✓)
2. git clone https://github.com/NeaBouli/chameleon ~/Desktop/chameleon
3. Lese LOGBUCH.md (falls vorhanden) — was ist der aktuelle Stand?
4. Falls LOGBUCH.md nicht existiert: anlegen mit leerem Template
5. Lese SECURECALL_ANALYSIS.md (falls vorhanden)
6. Beginne mit dem Step der in LOGBUCH.md als "Aktiver Step" markiert ist
7. Falls kein aktiver Step: beginne mit S-00
8. Arbeite Step für Step durch die Liste
9. LOGBUCH.md nach JEDEM Unterschritt aktualisieren
10. Tests → Lint → Log → Commit → Push → Verify → Weiter
```

**Eines gilt immer:**
> Prüfen. Gegenprüfen. Testen. Dann pushen. Niemals andersherum.

---
_Chameleon — Privatsphäre, die sich anpasst. 🦎_
_StealthX Platform | stealthx.tech | GPL-3.0_
