/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.overlay.engine

import com.stealthx.domain.engine.EncryptionEngine
import com.stealthx.domain.rules.RuleEngine
import com.stealthx.domain.rules.SecureRule
import com.stealthx.shared.model.EncryptedPayload
import com.stealthx.shared.model.SecurityLevel
import com.stealthx.shared.model.TriggerContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Overlay Engine — orchestrates text encryption based on active rules.
 *
 * Called by CryptoService via AIDL when AccessibilityService intercepts text.
 * Delegates crypto to EncryptionEngine (:domain), rule evaluation to RuleEngine.
 * NO direct crypto code here.
 */
@Singleton
class OverlayEngine @Inject constructor(
    private val encryptionEngine: EncryptionEngine,
    private val ruleEngine: RuleEngine
) {

    data class OverlayResult(
        val shouldEncrypt: Boolean,
        val processedText: String?,
        val securityLevel: SecurityLevel
    )

    /**
     * Process text from an app based on current rules.
     *
     * @param text         Intercepted text
     * @param packageName  Source app package name (used as AAD)
     * @param key          Encryption key
     * @param activeRules  Currently active rules
     * @param context      Current trigger context
     * @return             OverlayResult with decision
     */
    fun processText(
        text: String,
        packageName: String,
        key: ByteArray,
        activeRules: List<SecureRule>,
        context: TriggerContext
    ): OverlayResult {
        val securityLevel = ruleEngine.evaluate(activeRules, context)

        return when (securityLevel) {
            SecurityLevel.PUBLIC -> OverlayResult(
                shouldEncrypt = false,
                processedText = null,
                securityLevel = securityLevel
            )
            else -> {
                val payload = encryptionEngine.encryptText(text, key, packageName)
                val encoded = encodePayload(payload)
                OverlayResult(
                    shouldEncrypt = true,
                    processedText = encoded,
                    securityLevel = securityLevel
                )
            }
        }
    }

    /**
     * Attempt to decrypt text if it looks like a Chameleon payload.
     */
    fun tryDecrypt(text: String, key: ByteArray): String? {
        if (!text.startsWith(PAYLOAD_PREFIX)) return null
        return try {
            val payload = decodePayload(text)
            encryptionEngine.decryptText(payload, key)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PAYLOAD_PREFIX = "[CHAM:"
        private const val PAYLOAD_VERSION = "v1"

        fun encodePayload(payload: EncryptedPayload): String {
            val nonceHex = payload.nonce.joinToString("") { "%02x".format(it) }
            val ctHex = payload.ciphertext.joinToString("") { "%02x".format(it) }
            return "${PAYLOAD_PREFIX}${PAYLOAD_VERSION}:${nonceHex}:${ctHex}]"
        }

        fun decodePayload(text: String): EncryptedPayload {
            val inner = text.removePrefix(PAYLOAD_PREFIX).removeSuffix("]")
            val parts = inner.split(":")
            require(parts.size == 3) { "Invalid payload format" }
            val nonce = hexToBytes(parts[1])
            val ciphertext = hexToBytes(parts[2])
            return EncryptedPayload(
                ciphertext = ciphertext,
                nonce = nonce,
                paddedLength = ciphertext.size - 16,
                aad = ByteArray(0),
                algorithm = "XChaCha20-Poly1305",
                version = 1
            )
        }

        private fun hexToBytes(hex: String): ByteArray {
            return ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }
    }
}
