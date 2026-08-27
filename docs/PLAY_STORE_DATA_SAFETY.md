# Chameleon - Play Store Data Safety Form

This document reflects the `chameleon24.app` closed-alpha candidate at version
`0.1.13-alpha` (version code 14). Recheck it whenever network, entitlement, messaging,
location, analytics, or SDK behavior changes.

## Data Collection

| Question | Answer |
|----------|--------|
| Does the app collect or share user data? | Yes - limited data is transmitted off-device for app functionality |
| Is collected data encrypted in transit? | Yes |
| Can users request deletion? | Yes - through the privacy-policy contact channels |

## Data Types To Declare

| Play data type | Collected | Shared | Required | Purpose |
|----------------|-----------|--------|----------|---------|
| Personal info - User IDs | Yes | No* | Required for the default contact listener | App functionality; account management; security and fraud prevention |
| Personal info - Other info (activation code and entitlement token) | Yes | No | Optional; only after paid activation | App functionality; account management; security and fraud prevention |
| Personal info - Name (optional display handle) | Yes | No* | Optional | Contact exchange and app functionality |
| Messages | No | No | Not available in this alpha | - |
| Precise location | Yes** | Yes** | Optional; only after location permission and geofence setup | App functionality |

`*` User-directed signed contact bundles go to the recipient selected by the user.
Under Google Play's user-initiated transfer exception this is not declared as
third-party sharing, but it is still collected because it leaves the device through
the StealthX relay.

`**` Chameleon does not send location to StealthX. It supplies geofence coordinates to
the Google Play services Location SDK. The conservative Play declaration treats this
SDK processing as collected and shared with the service provider. Confirm against the
current Google Play services Location data-safety guidance before every submission.

## Data Not Collected

- Android contacts or address book
- Photos, videos, or user files
- Payment-card, bank-account, wallet, or purchase-history data
- Advertising ID
- Analytics, crash logs, diagnostics, or advertising data
- Accessibility-captured content (processed locally only)

## Processing Details

- A persistent pseudonymous `sx_...` identifier is sent to
  `wss://api.stealthx.tech/signal` for contact routing and relay operation.
- Signed public-key bundles can include routing identifiers, public keys, a timestamp,
  and an optional display handle. Private keys never leave the device.
- Cross-device messaging is disabled in this alpha. Reassess the Messages declaration
  before enabling any relay send/receive path.
- Paid activation sends a user-entered activation code and pseudonymous device-bound ID.
  Entitlement refresh later sends the signed entitlement token.
- Network endpoints use HTTPS or WSS. Activation and contact exchange additionally use
  certificate pinning; the message relay uses WSS and application-layer encryption.

## Storage And Deletion

- Local structured data uses SQLCipher; sensitive preferences use Android encrypted
  preferences. Cloud backup and device transfer are disabled.
- Users can delete contacts/messages in the app and can delete all local data by clearing
  app storage or uninstalling Chameleon.
- The current app has no user account or in-app server-deletion control. Server-side
  deletion requests use the contact channels in the privacy policy.
- Purchase and entitlement records may be retained for security, fraud prevention,
  refunds, tax, and legal obligations.

## SDK Inventory Relevant To Data Safety

- Google Play services Location (optional geofencing)
- OkHttp (StealthX HTTPS/WSS network transport)
- No Firebase, advertising, analytics, crash-reporting, Stripe, RevenueCat, or Google
  Play Billing SDK is included.

## Privacy Policy

URL: https://chameleon.stealthx.tech/privacy.html
