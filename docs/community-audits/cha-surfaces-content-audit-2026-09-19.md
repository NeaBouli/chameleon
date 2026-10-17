# Chameleon — Surfaces, Content & AI-Readiness Audit

- **Series:** Collateral Web3 Open Audits (sixth engagement: StealthX clients)
- **Date:** 2026-09-19 (recon 2026-09-15, resumed and verified 2026-09-19)
- **Target:** NeaBouli/chameleon @ `d48e37989125392b902b7298d6fd0af472f84d6d` + live `chameleon.stealthx.tech` (byte-identical to repo, verified)
- **Scope:** public site (index/faq/privacy/ifr/payment-success/wiki×4), README/LOGBUCH/ECOSYSTEM/CLAUDE_CODE_START/docs, cross-app invite wiring, SEO/AI anchors, license/security-policy layer
- **Method:** shared-surfaces recon agent + lead verification (live probes, anonymous GET only)
- **Register:** CHA-14 … CHA-26 (this report) — **0 Critical / 1 High / 5 Medium / 6 Low / 1 Info**

---

## Executive summary

Same platform pattern as SecureChat: the site itself is clean (no trackers, IFR gated with a test, honest disabled-state JSON-LD), but the invite flow leaks into the tracked platform page (CHA-14), and the content layer contradicts the code state in ways that matter: the hero claims "0 Data Collected" against the app's own privacy documents, the wiki hub invites granting the AccessibilityService permission that the manual explicitly says to refuse, the LOGBUCH documents a removed in-app wallet model with hardcoded IFR tier thresholds, and 129 GPL-licensed source files sit under a proprietary LICENSE while SECURITY.md calls the project "open-source".

## Severity table

| ID | Severity | Title |
|----|----------|-------|
| CHA-14 | High | App invite URLs carry sxId + key bundle + handle into the GA4-tracked, unvalidated `?link=` page on stealthx.tech |
| CHA-15 | Medium | Hero "0 Data Collected" contradicted by own privacy.html and Play Data Safety docs |
| CHA-16 | Medium | Wiki hub invites enabling the AccessibilityService the manual says to refuse |
| CHA-17 | Medium | LOGBUCH describes removed wallet/IFR code + hardcoded 2,000/6,000 IFR tier thresholds + GPL claim |
| CHA-18 | Medium | 129 Kotlin files with GPL-3.0 headers vs proprietary LICENSE; SECURITY.md calls the project "open-source" |
| CHA-19 | Medium | Security contact fragmentation (bergamolia@ vs kaspartisan@ vs contact@) — no canonical channel |
| CHA-20 | Low | ifrunit.tech image hotload on the homepage + preconnect to stealthx.tech |
| CHA-21 | Low | "Buy $IFR on Uniswap" CTA on a fail-closed product page |
| CHA-22 | Low | Pricing asymmetry: site shows PLANNED/no prices, docs/PRICING.md carries numbers |
| CHA-23 | Low | F-Droid remnants (FDROID_COMPATIBILITY.md intact, no fdroid/ dir; website-integration plans F-Droid badge) |
| CHA-24 | Low | README broken `CRYPTO_PROTOCOL_SPEC.md` link; "Android 26+" badge ambiguity |
| CHA-25 | Low | Package-name drift (`com.stealthx.chameleon` in stealth invite map vs actual `chameleon24.app`) |
| CHA-26 | Info | Sitemap lastmod stale + lists noindex ifr.html; robots AI opt-in amplifies drift |

---

## CHA-14 — High — Invite URLs feed the GA4-tracked platform page

**Evidence (lead-verified):** `data/.../identity/StealthXIdentity.kt:79-82` builds `https://stealthx.tech/invite/?app=chameleon&link=<urlencoded stealthx://add/…>` — sxId + full public key bundle + optional handle in the URL; the target page loads GA4 and navigates unvalidated deep links (STX-29/31 platform-side). Same defect as SCT-15, generated from this app's code. Neither the site nor the wiki documents the flow.

**Recommendation:** host invites on a tracker-free page; strip `&h=`; fix the platform `?link=` whitelist (STX-31); document the flow.

## CHA-15 — Medium — Hero "0 Data Collected" vs own documents

**Evidence:** `index.html:517` hero stat "**0** / Data Collected". Contradicted by the same repo's `privacy.html:42-57` ("Limited network processing": pseudonymous ID to the signaling service, activation codes, relay routing IDs) and `docs/PLAY_STORE_DATA_SAFETY.md` ("Does the app collect or share user data? **Yes**"). An absolute claim the vendor's own documents refute; Play Data Safety mismatch risk at listing time.

**Recommendation:** replace with "No advertising/analytics SDK" (true) or "local-first".

## CHA-16 — Medium — Wiki hub invites the dangerous permission the manual refuses

**Evidence:** `wiki/index.html:138` banner — "a privacy overlay that sits on top of WhatsApp, Telegram, SMS, browser… automatically encrypts and decrypts text in real time… then it works silently in the background" (present tense); `:151` card "Accessibility Service — **Enable once before the overlay works**. Covers on-device steps and ADB command." Versus `wiki/user-manual.html:152`: "No Accessibility Service or display-over-apps permission is required for the current alpha. **Do not grant these sensitive permissions.**" The hub invites granting a sensitive permission the manual says to refuse — for a feature that is fail-closed in code anyway (CHA report 1).

**Recommendation:** align the hub with the manual (planned/disabled wording).

## CHA-17 — Medium — LOGBUCH drift incl. hidden IFR tier thresholds

