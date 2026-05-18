# Chameleon — IFR Builder Registry

## Registration Status

**Status:** Pending Governance Registration

## Contract

- **BuilderRegistry:** `0xdfe6636DA47F8949330697e1dC5391267CEf0EE3`
- **Chain:** Ethereum Mainnet (Chain ID 1)
- **Governance Owner:** onlyOwner registration; transaction must be sent by IFR governance/timelock
- **Builder Wallet:** TBD
- **Builder ID:** no numeric ID; registry key is the builder wallet address

## Registration Process

1. Submit application to IFR governance with project details
2. Governance vote (IFR token holders)
3. On approval: `registerBuilder(wallet, name, url, category)` called by governance
4. Builder wallet stored in `IFRConstants.kt` when governance registration is confirmed
5. Users can verify Chameleon's registration on-chain

## Mainnet Call

Contract source: `/Users/gio/Desktop/repos/inferno/contracts/BuilderRegistry.sol`

```solidity
registerBuilder(
    address wallet,
    string calldata name,
    string calldata url,
    string calldata category
)
```

Valid categories are `creator`, `integration`, `tooling`, and `dao`.
For Chameleon the intended category is `integration`.

## Integration Points

- `IFRLockVerifier.kt` — reads `lockedAmount(wallet)` from IFRLock contract
- `IFRTierActivator.kt` — computes tier and activates features
- `TierGatedContent.kt` — UI guard for tier-locked features

## Governance

IFR Builder Registry is governed by IFR token holders. Registration requires:
- Source-available codebase (Source-Available)
- Security audit (planned with Trail of Bits)
- Community review period
- Governance vote approval
