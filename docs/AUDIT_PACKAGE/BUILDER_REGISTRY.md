# Chameleon — IFR Builder Registry

## Registration Status

**Status:** Pending Registration

## Contract

- **BuilderRegistry:** `0xdfe6636DA47F8949330697e1dC5391267CEf0EE3`
- **Chain:** Ethereum Mainnet (Chain ID 1)
- **Builder ID:** TBD (assigned upon registration)

## Registration Process

1. Submit application to IFR governance with project details
2. Governance vote (IFR token holders)
3. On approval: `registerBuilder(appName, contractAddress, metadata)` called by governance
4. Builder ID assigned and stored in `IFRConstants.kt`
5. Users can verify Chameleon's registration on-chain

## Integration Points

- `IFRLockVerifier.kt` — reads `lockedAmount(wallet)` from IFRLock contract
- `IFRTierActivator.kt` — computes tier and activates features
- `TierGatedContent.kt` — UI guard for tier-locked features

## Governance

IFR Builder Registry is governed by IFR token holders. Registration requires:
- Open source codebase (GPL-3.0)
- Security audit (planned with Trail of Bits)
- Community review period
- Governance vote approval
