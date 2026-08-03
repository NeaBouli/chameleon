package com.stealthx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.identity.IdentityIntegrityReason
import com.stealthx.data.identity.IdentityRecoveryManager
import com.stealthx.data.identity.StealthXId
import com.stealthx.features.messenger.transport.MessengerTransport
import com.stealthx.features.messenger.transport.MessengerTransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface IdentityRecoveryUiState {
    data object Idle : IdentityRecoveryUiState
    data object Running : IdentityRecoveryUiState
    data class Complete(val identity: StealthXId) : IdentityRecoveryUiState
    data class Failed(val message: String) : IdentityRecoveryUiState
}

@HiltViewModel
class IdentityRecoveryViewModel @Inject constructor(
    private val recoveryManager: IdentityRecoveryManager,
    private val transports: Map<MessengerTransportType, @JvmSuppressWildcards MessengerTransport>
) : ViewModel() {
    private val _state = MutableStateFlow<IdentityRecoveryUiState>(IdentityRecoveryUiState.Idle)
    val state: StateFlow<IdentityRecoveryUiState> = _state.asStateFlow()

    fun recover(reason: IdentityIntegrityReason) {
        if (_state.value is IdentityRecoveryUiState.Running) return
        _state.value = IdentityRecoveryUiState.Running
        viewModelScope.launch {
            transports.values.forEach { it.stopListening() }
            _state.value = runCatching { recoveryManager.recover(reason) }
                .fold(
                    onSuccess = IdentityRecoveryUiState::Complete,
                    onFailure = { IdentityRecoveryUiState.Failed("Identity recovery failed. No new sharing data was activated.") }
                )
        }
    }

    fun consumeResult() {
        if (_state.value !is IdentityRecoveryUiState.Running) {
            _state.value = IdentityRecoveryUiState.Idle
        }
    }
}
