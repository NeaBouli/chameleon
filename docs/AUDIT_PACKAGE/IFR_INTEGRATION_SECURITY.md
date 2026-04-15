# Chameleon — IFR Token Integration Security

## Contract Addresses (Ethereum Mainnet)

| Contract | Address | Purpose |
|----------|---------|---------|
| IFRLock | `0x769928aBDfc949D0718d8766a1C2d7dBb63954Eb` | Lock verification |
| BuilderRegistry | `0xdfe6636DA47F8949330697e1dC5391267CEf0EE3` | App registration |
| IFR Token | `0x77e99917Eca8539c62F509ED1193ac36580A6e7B` | ERC-20 token |

## Tier Thresholds

| Tier | Minimum Lock | Raw Value (9 decimals) |
|------|-------------|----------------------|
| FREE | 0 IFR | 0 |
| PRO | 2,000 IFR | 2,000,000,000,000 |
| ELITE | 6,000 IFR | 6,000,000,000,000 |

## Verification Flow

1. User enters wallet address (manual) or connects via WalletConnect (deep link)
2. `IFRLockVerifier` calls `lockedAmount(wallet)` via `eth_call` (read-only)
3. `IFRConstants.tierFromAmount()` determines tier
4. `IfrTierRepositoryImpl.saveTierResult()` stores result with HMAC
5. `TierGate` reads cached tier on subsequent access

## HMAC Tamper Protection

- **Algorithm:** HMAC-SHA256
- **Key:** Android Keystore hardware-backed key (`KeystoreManager.getOrCreateHmacKey()`)
- **Input:** `walletAddress + lockedAmount + tier + verifiedAt + expiresAt` (deterministic byte order)
- **On mismatch:** Always return `IfrTier.FREE` — never a higher tier
- **Expiry:** `verifiedAt + 30 days` — triggers re-verification

## Network Security

- No INTERNET permission in `:stealthx-ifr` manifest
- Internet permission in `:app` manifest (required for web3j RPC)
- Only `eth_call` — no `eth_sendTransaction`
- Fallback RPC endpoints: llamarpc, ankr, cloudflare-eth
- 10-second timeout per RPC call
- WalletConnect: Intent-based deep links only, no direct HTTP from Chameleon
