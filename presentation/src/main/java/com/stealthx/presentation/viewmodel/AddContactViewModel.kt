/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.entity.ContactKeyEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddContactUiState(
    val isSaving: Boolean = false,
    val contactAdded: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val contactKeyDao: ContactKeyDao
) : ViewModel() {

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
        val uri = Uri.parse(content)
        val segments = uri.pathSegments
        val sxId = segments.lastOrNull()
            ?: throw IllegalArgumentException("Missing sx_ ID in URI")
        if (!sxId.startsWith("sx_")) throw IllegalArgumentException("Invalid sx_ ID: $sxId")

        val decoder = java.util.Base64.getUrlDecoder()

        val xParam = uri.getQueryParameter("x")
            ?: throw IllegalArgumentException("Missing x (X25519 key)")
        val eParam = uri.getQueryParameter("e")
            ?: throw IllegalArgumentException("Missing e (Ed25519 key)")
        val sParam = uri.getQueryParameter("s")
            ?: throw IllegalArgumentException("Missing s (signature)")
        val cParam = uri.getQueryParameter("c")
            ?: throw IllegalArgumentException("Missing c (createdAt)")

        val x25519 = decoder.decode(xParam)
        val ed25519 = decoder.decode(eParam)
        val signature = decoder.decode(sParam)
        val createdAt = cParam.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid createdAt: $cParam")
        val handle = uri.getQueryParameter("h")

        val payload = buildString {
            append(sxId); append("|")
            append(handle ?: ""); append("|")
            append(x25519.joinToString("") { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
            append(ed25519.joinToString("") { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
            append(createdAt.toString())
        }.toByteArray(Charsets.UTF_8)

        val isVerified = runCatching {
            ChameleonCrypto.verify(payload, signature, ed25519)
        }.getOrDefault(false)

        val existing = contactKeyDao.getById(sxId)
        if (existing != null) throw IllegalStateException("Contact $sxId already exists")

        contactKeyDao.upsert(
            ContactKeyEntity(
                id = sxId,
                displayName = handle ?: sxId,
                identityKey = ed25519,
                dhPublicKey = x25519,
                signature = signature,
                isVerified = isVerified,
                createdAt = createdAt,
                lastUsedAt = null
            )
        )
    }
}
