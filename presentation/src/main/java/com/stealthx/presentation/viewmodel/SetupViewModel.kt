package com.stealthx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.stealthx.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val isInitiallySetup: Boolean = appPreferences.isOnboardingDone
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun completeSetup(): Boolean {
        val persisted = appPreferences.markOnboardingDone()
        _error.value = if (persisted) null else "Could not save setup state. Please try again."
        return persisted
    }
}
