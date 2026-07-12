package com.stealthx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.stealthx.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val isInitiallySetup: Boolean = appPreferences.isOnboardingDone

    fun completeSetup() {
        appPreferences.isOnboardingDone = true
    }
}
