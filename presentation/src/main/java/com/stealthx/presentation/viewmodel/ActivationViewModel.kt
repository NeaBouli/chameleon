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
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.domain.repository.AccessTierRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.AccessTier
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
    data class Success(val tier: AccessTier) : ActivationState()
    data class Error(val message: String) : ActivationState()
}

@HiltViewModel
class ActivationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tierGate: TierGate,
    private val tierRepository: AccessTierRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val state: StateFlow<ActivationState> = _state.asStateFlow()

    fun activate(code: String) {
        if (code.isBlank()) {
            _state.value = ActivationState.Error("Code cannot be empty")
            return
        }
        _state.value = ActivationState.Loading
        ActivationCodeClient.activate(context, code) { activation, error ->
            viewModelScope.launch(Dispatchers.IO) {
                if (activation != null) {
                    val accessTier = activation.tier
                    if (accessTier > AccessTier.FREE) {
                        prefs.entitlementToken = activation.entitlementToken
                        tierRepository.saveTierResult(
                            sourceId = "fiat_entitlement:${activation.productId}",
                            accessWeight = 0L,
                            tier = accessTier,
                            expiresAtEpochSeconds = activation.expiresAtEpochSeconds
                        )
                        tierGate.getTier()
                        _state.value = ActivationState.Success(accessTier)
                    } else {
                        _state.value = ActivationState.Error("Unknown tier received")
                    }
                } else {
                    val msg = when (error) {
                        "invalid_code" -> "Invalid or expired code"
                        "already_used" -> "Code already used"
                        "entitlement_missing" -> "Server entitlement is missing"
                        "entitlement_not_configured" -> "Secure purchase activation is not configured"
                        "entitlement_invalid" -> "Entitlement verification failed"
                        "network_error" -> "Connection failed — try again"
                        "timeout" -> "Verification timed out — try again"
                        "invalid_response" -> "Invalid verification response"
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
