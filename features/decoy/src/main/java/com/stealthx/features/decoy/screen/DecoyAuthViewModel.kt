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

data class DecoyAuthUiState(
    val requiresUnlock: Boolean = false,
    val isUnlocked: Boolean = true,
    val isDecoyMode: Boolean = false,
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DecoyAuthViewModel @Inject constructor(
    private val engine: DecoyProfileEngine,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<DecoyAuthUiState> = _uiState.asStateFlow()

    fun submitPin(pin: String) {
        if (pin.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "PIN required")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(isAuthenticating = true, errorMessage = null)
            val result = runCatching { engine.authenticatePin(pin, loadConfig()) }
            val nextState = result.fold(
                onSuccess = { mode ->
                    when (mode) {
                        DecoyProfileEngine.ProfileMode.REAL -> DecoyAuthUiState(
                            requiresUnlock = true,
                            isUnlocked = true
                        )
                        DecoyProfileEngine.ProfileMode.DECOY -> DecoyAuthUiState(
                            requiresUnlock = true,
                            isUnlocked = false,
                            isDecoyMode = true
                        )
                    }
                },
                onFailure = {
                    DecoyAuthUiState(
                        requiresUnlock = true,
                        isUnlocked = false,
                        errorMessage = "Invalid PIN"
                    )
                }
            )
            _uiState.value = nextState
        }
    }

    fun lock() {
        _uiState.value = initialState()
    }

    private fun initialState(): DecoyAuthUiState {
        val requiresUnlock = prefs.decoyEnabled &&
            prefs.decoyPinHashBase64 != null &&
            prefs.decoyPinSaltBase64 != null &&
            prefs.realPinHashBase64 != null &&
            prefs.realPinSaltBase64 != null
        return DecoyAuthUiState(
            requiresUnlock = requiresUnlock,
            isUnlocked = !requiresUnlock
        )
    }

    private fun loadConfig(): DecoyProfileEngine.DecoyConfig =
        DecoyProfileEngine.DecoyConfig(
            isEnabled = prefs.decoyEnabled,
            decoyPinHash = prefs.decoyPinHashBase64?.fromBase64(),
            decoyPinSalt = prefs.decoyPinSaltBase64?.fromBase64(),
            realPinHash = prefs.realPinHashBase64?.fromBase64(),
            realPinSalt = prefs.realPinSaltBase64?.fromBase64()
        )

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
