/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy.engine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decoy Profile Engine — dual profile system for plausible deniability.
 *
 * CONCEPT:
 * - User configures two PINs: real PIN and decoy PIN
 * - Decoy PIN shows a clean, empty profile with no sensitive data
 * - Real PIN shows the actual encrypted data and contacts
 * - The decoy profile is indistinguishable from a fresh install
 *
 * SECURITY:
 * - Real PIN is NEVER visible or derivable from decoy context
 * - Decoy profile has its own clean database (separate Room instance)
 * - No traces of real data in decoy mode
 * - Uses Android Keystore with separate key aliases per profile
 */
@Singleton
class DecoyProfileEngine @Inject constructor() {

    enum class ProfileMode { REAL, DECOY }

    data class DecoyConfig(
        val isEnabled: Boolean = false,
        val decoyPinHash: ByteArray? = null,
        val realPinHash: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DecoyConfig) return false
            return isEnabled == other.isEnabled
        }
        override fun hashCode(): Int = isEnabled.hashCode()
    }

    @Volatile
    private var currentMode: ProfileMode = ProfileMode.REAL

    /**
     * Determine which profile to load based on PIN.
     * Returns DECOY if pin matches decoy hash, REAL if matches real hash.
     */
    fun authenticatePin(pin: String, config: DecoyConfig): ProfileMode {
        if (!config.isEnabled || config.decoyPinHash == null || config.realPinHash == null) {
            return ProfileMode.REAL
        }

        val pinHash = hashPin(pin)

        return when {
            pinHash.contentEquals(config.decoyPinHash) -> {
                currentMode = ProfileMode.DECOY
                ProfileMode.DECOY
            }
            pinHash.contentEquals(config.realPinHash) -> {
                currentMode = ProfileMode.REAL
                ProfileMode.REAL
            }
            else -> throw SecurityException("Invalid PIN")
        }
    }

    fun getCurrentMode(): ProfileMode = currentMode

    fun isDecoyMode(): Boolean = currentMode == ProfileMode.DECOY

    /**
     * Hash PIN with SHA-256 for comparison.
     * Actual authentication uses Argon2id via ChameleonCrypto.
     */
    internal fun hashPin(pin: String): ByteArray {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
    }
}
