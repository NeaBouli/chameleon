# Chameleon — User Manual

**Version 0.1.0-alpha · StealthX Platform**

---

## What Is Chameleon?

Chameleon is a context-aware privacy layer for Android. It does not replace your existing apps — it runs silently on top of them. Using Android's Accessibility Service, Chameleon intercepts text as you type in any whitelisted app (WhatsApp, Telegram, Signal, Gmail, and others), encrypts it in real time using XChaCha20-Poly1305, and reinjects the ciphertext. Your recipient sees encrypted text. No one between you and them — not the app, not the platform, not your carrier — can read it.

Beyond message encryption, Chameleon provides a context-aware rule engine that automatically adjusts your security level based on your location, active app, WiFi network, time of day, or connected Bluetooth device. It also includes a Private Zone (encrypted file vault), a Decoy Profile system, and geofencing.

---

## How It Works

**Overlay Encryption:** Chameleon's Accessibility Service watches for text input events in whitelisted apps. When you type and send a message, the service intercepts the content, passes it to an isolated crypto process (running separately from the main app via AIDL), and receives back the encrypted ciphertext. This ciphertext is injected into the text field in place of your original text. You hit send. The recipient receives the encrypted string.

**Decryption:** The recipient uses their own Chameleon instance, which recognizes the ciphertext format and decrypts it on display — also through the Accessibility Service, transparently.

**Rule Engine:** You define rules that associate a trigger (location, app, WiFi, time, Bluetooth) with a security level (Public, Protected, Private, Camouflage). Chameleon evaluates all active rules at any given moment and applies the highest matching security level. This is fail-secure: the highest level always wins.

**Security Isolation:** The Accessibility Service contains no cryptographic code. All encryption and key management happen in an isolated `:crypto` process. Even if the Accessibility Service were compromised, your keys remain protected.

---

## Tier Overview

Chameleon uses IFR token locking for permanent tier access. No subscriptions.

| Feature | Free | Pro (≥ 2,000 IFR) | Elite (≥ 6,000 IFR) |
|---|---|---|---|
| Overlay encryption | Yes | Yes | Yes |
| Whitelisted apps | Yes | Yes | Yes |
| Manual geofencing | 3 zones max | Unlimited | Unlimited |
| Private Zone | 100 MB cap | Unlimited | Unlimited |
| Automation rules | No | Yes | Yes |
| Automatic geofencing triggers | No | Yes | Yes |
| Decoy profile | No | No | Yes |
| Multi-decoy profiles | No | No | Yes |
| Advanced threat detection | No | No | Yes |
| Zero telemetry mode | No | No | Yes |

---

## First-Time Setup

### Step 1 — Enable the Accessibility Service

Chameleon's overlay encryption requires Android's Accessibility Service permission. Without it, the app can display its dashboard but cannot intercept or encrypt any text.

To enable it:
1. Open Android **Settings**
2. Go to **Accessibility**
3. Find **Chameleon** in the list of installed services
4. Tap it and toggle **Use service** on
5. Confirm the system warning

The Accessibility Service must remain enabled for overlay encryption to function. If Android disables it (e.g., after an update), return to Accessibility Settings and re-enable it.

### Step 2 — Configure the Overlay

Open Chameleon → **Settings** → **Overlay Encryption**. Toggle **Overlay Active** on. Enable the apps you want Chameleon to monitor (WhatsApp, Telegram, Signal, Discord, Gmail are pre-configured).

### Step 3 — Set Your Security Level (Optional)

The Dashboard shows your current security level. You can set it manually by tapping the level indicator. To have Chameleon set it automatically based on context, configure the Rule Engine (Pro tier).

### Step 4 — Unlock Features (Optional)

If you hold IFR tokens and want to unlock Pro or Elite features, open Settings → **IFR Token** and connect your Ethereum wallet.

---

## Dashboard

The Dashboard is the home screen. It shows:

- **Current security level** — color-coded icon (green, yellow, orange, or red)
- **Tier badge** — top right (FREE, PRO, or ELITE)
- **Active Rules** — a list of automation rules currently matching your context
- **Quick action buttons** — Overlay, Messenger, Keys

---

## Security Levels

Chameleon uses four security levels:

| Level | Color | Description |
|---|---|---|
| **Public** | Green | No encryption. Use only in fully trusted, private environments. |
| **Protected** | Yellow | Standard encryption. The default level. |
| **Private** | Orange | High encryption with stricter key parameters. |
| **Camouflage** | Red | Maximum protection. All security features active. Stealth mode. |

