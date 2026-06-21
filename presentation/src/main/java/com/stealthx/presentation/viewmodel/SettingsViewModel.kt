/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import com.stealthx.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _overlayEnabled = MutableStateFlow(prefs.overlayEnabled)
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    private val _overlayWhitelist = MutableStateFlow(prefs.overlayWhitelistPackages)
    val overlayWhitelist: StateFlow<Set<String>> = _overlayWhitelist.asStateFlow()

    private val _backgroundListenerEnabled = MutableStateFlow(prefs.backgroundListenerEnabled)
    val backgroundListenerEnabled: StateFlow<Boolean> = _backgroundListenerEnabled.asStateFlow()

    fun setOverlayEnabled(enabled: Boolean) {
        _overlayEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            prefs.overlayEnabled = enabled
        }
    }

    fun setOverlayPackageEnabled(packageName: String, enabled: Boolean) {
        val normalized = packageName.trim()
        if (normalized.isBlank()) return
        val updated = if (enabled) {
            _overlayWhitelist.value + normalized
        } else {
            _overlayWhitelist.value - normalized
        }
        _overlayWhitelist.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            prefs.overlayWhitelistPackages = updated
        }
    }

    fun setBackgroundListenerEnabled(enabled: Boolean) {
        _backgroundListenerEnabled.value = enabled
        prefs.backgroundListenerEnabled = enabled
        val intent = Intent().setClassName(context.packageName, LISTENER_SERVICE_CLASS)
        runCatching {
            if (enabled) context.startForegroundService(intent) else context.stopService(intent)
        }
    }

    private companion object {
        const val LISTENER_SERVICE_CLASS = "com.stealthx.chameleon.service.ContactListenerService"
    }
}
