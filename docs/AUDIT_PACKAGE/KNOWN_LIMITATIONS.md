# Chameleon — Known Limitations

## Not Protected

| Limitation | Reason | Mitigation |
|-----------|--------|------------|
| Root detection | Rooted devices can read process memory | Warning dialog, not enforcement |
| Metadata protection | Chameleon encrypts content, not who/when/which-app | Out of scope — use Tor for metadata privacy |
| Anonymity network | No onion routing or mix networks | Not a goal — Chameleon is privacy, not anonymity |
| Key extraction during use | Keys exist in memory during crypto operations | Minimal key lifetime, immediate wipe after use |
| Side-channel attacks | Timing/power analysis on mobile hardware | Lazysodium uses constant-time libsodium internally |
| Hardware implants | Modified firmware, keyloggers | Cannot be mitigated in software |
| Social engineering | Users sharing PINs or keys | Education, not technology |
| JVM memory guarantees | `Arrays.fill()` may be optimized away by JIT | libsodium `sodium_memzero()` used where possible |

## Architectural Constraints

- **No external audit completed yet** — code has not been reviewed by a third-party security firm
- **AccessibilityService trust** — the service has broad text access; the package whitelist is the only control
- **Single-device model** — keys do not sync across devices; loss of device = loss of keys
- **IFR cache offline** — tier is cached locally; if cache is tampered AND HMAC key is compromised, tier could be spoofed (requires hardware Keystore breach)

## Recommended Auditor

[Trail of Bits](https://www.trailofbits.com/) — experienced with XChaCha20 + Double Ratchet stacks (audited SimpleX Chat, similar architecture).