When multiple rules are active simultaneously, the highest security level always takes precedence.

---

## Settings — Complete Reference

Access Settings from the gear icon on the Dashboard.

---

### IFR Token (Tier Upgrade)

The top section of Settings shows your current tier and provides access to the IFR unlock flow.

**Current Tier** — Displayed as a badge (FREE / PRO / ELITE).

**Upgrade button** — Opens the IFR Unlock screen (visible when not on Elite).

**IFR Unlock Screen:**

*Tier Status Card:*
Shows your current tier, locked IFR amount, wallet address, and cache expiry (30-day window).

*Connect Wallet (WalletConnect):*
Tap **Connect Wallet**. Chameleon launches your installed Ethereum wallet app (MetaMask, Trust Wallet, etc.). Your wallet signs a challenge proving ownership of the address. Chameleon then queries the IFR contract on Ethereum Mainnet directly.

- ≥ 6,000 IFR locked → Elite (permanent, no expiry)
- ≥ 2,000 IFR locked → Pro (permanent, no expiry)
- < 2,000 IFR → Free (balance shown, no unlock)

*Manual Address Entry:*
Paste your Ethereum address (0x format). Chameleon verifies the locked balance on-chain. Manual verifications expire after 30 days and re-verify every 24 hours. If re-verification is not possible (offline), the cached tier is kept until expiry.

The verification result is stored in an encrypted local database. The cache is protected by an HMAC-SHA256 tag computed with a hardware-backed key from Android Keystore. If the cache is tampered with, the tier reverts to Free.

---

### Overlay Encryption

Controls the core text encryption feature.

**Overlay Active**
Master toggle for the entire overlay encryption system. When off, Chameleon does not intercept any text in any app. Default: On.

**Whitelisted Apps**

Pre-configured apps (each with an on/off toggle):
- WhatsApp
- Telegram
- Signal
- Discord
- Gmail

Toggle any app on to enable text interception in that app. Toggle off to disable it.

**Custom Apps**
To add any other app, enter its package name in the input field (e.g., `com.custom.messenger`) and tap **Add**. The package name must contain at least one dot. All added apps appear in the list with their own toggle.

*How to find a package name:* In Android Settings → Apps, find the app and look at the URL in the Google Play Store link, or use a package name finder app.

**Security properties of the overlay window:**
- FLAG_SECURE: The overlay itself cannot be screenshotted.
- FLAG_NOT_FOCUSABLE: The overlay never steals keyboard focus from the underlying app.
- Overlay is never drawn on the lock screen.

---

### Private Zone

An encrypted file vault stored locally on your device. Files stored here are encrypted with XChaCha20-Poly1305. File names on disk are SHA-256 hashed — the original names are never written to storage unencrypted.

The vault key is generated randomly on first use and stored in encrypted SharedPreferences (AES-256-GCM). It is never uploaded or backed up.

**File count** — Shown at the top of the screen ("X encrypted files").

**File list** — Scrollable list of all stored files, each showing its name and "Encrypted vault item" label.

**Import File**
Opens the system file picker. Select any file type. The file is encrypted and stored in the vault. The original file is not deleted from its source location — you must delete it manually if needed.

**Secure Photo**
Opens the camera. Take a photo. It is immediately compressed to JPEG (92% quality) and stored encrypted as `photo_TIMESTAMP.jpg`. The photo is never saved to your gallery or camera roll.

**Storage Limits:**
- Free tier: 100 MB total. An error is shown if you exceed the limit.
- Pro and Elite: Unlimited.

---

### Geofencing

Define geographic zones. When you are physically inside a zone, the Rule Engine can use it as a trigger. On Pro and Elite, geofencing can trigger automatic security level changes.

**Free tier:** Up to 3 zones.
**Pro / Elite:** Unlimited zones.

**Permissions required:**

Before you can add zones, Chameleon needs location access.

1. Tap **Allow Location** → grants `ACCESS_FINE_LOCATION` at runtime.
2. On Android 10+: Tap **Allow Background Location** → on Android 11 and above, this redirects you to Android Settings where you must choose "Allow all the time" for Chameleon. On Android 10, the permission is requested directly.

Background location is required for geofencing to trigger when Chameleon is not in the foreground.

**Adding a Zone:**

Fill in all four fields and tap **Add Geofence Zone**:

