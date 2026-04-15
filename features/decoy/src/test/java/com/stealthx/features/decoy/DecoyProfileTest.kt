/*
 * Chameleon — Decoy Feature Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy

import com.stealthx.features.decoy.engine.DecoyProfileEngine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DecoyProfileEngine")
class DecoyProfileTest {

    private val engine = DecoyProfileEngine()

    @Test
    @DisplayName("Default mode is REAL")
    fun `default is real mode`() {
        assertEquals(DecoyProfileEngine.ProfileMode.REAL, engine.getCurrentMode())
        assertFalse(engine.isDecoyMode())
    }

    @Test
    @DisplayName("Disabled config always returns REAL")
    fun `disabled config returns real`() {
        val config = DecoyProfileEngine.DecoyConfig(isEnabled = false)
        val result = engine.authenticatePin("1234", config)
        assertEquals(DecoyProfileEngine.ProfileMode.REAL, result)
    }

    @Test
    @DisplayName("Matching decoy PIN returns DECOY mode")
    fun `decoy pin activates decoy`() {
        val decoyHash = engine.hashPin("9999")
        val realHash = engine.hashPin("1234")
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled = true,
            decoyPinHash = decoyHash,
            realPinHash = realHash
        )

        val result = engine.authenticatePin("9999", config)
        assertEquals(DecoyProfileEngine.ProfileMode.DECOY, result)
        assertTrue(engine.isDecoyMode())
    }

    @Test
    @DisplayName("Matching real PIN returns REAL mode")
    fun `real pin activates real`() {
        val decoyHash = engine.hashPin("9999")
        val realHash = engine.hashPin("1234")
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled = true,
            decoyPinHash = decoyHash,
            realPinHash = realHash
        )

        val result = engine.authenticatePin("1234", config)
        assertEquals(DecoyProfileEngine.ProfileMode.REAL, result)
    }

    @Test
    @DisplayName("Wrong PIN throws SecurityException")
    fun `wrong pin throws`() {
        val decoyHash = engine.hashPin("9999")
        val realHash = engine.hashPin("1234")
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled = true,
            decoyPinHash = decoyHash,
            realPinHash = realHash
        )

        assertThrows(SecurityException::class.java) {
            engine.authenticatePin("0000", config)
        }
    }

    @Test
    @DisplayName("PIN hash is deterministic")
    fun `pin hash deterministic`() {
        val h1 = engine.hashPin("test123")
        val h2 = engine.hashPin("test123")
        assertArrayEquals(h1, h2)
    }

    @Test
    @DisplayName("Different PINs produce different hashes")
    fun `different pins different hashes`() {
        val h1 = engine.hashPin("1111")
        val h2 = engine.hashPin("2222")
        assertFalse(h1.contentEquals(h2))
    }
}
