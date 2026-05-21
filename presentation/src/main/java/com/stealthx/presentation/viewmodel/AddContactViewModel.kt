/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.shared.SxIdValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AddContactUiState(
    val isSaving: Boolean = false,
    val contactAdded: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class AddContactViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactKeyDao: ContactKeyDao
) : ViewModel() {

    private companion object {
        const val SIGNAL_URL = "wss://api.stealthx.tech/signal"
    }

    private val certPinner = CertificatePinner.Builder()
        .add("api.stealthx.tech", "sha256/1e85xNSEj+dcImOJS0iNkfMZOrZdvJJzzPCqT1/CZDc=")
        .add("api.stealthx.tech", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=")
        .build()

    private val exchangeClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .certificatePinner(certPinner)
        .build()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun sendExchange(toSxId: String) {
        ioScope.launch {
            try {
                val myBundle = StealthXIdentity.createQrContent(context)
                val req = Request.Builder().url(SIGNAL_URL).build()
                exchangeClient.newWebSocket(req, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        ws.send(JSONObject().apply {
                            put("type", "CONTACT_EXCHANGE")
                            put("to", toSxId)
                            put("bundle", myBundle)
                        }.toString())
                        ws.close(1000, null)
                    }
                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {}
                })
            } catch (_: Exception) {}
        }
    }

    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState

    fun consumeContactAdded() {
        _uiState.value = _uiState.value.copy(contactAdded = false)
    }

    fun addFromQrContent(content: String) {
        if (!content.startsWith("stealthx://add/")) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid QR — not a StealthX identity link")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, statusMessage = null)
        viewModelScope.launch {
            runCatching { parseAndSave(content) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        contactAdded = true,
                        statusMessage = "Contact added successfully"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to add contact"
                    )
                }
        }
    }

    private suspend fun parseAndSave(content: String) {
        // Use java.net.URI — no Android dependency, fully testable in JVM tests
        val uri = URI(content)
        val sxId = uri.path.substringAfterLast('/')

        // FIX 1: strict sx_ ID validation via shared validator
        SxIdValidator.requireValid(sxId)

        val params = uri.rawQuery
            ?.split("&")
            ?.associate { part ->
                val eq = part.indexOf('=')
                if (eq < 0) part to ""
                else part.substring(0, eq) to URLDecoder.decode(part.substring(eq + 1), "UTF-8")
            } ?: emptyMap()

        val decoder = Base64.getUrlDecoder()

        val xParam = params["x"] ?: throw IllegalArgumentException("Missing x (X25519 key)")
        val eParam = params["e"] ?: throw IllegalArgumentException("Missing e (Ed25519 key)")
        val sParam = params["s"] ?: throw IllegalArgumentException("Missing s (signature)")
        val cParam = params["c"] ?: throw IllegalArgumentException("Missing c (createdAt)")

        val x25519   = runCatching { decoder.decode(xParam) }.getOrElse { throw IllegalArgumentException("Invalid base64url in x") }
        val ed25519  = runCatching { decoder.decode(eParam) }.getOrElse { throw IllegalArgumentException("Invalid base64url in e") }
        val signature = runCatching { decoder.decode(sParam) }.getOrElse { throw IllegalArgumentException("Invalid base64url in s") }
        val createdAt = cParam.toLongOrNull() ?: throw IllegalArgumentException("Invalid createdAt: $cParam")
        val handle    = params["h"]?.takeIf { it.isNotEmpty() }

        // FIX 3: key and signature length validation
        require(x25519.size == 32)    { "Invalid X25519 key length: ${x25519.size} (expected 32)" }
        require(ed25519.size == 32)   { "Invalid Ed25519 key length: ${ed25519.size} (expected 32)" }
        require(signature.size == 64) { "Invalid signature length: ${signature.size} (expected 64)" }

        val payload = buildString {
            append(sxId); append("|")
            append(handle ?: ""); append("|")
            append(x25519.joinToString("")   { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
            append(ed25519.joinToString("")  { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
            append(createdAt.toString())
        }.toByteArray(Charsets.UTF_8)

        // FIX 2: fail-closed — invalid signature → throw, not saved
        val isVerified = runCatching { ChameleonCrypto.verify(payload, signature, ed25519) }.getOrDefault(false)
        if (!isVerified) throw SecurityException("Signature verification failed — bundle rejected")

        val existing = contactKeyDao.getById(sxId)
        if (existing != null) throw IllegalStateException("Contact $sxId already exists")

        contactKeyDao.upsert(
            ContactKeyEntity(
                id          = sxId,
                displayName = handle ?: sxId,
                identityKey = ed25519,
                dhPublicKey = x25519,
                signature   = signature,
                isVerified  = true,
                createdAt   = createdAt,
                lastUsedAt  = null
            )
        )

        sendExchange(sxId)
    }
}
