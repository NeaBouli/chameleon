/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core.accessibility

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Base64
import com.stealthx.core.ICryptoBridge
import com.stealthx.core.ProcessTextResult
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.security.KeystoreManager
import com.stealthx.shared.model.EncryptedPayload

/**
 * CryptoService — runs in isolated :crypto process.
 *
 * Handles ALL crypto decisions: encryption, decryption, rule evaluation.
 * The AccessibilityService communicates with this via AIDL only.
 *
 * CRITICAL: SodiumInitializer.ensureInit() must be called here too —
 * each Android process needs its own JNI initialization.
 */
class CryptoService : Service() {

    private lateinit var overlayKey: ByteArray

    @Volatile
    private var securityLevel: Int = 0

    override fun onCreate() {
        super.onCreate()
        SodiumInitializer.ensureInit()
        overlayKey = loadOrGenerateOverlayKey()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val binder = object : ICryptoBridge.Stub() {

        override fun processText(text: String?, packageName: String?): ProcessTextResult {
            if (text == null || packageName == null) return ProcessTextResult(false, null)
            if (securityLevel == 0) return ProcessTextResult(false, null)

            return if (text.startsWith(PAYLOAD_PREFIX)) {
                val decrypted = tryDecrypt(text, packageName)
                if (decrypted != null) ProcessTextResult(true, decrypted)
                else ProcessTextResult(false, null)
            } else {
                val encrypted = tryEncrypt(text, packageName)
                if (encrypted != null) ProcessTextResult(true, encrypted)
                else ProcessTextResult(false, null)
            }
        }

        override fun notifySecurityLevel(level: Int) {
            securityLevel = level
        }

        override fun isReady(): Boolean = SodiumInitializer.isInitialized()
    }

    private fun tryEncrypt(text: String, packageName: String): String? = try {
        val aad = packageName.toByteArray(Charsets.UTF_8)
        val payload = ChameleonCrypto.encrypt(text.toByteArray(Charsets.UTF_8), overlayKey, aad)
        encodePayload(payload)
    } catch (_: Exception) { null }

    private fun tryDecrypt(text: String, packageName: String): String? = try {
        val payload = decodePayload(text).copy(aad = packageName.toByteArray(Charsets.UTF_8))
        String(ChameleonCrypto.decrypt(payload, overlayKey), Charsets.UTF_8)
    } catch (_: Exception) { null }

    private fun loadOrGenerateOverlayKey(): ByteArray {
        val prefs = getSharedPreferences("chameleon_crypto_service", Context.MODE_PRIVATE)
        val prefKey = "overlay_key_enc"
        val ksManager = KeystoreManager(applicationContext)
        val stored = prefs.getString(prefKey, null)
        return if (stored == null) {
            val raw = ChameleonCrypto.randomKey()
            val blob = ksManager.encryptBytes("chameleon_overlay_key_wrap", raw)
            prefs.edit().putString(prefKey, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
            raw
        } else {
            val blob = Base64.decode(stored, Base64.NO_WRAP)
            ksManager.decryptBytes("chameleon_overlay_key_wrap", blob)
        }
    }

    companion object {
        private const val PAYLOAD_PREFIX = "[CHAM:"
        private const val PAYLOAD_VERSION = "v1"

        internal fun encodePayload(payload: EncryptedPayload): String {
            val nonceHex = payload.nonce.joinToString("") { "%02x".format(it) }
            val ctHex = payload.ciphertext.joinToString("") { "%02x".format(it) }
            return "${PAYLOAD_PREFIX}${PAYLOAD_VERSION}:${nonceHex}:${ctHex}:${payload.paddedLength}]"
        }

        internal fun decodePayload(text: String): EncryptedPayload {
            val inner = text.removePrefix(PAYLOAD_PREFIX).removeSuffix("]")
            val parts = inner.split(":")
            require(parts.size == 4) { "Invalid payload format" }
            return EncryptedPayload(
                ciphertext   = hexToBytes(parts[2]),
                nonce        = hexToBytes(parts[1]),
                paddedLength = parts[3].toInt(),
                aad          = ByteArray(0),
                algorithm    = "XChaCha20-Poly1305",
                version      = 1
            )
        }

        private fun hexToBytes(hex: String): ByteArray =
            ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
