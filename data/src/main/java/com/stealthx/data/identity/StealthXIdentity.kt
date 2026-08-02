/*
 * StealthX Unified Identity System
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.identity

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.stealthx.crypto.ChameleonCrypto
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64

data class StealthXId(
    val raw: String,
    val customHandle: String?,
    val publicKeyHex: String,
    val createdAt: Long
) {
    val displayId: String get() = customHandle ?: raw
    val deepLink: String get() = "stealthx://add/$raw"
    val qrContent: String get() = deepLink
}

object StealthXIdentity {
    private const val PREFS_NAME = "stealthx_identity"
    private const val KEY_RAW_ID = "raw_id"
    private const val KEY_PUBLIC_KEY = "public_key"
    private const val KEY_CUSTOM_HANDLE = "custom_handle"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_ED25519_PUBLIC = "ed25519_public"
    private const val KEY_ED25519_PRIVATE = "ed25519_private"
    private const val KEY_X25519_PUBLIC = "x25519_public"
    private const val KEY_X25519_PRIVATE = "x25519_private"
    private const val KEY_SCHEMA_VERSION = "identity_schema_version"
    private const val ID_PREFIX = "sx_"
    private val lock = Any()
    @Volatile private var persistenceFailed = false

    fun getOrCreateWithSeed(context: Context): StealthXId = synchronized(lock) {
        val resolved = resolve(getEncryptedPrefs(context), createIfMissing = true)
            ?: throw IdentityIntegrityException(IdentityIntegrityReason.PERSISTENCE_FAILED)
        try {
            resolved.toIdentity()
        } finally {
            resolved.wipePrivateKeys()
        }
    }

    fun createQrContent(context: Context): String = synchronized(lock) {
        val resolved = resolve(getEncryptedPrefs(context), createIfMissing = true)
            ?: throw IdentityIntegrityException(IdentityIntegrityReason.PERSISTENCE_FAILED)
        try {
            val payload = buildSignPayload(
                resolved.rawId,
                resolved.customHandle,
                resolved.x25519Public,
                resolved.ed25519Public,
                resolved.createdAt
            )
            val signature = ChameleonCrypto.sign(payload, resolved.ed25519Private)
            val encoder = Base64.getUrlEncoder().withoutPadding()
            val handle = resolved.customHandle
                ?.let { "&h=${URLEncoder.encode(it, "UTF-8")}" }
                .orEmpty()
            "stealthx://add/${resolved.rawId}" +
                "?x=${encoder.encodeToString(resolved.x25519Public)}" +
                "&e=${encoder.encodeToString(resolved.ed25519Public)}" +
                "&s=${encoder.encodeToString(signature)}" +
                "&c=${resolved.createdAt}$handle"
        } finally {
            resolved.wipePrivateKeys()
        }
    }

    fun createInviteUrl(context: Context): String {
        val encoded = URLEncoder.encode(createQrContent(context), "UTF-8")
        return "https://stealthx.tech/invite/?app=chameleon&link=$encoded"
    }

    fun setCustomHandle(context: Context, handle: String): Result<Unit> {
        if (!handle.matches(Regex("@[a-zA-Z0-9_]{3,20}"))) {
            return Result.failure(IllegalArgumentException("Handle must be @username (3-20 chars)"))
        }
        return runCatching {
            synchronized(lock) {
                val prefs = getEncryptedPrefs(context)
                val resolved = resolve(prefs, createIfMissing = false)
                    ?: throw IllegalStateException("Identity is not initialized")
                try {
                    commit(prefs.edit().putString(KEY_CUSTOM_HANDLE, handle))
                } finally {
                    resolved.wipePrivateKeys()
                }
            }
        }
    }

    fun get(context: Context): StealthXId? = synchronized(lock) {
        runCatching {
            val resolved = resolve(getEncryptedPrefs(context), createIfMissing = false)
                ?: return@synchronized null
            try {
                resolved.toIdentity()
            } finally {
                resolved.wipePrivateKeys()
            }
        }.getOrNull()
    }

    fun isIdBoundToPublicKey(sxId: String, ed25519Public: ByteArray): Boolean {
        if (ed25519Public.size != 32) return false
        return sxId == idForPublicKey(ed25519Public)
    }

    fun idForPublicKey(ed25519Public: ByteArray): String {
        require(ed25519Public.size == 32) { "Ed25519 public key must be 32 bytes" }
        return ID_PREFIX + deriveShortId(ed25519Public.toHex())
    }

    private fun resolve(prefs: SharedPreferences, createIfMissing: Boolean): ResolvedIdentity? {
        if (persistenceFailed) {
            throw IdentityIntegrityException(IdentityIntegrityReason.PERSISTENCE_FAILED)
        }
        val snapshot = readSnapshot(prefs)
        return when (
            val plan = IdentityStateClassifier.classify(
                snapshot,
                ChameleonCrypto::isValidSigningKeyPair,
                ChameleonCrypto::isValidX25519KeyPair
            )
        ) {
            IdentityPlan.Fresh -> if (createIfMissing) createFresh(prefs, snapshot.customHandle) else null
            is IdentityPlan.Existing -> migrateAndResolve(prefs, plan)
        }
    }

    private fun createFresh(prefs: SharedPreferences, customHandle: String?): ResolvedIdentity {
        val (edPublic, edPrivate) = ChameleonCrypto.generateSigningKeyPair()
        var xPrivateToWipe: ByteArray? = null
        try {
            val (xPublic, xPrivate) = ChameleonCrypto.generateX25519KeyPair()
            xPrivateToWipe = xPrivate
            val publicKeyHex = edPublic.toHex()
            val rawId = ID_PREFIX + deriveShortId(publicKeyHex)
            val createdAt = System.currentTimeMillis()
            if (!ChameleonCrypto.isValidSigningKeyPair(edPublic, edPrivate)) {
                throw IdentityIntegrityException(IdentityIntegrityReason.INVALID_ED25519_KEYPAIR)
            }
            if (!ChameleonCrypto.isValidX25519KeyPair(xPublic, xPrivate)) {
                throw IdentityIntegrityException(IdentityIntegrityReason.INVALID_X25519_KEYPAIR)
            }
            val editor = prefs.edit()
                .putString(KEY_RAW_ID, rawId)
                .putString(KEY_PUBLIC_KEY, publicKeyHex)
                .putLong(KEY_CREATED_AT, createdAt)
                .putString(KEY_ED25519_PUBLIC, edPublic.toBase64())
                .putString(KEY_ED25519_PRIVATE, edPrivate.toBase64())
                .putString(KEY_X25519_PUBLIC, xPublic.toBase64())
                .putString(KEY_X25519_PRIVATE, xPrivate.toBase64())
                .putInt(KEY_SCHEMA_VERSION, IdentityStateClassifier.CURRENT_SCHEMA_VERSION)
            customHandle?.let { editor.putString(KEY_CUSTOM_HANDLE, it) }
            commit(editor)
            return ResolvedIdentity(
                rawId,
                publicKeyHex,
                customHandle,
                createdAt,
                edPublic,
                edPrivate,
                xPublic,
                xPrivate
            )
        } catch (error: Throwable) {
            ChameleonCrypto.wipeBytes(edPrivate)
            xPrivateToWipe?.let(ChameleonCrypto::wipeBytes)
            throw error
        }
    }

    private fun migrateAndResolve(prefs: SharedPreferences, plan: IdentityPlan.Existing): ResolvedIdentity {
        var xPublic = plan.x25519Public
        var xPrivate = plan.x25519Private
        var generatedXPrivate: ByteArray? = null
        val createdAt = if (plan.repairCreatedAt) System.currentTimeMillis() else plan.createdAt

        if (plan.generateX25519) {
            val generated = try {
                ChameleonCrypto.generateX25519KeyPair()
            } catch (error: Throwable) {
                ChameleonCrypto.wipeBytes(plan.ed25519Private)
                plan.x25519Private?.let(ChameleonCrypto::wipeBytes)
                throw error
            }
            xPublic = generated.first
            xPrivate = generated.second
            generatedXPrivate = generated.second
            val generatedPairIsValid = runCatching {
                ChameleonCrypto.isValidX25519KeyPair(generated.first, generated.second)
            }.getOrDefault(false)
            if (!generatedPairIsValid) {
                ChameleonCrypto.wipeBytes(generated.second)
                ChameleonCrypto.wipeBytes(plan.ed25519Private)
                throw IdentityIntegrityException(IdentityIntegrityReason.INVALID_X25519_KEYPAIR)
            }
        }

        val needsCommit = plan.deriveMissingIdentity || plan.repairPublicMetadata ||
            plan.generateX25519 || plan.repairCreatedAt || plan.updateSchemaVersion
        if (needsCommit) {
            try {
                val editor = prefs.edit()
                    .putString(KEY_RAW_ID, plan.rawId)
                    .putString(KEY_PUBLIC_KEY, plan.canonicalPublicKeyHex)
                    .putLong(KEY_CREATED_AT, createdAt)
                    .putInt(KEY_SCHEMA_VERSION, IdentityStateClassifier.CURRENT_SCHEMA_VERSION)
                if (plan.generateX25519) {
                    editor.putString(KEY_X25519_PUBLIC, xPublic!!.toBase64())
                    editor.putString(KEY_X25519_PRIVATE, xPrivate!!.toBase64())
                }
                commit(editor)
            } catch (error: Throwable) {
                generatedXPrivate?.let(ChameleonCrypto::wipeBytes)
                ChameleonCrypto.wipeBytes(plan.ed25519Private)
                plan.x25519Private?.let(ChameleonCrypto::wipeBytes)
                throw error
            }
        }

        return ResolvedIdentity(
            plan.rawId,
            plan.canonicalPublicKeyHex,
            plan.customHandle,
            createdAt,
            plan.ed25519Public,
            plan.ed25519Private,
            requireNotNull(xPublic),
            requireNotNull(xPrivate)
        )
    }

    private fun readSnapshot(prefs: SharedPreferences) = IdentitySnapshot(
        rawId = prefs.getString(KEY_RAW_ID, null),
        publicKeyHex = prefs.getString(KEY_PUBLIC_KEY, null),
        customHandle = prefs.getString(KEY_CUSTOM_HANDLE, null),
        createdAt = prefs.getLong(KEY_CREATED_AT, 0L),
        ed25519Public = prefs.getString(KEY_ED25519_PUBLIC, null),
        ed25519Private = prefs.getString(KEY_ED25519_PRIVATE, null),
        x25519Public = prefs.getString(KEY_X25519_PUBLIC, null),
        x25519Private = prefs.getString(KEY_X25519_PRIVATE, null),
        schemaVersion = prefs.getInt(KEY_SCHEMA_VERSION, 0)
    )

    private fun commit(editor: SharedPreferences.Editor) {
        val success = editor.commit()
        if (!success) persistenceFailed = true
        IdentityStateClassifier.requireCommit(success)
    }

    private fun buildSignPayload(
        sxId: String,
        handle: String?,
        x25519: ByteArray,
        ed25519: ByteArray,
        createdAt: Long
    ): ByteArray = buildString {
        append(sxId); append("|")
        append(handle ?: ""); append("|")
        append(x25519.toHex()); append("|")
        append(ed25519.toHex()); append("|")
        append(createdAt)
    }.toByteArray(Charsets.UTF_8)

    internal fun deriveShortId(publicKeyHex: String): String {
        val base58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val bytes = publicKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        return hash.take(9).joinToString("") { b -> base58[((b.toInt() and 0xFF) % 58)].toString() }
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private data class ResolvedIdentity(
        val rawId: String,
        val publicKeyHex: String,
        val customHandle: String?,
        val createdAt: Long,
        val ed25519Public: ByteArray,
        val ed25519Private: ByteArray,
        val x25519Public: ByteArray,
        val x25519Private: ByteArray
    ) {
        fun toIdentity() = StealthXId(rawId, customHandle, publicKeyHex, createdAt)

        fun wipePrivateKeys() {
            ChameleonCrypto.wipeBytes(ed25519Private)
            ChameleonCrypto.wipeBytes(x25519Private)
        }
    }
}
