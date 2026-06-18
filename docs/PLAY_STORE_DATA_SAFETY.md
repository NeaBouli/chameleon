# Chameleon — Play Store Data Safety Form

## Data Collection

| Question | Answer |
|----------|--------|
| Does your app collect or share user data? | No |
| Does your app collect any of the required data types? | No |

## Data Types

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Location | No* | No | *Geofencing is local-only, never sent to server |
| Contacts | No | No | Contact keys stored locally, never uploaded |
| Messages | No | No | All messages local, no server |
| Photos | No | No | Private Zone photos encrypted locally |
| Files | No | No | Encrypted locally, never uploaded |
| Device identifiers | No | No | No analytics, no telemetry |
| Crash logs | No | No | No crash reporting SDK |

## Encryption

| Question | Answer |
|----------|--------|
| Is data encrypted in transit? | N/A — no network data by default |
| Is data encrypted at rest? | Yes — SQLCipher + XChaCha20-Poly1305 |

## Data Deletion

Users can delete all data by:
1. Clearing app data in Android Settings
2. Uninstalling the app
3. Using the in-app "Clear All Data" function

## IFR Token Verification

The public app does not perform wallet verification for IFR discounts. IFR discounts are verified on the website before Stripe checkout.
- **Data received:** Locked token amount (public on blockchain)
- **Storage:** Cached locally with HMAC tamper protection
- **No account, no registration, no server**

## Privacy Policy

URL: https://stealthx.tech/chameleon/privacy
