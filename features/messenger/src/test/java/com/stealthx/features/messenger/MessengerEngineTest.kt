/*
 * Chameleon — Messenger Feature Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger

import com.stealthx.domain.tier.TierGate
import com.stealthx.features.messenger.engine.MessengerEngine
import com.stealthx.domain.keys.X25519KeyManager
import com.stealthx.shared.model.AccessTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private val proTierGate = object : TierGate {
    override val currentTier: Flow<AccessTier> = flowOf(AccessTier.PRO)
    override fun getTierSync(): AccessTier = AccessTier.PRO
    override suspend fun getTier(): AccessTier = AccessTier.PRO
    override suspend fun isCacheValid(): Boolean = true
    override suspend fun invalidateCache() {}
}

@DisplayName("MessengerEngine")
class MessengerEngineTest {

    private val keyManager = X25519KeyManager()

    @Test
    @DisplayName("Safety number is deterministic for same key pair")
    fun `safety number deterministic`() {
        val engine = MessengerEngine(keyManager, proTierGate)
        val keyA = ByteArray(32) { (it + 1).toByte() }
        val keyB = ByteArray(32) { (it + 33).toByte() }

        val sn1 = engine.computeSafetyNumber(keyA, keyB)
        val sn2 = engine.computeSafetyNumber(keyA, keyB)

        assertEquals(sn1, sn2)
    }

    @Test
    @DisplayName("Safety number is symmetric (A,B == B,A)")
    fun `safety number symmetric`() {
        val engine = MessengerEngine(keyManager, proTierGate)
        val keyA = ByteArray(32) { (it + 1).toByte() }
        val keyB = ByteArray(32) { (it + 33).toByte() }

        val snAB = engine.computeSafetyNumber(keyA, keyB)
        val snBA = engine.computeSafetyNumber(keyB, keyA)

        assertEquals(snAB, snBA)
    }

    @Test
    @DisplayName("Different keys produce different safety numbers")
    fun `different keys different safety numbers`() {
        val engine = MessengerEngine(keyManager, proTierGate)
        val keyA = ByteArray(32) { 0x01 }
        val keyB = ByteArray(32) { 0x02 }
        val keyC = ByteArray(32) { 0x03 }

        val snAB = engine.computeSafetyNumber(keyA, keyB)
        val snAC = engine.computeSafetyNumber(keyA, keyC)

        assertNotEquals(snAB, snAC)
    }

    @Test
    @DisplayName("Contact equality by ID")
    fun `contact equality`() {
        val c1 = MessengerEngine.Contact("id1", "Alice", ByteArray(32), true, "123")
        val c2 = MessengerEngine.Contact("id1", "Bob", ByteArray(32), false, "456")
        assertEquals(c1, c2)
    }
}
