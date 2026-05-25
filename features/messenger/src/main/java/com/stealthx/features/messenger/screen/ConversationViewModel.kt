/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.features.messenger.repository.DecryptedMessage
import com.stealthx.features.messenger.repository.MessengerRepository
import com.stealthx.features.messenger.transport.MessengerTransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val isSending: Boolean = false,
    val sendError: String? = null,
    val selectedTransport: MessengerTransportType = MessengerTransportType.SERVER_RELAY,
    val contactName: String = ""
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val repository: MessengerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val contactId: String = checkNotNull(savedStateHandle["contactId"])

    val messages: StateFlow<List<DecryptedMessage>> =
        repository.observeMessages(contactId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val name = repository.getContactName(contactId)
            _uiState.value = _uiState.value.copy(contactName = name)
        }
    }

    fun selectTransport(transport: MessengerTransportType) {
        _uiState.value = _uiState.value.copy(selectedTransport = transport)
    }

    fun send(text: String) {
        if (text.isBlank()) return
        val transport = _uiState.value.selectedTransport
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSending = true, sendError = null)
            val result = runCatching { repository.sendMessage(contactId, text, transport) }
            _uiState.value = _uiState.value.copy(
                isSending = false,
                sendError = result.exceptionOrNull()?.message
            )
        }
    }

    fun markRead() {
        viewModelScope.launch(Dispatchers.IO) { repository.markRead(contactId) }
    }
}
