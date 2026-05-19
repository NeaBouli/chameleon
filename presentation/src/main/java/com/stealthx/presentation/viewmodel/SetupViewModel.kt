package com.stealthx.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthx.core.permission.PermissionManager
import com.stealthx.core.permission.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    val isInitiallySetup: Boolean = permissionManager.currentState().allGranted

    val permissionState: StateFlow<PermissionState> = permissionManager
        .observePermissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), permissionManager.currentState())

    fun accessibilitySettingsIntent(): Intent = permissionManager.accessibilitySettingsIntent()
    fun overlaySettingsIntent(): Intent = permissionManager.overlaySettingsIntent()
}
