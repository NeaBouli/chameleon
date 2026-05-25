/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.features.messenger.repository.ConversationSummary
import com.stealthx.features.messenger.repository.MessengerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MessengerViewModel @Inject constructor(
    private val repository: MessengerRepository
) : ViewModel() {

    val conversations: StateFlow<List<ConversationSummary>> =
        repository.observeConversationSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
