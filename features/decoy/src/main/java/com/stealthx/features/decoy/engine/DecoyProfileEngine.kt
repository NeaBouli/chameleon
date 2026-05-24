/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy.engine

import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.IfrTier
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
 * - PIN hashing uses Argon2id via ChameleonCrypto (memory-hard, brute-force resistant)
 */
@Singleton
class DecoyProfileEngine @Inject constructor(
    private val tierGate: TierGate
) {

    private fun requireElite() {
        if (tierGate.getTierSync() < IfrTier.ELITE) {
            throw SecurityException("DecoyProfileEngine requires ELITE tier")
        }
    }

    init {
        SodiumInitializer.ensureInit()
    }

    enum class ProfileMode { REAL, DECOY }

    data class DecoyConfig(
        val isEnabled: Boolean = false,
        val decoyPinHash: ByteArray? = null,
        val decoyPinSalt: ByteArray? = null,
        val realPinHash: ByteArray? = null,
        val realPinSalt: ByteArray? = null
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

    /** Generate a fresh random Argon2id salt for PIN hashing. */
    fun generatePinSalt(): ByteArray = ChameleonCrypto.generateSalt()

    /**
     * Determine which profile to load based on PIN.
     * Returns DECOY if pin matches decoy hash, REAL if matches real hash.
     * Both hashes are verified to resist timing-based disambiguation.
     */
    fun authenticatePin(pin: String, config: DecoyConfig): ProfileMode {
        requireElite()
        if (!config.isEnabled ||
            config.decoyPinHash == null || config.decoyPinSalt == null ||
            config.realPinHash == null || config.realPinSalt == null) {
            return ProfileMode.REAL
        }

        val decoyDerived = hashPin(pin, config.decoyPinSalt)
        val realDerived  = hashPin(pin, config.realPinSalt)

        return when {
            decoyDerived.contentEquals(config.decoyPinHash) -> {
                currentMode = ProfileMode.DECOY
                ProfileMode.DECOY
            }
            realDerived.contentEquals(config.realPinHash) -> {
                currentMode = ProfileMode.REAL
                ProfileMode.REAL
            }
            else -> throw SecurityException("Invalid PIN")
        }
    }

    /**
     * Authenticate PIN against real PIN, single decoy PIN, and a list of multi-decoy entries.
     * Multi-decoy entries are Pair(pinHash, pinSalt).
     * Does not require Elite tier — users must always be able to authenticate with configured PINs
     * even if tier changes after setup.
     */
    fun authenticateWithMultiDecoy(
        pin: String,
        config: DecoyConfig,
        multiDecoyEntries: List<Pair<ByteArray, ByteArray>>
    ): ProfileMode {
        if (!config.isEnabled ||
            config.realPinHash == null || config.realPinSalt == null) {
            return ProfileMode.REAL
        }

        val realDerived = hashPin(pin, config.realPinSalt)
        if (realDerived.contentEquals(config.realPinHash)) {
            currentMode = ProfileMode.REAL
            return ProfileMode.REAL
        }

        if (config.decoyPinHash != null && config.decoyPinSalt != null) {
            val decoyDerived = hashPin(pin, config.decoyPinSalt)
            if (decoyDerived.contentEquals(config.decoyPinHash)) {
                currentMode = ProfileMode.DECOY
                return ProfileMode.DECOY
            }
        }

        for ((hash, salt) in multiDecoyEntries) {
            val derived = hashPin(pin, salt)
            if (derived.contentEquals(hash)) {
                currentMode = ProfileMode.DECOY
                return ProfileMode.DECOY
            }
        }

        throw SecurityException("Invalid PIN")
    }

    fun getCurrentMode(): ProfileMode = currentMode

    fun isDecoyMode(): Boolean = currentMode == ProfileMode.DECOY

    /**
     * Hash PIN with Argon2id (memory-hard, brute-force resistant).
     * Each PIN requires its own unique salt — use [generatePinSalt] to create one.
     */
    internal fun hashPin(pin: String, salt: ByteArray): ByteArray =
        ChameleonCrypto.deriveKey(pin.toCharArray(), salt)
}
