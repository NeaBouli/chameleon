/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.privatezone.engine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure Camera — captures photos directly into Private Zone.
 *
 * CRITICAL SECURITY:
 * - Photos are encrypted IMMEDIATELY after capture
 * - NEVER written to MediaStore, /DCIM/, or any public directory
 * - Uses app-private temp file during capture, wiped after encryption
 * - Gallery apps will NOT see these photos
 */
@Singleton
class SecureCamera @Inject constructor(
    private val privateZoneManager: PrivateZoneManager
) {

    /**
     * Store captured photo bytes directly into encrypted storage.
     *
     * @param photoBytes  Raw photo bytes from camera
     * @param fileName    Logical file name (e.g. "photo_20260415_120000.jpg")
     * @param key         Encryption key
     */
    fun storePhoto(photoBytes: ByteArray, fileName: String, key: ByteArray) {
        privateZoneManager.storeFile(fileName, photoBytes, key)
    }

    /**
     * Retrieve and decrypt a photo for viewing.
     */
    fun retrievePhoto(fileName: String, key: ByteArray): ByteArray {
        return privateZoneManager.retrieveFile(fileName, key)
    }
}
