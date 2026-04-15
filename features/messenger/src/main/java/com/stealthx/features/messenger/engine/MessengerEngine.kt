/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.engine

import com.stealthx.domain.keys.X25519KeyManager
import com.stealthx.shared.model.PublicKeyBundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Messenger Engine — local E2E encrypted messaging.
 *
 * CRITICAL: No network calls. All messages stored locally in Room.
 * Key exchange happens via QR code or NFC — never over the network.
 * Double Ratchet sessions are managed per contact.
 */
@Singleton
class MessengerEngine @Inject constructor(
    private val keyManager: X25519KeyManager
) {

    data class Contact(
        val id: String,
        val displayName: String,
        val identityKey: ByteArray,
        val isVerified: Boolean,
        val safetyNumber: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Contact) return false
            return id == other.id
        }
        override fun hashCode(): Int = id.hashCode()
    }

    data class Message(
        val id: String,
        val contactId: String,
        val text: String,
        val timestamp: Long,
        val isOutgoing: Boolean,
        val isRead: Boolean
    )

    /**
     * Generate our public key bundle for QR/NFC exchange.
     *
     * @param identityKeyPair Ed25519 identity key pair from key storage
     */
    fun createMyBundle(
        displayName: String,
        identityKeyPair: Pair<ByteArray, ByteArray>
    ): PublicKeyBundle {
        val dhKp = keyManager.generateKeyPair()
        return keyManager.createPublicBundle(identityKeyPair, dhKp, displayName)
    }

    /**
     * Verify a received contact's key bundle.
     */
    fun verifyContactBundle(bundle: PublicKeyBundle): Boolean {
        return keyManager.verifyBundle(bundle)
    }

    /**
     * Compute a safety number for visual verification.
     * Format: 12 groups of 5 digits (like Signal).
     */
    fun computeSafetyNumber(myIdentityKey: ByteArray, theirIdentityKey: ByteArray): String {
        val combined = if (myIdentityKey.contentHashCode() < theirIdentityKey.contentHashCode()) {
            myIdentityKey + theirIdentityKey
        } else {
            theirIdentityKey + myIdentityKey
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(combined)
        return digest.take(30).chunked(5).joinToString(" ") { chunk ->
            chunk.joinToString("") { "%02d".format(it.toInt() and 0xFF % 100) }
        }
    }
}
