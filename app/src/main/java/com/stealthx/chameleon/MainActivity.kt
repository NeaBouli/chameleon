/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.chameleon

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.stealthx.features.decoy.screen.DecoyAuthViewModel
import com.stealthx.features.decoy.screen.DecoyModeScreen
import com.stealthx.features.decoy.screen.DecoyUnlockScreen
import com.stealthx.presentation.nav.StealthXNavGraph
import com.stealthx.presentation.theme.StealthXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val decoyAuthViewModel: DecoyAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FLAG_SECURE on all Activities — prevent screenshots & screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            StealthXTheme {
                val authState by decoyAuthViewModel.uiState.collectAsState()
                when {
                    authState.isDecoyMode -> DecoyModeScreen(onLock = decoyAuthViewModel::lock)
                    authState.requiresUnlock && !authState.isUnlocked -> DecoyUnlockScreen(
                        state = authState,
                        onSubmitPin = decoyAuthViewModel::submitPin
                    )
                    else -> {
                        val navController = rememberNavController()
                        StealthXNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