- **Zone name** — A label for this zone (e.g., "Home", "Office", "Airport").
- **Latitude** — Between -90.0 and 90.0.
- **Longitude** — Between -180.0 and 180.0.
- **Radius (meters)** — Minimum 100 meters (enforced by Android's geofencing API). GPS accuracy is typically ±5–10 meters.

Each zone appears as a card showing: name, coordinates, and radius.

*Note on accuracy:* GPS drift means the effective boundary is approximately ±10 meters around the specified radius. For office buildings or other enclosed spaces, set a radius of at least 150–200 meters to account for GPS uncertainty indoors.

---

### Rule Engine *(Pro and Elite)*

The Rule Engine lets you define context-aware triggers that automatically set your security level.

This setting is locked on Free tier. Tap **Unlock** to open the IFR unlock flow.

**Rule Trigger Types:**

| Trigger | How It Works |
|---|---|
| **App** | Rule activates when a specific app is in the foreground (e.g., "when Telegram is open → Private") |
| **WiFi** | Rule activates when connected to a specific SSID (e.g., "when on 'CoffeeShop_WiFi' → Camouflage") |
| **Location** | Rule activates when inside a named geofence zone (e.g., "when at Work → Protected") |
| **Time** | Rule activates during a time window (e.g., "weekdays 09:00–17:00 → Protected") |
| **Bluetooth** | Rule activates when a specific Bluetooth device is connected (e.g., "when car BT is connected → Protected") |

**Conflict Resolution:**
If multiple rules are active at the same time, Chameleon applies the highest security level among all matching rules. It never downgrades. If no rules match, the default level is Protected.

*Example:* You have a rule "Work WiFi → Protected" and another rule "Evening hours → Private". If both apply at the same time (working late), the result is Private — the higher of the two.

**Dashboard display:**
Active rules are listed on the Dashboard under "Active Rules", showing the rule name, trigger type, and resulting security level.

---

### Decoy Profile *(Elite only)*

The Decoy Profile creates a second, empty identity accessible via a wrong PIN. If someone forces you to unlock the device, you enter the decoy PIN and they see a clean, empty app with no messages, no files, and no zones configured.

This setting is locked on Free and Pro tiers.

**Setup:**

1. **Real PIN** — Enter 4–12 digits. This is the PIN that unlocks your actual data.
2. **Decoy PIN** — Enter 4–12 digits. Must not match your Real PIN. This is the PIN that shows the empty decoy.
3. **Confirm Decoy PIN** — Re-enter the decoy PIN to confirm.
4. Tap **Save Decoy Profile**.

Status changes to **"Status: Enabled"** (green).

**How it works on launch:**
When Decoy is enabled, Chameleon shows a PIN unlock screen on every launch.
- Enter the Real PIN → actual profile loads (all data accessible)
- Enter the Decoy PIN → decoy mode loads (empty profile, no data visible)
- In decoy mode, tap **Lock** to return to the PIN screen

**To disable:**
Return to Settings → Decoy Profile → tap **Disable Decoy Profile**.

**PIN storage:**
Both PINs are hashed with Argon2id (64 MB memory cost, 3 iterations) with a unique random salt per PIN. The hashes and salts are stored in encrypted SharedPreferences. The raw PINs are never stored.

---

### About

- Version: 0.1.0-alpha
- License: Source-Available
- Platform: StealthX

---

## Permissions Reference

| Permission | Purpose | When Requested |
|---|---|---|
| Accessibility Service | Text interception for overlay encryption | Manual (Android Settings) |
| System Alert Window | Overlay display over other apps | Checked at overlay activation |
| Camera | QR code scanning for key exchange | On demand |
| NFC | Key exchange via NFC (future) | On demand |
| Fine Location | Geofencing zone detection | Runtime, when adding first zone |
| Background Location | Geofencing while app is in background | Runtime, after fine location granted |
| Foreground Service | Location tracking worker for geofencing | Declared, no prompt |
| Biometric | Optional PIN/face/fingerprint auth | Declared, used if configured |
| Vibrate | Security alert haptics | Declared, no prompt |
| Boot Completed | Auto-restart rule engine after reboot | Declared, no prompt |

Chameleon does not request internet permission. IFR verification is performed by your external wallet app. Chameleon communicates with the wallet app via Android Intents, not direct network calls.

---

## Common Workflows

### Enable Overlay Encryption on WhatsApp

1. Open Chameleon → Settings → Overlay Encryption.
2. Ensure **Overlay Active** is toggled on.
3. Find WhatsApp in the whitelist and ensure its toggle is on.
4. Open WhatsApp.
5. Type a message and tap Send.
6. Your contact receives the encrypted ciphertext. They need Chameleon to decrypt it automatically.

### Import a Secret File

1. Settings → Private Zone.
2. Tap **Import File**.
3. Navigate to the file in the system picker and select it.
4. The file is encrypted and added to the vault.
5. After confirming it appears in the list, delete the original from its source location if needed.

### Take a Secure Photo

1. Settings → Private Zone.
2. Tap **Secure Photo**.
3. The camera opens. Take the photo.
4. The photo is stored encrypted in the vault and never appears in your gallery.

### Set Up a Geofence Zone

1. Settings → Geofencing.
2. Grant location permissions if prompted.
3. Enter a zone name, latitude, longitude, and radius.
4. Tap **Add Geofence Zone**.
5. The zone appears in the list and is now available as a trigger in the Rule Engine (Pro).

### Create a Decoy Profile (Elite)

1. Settings → Decoy Profile.
2. Enter your Real PIN (digits only, 4–12 characters).
3. Enter a Decoy PIN (different from Real PIN).
4. Re-enter the Decoy PIN to confirm.
5. Tap **Save Decoy Profile**.
6. Force-close and reopen the app.
7. A PIN screen appears — enter the Decoy PIN to test the empty decoy state.

### Unlock Pro Tier via WalletConnect

1. Settings → tap the Upgrade button (top of Settings).
2. On the IFR Unlock screen, tap **Connect Wallet**.
3. Your wallet app opens. Sign the authorization message.
4. Chameleon verifies your locked IFR balance on Ethereum.
5. If ≥ 2,000 IFR are locked, Pro features unlock immediately.
6. Return to Settings — the tier badge updates and Pro features are available.

---

## Security Architecture — Brief Overview

**Cryptographic primitives:**
- XChaCha20-Poly1305 — authenticated encryption for messages and vault files
- X25519 — ephemeral Diffie-Hellman key exchange
- Ed25519 — public key signing and contact verification
- Argon2id — PIN and passphrase hashing (memory-hard, GPU-resistant)
- HMAC-SHA256 — IFR cache integrity validation
- AES-256-GCM — encrypted SharedPreferences (AndroidX Security Crypto)

**Key storage:**
- Android Keystore (TEE or StrongBox) — hardware-backed storage for master keys
- Private keys never exported to plaintext, never leave the hardware boundary
- Vault key (Private Zone) — 32 random bytes stored in encrypted SharedPreferences

**Process isolation:**
- Accessibility Service and crypto engine run in separate processes
- Separated via AIDL (Android Interface Definition Language)
- A compromised Accessibility Service cannot access keys or perform decryption

**Three rules Chameleon never breaks:**
1. On any error, encrypt — never fall back to plaintext silently.
2. Private keys never leave Android Keystore.
3. The highest security level always wins when multiple rules conflict.

---

## Troubleshooting

**Overlay is not encrypting my text**
Check that the Accessibility Service is still enabled: Android Settings → Accessibility → Chameleon → Use service: On. Android may disable accessibility services after certain system updates or security policy changes.

Also verify that the target app is in your whitelist (Settings → Overlay Encryption) and that **Overlay Active** is toggled on.

**Geofencing is not triggering**
Ensure background location is granted: Android Settings → Apps → Chameleon → Permissions → Location → set to "Allow all the time". On some devices (Samsung, Huawei, Xiaomi), additional battery optimization settings must be disabled for background services to run reliably.

**The Decoy PIN screen is not appearing**
Decoy must be enabled and saved correctly. Open Settings → Decoy Profile and confirm the status shows "Status: Enabled" (green). Force-close the app (not just minimize it) and reopen.

**My files are not showing in Private Zone**
Files imported into the vault are never written to the regular file system — they exist only in the encrypted vault directory. They will not appear in your gallery, file manager, or any other app.

**My tier shows Free after verifying IFR**
Ensure your tokens are locked in the IFR contract, not just held in your wallet. Visit ifrunit.tech to lock tokens. After locking, return to Settings → IFR Token and verify again.

**The app crashes when opening on a new device**
If Decoy Profile was enabled on a previous installation and you reinstall, the hashed PINs are gone. The app will open normally (no PIN screen) since there is no profile to unlock. Reconfigure Decoy if needed.
