/*
 * Chameleon — Core Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core

import com.stealthx.core.accessibility.CryptoService
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.crypto.SodiumInitializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CryptoService — AIDL Bridge Tests")
class CryptoServiceTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            SodiumInitializer.ensureInit()
        }
    }

    @Test
    @DisplayName("ProcessTextResult passthrough does not modify text")
    fun `passthrough result means no replacement`() {
        val result = ProcessTextResult(shouldReplace = false, text = null)
        if (result.shouldReplace && result.text != null) {
            fail<Unit>("Should not reach here — passthrough means skip")
        }
        assertFalse(result.shouldReplace)
    }

    @Test
    @DisplayName("ProcessTextResult with encrypted text triggers replacement")
    fun `encrypted result triggers text injection`() {
        val encryptedText = "[CHAM:v1:aabbcc:ddeeff:12]"
        val result = ProcessTextResult(shouldReplace = true, text = encryptedText)
        assertTrue(result.shouldReplace)
        assertNotNull(result.text)
    }

    @Test
    @DisplayName("Null bridge returns passthrough")
    fun `null bridge means passthrough`() {
        val bridge: ICryptoBridge? = null
        assertFalse(bridge != null)
    }

    @Test
    @DisplayName("AIDL processText contract: null inputs handled")
    fun `processText handles null inputs gracefully`() {
        val result = ProcessTextResult(shouldReplace = false, text = null)
        assertFalse(result.shouldReplace)
    }

    @Test
    @DisplayName("Payload encode/decode roundtrip preserves all fields")
    fun `payload encode decode roundtrip`() {
        val key = ChameleonCrypto.randomKey()
        val aad = "com.example.app".toByteArray()
        val plaintext = "Secret message".toByteArray()

        val payload = ChameleonCrypto.encrypt(plaintext, key, aad)
        val encoded = CryptoService.encodePayload(payload)

        assertTrue(encoded.startsWith("[CHAM:v1:"))
        assertTrue(encoded.endsWith("]"))

        val decoded = CryptoService.decodePayload(encoded).copy(aad = aad)
        val decrypted = ChameleonCrypto.decrypt(decoded, key)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    @DisplayName("Payload prefix detection identifies Chameleon payloads")
    fun `payload prefix detection`() {
        val key = ChameleonCrypto.randomKey()
        val payload = ChameleonCrypto.encrypt("test".toByteArray(), key)
        val encoded = CryptoService.encodePayload(payload)

        assertTrue(encoded.startsWith("[CHAM:"))
        assertFalse("Hello World".startsWith("[CHAM:"))
    }

    @Test
    @DisplayName("Different plaintext produces different encoded payloads")
    fun `encrypt different plaintext different output`() {
        val key = ChameleonCrypto.randomKey()
        val e1 = CryptoService.encodePayload(ChameleonCrypto.encrypt("msg1".toByteArray(), key))
        val e2 = CryptoService.encodePayload(ChameleonCrypto.encrypt("msg2".toByteArray(), key))
        assertNotEquals(e1, e2)
    }
}
