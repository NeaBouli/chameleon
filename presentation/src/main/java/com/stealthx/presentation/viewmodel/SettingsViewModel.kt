/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    private val _overlayEnabled = MutableStateFlow(prefs.overlayEnabled)
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    fun setOverlayEnabled(enabled: Boolean) {
        _overlayEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            prefs.overlayEnabled = enabled
        }
    }
}
