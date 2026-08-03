/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptedSharedPreferences wrapper.
 *
 * Uses AndroidX Security Crypto with MasterKey (AES-256-GCM in Keystore).
 * All values are encrypted at rest.
 *
 * SECURITY:
 * - MasterKey is hardware-backed (Keystore)
 * - Scheme: AES256_SIV for keys, AES256_GCM for values
 * - NO plaintext database keys stored here
 * - Used for non-sensitive app preferences only
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_NAME = "chameleon_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SECURITY_LEVEL_DEFAULT = "security_level_default"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_OVERLAY_DISABLED_MIGRATION = "overlay_disabled_alpha_migration"
        private const val KEY_OVERLAY_WHITELIST = "overlay_whitelist"
        private const val KEY_BACKGROUND_LISTENER_ENABLED = "background_listener_enabled"
        private const val KEY_PRIVATE_ZONE_KEY = "private_zone_key"
        private const val KEY_DECOY_ENABLED = "decoy_enabled"
        private const val KEY_DECOY_PIN_HASH = "decoy_pin_hash"
        private const val KEY_DECOY_PIN_SALT = "decoy_pin_salt"
        private const val KEY_REAL_PIN_HASH = "real_pin_hash"
        private const val KEY_REAL_PIN_SALT = "real_pin_salt"
        private const val KEY_GEOFENCE_ZONES = "geofence_zones"
        private const val KEY_DECOY_PROFILES = "decoy_profiles_json"
        private const val KEY_ENTITLEMENT_TOKEN = "fiat_entitlement_token"
        private val DEFAULT_OVERLAY_WHITELIST = setOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms",
            "com.discord",
            "com.google.android.gm"
        )
    }

    init {
        if (!prefs.getBoolean(KEY_OVERLAY_DISABLED_MIGRATION, false)) {
            prefs.edit()
                .putBoolean(KEY_OVERLAY_ENABLED, false)
                .putBoolean(KEY_OVERLAY_DISABLED_MIGRATION, true)
                .apply()
        }
    }

    var isOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    fun markOnboardingDone(): Boolean =
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).commit()

    var defaultSecurityLevel: String
        get() = prefs.getString(KEY_SECURITY_LEVEL_DEFAULT, "PROTECTED") ?: "PROTECTED"
        set(value) = prefs.edit().putString(KEY_SECURITY_LEVEL_DEFAULT, value).apply()

    var overlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var overlayWhitelistPackages: Set<String>
        get() = prefs.getStringSet(KEY_OVERLAY_WHITELIST, DEFAULT_OVERLAY_WHITELIST) ?: DEFAULT_OVERLAY_WHITELIST
        set(value) = prefs.edit().putStringSet(KEY_OVERLAY_WHITELIST, value).apply()

    var backgroundListenerEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_LISTENER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_LISTENER_ENABLED, value).apply()

    var privateZoneKeyBase64: String?
        get() = prefs.getString(KEY_PRIVATE_ZONE_KEY, null)
        set(value) = prefs.edit().putString(KEY_PRIVATE_ZONE_KEY, value).apply()

    var decoyEnabled: Boolean
        get() = prefs.getBoolean(KEY_DECOY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DECOY_ENABLED, value).apply()

    var decoyPinHashBase64: String?
        get() = prefs.getString(KEY_DECOY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_DECOY_PIN_HASH, value).apply()

    var decoyPinSaltBase64: String?
        get() = prefs.getString(KEY_DECOY_PIN_SALT, null)
        set(value) = prefs.edit().putString(KEY_DECOY_PIN_SALT, value).apply()

    var realPinHashBase64: String?
        get() = prefs.getString(KEY_REAL_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_REAL_PIN_HASH, value).apply()

    var realPinSaltBase64: String?
        get() = prefs.getString(KEY_REAL_PIN_SALT, null)
        set(value) = prefs.edit().putString(KEY_REAL_PIN_SALT, value).apply()

    var decoyProfilesJson: String
        get() = prefs.getString(KEY_DECOY_PROFILES, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_DECOY_PROFILES, value).apply()

    var geofenceZones: Set<String>
        get() = prefs.getStringSet(KEY_GEOFENCE_ZONES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_GEOFENCE_ZONES, value).apply()

    var entitlementToken: String?
        get() = prefs.getString(KEY_ENTITLEMENT_TOKEN, null)
        set(value) = if (value == null) prefs.edit().remove(KEY_ENTITLEMENT_TOKEN).apply()
                     else prefs.edit().putString(KEY_ENTITLEMENT_TOKEN, value).apply()

    fun clearEntitlementToken(): Boolean =
        prefs.edit().remove(KEY_ENTITLEMENT_TOKEN).commit()

    fun clear() {
        prefs.edit().clear().apply()
    }

}
