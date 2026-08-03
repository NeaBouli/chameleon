package com.stealthx.data.identity

import android.content.SharedPreferences
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.crypto.SodiumInitializer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Base64

class IdentityRecoveryTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setupCrypto() {
            SodiumInitializer.ensureInit()
        }
    }

    private val encoder = Base64.getEncoder()

    @Test
    fun `legacy mismatched identity is cleared only after reason matches`() {
        val (edPublic, edPrivate) = ChameleonCrypto.generateSigningKeyPair()
        val (xPublic, xPrivate) = ChameleonCrypto.generateX25519KeyPair()
        val editor = mockk<SharedPreferences.Editor>()
        val prefs = identityPrefs(
            rawId = "sx_legacy01",
            edPublic = edPublic,
            edPrivate = edPrivate,
            xPublic = xPublic,
            xPrivate = xPrivate,
            editor = editor
        )

        every { editor.clear() } returns editor
        every { editor.commit() } returns true

        StealthXIdentity.resetIrrecoverable(
            prefs,
            IdentityIntegrityReason.IDENTITY_KEY_MISMATCH
        )

        verify(exactly = 1) { editor.clear() }
        verify(exactly = 1) { editor.commit() }
        ChameleonCrypto.wipeBytes(edPrivate)
        ChameleonCrypto.wipeBytes(xPrivate)
    }

    @Test
    fun `healthy identity cannot be reset`() {
        val (edPublic, edPrivate) = ChameleonCrypto.generateSigningKeyPair()
        val (xPublic, xPrivate) = ChameleonCrypto.generateX25519KeyPair()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val prefs = identityPrefs(
            rawId = StealthXIdentity.idForPublicKey(edPublic),
            edPublic = edPublic,
            edPrivate = edPrivate,
            xPublic = xPublic,
            xPrivate = xPrivate,
            editor = editor
        )

        assertThrows(IllegalStateException::class.java) {
            StealthXIdentity.resetIrrecoverable(
                prefs,
                IdentityIntegrityReason.IDENTITY_KEY_MISMATCH
            )
        }
        verify(exactly = 0) { editor.clear() }
        ChameleonCrypto.wipeBytes(edPrivate)
        ChameleonCrypto.wipeBytes(xPrivate)
    }

    @Test
    fun `legacy identity cannot be reset under a different failure reason`() {
        val (edPublic, edPrivate) = ChameleonCrypto.generateSigningKeyPair()
        val (xPublic, xPrivate) = ChameleonCrypto.generateX25519KeyPair()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val prefs = identityPrefs(
            rawId = "sx_legacy02",
            edPublic = edPublic,
            edPrivate = edPrivate,
            xPublic = xPublic,
            xPrivate = xPrivate,
            editor = editor
        )

        assertThrows(IllegalStateException::class.java) {
            StealthXIdentity.resetIrrecoverable(
                prefs,
                IdentityIntegrityReason.PUBLIC_KEY_MISMATCH
            )
        }
        verify(exactly = 0) { editor.clear() }
        ChameleonCrypto.wipeBytes(edPrivate)
        ChameleonCrypto.wipeBytes(xPrivate)
    }

    @Test
    fun `storage persistence failure is never user resettable`() {
        assertFalse(IdentityIntegrityReason.PERSISTENCE_FAILED.isUserRecoverable)
        IdentityIntegrityReason.entries
            .filterNot { it == IdentityIntegrityReason.PERSISTENCE_FAILED }
            .forEach { assertTrue(it.isUserRecoverable, it.name) }
    }

    private fun identityPrefs(
        rawId: String,
        edPublic: ByteArray,
        edPrivate: ByteArray,
        xPublic: ByteArray,
        xPrivate: ByteArray,
        editor: SharedPreferences.Editor
    ): SharedPreferences {
        val values = mapOf(
            "raw_id" to rawId,
            "public_key" to edPublic.toHex(),
            "ed25519_public" to encoder.encodeToString(edPublic),
            "ed25519_private" to encoder.encodeToString(edPrivate),
            "x25519_public" to encoder.encodeToString(xPublic),
            "x25519_private" to encoder.encodeToString(xPrivate)
        )
        return mockk {
            every { getString(any(), any()) } answers {
                values[firstArg()] ?: secondArg()
            }
            every { getLong("created_at", any()) } returns 1234L
            every { getInt("identity_schema_version", any()) } returns
                IdentityStateClassifier.CURRENT_SCHEMA_VERSION
            every { edit() } returns editor
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
