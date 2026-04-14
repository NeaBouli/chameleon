/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore manager.
 *
 * ALL private keys stay inside the hardware Secure Element.
 * Keys NEVER leave the chip — crypto operations happen inside.
 *
 * Hierarchy:
 *   StrongBox (Titan M / dedicated SE) — preferred
 *   TEE (Trusted Execution Environment)  — fallback if no StrongBox
 *   Software (never used for production keys)
 *
 * Per-use authentication: keys require biometric/PIN for every use.
 * Time-based auth (-1) is explicitly disabled.
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val PURPOSE_ENCRYPT_DECRYPT =
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        const val PURPOSE_SIGN_VERIFY =
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    }

    // ── AES-256 for local symmetric encryption ─────────────────

    /**
     * Generate or retrieve an AES-256 key for symmetric encryption.
     * Used for database encryption key wrapping and EncryptedSharedPreferences.
     *
     * NOTE: We use AES-256-GCM *only* within the Keystore for key wrapping.
     * All application-level encryption uses XChaCha20 via :stealthx-crypto.
     */
    fun getOrCreateAesKey(alias: String, requireAuth: Boolean = true): SecretKey {
        if (keyStore.containsAlias(alias)) {
            return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val keySpec = KeyGenParameterSpec.Builder(alias, PURPOSE_ENCRYPT_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(requireAuth)
            .setUserAuthenticationValidityDurationSeconds(-1) // Per-use auth
            .setStrongBoxBacked(isStrongBoxAvailable())
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .apply { init(keySpec) }
            .generateKey()
    }

    // ── Ed25519 for signing (key bundles, attestation) ─────────

    /**
     * Generate an Ed25519 signing key pair in the Keystore.
     * Used for signing public key bundles during contact key exchange.
     */
    fun getOrCreateSigningKeyPair(alias: String): KeyPair {
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
            return KeyPair(entry.certificate.publicKey, entry.privateKey)
        }

        val keySpec = KeyGenParameterSpec.Builder(alias, PURPOSE_SIGN_VERIFY)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("ED25519"))
            .setDigests(KeyProperties.DIGEST_NONE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1)
            .setStrongBoxBacked(isStrongBoxAvailable())
            .build()

        return KeyPairGenerator.getInstance("EC", KEYSTORE_PROVIDER)
            .apply { initialize(keySpec) }
            .generateKeyPair()
    }

    // ── HMAC key for IFR tier cache protection ──────────────────

    /**
     * Generate or retrieve an HMAC-SHA256 key for IFR cache HMAC.
     * Used to detect tampering with cached tier results.
     * Does NOT require user auth — called on app start during tier check.
     */
    fun getOrCreateHmacKey(alias: String): SecretKey {
        if (keyStore.containsAlias(alias)) {
            return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val keySpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(256)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false) // Called without user interaction
            .setStrongBoxBacked(false) // HMAC keys: TEE sufficient
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE_PROVIDER)
            .apply { init(keySpec) }
            .generateKey()
    }

    // ── Utility ─────────────────────────────────────────────────

    fun deleteKey(alias: String) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    fun listAllAliases(): List<String> = keyStore.aliases().toList()

    fun containsKey(alias: String): Boolean = keyStore.containsAlias(alias)

    /**
     * Check if StrongBox (dedicated Secure Element) is available.
     * Titan M, StrongBox, embedded SE chips qualify.
     * Falls back to TEE if unavailable.
     */
    fun isStrongBoxAvailable(): Boolean {
        return try {
            val testAlias = "_sb_test_${System.currentTimeMillis()}"
            val spec = KeyGenParameterSpec.Builder(testAlias, PURPOSE_ENCRYPT_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setStrongBoxBacked(true)
                .build()
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                .apply { init(spec) }
                .generateKey()
            keyStore.deleteEntry(testAlias)
            true
        } catch (e: StrongBoxUnavailableException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
