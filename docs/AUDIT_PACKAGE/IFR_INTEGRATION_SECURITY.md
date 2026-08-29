# Chameleon - IFR Purchase Discount Boundary

_Current as of 2026-08-28. This document replaces the retired in-app IFR design._

## Current Product Boundary

Chameleon contains no WalletConnect client, wallet-address input, Ethereum RPC lookup,
IFR lock verification or token-derived feature tier. The Android app never asks a user to
prove IFR ownership and stores no wallet state.

IFR is relevant only before purchase on the public website. The planned checkout flow is:

1. The customer connects a wallet in the browser and signs a nonce.
2. The seller verifies the signature and reads the IFR balance server-side.
3. Any positive IFR balance qualifies for the discount displayed by the seller; there is
   no token-amount tier threshold.
4. After payment and fiscal processing, the customer receives a server-signed,
   device-bound activation code for Chameleon.
5. The Android app verifies the signed activation credential and applies the purchased
   access tier. It does not receive or retain the wallet address.

The browser verification and discounted checkout remain disabled until payment and Greek
fiscal integration are approved. Failure of any verification or checkout dependency is
fail-closed.

## Retired Design

Earlier audit drafts described an `IFRLock` contract lookup, manual wallet entry,
WalletConnect deep links, fixed 2,000/6,000 IFR thresholds and an HMAC-protected local IFR
cache. Those components were removed from the Android source and Gradle graph. They are not
part of any current Chameleon build and must not be reintroduced as a mobile unlock path.

## Security Properties

- Wallet signatures are requested only by the seller's browser checkout.
- A wallet address alone is never accepted as proof of ownership.
- Chameleon accepts only server-signed activation credentials for paid access.
- Private signing keys and payment/fiscal secrets remain server-side.
- Android feature access fails closed when a credential is absent, invalid, expired or
  revoked.
