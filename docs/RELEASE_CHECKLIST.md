# Chameleon — Release Checklist

## Pre-Release (v1.0.0)

### Security
- [ ] External security audit completed (Trail of Bits)
- [ ] All MASVS PARTIAL findings documented and mitigated
- [ ] Penetration test on release APK
- [ ] Signing key generated and securely stored (offline backup)

### Build
- [ ] `./gradlew testAll` — all tests pass
- [ ] `./gradlew assembleRelease` — release APK builds
- [ ] `./gradlew detekt` — no errors
- [ ] ProGuard/R8 verification on release APK
- [ ] APK size within acceptable range (<25 MB)

### Distribution
- [ ] License reconciled before considering F-Droid distribution
- [ ] Play Store listing prepared (screenshots, description)
- [ ] Play Store Data Safety Form completed
- [ ] Privacy policy published at stealthx.tech/chameleon/privacy

### Documentation
- [ ] README.md complete and current
- [ ] LOGBUCH.md all steps DONE
- [ ] SECURITY.md responsible disclosure policy published
- [ ] CONTRIBUTING.md code review process documented
- [ ] Audit Package complete (docs/AUDIT_PACKAGE/)

### Infrastructure
- [ ] stealthx.tech Chameleon page live
- [ ] IFR BuilderRegistry registration on-chain confirmed
- [ ] GitHub Discussions enabled
- [ ] Issue templates configured

### Post-Release
- [ ] Monitor Play Store review
- [ ] Announce on StealthX channels
- [ ] Update IFR ecosystem documentation
