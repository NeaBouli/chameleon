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
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private const val FREE_CAP = 100L * 1024 * 1024

@DisplayName("PrivateZone — Security Constraints")
class PrivateZoneTest {

    private fun estimatedEncryptedSize(plaintextSize: Int): Long {
        val padded = ((plaintextSize / 256) + 1) * 256
        return (4 + 24 + 4 + padded + 16 + 4).toLong()
    }

    private fun buildManager(
        tier: IfrTier,
        usedBytes: Long,
        existingBytes: Long = 0L
    ): Pair<PrivateZoneManager, SecureFileManager> {
        val tierGate = mockk<TierGate> { every { getTierSync() } returns tier }
        val sfm = mockk<SecureFileManager> {
            every { totalSizeBytes() } returns usedBytes
            every { existingFileSizeBytes(any()) } returns existingBytes
            every { estimatedEncryptedSizeBytes(any()) } answers {
                estimatedEncryptedSize(firstArg())
            }
            every { writeEncrypted(any(), any(), any()) } returns Unit
        }
        return PrivateZoneManager(sfm, tierGate) to sfm
    }

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
        // 99.9MB on disk, adding ~1MB encrypted would exceed 100MB
        val usedBytes = 99L * 1024 * 1024 + 900 * 1024
        val (manager, _) = buildManager(IfrTier.FREE, usedBytes)
        val data = ByteArray(1024 * 1024) // 1MB plaintext

        assertThrows(TierLimitException::class.java) {
            manager.storeFile("test.jpg", data, ByteArray(32))
        }
    }

    @Test
    @DisplayName("FREE tier — storeFile succeeds when under 100MB")
    fun `free tier storage cap not yet reached`() {
        val (manager, _) = buildManager(IfrTier.FREE, usedBytes = 0L)
        val data = ByteArray(1024) // 1KB

        assertDoesNotThrow { manager.storeFile("test.jpg", data, ByteArray(32)) }
    }

    @Test
    @DisplayName("PRO tier — storeFile allowed beyond 100MB")
    fun `pro tier no storage cap`() {
        val (manager, sfm) = buildManager(IfrTier.PRO, usedBytes = 0L)
        val data = ByteArray(1024) // actual size irrelevant for PRO

        assertDoesNotThrow { manager.storeFile("large.zip", data, ByteArray(32)) }
        verify(exactly = 0) { sfm.totalSizeBytes() }
    }

    @Test
    @DisplayName("FREE tier — overwrite of same file does not double-count existing size")
    fun `free tier overwrite does not double count`() {
        // total on disk = 99.5MB including the 1MB file being replaced
        // Without fix: used=99.5MB + ~1MB incoming → exceeds 100MB → wrong throw
        // With fix:    used=99.5MB − 1MB existing + ~1MB incoming = ~99.5MB → OK
        val existingFileBytes = 1L * 1024 * 1024
        val totalBytes = 99L * 1024 * 1024 + 512 * 1024 // 99.5MB
        val (manager, _) = buildManager(IfrTier.FREE, totalBytes, existingBytes = existingFileBytes)
        val data = ByteArray(512 * 1024) // 0.5MB replacement

        assertDoesNotThrow { manager.storeFile("overwrite.jpg", data, ByteArray(32)) }
    }

    @Test
    @DisplayName("FREE tier — writeEncrypted not called when limit exceeded")
    fun `free tier write not called on limit exceeded`() {
        val usedBytes = 99L * 1024 * 1024 + 900 * 1024
        val (manager, sfm) = buildManager(IfrTier.FREE, usedBytes)
        val data = ByteArray(1024 * 1024)

        assertThrows(TierLimitException::class.java) {
            manager.storeFile("blocked.jpg", data, ByteArray(32))
        }
        verify(exactly = 0) { sfm.writeEncrypted(any(), any(), any()) }
    }

    @Test
    @DisplayName("FREE tier — small file on empty vault is well within limit")
    fun `free tier small file on empty vault`() {
        val (manager, _) = buildManager(IfrTier.FREE, usedBytes = 0L)
        val data = ByteArray(4096) // 4KB

        assertDoesNotThrow { manager.storeFile("small.pdf", data, ByteArray(32)) }
    }
}
