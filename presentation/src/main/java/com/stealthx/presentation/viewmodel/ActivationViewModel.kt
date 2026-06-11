/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.activation.ActivationCodeClient
import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.IfrTier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ActivationState {
    data object Idle : ActivationState()
    data object Loading : ActivationState()
    data class Success(val tier: IfrTier) : ActivationState()
    data class Error(val message: String) : ActivationState()
}

@HiltViewModel
class ActivationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tierGate: TierGate,
    private val tierRepository: IfrTierRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val state: StateFlow<ActivationState> = _state.asStateFlow()

    fun activate(code: String) {
        if (code.isBlank()) {
            _state.value = ActivationState.Error("Code cannot be empty")
            return
        }
        _state.value = ActivationState.Loading
        ActivationCodeClient.activate(context, code) { tierName, error ->
            viewModelScope.launch(Dispatchers.IO) {
                if (tierName != null) {
                    val ifrTier = try { IfrTier.valueOf(tierName.uppercase()) } catch (_: Exception) { null }
                    if (ifrTier != null && ifrTier > IfrTier.FREE) {
                        tierRepository.saveTierResult("activation_code", 0L, ifrTier)
                        tierGate.getTier()
                        _state.value = ActivationState.Success(ifrTier)
                    } else {
                        _state.value = ActivationState.Error("Unknown tier received")
                    }
                } else {
                    val msg = when (error) {
                        "invalid_code" -> "Invalid or expired code"
                        "already_used" -> "Code already used"
                        "network_error" -> "Connection failed — try again"
                        else -> error ?: "Unknown error"
                    }
                    _state.value = ActivationState.Error(msg)
                }
            }
        }
    }

    fun reset() {
        _state.value = ActivationState.Idle
    }
}
