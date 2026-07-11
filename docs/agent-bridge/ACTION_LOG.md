# Action Log

## 2026-07-11 — Codex Chameleon Fiat-Entitlement-Verifier

- Chameleon akzeptiert bezahlte PRO/ELITE-Tiers nur noch ueber ein Ed25519-signiertes, an Audience `chameleon`, lokale Client-ID, Produkt und Ablauf gebundenes Token.
- Produkt-Allowlist akzeptiert ausschliesslich `chameleon_*`; kopierte SecureChat-/Suite-/fremde Tokens und manipulierte/abgelaufene Tokens failen geschlossen.
- Public Key ist Build-Konfiguration, Private Key bleibt Runtime-only auf dem Signaling-Server. Ohne Public Key bleibt Fiat-Aktivierung geschlossen.
- Verifiziertes Ablaufdatum wird im bestehenden HMAC-Repository/TierGate gespeichert; kein paralleler Tier-Cache.
- Serververtrag: StealthX Branch `codex-payment-hardening-20260711`, Commit `0b4fa1b`.
- Verifikation: Crypto- und Data-Tests PASS; Presentation Compile PASS; Gradle BUILD SUCCESSFUL. Keine Keys, Zahlung, externe Anfrage oder Deployment.

## 2026-07-11 — Codex Payment-Ownership dokumentiert

- Rollen, Status und offene Payment-/Etimologio-Gates fuer Chameleon eingetragen.
- Keine Secrets oder Steuerdaten in die Public Bridge geschrieben.
- Keine Zahlung, Rechnung, Provider-/AADE-Anfrage oder Deployment ausgefuehrt.
- Aufgabenmatrix ergaenzt: Codex implementiert Payment/Entitlement/Etimologio; Core-Dev bleibt bei Produkt/Krypto und reviewt die Integrationsgrenze.
- Gio-Folgeentscheidung eingetragen: Codex uebernimmt das gesamte Public Repo; andere Devs nur nach Handover/als Reviewer.

## 2026-05-08
