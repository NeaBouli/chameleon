/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.privatezone.engine

import com.stealthx.data.crypto.SecureFileManager
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierLimitException
import com.stealthx.shared.model.AccessTier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Private Zone Manager — encrypted file storage.
 *
 * CRITICAL SECURITY:
 * - All files encrypted via SecureFileManager (XChaCha20-Poly1305)
 * - File names are SHA-256 hashed on disk
 * - NEVER writes to MediaStore or /DCIM/
 * - NEVER stores unencrypted data on disk
 * - Photos from SecureCamera go directly to encrypted storage
 *
 * TIER LIMITS:
 * - FREE: 100MB total storage cap
 * - PRO / ELITE: unlimited
 */
@Singleton
class PrivateZoneManager @Inject constructor(
    private val secureFileManager: SecureFileManager,
    private val tierGate: TierGate
) {
    companion object {
        private const val FREE_STORAGE_CAP_BYTES = 100L * 1024 * 1024 // 100 MB
    }

    data class SecureFile(
        val name: String,
        val size: Long,
        val mimeType: String,
        val createdAt: Long
    )

    /**
     * Store an encrypted file. Throws [TierLimitException] if FREE tier storage cap exceeded.
     *
     * Synchronized to prevent TOCTOU race: two concurrent writes both seeing the same used-bytes
     * and both passing the check, causing them to jointly exceed the 100MB cap.
     *
     * Check uses on-disk size units throughout:
     * - [totalSizeBytes] measures on-disk encrypted files
     * - [estimatedEncryptedSizeBytes] estimates the on-disk footprint of [data]
     * - Existing file size is subtracted to avoid double-counting on overwrite
     */
    @Synchronized
    fun storeFile(name: String, data: ByteArray, key: ByteArray) {
        if (tierGate.getTierSync() < AccessTier.PRO) {
            val existing = secureFileManager.existingFileSizeBytes(name)
            val used = secureFileManager.totalSizeBytes() - existing
            val incoming = secureFileManager.estimatedEncryptedSizeBytes(data.size)
            if (used + incoming > FREE_STORAGE_CAP_BYTES) {
                val usedMb = used / (1024 * 1024)
                throw TierLimitException(
                    "Private Zone storage limit reached (${usedMb}MB / 100MB). Buy Pro Lifetime for unlimited storage."
                )
            }
        }
        secureFileManager.writeEncrypted(name, data, key)
    }

    fun retrieveFile(name: String, key: ByteArray): ByteArray {
        return secureFileManager.readEncrypted(name, key)
    }

    fun deleteFile(name: String): Boolean {
        return secureFileManager.delete(name)
    }

    fun fileExists(name: String): Boolean {
        return secureFileManager.exists(name)
    }

    fun listFiles(): List<String> {
        return secureFileManager.listFiles()
    }

    fun totalSizeBytes(): Long = secureFileManager.totalSizeBytes()
}
