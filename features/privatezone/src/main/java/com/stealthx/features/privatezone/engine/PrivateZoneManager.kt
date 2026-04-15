/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.privatezone.engine

import com.stealthx.data.crypto.SecureFileManager
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
 */
@Singleton
class PrivateZoneManager @Inject constructor(
    private val secureFileManager: SecureFileManager
) {

    data class SecureFile(
        val name: String,
        val size: Long,
        val mimeType: String,
        val createdAt: Long
    )

    fun storeFile(name: String, data: ByteArray, key: ByteArray) {
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
}
