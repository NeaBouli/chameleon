package com.stealthx.data.identity

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

class IdentityStateTest {
    private val edPublic = ByteArray(32) { (it + 1).toByte() }
    private val edPrivate = ByteArray(64) { (it + 2).toByte() }
    private val xPublic = ByteArray(32) { (it + 3).toByte() }
    private val xPrivate = ByteArray(32) { (it + 4).toByte() }
    private val rawId = StealthXIdentity.idForPublicKey(edPublic)

    private fun b64(value: ByteArray) = Base64.getEncoder().encodeToString(value)

    private fun snapshot(
        raw: String? = rawId,
        publicHex: String? = edPublic.joinToString("") { "%02x".format(it) },
        createdAt: Long = 1234L,
        edPub: String? = b64(edPublic),
        edPriv: String? = b64(edPrivate),
        xPub: String? = b64(xPublic),
        xPriv: String? = b64(xPrivate),
        version: Int = IdentityStateClassifier.CURRENT_SCHEMA_VERSION
    ) = IdentitySnapshot(
        rawId = raw,
        publicKeyHex = publicHex,
        customHandle = null,
        createdAt = createdAt,
        ed25519Public = edPub,
        ed25519Private = edPriv,
        x25519Public = xPub,
        x25519Private = xPriv,
        schemaVersion = version
    )

    private fun classify(
        snapshot: IdentitySnapshot,
        edPairIsValid: Boolean = true,
        xPairIsValid: Boolean = true
    ) = IdentityStateClassifier.classify(
        snapshot,
        { _, _ -> edPairIsValid },
        { _, _ -> xPairIsValid }
    )

    private fun assertReason(reason: IdentityIntegrityReason, block: () -> Unit) {
        val error = assertThrows(IdentityIntegrityException::class.java, block)
        assertEquals(reason, error.reason)
    }

    @Test
    fun `empty storage is classified as fresh`() {
        val plan = classify(
            IdentitySnapshot(null, null, null, 0L, null, null, null, null, 0)
        )
        assertEquals(IdentityPlan.Fresh, plan)
    }

    @Test
    fun `healthy identity remains unchanged across restart`() {
        repeat(2) {
            val plan = classify(snapshot()) as IdentityPlan.Existing
            assertFalse(plan.deriveMissingIdentity)
            assertFalse(plan.repairPublicMetadata)
            assertFalse(plan.generateX25519)
            assertFalse(plan.repairCreatedAt)
            assertFalse(plan.updateSchemaVersion)
            assertArrayEquals(edPublic, plan.ed25519Public)
        }
    }

    @Test
    fun `missing x25519 pair is repaired without replacing ed25519`() {
        val plan = classify(snapshot(xPub = null, xPriv = null)) as IdentityPlan.Existing
        assertTrue(plan.generateX25519)
        assertArrayEquals(edPublic, plan.ed25519Public)
        assertArrayEquals(edPrivate, plan.ed25519Private)
    }

    @Test
    fun `missing raw id and metadata are derived from valid ed25519 pair`() {
        val plan = classify(snapshot(raw = null, publicHex = null)) as IdentityPlan.Existing
        assertTrue(plan.deriveMissingIdentity)
        assertTrue(plan.repairPublicMetadata)
        assertEquals(rawId, plan.rawId)
    }

    @Test
    fun `missing public metadata and timestamp are safely repairable`() {
        val plan = classify(snapshot(publicHex = null, createdAt = 0L)) as IdentityPlan.Existing
        assertTrue(plan.repairPublicMetadata)
        assertTrue(plan.repairCreatedAt)
    }

    @Test
    fun `schema version upgrade is metadata-only`() {
        val plan = classify(snapshot(version = 0)) as IdentityPlan.Existing
        assertTrue(plan.updateSchemaVersion)
        assertFalse(plan.generateX25519)
    }

    @Test
    fun `raw id mismatch fails closed`() {
        lateinit var decodedPrivateKey: ByteArray
        assertReason(IdentityIntegrityReason.IDENTITY_KEY_MISMATCH) {
            IdentityStateClassifier.classify(
                snapshot(raw = "sx_123456789"),
                { _, privateKey -> decodedPrivateKey = privateKey; true },
                { _, _ -> true }
            )
        }
        assertTrue(decodedPrivateKey.all { it == 0.toByte() })
    }

    @Test
    fun `public key metadata mismatch fails closed`() {
        assertReason(IdentityIntegrityReason.PUBLIC_KEY_MISMATCH) {
            classify(snapshot(publicHex = "00".repeat(32)))
        }
    }

    @Test
    fun `invalid ed25519 pair fails closed`() {
        assertReason(IdentityIntegrityReason.INVALID_ED25519_KEYPAIR) {
            classify(snapshot(), edPairIsValid = false)
        }
    }

    @Test
    fun `partial ed25519 pair fails closed`() {
        assertReason(IdentityIntegrityReason.PARTIAL_ED25519_KEYPAIR) {
            classify(snapshot(edPriv = null))
        }
    }

    @Test
    fun `partial x25519 pair fails closed`() {
        assertReason(IdentityIntegrityReason.PARTIAL_X25519_KEYPAIR) {
            classify(snapshot(xPriv = null))
        }
    }

    @Test
    fun `invalid x25519 key length fails closed`() {
        assertReason(IdentityIntegrityReason.INVALID_X25519_KEYPAIR) {
            classify(snapshot(xPub = b64(ByteArray(31))))
        }
    }

    @Test
    fun `mismatched x25519 keypair fails closed`() {
        lateinit var decodedXPrivateKey: ByteArray
        assertReason(IdentityIntegrityReason.INVALID_X25519_KEYPAIR) {
            IdentityStateClassifier.classify(
                snapshot(),
                { _, _ -> true },
                { _, privateKey -> decodedXPrivateKey = privateKey; false }
            )
        }
        assertTrue(decodedXPrivateKey.all { it == 0.toByte() })
    }

    @Test
    fun `malformed base64 fails closed`() {
        assertReason(IdentityIntegrityReason.MALFORMED_KEY_ENCODING) {
            classify(snapshot(edPub = "not-base64!"))
        }
    }

    @Test
    fun `failed atomic commit is reported without a key-specific message`() {
        assertReason(IdentityIntegrityReason.PERSISTENCE_FAILED) {
            IdentityStateClassifier.requireCommit(false)
        }
    }
}
