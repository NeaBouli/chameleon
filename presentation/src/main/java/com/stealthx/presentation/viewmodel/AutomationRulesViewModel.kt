/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.domain.repository.SecureRuleRepository
import com.stealthx.domain.rules.ActionType
import com.stealthx.domain.rules.SecureRule
import com.stealthx.domain.rules.TriggerType
import com.stealthx.shared.model.SecurityLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AutomationRulesViewModel @Inject constructor(
    private val repository: SecureRuleRepository
) : ViewModel() {

    val rules: StateFlow<List<SecureRule>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRule(
        name: String,
        triggerType: TriggerType,
        triggerValue: String,
        securityLevel: SecurityLevel,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.save(
                SecureRule(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    triggerType = triggerType,
                    triggerValue = triggerValue.trim(),
                    actionType = ActionType.SET_LEVEL,
                    securityLevel = securityLevel,
                    priority = 0,
                    isEnabled = true,
                    createdAt = System.currentTimeMillis() / 1000,
                    lastTriggered = null,
                    triggerCount = 0
                )
            )
            onSaved()
        }
    }

    fun toggleRule(rule: SecureRule) {
        viewModelScope.launch {
            repository.save(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(rule: SecureRule) {
        viewModelScope.launch {
            repository.delete(rule)
        }
    }
}
