package com.stealthx.data.identity

import java.util.Base64
import java.util.Arrays

enum class IdentityIntegrityReason {
    PARTIAL_ED25519_KEYPAIR,
    INVALID_ED25519_KEYPAIR,
    MALFORMED_KEY_ENCODING,
    IDENTITY_KEY_MISMATCH,
    PUBLIC_KEY_MISMATCH,
    PARTIAL_X25519_KEYPAIR,
    INVALID_X25519_KEYPAIR,
    PERSISTENCE_FAILED
}

class IdentityIntegrityException(
    val reason: IdentityIntegrityReason
) : SecurityException("Identity integrity check failed (${reason.name})")

internal data class IdentitySnapshot(
    val rawId: String?,
    val publicKeyHex: String?,
    val customHandle: String?,
    val createdAt: Long,
    val ed25519Public: String?,
    val ed25519Private: String?,
    val x25519Public: String?,
    val x25519Private: String?,
    val schemaVersion: Int
)

internal sealed interface IdentityPlan {
    data object Fresh : IdentityPlan

    data class Existing(
        val rawId: String,
        val canonicalPublicKeyHex: String,
        val customHandle: String?,
        val createdAt: Long,
        val ed25519Public: ByteArray,
        val ed25519Private: ByteArray,
        val x25519Public: ByteArray?,
        val x25519Private: ByteArray?,
        val repairPublicMetadata: Boolean,
        val deriveMissingIdentity: Boolean,
        val generateX25519: Boolean,
        val repairCreatedAt: Boolean,
        val updateSchemaVersion: Boolean
    ) : IdentityPlan
}

internal object IdentityStateClassifier {
    const val CURRENT_SCHEMA_VERSION = 2

    fun classify(
        snapshot: IdentitySnapshot,
        verifyEd25519Pair: (publicKey: ByteArray, privateKey: ByteArray) -> Boolean,
        verifyX25519Pair: (publicKey: ByteArray, privateKey: ByteArray) -> Boolean
    ): IdentityPlan {
        val hasRawId = !snapshot.rawId.isNullOrBlank()
        val hasPublicMetadata = !snapshot.publicKeyHex.isNullOrBlank()
        val hasEdPublic = snapshot.ed25519Public != null
        val hasEdPrivate = snapshot.ed25519Private != null
        val hasXPublic = snapshot.x25519Public != null
        val hasXPrivate = snapshot.x25519Private != null

        if (!hasRawId && !hasPublicMetadata && !hasEdPublic && !hasEdPrivate &&
            !hasXPublic && !hasXPrivate
        ) {
            return IdentityPlan.Fresh
        }

        if (hasEdPublic != hasEdPrivate || !hasEdPublic) {
            throw IdentityIntegrityException(IdentityIntegrityReason.PARTIAL_ED25519_KEYPAIR)
        }

        val edPublic = decode(snapshot.ed25519Public!!)
        val edPrivate = decode(snapshot.ed25519Private!!)
        val edPairIsValid = edPublic.size == 32 && edPrivate.size == 64 &&
            runCatching { verifyEd25519Pair(edPublic, edPrivate) }.getOrDefault(false)
        if (!edPairIsValid) {
            fail(IdentityIntegrityReason.INVALID_ED25519_KEYPAIR, edPrivate)
        }

        val canonicalPublicKeyHex = edPublic.toHex()
        val derivedRawId = "sx_${StealthXIdentity.deriveShortId(canonicalPublicKeyHex)}"
        val rawId = snapshot.rawId?.takeIf { it.isNotBlank() } ?: derivedRawId
        if (rawId != derivedRawId) {
            fail(IdentityIntegrityReason.IDENTITY_KEY_MISMATCH, edPrivate)
        }

        val repairPublicMetadata = !hasPublicMetadata
        if (hasPublicMetadata && !snapshot.publicKeyHex.equals(canonicalPublicKeyHex, ignoreCase = true)) {
            fail(IdentityIntegrityReason.PUBLIC_KEY_MISMATCH, edPrivate)
        }

        if (hasXPublic != hasXPrivate) {
            fail(IdentityIntegrityReason.PARTIAL_X25519_KEYPAIR, edPrivate)
        }

        val xPublic: ByteArray?
        val xPrivate: ByteArray?
        try {
            xPublic = snapshot.x25519Public?.let(::decode)
            xPrivate = snapshot.x25519Private?.let(::decode)
        } catch (error: IdentityIntegrityException) {
            Arrays.fill(edPrivate, 0.toByte())
            throw error
        }
        val xPairIsValid = xPublic == null || (
            xPrivate != null && xPublic.size == 32 && xPrivate.size == 32 &&
                runCatching { verifyX25519Pair(xPublic, xPrivate) }.getOrDefault(false)
            )
        if (!xPairIsValid) {
            xPrivate?.let { Arrays.fill(it, 0.toByte()) }
            fail(IdentityIntegrityReason.INVALID_X25519_KEYPAIR, edPrivate)
        }

        return IdentityPlan.Existing(
            rawId = rawId,
            canonicalPublicKeyHex = canonicalPublicKeyHex,
            customHandle = snapshot.customHandle,
            createdAt = snapshot.createdAt,
            ed25519Public = edPublic,
            ed25519Private = edPrivate,
            x25519Public = xPublic,
            x25519Private = xPrivate,
            repairPublicMetadata = repairPublicMetadata,
            deriveMissingIdentity = !hasRawId,
            generateX25519 = xPublic == null,
            repairCreatedAt = snapshot.createdAt <= 0L,
            updateSchemaVersion = snapshot.schemaVersion != CURRENT_SCHEMA_VERSION
        )
    }

    fun requireCommit(success: Boolean) {
        if (!success) {
            throw IdentityIntegrityException(IdentityIntegrityReason.PERSISTENCE_FAILED)
        }
    }

    private fun decode(value: String): ByteArray = try {
        Base64.getDecoder().decode(value)
    } catch (_: IllegalArgumentException) {
        throw IdentityIntegrityException(IdentityIntegrityReason.MALFORMED_KEY_ENCODING)
    }

    private fun fail(reason: IdentityIntegrityReason, vararg secrets: ByteArray): Nothing {
        secrets.forEach { Arrays.fill(it, 0.toByte()) }
        throw IdentityIntegrityException(reason)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
