/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy.screen

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.features.decoy.engine.DecoyProfileEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DecoySetupUiState(
    val isEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DecoySetupViewModel @Inject constructor(
    private val engine: DecoyProfileEngine,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DecoySetupUiState(isEnabled = prefs.decoyEnabled)
    )
    val uiState: StateFlow<DecoySetupUiState> = _uiState.asStateFlow()

    fun savePins(realPin: String, decoyPin: String, confirmDecoyPin: String) {
        val validationError = validate(realPin, decoyPin, confirmDecoyPin)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError, statusMessage = null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, statusMessage = null)
            try {
                val realSalt = engine.generatePinSalt()
                val decoySalt = engine.generatePinSalt()
                val realHash = engine.hashPin(realPin, realSalt)
                val decoyHash = engine.hashPin(decoyPin, decoySalt)

                prefs.realPinSaltBase64 = realSalt.toBase64()
                prefs.realPinHashBase64 = realHash.toBase64()
                prefs.decoyPinSaltBase64 = decoySalt.toBase64()
                prefs.decoyPinHashBase64 = decoyHash.toBase64()
                prefs.decoyEnabled = true

                _uiState.value = DecoySetupUiState(
                    isEnabled = true,
                    statusMessage = "Decoy profile enabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Could not save decoy profile"
                )
            }
        }
    }

    fun disableDecoy() {
        prefs.decoyEnabled = false
        _uiState.value = DecoySetupUiState(
            isEnabled = false,
            statusMessage = "Decoy profile disabled"
        )
    }

    private fun validate(realPin: String, decoyPin: String, confirmDecoyPin: String): String? {
        if (realPin.length < MIN_PIN_LENGTH) return "Real PIN must be at least $MIN_PIN_LENGTH digits"
        if (decoyPin.length < MIN_PIN_LENGTH) return "Decoy PIN must be at least $MIN_PIN_LENGTH digits"
        if (!realPin.all(Char::isDigit) || !decoyPin.all(Char::isDigit)) return "PINs must contain digits only"
        if (realPin == decoyPin) return "Real and decoy PINs must be different"
        if (decoyPin != confirmDecoyPin) return "Decoy PIN confirmation does not match"
        return null
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        const val MIN_PIN_LENGTH = 4
    }
}