**Evidence:** `LOGBUCH.md` (dated 2026-05-10): "ALLE STEPS DONE (S-00 bis S-10)… v0.1.0-alpha ready for audit" (README says S-08/S-09 in progress, S-10 blocked); `:126-134` describes `WalletConnectManager`, `IFRLockVerifier` (web3j eth_call), `IFRTierActivator` "tier from amount" — in-app wallet/IFR code the current llms.txt:20 says "does not exist"; `:282-283` hardcode `PRO Threshold: 2.000 IFR`, `ELITE Threshold: 6.000 IFR` — a token-amount tier model the platform publicly denies; `:269-270` "Lizenz: GPL-3.0" (root LICENSE is proprietary source-available); `:180` F-Droid metadata creation. Public docs describing removed wallet logic mislead token buyers.

**Recommendation:** mark LOGBUCH as historical snapshot or correct it; remove threshold numbers.

## CHA-18 — Medium — GPL headers vs proprietary LICENSE + "open-source" mislabel

**Evidence (lead-verified):** 129 Kotlin files carry `SPDX-License-Identifier: GPL-3.0-or-later` (e.g. `core/.../ProcessTextResult.kt:4`) while the root LICENSE forbids copy/build/run/distribute without written permission; `SECURITY.md:60` says "We are **an open-source project**" — false under the current LICENSE; both READMEs correctly say "source-available, not open source". (SecureChat: 70 files, SCT-21.) The repos' own agent docs admit the conflict is unresolved.

**Recommendation:** legal decision, then mechanical header/LICENSE reconciliation; fix the "open-source" sentence meanwhile (one word).

## CHA-19 — Medium — Security contact fragmentation

**Evidence map:** `SECURITY.md:15` — `bergamolia@protonmail.com` **only** (no advisory mention); `.github/ISSUE_TEMPLATE/security_vulnerability.yml` — same email; but `privacy.html:80` and `docs/user-manual.md` give `kaspartisan@proton.me`; LOGBUCH shows the git history was rewritten to bergamolia; both LICENSEs say `contact@stealthx.tech`; the platform parent's SECURITY.md (stealth) still carries the "report privately → public issues" paradox (STX-30). A researcher with a Chameleon crypto bug finds two different emails depending on the file.

**Recommendation:** one platform security contact + advisories URL, replicated to all SECURITY.md/privacy pages.

## CHA-20 — Low — ifrunit.tech image hotload

`index.html:762,778` hotload `https://ifrunit.tech/assets/ifr_icon_256.png` + `preconnect` to stealthx.tech — third-party IP/referer leak on every homepage visit. Vendor locally.

## CHA-21 — Low — Uniswap CTA on a fail-closed product

`index.html:777` links the Uniswap token page; contract address also in `ifr.html` / `wiki/ifr-unlock.html`. Solicitation optics on an unsellable product — label as informational or gate.

## CHA-22 — Low — Pricing asymmetry

The site shows PLANNED/no prices with disabled buttons (`index.html:805-843` — correct posture), but both repos' `docs/PRICING.md` carry concrete numbers (€9/€19→€14.99/€23.99), and `ECOSYSTEM.md:58` here says "EUR 9 Lifetime" while securechat's copy says price controlled by VLABS. Pick one story.

## CHA-23 — Low — F-Droid remnants

`docs/FDROID_COMPATIBILITY.md` intact, claims "The F-Droid metadata includes build instructions… A Dockerfile is provided" — no `fdroid/` dir, no Dockerfile in repo; BRIDGE.md says F-Droid metadata was removed; `docs/STEALTHX_WEBSITE_INTEGRATION.md:11,25` still plans an F-Droid badge + `/chameleon` page; `CLAUDE_CODE_START.md:349` "Release F-Droid". README.md:91 correctly says not eligible under the current license. Delete or banner-mark.

## CHA-24 — Low — README defects

`README.md:9` badge links `docs/CRYPTO_PROTOCOL_SPEC.md` — **file does not exist** (broken link on the repo's front page); badge "Platform Android 26+" reads as Android version 26 (means API 26; actual minSdk 26/targetSdk 36 per build.gradle.kts).

## CHA-25 — Low — Package-name drift

stealth `invite.html:92` maps chameleon to `com.stealthx.chameleon`; the actual `applicationId` is `chameleon24.app` (`app/build.gradle.kts:39`). Display-only in invite.html (unused for navigation) but wrong; LOGBUCH/BRIDGE carry the same stale name.

## CHA-26 — Info — Anchor hygiene

Sitemap complete (7/7 public pages) but `lastmod 2026-08-28` stale (live Last-Modified 2026-09-04) and lists `ifr.html` (noindex); robots.txt explicitly allows GPTBot/Google-Extended/anthropic-ai — so CHA-15/16/17 propagate into AI answers; no humans.txt (asymmetric with SCT, optional file).

---

## Verified strengths

- **No GA4/trackers/third-party scripts on any page** (live-verified, repo-wide grep clean).
- IFR surfaces fail-closed with a real JS gate test; `payment-success.html` inert and explicit.
- Disabled state honestly disclosed in the site's JSON-LD (`index.html:34`) and wiki user-manual — the correction target is the hub/marketing drift (CHA-15/16), not the base disclosure.
- llms.txt fully consistent with build state (versions, disabled paths, closed Play testing) — best practice.
- LICENSE text identical to securechat's except product name; LICENSE↔README↔FAQ "source-available" wording consistent (the GPL headers are the contradiction, CHA-18).
- Sitemap/robots/JSON-LD/canonical complete and consistent; a11y basics fine (single h1, alts, lang).
