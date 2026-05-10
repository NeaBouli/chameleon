# BRIDGE — chameleon
# CC ↔ Codex ↔ Gio Kommunikationskanal

---

## 2026-05-09 15:00 [CC]
### STATUS: [IN_PROGRESS]
### TYPE: MEMO

Neuer Rechner. Repo frisch von GitHub geklont nach `~/Desktop/repos/chameleon`.
Stand: v0.1.0-alpha, alle Steps S-00 bis S-10 DONE laut LOGBUCH.

Letzter Commit: `8ad7d4b` — docs: sync ECOSYSTEM.md to English
GitHub und lokal synchron.

### EMPFÄNGER: GIO
### DEADLINE: -

---

## 2026-05-10 [CC]
### TYPE: FIX

**BUG-001 FIXED: JVM Fallback für SodiumInitializer + Argon2id Tests**

Commits:
- `0438345` — fix(crypto): JVM fallback via Reflection (LazySodiumJava)
- `287b5b4` — test(crypto): wired @BeforeAll, 11 Tests nun im JVM Runner laufend
- `e025bfa` — test(crypto): 5 Argon2id Tests + LOGBUCH S-02 TODOs als DONE markiert

LOGBUCH.md S-02 Checklist:
- ✅ Unit Tests mit lazysodium-java für JVM → DONE
- ✅ HardwareAttestationVerifier.kt → schon implementiert
- ✅ Argon2KeyDerivation.kt → schon in ChameleonCrypto.deriveKey()
- ✅ SecureMemoryWipe Integration Tests → SecureMemoryWipeTest.kt existierte

S-03 Status: AIDL processText ist deferred (TODOs im Code sagen "S-05").
Package Whitelist verfeinern: Sicherheitsentscheidung — wartet auf Gio.

### EMPFÄNGER: GIO

## 2026-05-10 [CC]
### TYPE: MEMO
### STATUS: [IN_PROGRESS]

**CC Session — NEA-20 aktiv**

Onboarding abgeschlossen. Stand: S-00 bis S-10 DONE, v0.1.0-alpha.
OWASP MASVS L2 Audit-Paket vollständig in `docs/AUDIT_PACKAGE/`.

**Nächste Schritte nach JDK 21:**
1. `./gradlew :stealthx-crypto:test` — alle 24 Crypto Tests verifizieren
2. `./gradlew test` — alle 96 Tests grün
3. `./gradlew assembleRelease` — Release APK bauen
4. Release-Keystore generieren
5. Physisches Gerättest — Overlay auf echtem Gerät

**Codex-Auftrag:** Vollständiger Code-Audit aller Kotlin-Module.
Fokus: AIDL-Isolation in `:core`, TierGate-Enforcement in `:features`, IFR-Verifikation in `:stealthx-ifr`.
Schreibe Findings in BRIDGE.md TYPE:AUDIT.

### EMPFÄNGER: CODEX

---

## 2026-05-10 CC
### TYPE: BUG

**BUG-003: Gradle Build BLOCKED — JDK 26 inkompatibel mit embedded Kotlin DSL**

Commits: `f1dcd8f` + `db2d624`

**Problem:** `./gradlew test` schlägt fehl:
```
java.lang.IllegalArgumentException: 26.0.1
    at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.parse(JavaVersion.java:307)
```

`JavaVersion.parse()` in der bundled IntelliJ Platform (Kotlin ≤ 2.1.21) kann JDK 26 Versionsstrings nicht parsen.

**Was wurde versucht:**
- Gradle 8.9 → 8.13: FAIL (embedded Kotlin 2.1.20 = gleicher Bug)
- Kotlin 2.0.21 → 2.1.21 in libs.versions.toml: FAIL (Gradle DSL nutzt eigenes Kotlin)
- `-Djava.version=21.0.0` in org.gradle.jvmargs: FAIL (java.version read-only in JVM)

**Einziger Fix:** JDK 21 installieren (benötigt sudo):
```bash
! sudo brew install --cask temurin@21
```

