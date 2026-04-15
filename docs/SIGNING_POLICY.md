# Chameleon — Signing Key Policy

## Keystore Location

The release signing keystore is stored **offline** — never in the repository.

| Item | Location |
|------|----------|
| Keystore file | Offline USB drive + encrypted cloud backup |
| Keystore password | Password manager (separate from backup) |
| Key alias | `chameleon` |
| Key algorithm | RSA 4096 |
| Validity | 25 years |

## Access Control

- Only the project maintainer has access to the signing key
- GitHub Actions uses encrypted secrets for CI/CD release builds
- Secrets: `KEYSTORE_FILE` (base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

## Recovery Process

1. Primary: offline USB drive
2. Secondary: encrypted cloud backup (AES-256, separate password)
3. If both lost: new signing key required (breaks update chain)

## Key Rotation

- No planned rotation (Android requires same key for updates)
- If key is compromised: publish new app with new package name, migrate users

## NEVER

- Never commit the keystore to git
- Never store the password in plaintext
- Never share the key via email or chat
- Never use the same key for other projects
