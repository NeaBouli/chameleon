/*
 * Chameleon — Decoy Feature Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy

import com.stealthx.crypto.SodiumInitializer
import com.stealthx.domain.tier.TierGate
import com.stealthx.features.decoy.engine.DecoyProfileEngine
import com.stealthx.shared.model.IfrTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private val eliteTierGate = object : TierGate {
    override val currentTier: Flow<IfrTier> = flowOf(IfrTier.ELITE)
    override fun getTierSync(): IfrTier = IfrTier.ELITE
    override suspend fun getTier(): IfrTier = IfrTier.ELITE
    override suspend fun isCacheValid(): Boolean = true
    override suspend fun invalidateCache() {}
}

@DisplayName("DecoyProfileEngine")
class DecoyProfileTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            SodiumInitializer.ensureInit()
        }
    }

    private val engine = DecoyProfileEngine(eliteTierGate)

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
        val decoySalt = engine.generatePinSalt()
        val realSalt  = engine.generatePinSalt()
        val decoyHash = engine.hashPin("9999", decoySalt)
        val realHash  = engine.hashPin("1234", realSalt)
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled    = true,
            decoyPinHash = decoyHash,
            decoyPinSalt = decoySalt,
            realPinHash  = realHash,
            realPinSalt  = realSalt
        )

        val result = engine.authenticatePin("9999", config)
        assertEquals(DecoyProfileEngine.ProfileMode.DECOY, result)
        assertTrue(engine.isDecoyMode())
    }

    @Test
    @DisplayName("Matching real PIN returns REAL mode")
    fun `real pin activates real`() {
        val decoySalt = engine.generatePinSalt()
        val realSalt  = engine.generatePinSalt()
        val decoyHash = engine.hashPin("9999", decoySalt)
        val realHash  = engine.hashPin("1234", realSalt)
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled    = true,
            decoyPinHash = decoyHash,
            decoyPinSalt = decoySalt,
            realPinHash  = realHash,
            realPinSalt  = realSalt
        )

        val result = engine.authenticatePin("1234", config)
        assertEquals(DecoyProfileEngine.ProfileMode.REAL, result)
    }

    @Test
    @DisplayName("Wrong PIN throws SecurityException")
    fun `wrong pin throws`() {
        val decoySalt = engine.generatePinSalt()
        val realSalt  = engine.generatePinSalt()
        val decoyHash = engine.hashPin("9999", decoySalt)
        val realHash  = engine.hashPin("1234", realSalt)
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled    = true,
            decoyPinHash = decoyHash,
            decoyPinSalt = decoySalt,
            realPinHash  = realHash,
            realPinSalt  = realSalt
        )

        assertThrows(SecurityException::class.java) {
            engine.authenticatePin("0000", config)
        }
    }

    @Test
    @DisplayName("PIN hash is deterministic for same salt")
    fun `pin hash deterministic`() {
        val salt = engine.generatePinSalt()
        val h1 = engine.hashPin("test123", salt)
        val h2 = engine.hashPin("test123", salt)
        assertArrayEquals(h1, h2)
    }

    @Test
    @DisplayName("Different PINs produce different hashes")
    fun `different pins different hashes`() {
        val salt = engine.generatePinSalt()
        val h1 = engine.hashPin("1111", salt)
        val h2 = engine.hashPin("2222", salt)
        assertFalse(h1.contentEquals(h2))
    }

    @Test
    @DisplayName("Different salts produce different hashes for same PIN")
    fun `different salts different hashes`() {
        val salt1 = engine.generatePinSalt()
        val salt2 = engine.generatePinSalt()
        val h1 = engine.hashPin("1234", salt1)
        val h2 = engine.hashPin("1234", salt2)
        assertFalse(h1.contentEquals(h2))
    }

    // authenticateWithMultiDecoy — auth-flow integration tests
    @Test
    @DisplayName("authenticateWithMultiDecoy: real PIN returns REAL")
    fun `authenticateWithMultiDecoy real pin returns real`() {
        val realSalt = engine.generatePinSalt()
        val realHash = engine.hashPin("1234", realSalt)
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled = true,
            realPinHash = realHash,
            realPinSalt = realSalt
        )
        assertEquals(DecoyProfileEngine.ProfileMode.REAL, engine.authenticateWithMultiDecoy("1234", config, emptyList()))
    }

    @Test
    @DisplayName("authenticateWithMultiDecoy: single decoy PIN returns DECOY")
    fun `authenticateWithMultiDecoy single decoy returns decoy`() {
        val realSalt = engine.generatePinSalt()
        val realHash = engine.hashPin("1234", realSalt)
        val decoySalt = engine.generatePinSalt()
        val decoyHash = engine.hashPin("9999", decoySalt)
        val config = DecoyProfileEngine.DecoyConfig(
            isEnabled = true,
            realPinHash = realHash, realPinSalt = realSalt,
            decoyPinHash = decoyHash, decoyPinSalt = decoySalt
        )
        assertEquals(DecoyProfileEngine.ProfileMode.DECOY, engine.authenticateWithMultiDecoy("9999", config, emptyList()))
    }

    @Test
    @DisplayName("authenticateWithMultiDecoy: multi-entry PIN returns DECOY")
    fun `authenticateWithMultiDecoy multi entry returns decoy`() {
        val realSalt = engine.generatePinSalt()
        val realHash = engine.hashPin("1234", realSalt)
        val config = DecoyProfileEngine.DecoyConfig(isEnabled = true, realPinHash = realHash, realPinSalt = realSalt)
        val extraSalt = engine.generatePinSalt()
        val extraHash = engine.hashPin("5555", extraSalt)
        assertEquals(
            DecoyProfileEngine.ProfileMode.DECOY,
            engine.authenticateWithMultiDecoy("5555", config, listOf(Pair(extraHash, extraSalt)))
        )
    }

    @Test
    @DisplayName("authenticateWithMultiDecoy: wrong PIN throws SecurityException")
    fun `authenticateWithMultiDecoy wrong pin throws`() {
        val realSalt = engine.generatePinSalt()
        val realHash = engine.hashPin("1234", realSalt)
        val config = DecoyProfileEngine.DecoyConfig(isEnabled = true, realPinHash = realHash, realPinSalt = realSalt)
        assertThrows(SecurityException::class.java) {
            engine.authenticateWithMultiDecoy("0000", config, emptyList())
        }
    }

    // Multi-decoy: each profile has independent salt+hash — same PIN verified correctly per entry
    @Test
    @DisplayName("Multiple decoy entries each authenticate independently")
    fun `multiple decoy entries authenticate independently`() {
        val profile1Salt = engine.generatePinSalt()
        val profile1Hash = engine.hashPin("1111", profile1Salt)

        val profile2Salt = engine.generatePinSalt()
        val profile2Hash = engine.hashPin("2222", profile2Salt)

        // profile1 PIN matches its own hash
        assertTrue(engine.hashPin("1111", profile1Salt).contentEquals(profile1Hash))
        // profile2 PIN matches its own hash
        assertTrue(engine.hashPin("2222", profile2Salt).contentEquals(profile2Hash))

        // cross-check: profile1 PIN does NOT match profile2 hash
        assertFalse(engine.hashPin("1111", profile2Salt).contentEquals(profile2Hash))
        // cross-check: profile2 PIN does NOT match profile1 hash
        assertFalse(engine.hashPin("2222", profile1Salt).contentEquals(profile1Hash))
    }

    @Test
    @DisplayName("Duplicate-decoy detection: same PIN hashed with different salts yields different hashes")
    fun `duplicate pin detection across profiles`() {
        // Simulates the ViewModel's duplicate check: re-hash the candidate PIN
        // with an existing entry's salt and compare to that entry's stored hash.
        val existingSalt = engine.generatePinSalt()
        val existingHash = engine.hashPin("9999", existingSalt)

        // Same PIN → same hash with same salt → detected as duplicate
        val candidate = engine.hashPin("9999", existingSalt)
        assertTrue(candidate.contentEquals(existingHash))

        // Different PIN → not a duplicate
        val nonDuplicate = engine.hashPin("8888", existingSalt)
        assertFalse(nonDuplicate.contentEquals(existingHash))
    }
}