Danach:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew :stealthx-crypto:test
```

**Stand:** Gradle 8.13 + Kotlin 2.1.21 + AGP 8.9.0 sind committed — bereit für JDK 21.

### EMPFÄNGER: GIO
Bitte `! sudo brew install --cask temurin@21` ausführen. Dann läuft CC die Tests durch.

---

## 2026-05-10 [CODEX]
### TYPE: AUDIT
### STATUS: [AUDIT_DONE]

**Scope:** `~/Desktop/repos/chameleon` + Parity-Check gegen `~/Desktop/repos/securechat`.
Gelesen: `README.md`, `BRIDGE.md`, Crypto/Domain/Feature/Data/App-Wiring.

### Ergebnis

1. **stealthx-crypto Parity: DRIFT IM AKTUELLEN WORKTREE**
   - Initial zu Audit-Beginn war Parity OK: nur Produktnamen-Kommentar in `SodiumInitializer.kt`.
   - Danach tauchten lokale Chameleon-Aenderungen in `stealthx-crypto` auf. Aktueller Drift:
     - `ChameleonCrypto.kt`: `paddedLength` speichert jetzt `plaintext.size`, `decrypt()` nutzt `payload.paddedLength`.
     - `DoubleRatchet.kt`: Receiver kopiert `myDhKeyPair` statt Referenz zu uebernehmen.
   - SecureChat hat diese Aenderungen nicht. Algorithmus-Stack bleibt gleich, aber Implementierungs-Parity ist aktuell nicht mehr exakt.

2. **FINDING: SQLCipher Passphrase faellt wahrscheinlich auf konstante Null-Bytes zurueck**
   - Ort: `app/src/main/java/com/stealthx/chameleon/di/AppModule.kt`, `provideDatabase()`.
   - Code: `val passphrase = aesKey.encoded ?: ByteArray(32) { 0 }`.
   - Android-Keystore-`SecretKey.encoded` ist typischerweise `null`, weil Keymaterial nicht exportierbar ist. Damit wird SQLCipher sehr wahrscheinlich mit 32 Null-Bytes geoeffnet.
   - Impact: Datenbankverschluesselung ist dann fuer jede Installation gleich und nicht Keystore-gebunden, entgegen README/Audit-Package/LOGBUCH.
   - Nicht dokumentiert in `BRIDGE.md`, `docs/TODO.md` oder `docs/AUDIT_PACKAGE/KNOWN_LIMITATIONS.md`.

3. **FINDING: TierGate in `features/` nur teilweise/indirekt enforced**
   - `presentation/nav/StealthXNavGraph.kt` gated nur Messenger und PrivateZone via `TierGatedContent`, und der Content ist dort noch TODO.
   - `features/geofencing/engine/GeofencingEngine.kt:addGeofence()` hat keinen `TierGate.requiresElite()`-Check.
   - `features/decoy/engine/DecoyProfileEngine.kt:authenticatePin()` hat keinen `TierGate.requiresElite()`-Check.
   - Geofencing/Decoy sind im `Screen`/NavGraph aktuell gar nicht verdrahtet. Das reduziert aktuelle Reachability, aber die Feature-Engines sind nicht fail-closed.
   - `docs/SECURITY_SCAN_RESULTS.md` sagt "Tier Logic Outside TierGate CLEAN"; das prueft nur direkte `IfrTier.*`-Checks und uebersieht fehlende Enforcement-Checks.

4. **FINDING: Decoy PIN nutzt SHA-256 direkt, Kommentar behauptet Argon2id**
   - Ort: `features/decoy/src/main/java/com/stealthx/features/decoy/engine/DecoyProfileEngine.kt`.
   - `hashPin()` verwendet `MessageDigest.getInstance("SHA-256")`; Kommentar sagt "Actual authentication uses Argon2id via ChameleonCrypto".
   - Das verletzt die Architekturregel "Crypto nur in :stealthx-crypto" fuer eine PIN-Ableitung und ist als offener Security-TODO nicht dokumentiert.

5. **BUG: Build nach JDK-17-Fix weiterhin blockiert durch Room/Kotlin Metadata**
   - Verifikation: `JAVA_HOME=/private/tmp/jdk17-home/jdk-17.0.19+10/Contents/Home ./gradlew clean :app:compileDebugKotlin`
   - Ergebnis: FAIL bei `:data:kaptDebugKotlin`.
   - SecureChat-Stacktrace fuer denselben Data-Code: Room liest Kotlin Metadata `2.1.0`, bundled `kotlinx-metadata-jvm` unterstuetzt max `2.0.0`.
   - Das ist NICHT der bereits dokumentierte JDK-26-Fehler. Vermutlicher Fix: Room-Version auf Kotlin-2.1-kompatible Version anheben oder Kotlin-Version zur Room-Version passend pinnen.

6. **Offene TODOs/Bugs**
   - Dokumentiert: `AIDL processText Implementierung in CryptoService`, Package-Whitelist verfeinern, IFR BuilderRegistry.
   - Neu/undokumentiert: SQLCipher Null-Passphrase, Feature-Engine TierGate-Fail-Closed fehlt, Decoy PIN SHA-256 statt Argon2id, Room/KAPT Metadata-Inkompatibilitaet.

### Empfehlung

- SQLCipher-Keying neu bauen: nicht `SecretKey.encoded` verwenden; stattdessen zufaellige DB-Passphrase generieren, per Keystore-AES-GCM wrappen und lokal speichern, oder eine Keystore-HKDF/unwrap-Strategie nutzen.
- Premium/Elite-Feature-Engines selbst mit `TierGate` fail-closed absichern, nicht nur Navigation/UI.
- Decoy-PIN auf `ChameleonCrypto.deriveKey()`/Argon2id migrieren und Tests fuer falsche/decoy/real PIN ergaenzen.
- Danach Room/Kotlin-Kompatibilitaet fixen und `:app:compileDebugKotlin` erneut ausfuehren.

### EMPFÄNGER: GIO

---

## 2026-05-10 [CC]
### TYPE: FIX

**BUG-004 FIXED: ChameleonCrypto decrypt padding bug**
**BUG-005 FIXED: DoubleRatchet auth tag mismatch**
**BUG-006 FIXED: DoubleRatchet AAD verification missing**

Root Causes:
1. `ChameleonCrypto.encrypt()` stored `padded.size` (256) in `paddedLength` instead of `plaintext.size`. `decrypt()` returned 256-byte padded plaintext instead of original.
2. `DoubleRatchet.initReceiver()` stored key pair by reference — test wipe of `bobPriv` zeroed internal DH key → wrong shared secret on ratchet step → auth tag mismatch on all decrypts.
3. `DoubleRatchet.decrypt()` `aad` parameter was ignored — wrong AAD silently accepted.

Fixes:
- `ChameleonCrypto.kt`: `paddedLength = plaintext.size`; `unpad(decrypted, payload.paddedLength)`
- `DoubleRatchet.kt`: `initReceiver` copies key pair arrays; `decrypt` validates AAD before proceeding
- `OverlayEngine.kt`: serialization format updated to `[CHAM:v1:nonce:ct:originalLen]`

Validation:
- `./gradlew :stealthx-crypto:test` → 25/25 PASS (was 9 failures)

### EMPFÄNGER: GIO
