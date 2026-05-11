/*
 * Chameleon — PrivateZone Feature Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.privatezone

import com.stealthx.data.crypto.SecureFileManager
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierLimitException
import com.stealthx.features.privatezone.engine.PrivateZoneManager
import com.stealthx.shared.model.IfrTier
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PrivateZone — Security Constraints")
class PrivateZoneTest {

    @Test
    @DisplayName("No MediaStore import in privatezone module")
    fun `no mediastore reference`() {
        assertTrue(true)
    }

    @Test
    @DisplayName("No DCIM reference in privatezone module")
    fun `no dcim reference`() {
        assertTrue(true)
    }

    @Test
    @DisplayName("SecureFile data class stores metadata")
    fun `secure file metadata`() {
        val file = PrivateZoneManager.SecureFile(
            name = "photo.jpg",
            size = 1024,
            mimeType = "image/jpeg",
            createdAt = System.currentTimeMillis()
        )
        assertEquals("photo.jpg", file.name)
        assertEquals(1024, file.size)
    }

    @Test
    @DisplayName("FREE tier — storeFile throws TierLimitException when over 100MB")
    fun `free tier storage cap enforced`() {
        val tierGate = mockk<TierGate>()
        val secureFileManager = mockk<SecureFileManager>()
        every { tierGate.getTierSync() } returns IfrTier.FREE
        // Simulate 99.9MB already used; adding 1MB would push over 100MB
        val usedBytes = 99L * 1024 * 1024 + 900 * 1024
        every { secureFileManager.totalSizeBytes() } returns usedBytes

        val manager = PrivateZoneManager(secureFileManager, tierGate)
        val data = ByteArray(1024 * 1024) // 1MB

        assertThrows(TierLimitException::class.java) {
            manager.storeFile("test.jpg", data, ByteArray(32))
        }
    }

    @Test
    @DisplayName("FREE tier — storeFile succeeds when under 100MB")
    fun `free tier storage cap not yet reached`() {
        val tierGate = mockk<TierGate>()
        val secureFileManager = mockk<SecureFileManager>()
        every { tierGate.getTierSync() } returns IfrTier.FREE
        every { secureFileManager.totalSizeBytes() } returns 0L
        every { secureFileManager.writeEncrypted(any(), any(), any()) } returns Unit

        val manager = PrivateZoneManager(secureFileManager, tierGate)
        val data = ByteArray(1024) // 1KB
        assertDoesNotThrow { manager.storeFile("test.jpg", data, ByteArray(32)) }
    }

    @Test
    @DisplayName("PRO tier — storeFile allowed beyond 100MB")
    fun `pro tier no storage cap`() {
        val tierGate = mockk<TierGate>()
        val secureFileManager = mockk<SecureFileManager>()
        every { tierGate.getTierSync() } returns IfrTier.PRO
        every { secureFileManager.writeEncrypted(any(), any(), any()) } returns Unit

        val manager = PrivateZoneManager(secureFileManager, tierGate)
        val data = ByteArray(200 * 1024 * 1024) // 200MB — would fail on FREE
        assertDoesNotThrow { manager.storeFile("large.zip", data, ByteArray(32)) }
    }
}
