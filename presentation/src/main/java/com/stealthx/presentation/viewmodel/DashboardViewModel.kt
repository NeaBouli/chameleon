/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.domain.repository.SecureRuleRepository
import com.stealthx.domain.rules.SecureRule
import com.stealthx.domain.tier.TierGate
import com.stealthx.shared.model.IfrTier
import com.stealthx.shared.model.SecurityLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tierGate: TierGate,
    private val ruleRepository: SecureRuleRepository
) : ViewModel() {

    val currentTier: StateFlow<IfrTier> = tierGate.currentTier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IfrTier.FREE)

    private val _securityLevel = MutableStateFlow(SecurityLevel.PROTECTED)
    val securityLevel: StateFlow<SecurityLevel> = _securityLevel.asStateFlow()

    private val _activeRules = MutableStateFlow<List<SecureRule>>(emptyList())
    val activeRules: StateFlow<List<SecureRule>> = _activeRules.asStateFlow()

    init {
        viewModelScope.launch {
            tierGate.getTier()
        }
        viewModelScope.launch {
            ruleRepository.observeEnabled().collect { rules ->
                _activeRules.value = rules
            }
        }
    }

    fun updateSecurityLevel(level: SecurityLevel) {
        _securityLevel.value = level
    }
}
