# Contributing to Chameleon

Thank you for your interest in contributing to Chameleon.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/chameleon.git`
3. Create a branch: `git checkout -b feature/your-feature`
4. Make your changes
5. Run tests: `./gradlew testAll`
6. Submit a Pull Request

## Code Review Process

All contributions require code review before merging.

### Standard Changes
- One approving review required
- CI must pass (build + tests + security scan)

### Security-Critical Changes

Changes to these modules require **additional security review**:

| Module | Reviewer |
|--------|----------|
| `:stealthx-crypto` | Project maintainer + crypto review |
| `:security` | Project maintainer + crypto review |
| `:core` (AIDL/IPC) | Project maintainer |
| `:stealthx-ifr` (IFR/HMAC) | Project maintainer |

### What Triggers Security Review

- Any change to encryption/decryption logic
- Any change to key management or storage
- Any change to AIDL interface definitions
- Any change to IFR tier verification or HMAC
- Any change to ProGuard rules
- Any new dependency addition

## Architecture Rules

These rules are enforced and must never be violated:

1. **Crypto only in `:stealthx-crypto`** — no lazysodium imports elsewhere
2. **Tier checks only via `TierGatedContent`** — no `if(isPro)` in features
3. **`:domain` never imports `:data`** — only repository interfaces
4. **`:security` imports only `:shared`** — no other module dependencies
5. **No debug logs in crypto/security** — zero `Log.d`/`println`
6. **No `java.util.Random()`** — only `SecureRandom` or libsodium random

## Commit Convention

```
feat(module): short description
fix(module): short description
test(module): short description
security(module): short description
docs: short description
chore: short description
```

## License

By contributing, you agree that your contributions will be licensed under GPL-3.0-or-later.
