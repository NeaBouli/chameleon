/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.privatezone.screen

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.tier.TierLimitException
import com.stealthx.features.privatezone.engine.PrivateZoneManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivateZoneUiState(
    val fileCount: Int = 0,
    val files: List<String> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PrivateZoneViewModel @Inject constructor(
    private val privateZoneManager: PrivateZoneManager,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivateZoneUiState())
    val uiState: StateFlow<PrivateZoneUiState> = _uiState.asStateFlow()

    init {
        refreshFileList()
    }

    fun importFile(displayName: String, bytes: ByteArray) {
        store(displayName, bytes, successPrefix = "Imported")
    }

    fun storePhoto(bytes: ByteArray) {
        val name = "photo_${System.currentTimeMillis()}.jpg"
        store(name, bytes, successPrefix = "Stored")
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null, errorMessage = null)
    }

    private fun store(name: String, bytes: ByteArray, successPrefix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                privateZoneManager.storeFile(name, bytes, vaultKey())
                val files = privateZoneManager.listFiles().sorted()
                _uiState.value = PrivateZoneUiState(
                    fileCount = files.size,
                    files = files,
                    statusMessage = "$successPrefix $name"
                )
            } catch (e: TierLimitException) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Private Zone write failed")
            }
        }
    }

    fun refreshFileList() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = privateZoneManager.listFiles().sorted()
            _uiState.value = _uiState.value.copy(
                fileCount = files.size,
                files = files
            )
        }
    }

    private fun vaultKey(): ByteArray {
        prefs.privateZoneKeyBase64?.let {
            return Base64.decode(it, Base64.NO_WRAP)
        }
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.privateZoneKeyBase64 = Base64.encodeToString(key, Base64.NO_WRAP)
        return key
    }
}
