/*
 * Chameleon — Overlay Feature Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.overlay

import com.stealthx.features.overlay.engine.OverlayEngine
import com.stealthx.shared.model.EncryptedPayload
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OverlayEngine — Payload Encoding")
class OverlayEngineTest {

    @Test
    @DisplayName("encodePayload produces [CHAM:v1:...] format")
    fun `encode produces correct format`() {
        val payload = EncryptedPayload(
            ciphertext = byteArrayOf(0x01, 0x02, 0x03),
            nonce = byteArrayOf(0xAA.toByte(), 0xBB.toByte()),
            paddedLength = 3,
            aad = ByteArray(0),
            algorithm = "XChaCha20-Poly1305",
            version = 1
        )
        val encoded = OverlayEngine.encodePayload(payload)
        assertTrue(encoded.startsWith("[CHAM:v1:"))
        assertTrue(encoded.endsWith("]"))
    }

    @Test
    @DisplayName("decodePayload round-trips correctly")
    fun `encode decode roundtrip`() {
        val nonce = ByteArray(24) { (it + 1).toByte() }
        val ciphertext = ByteArray(32) { (it + 0x10).toByte() }
        val payload = EncryptedPayload(ciphertext, nonce, 16, ByteArray(0), "XChaCha20-Poly1305", 1)

        val encoded = OverlayEngine.encodePayload(payload)
        val decoded = OverlayEngine.decodePayload(encoded)

        assertArrayEquals(nonce, decoded.nonce)
        assertArrayEquals(ciphertext, decoded.ciphertext)
    }

    @Test
    @DisplayName("Non-CHAM text is not a payload")
    fun `non payload text ignored`() {
        assertFalse("Hello world".startsWith("[CHAM:"))
    }

    @Test
    @DisplayName("Invalid payload format throws")
    fun `invalid format throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            OverlayEngine.decodePayload("[CHAM:v1:bad]")
        }
    }
}
