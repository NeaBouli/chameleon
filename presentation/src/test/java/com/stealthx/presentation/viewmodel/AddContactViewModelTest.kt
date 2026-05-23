/*
 * Chameleon — AddContactViewModel Unit Tests
 * Security: fail-closed QR import validation
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import android.content.Context
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.exchange.ContactExchangeManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AddContactViewModel — fail-closed QR import security")
class AddContactViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: ContactKeyDao
    private lateinit var exchangeManager: ContactExchangeManager
    private lateinit var context: Context
    private lateinit var vm: AddContactViewModel

    private val validX25519   = ByteArray(32) { (it + 1).toByte() }
    private val validEd25519  = ByteArray(32) { (it + 10).toByte() }
    private val validSig      = ByteArray(64) { (it + 5).toByte() }
    private val validSxId     = "sx_aB3dE7gH9"
    private val validCreatedAt = 1716249600000L

    private fun b64url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun validUri(
        sxId: String = validSxId,
        x: ByteArray = validX25519,
        e: ByteArray = validEd25519,
        s: ByteArray = validSig,
        c: Long = validCreatedAt
    ) = "stealthx://add/$sxId?x=${b64url(x)}&e=${b64url(e)}&s=${b64url(s)}&c=$c"

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = mockk(relaxed = true)
        exchangeManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        coEvery { dao.getById(any()) } returns null
        vm = AddContactViewModel(context, dao, exchangeManager)
        mockkObject(ChameleonCrypto)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(ChameleonCrypto)
        Dispatchers.resetMain()
    }

    // ── FIX 1: sx_ ID validation ──────────────────────────────────────────────

    @Test
    @DisplayName("rejects URI that does not start with stealthx://add/")
    fun `rejects non-stealthx uri`() = runTest {
        vm.addFromQrContent("https://example.com/malicious")
        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.contactAdded)
    }

    @Test
    @DisplayName("rejects malformed sx_ ID — too short")
    fun `rejects malformed sx_id too short`() = runTest {
        vm.addFromQrContent(validUri(sxId = "sx_abc"))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.contactAdded)
        assertNotNull(state.errorMessage)
    }

    @Test
    @DisplayName("rejects sx_ ID with forbidden Base58 characters")
    fun `rejects sx_id with forbidden chars`() = runTest {
        vm.addFromQrContent(validUri(sxId = "sx_000000000"))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.contactAdded)
        assertNotNull(state.errorMessage)
    }

    // ── FIX 3: missing / wrong-length parameters ──────────────────────────────

    @Test
    @DisplayName("rejects when x parameter is missing")
    fun `rejects missing x param`() = runTest {
        val uri = "stealthx://add/$validSxId?e=${b64url(validEd25519)}&s=${b64url(validSig)}&c=$validCreatedAt"
        vm.addFromQrContent(uri)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.contactAdded)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    @DisplayName("rejects X25519 key with wrong length")
    fun `rejects x25519 wrong length`() = runTest {
        vm.addFromQrContent(validUri(x = ByteArray(16)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.contactAdded)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("X25519"), "Expected X25519 in: ${state.errorMessage}")
    }

    @Test
    @DisplayName("rejects Ed25519 key with wrong length")
    fun `rejects ed25519 wrong length`() = runTest {
        vm.addFromQrContent(validUri(e = ByteArray(16)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.contactAdded)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Ed25519"), "Expected Ed25519 in: ${state.errorMessage}")
    }

    @Test
    @DisplayName("rejects signature with wrong length")
    fun `rejects signature wrong length`() = runTest {
        vm.addFromQrContent(validUri(s = ByteArray(32)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.contactAdded)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("signature"), "Expected 'signature' in: ${state.errorMessage}")
    }

    // ── FIX 2: fail-closed on invalid signature ───────────────────────────────

    @Test
    @DisplayName("rejects bundle with invalid signature — not saved to DB")
    fun `rejects invalid signature — not saved`() = runTest {
        coEvery { ChameleonCrypto.verify(any(), any(), any()) } returns false

        vm.addFromQrContent(validUri())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.contactAdded)
        assertNotNull(state.errorMessage)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    @DisplayName("rejects bundle when crypto throws — not saved to DB")
    fun `rejects bundle when crypto throws`() = runTest {
        coEvery { ChameleonCrypto.verify(any(), any(), any()) } throws RuntimeException("Sodium not init")

        vm.addFromQrContent(validUri())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.contactAdded)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saves contact when signature is valid")
    fun `saves contact on valid bundle`() = runTest {
        coEvery { ChameleonCrypto.verify(any(), any(), any()) } returns true

        vm.addFromQrContent(validUri())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.contactAdded)
        assertNull(state.errorMessage)
        coVerify(exactly = 1) {
            dao.upsert(match { it.id == validSxId && it.isVerified })
        }
    }

    @Test
    @DisplayName("duplicate contact is rejected")
    fun `duplicate contact rejected`() = runTest {
        coEvery { ChameleonCrypto.verify(any(), any(), any()) } returns true
        coEvery { dao.getById(validSxId) } returns ContactKeyEntity(
            id = validSxId, displayName = validSxId,
            identityKey = validEd25519, dhPublicKey = validX25519,
            signature = validSig, isVerified = true,
            createdAt = validCreatedAt, lastUsedAt = null
        )

        vm.addFromQrContent(validUri())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.contactAdded)
        assertNotNull(vm.uiState.value.errorMessage)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }
}
