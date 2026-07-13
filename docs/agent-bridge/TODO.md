# TODO

## Payment / Etimologio — Owner Codex

**Zuweisung:** Alle Payment- und Nicht-Payment-Produktaufgaben dieses Public Repos sind Codex-Aufgaben. Andere Devs arbeiten nur nach Bridge-Handover oder als Reviewer.

- [x] Chameleon Pro/Elite als VLABS-Shopwaren mit Produktseiten und Rechtscopy vorbereiten.
- [x] Privat-/Firma-/AFM-Datenerfassung und internen Invoice-/Etimologio-Draft-Vertrag auf VLABS-Seite vorbereiten.
- [x] Signierten, idempotenten Chameleon-Entitlement-Consumer implementieren und testen.
- [x] Vollrefund-/Dispute-Revoke serverseitig und signierte Lease-Ablaufgrenze clientseitig implementieren; Partial Refund bleibt Operator-Review.
- [x] Automatische Lease-Erneuerung bei App-Start und danach alle sieben Tage implementieren.
- [x] Unsicheren clientseitigen Google-Play-Unlock entfernen und Regression-Guard ergaenzen.
- [x] Nicht interoperable Overlay-/Messenger-Pfade bis zur Verifikation fail-closed schalten.
- [ ] Authentifiziertes Overlay-Pairing mit gemeinsamem Peer-/Recipient-Key-Protokoll entwerfen, implementieren und kryptografisch reviewen.
- [ ] Accessibility-Overlay auf zwei physischen Geraeten und allen beworbenen Apps/Android-Versionen loop-frei testen.
- [ ] Messenger-Sessionaufbau fuer Initiator/Empfaenger angleichen; Transport-Start, Peer-Identitaet, Discovery und Relay-TLS absichern.
- [ ] Zwei-Geraete-Integrationstests fuer Messenger Repository plus Bluetooth/WiFi Direct/Relay bestehen.
- [ ] Source-Available/GPL-Dateikopf-Konflikt rechtlich entscheiden; bis dahin keine F-Droid-Auslieferung.
- [ ] Private Runtime-Secrets setzen und Stripe-Testmode E2E durchfuehren.
- [ ] Runtime-Ed25519-Public-Key in Release-Build setzen; Private Key bleibt ausschliesslich auf dem Signaling-Server.
- [x] Kanonischen Node-Signer-Token als echte Cross-Repo-Kompatibilitaetsregression im Kotlin-Verifier pruefen.
- [ ] Accountant Mapping sowie Gio Launch-/Deployment-Freigabe; erst danach `Coming Soon` entfernen.
- [ ] Reviewer nach Handover: Security-/Regression-Review des Entitlement-Verifiers und der `TierGate`-Integration.
