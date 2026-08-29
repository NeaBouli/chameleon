# Contributing to Chameleon

Thank you for your interest in Chameleon. We value transparency, security review, and responsible vulnerability reports.

## What We Accept

- Bug reports via GitHub Issues
- Feature requests via GitHub Issues
- Security vulnerability reports via the process in [SECURITY.md](SECURITY.md)

## What We Do Not Accept

We do not accept code contributions or pull requests.

This repository is source-available for transparency and independent security auditing only. Forks, builds, derivative works, redistribution, rebranding, hosting, and any use of Chameleon or official StealthX services require prior written permission from Vendetta Labs.

- Pull requests will be closed without review.
- Patches, code suggestions, or implementation changes submitted via issues or other channels will not be incorporated.

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
| `:stealthx-access` and activation verification | Project maintainer |

### What Triggers Security Review

- Any change to encryption/decryption logic
- Any change to key management or storage
- Any change to AIDL interface definitions
- Any change to activation signatures, entitlement verification or access-tier persistence
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

Chameleon is licensed under the StealthX Source-Available License. You may read and inspect the source code for transparency and security review, but you may not copy, modify, build, run, distribute, rebrand, host, or use Chameleon without prior written permission from Vendetta Labs.
## Gradle dependency verification

Dependencies are checksum-locked in `gradle/verification-metadata.xml`. When a reviewed
dependency update changes the graph, rerun the same affected Gradle CI tasks with
`--write-verification-metadata sha256`, inspect the metadata diff for only the expected
component/version changes, and then rerun the tasks without the write flag. Do not accept
unrelated checksum churn.
