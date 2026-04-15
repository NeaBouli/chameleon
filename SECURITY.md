# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 0.1.x   | Yes       |

## Reporting a Vulnerability

If you discover a security vulnerability in Chameleon, please report it responsibly.

### How to Report

1. **Email:** bergamolia@protonmail.com
2. **Subject:** `[SECURITY] Chameleon — Brief description`
3. **Include:**
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact assessment
   - Suggested fix (if available)

### Response Timeline

| Step | Timeframe |
|------|-----------|
| Acknowledgment | 48 hours |
| Initial assessment | 7 days |
| Fix development | 30 days |
| Public disclosure | 90 days after report |

### Embargo Policy

- **90-day embargo** from report to public disclosure
- If a fix is released before 90 days, disclosure may happen earlier with reporter's consent
- Critical vulnerabilities (remote code execution, key extraction) may have accelerated timelines

### Scope

In scope:
- Cryptographic implementation flaws
- Key material leakage
- AIDL/IPC bypass
- IFR tier spoofing
- AccessibilityService abuse beyond whitelist
- SQLCipher key extraction

Out of scope:
- Rooted/jailbroken device attacks
- Physical device access with unlocked bootloader
- Social engineering
- Denial of service without security impact

### Recognition

Security researchers who report valid vulnerabilities will be credited in the release notes (with consent).

### No Bounty Program

Chameleon does not currently offer a bug bounty program. We are an open-source project and appreciate responsible disclosure.
