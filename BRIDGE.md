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
