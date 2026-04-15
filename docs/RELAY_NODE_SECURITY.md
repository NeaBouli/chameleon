# SecureChat Relay Node — App Signature Security

## How It Works
Every SecureChat app sends its SHA-256 app signature hash
when connecting to a relay node. Relay nodes can verify
they are talking to an official StealthX app.

## Official Fingerprint
The SHA-256 fingerprint of the official SecureChat APK
will be entered here after the first release.
Status: PENDING (app still in development)

## Relay Node Configuration
Set environment variable:
ALLOWED_SIGNATURES=<fingerprint>

## Why This Matters
A fork can copy the code (GPL-3.0 allows that).
But it cannot copy your keystore.
Without your keystore, the fork has a different fingerprint.
Relay nodes that set ALLOWED_SIGNATURES will reject forks.

## Node Operator Decision
Relay node operators decide whether to set ALLOWED_SIGNATURES
or allow all clients:
- Official Nodes: ALLOWED_SIGNATURES set (recommended)
- Community Nodes: open (promotes decentralization)
