/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stealthx.features.messenger.screen.MessengerScreen
import com.stealthx.features.overlay.screen.OverlayScreen
import com.stealthx.features.privatezone.screen.PrivateZoneScreen
import com.stealthx.ifr.compose.TierGatedContent
import com.stealthx.presentation.screen.DashboardScreen
import com.stealthx.presentation.screen.IFRUnlockScreen
import com.stealthx.presentation.screen.KeyExchangeScreen
import com.stealthx.presentation.screen.SettingsScreen
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.DashboardViewModel
import com.stealthx.presentation.viewmodel.IFRViewModel
import com.stealthx.shared.model.IfrTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthXNavGraph(navController: NavHostController) {
    val dashboardVm: DashboardViewModel = hiltViewModel()
    val ifrVm: IFRViewModel = hiltViewModel()
    val currentTier by dashboardVm.currentTier.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardVm,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToKeyExchange = { navController.navigate(Screen.KeyExchange.route) },
                onNavigateToIFR = { navController.navigate(Screen.IFRUnlock.route) },
                onNavigateToOverlay = { navController.navigate(Screen.Overlay.route) },
                onNavigateToMessenger = { navController.navigate(Screen.Messenger.route) }
            )
        }

        composable(Screen.Overlay.route) {
            FeatureScaffold(title = "Overlay", onBack = { navController.popBackStack() }) { modifier ->
                OverlayScreen(modifier = modifier)
            }
        }

        composable(Screen.Messenger.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = IfrTier.PRO,
                featureName = "Encrypted Messenger",
                onUnlockClicked = { navController.navigate(Screen.IFRUnlock.route) }
            ) {
                FeatureScaffold(title = "Messenger", onBack = { navController.popBackStack() }) { modifier ->
                    MessengerScreen(
                        onAddContact = { navController.navigate(Screen.KeyExchange.route) },
                        modifier = modifier
                    )
                }
            }
        }

        composable(Screen.PrivateZone.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = IfrTier.PRO,
                featureName = "Private Zone",
                onUnlockClicked = { navController.navigate(Screen.IFRUnlock.route) }
            ) {
                FeatureScaffold(title = "Private Zone", onBack = { navController.popBackStack() }) { modifier ->
                    PrivateZoneScreen(
                        fileCount = 0,
                        onImportFile = { /* TODO: file picker intent */ },
                        onTakePhoto = { /* TODO: camera intent */ },
                        modifier = modifier
                    )
                }
            }
        }

        composable(Screen.IFRUnlock.route) {
            IFRUnlockScreen(
                viewModel = ifrVm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToIFR = { navController.navigate(Screen.IFRUnlock.route) },
                currentTier = currentTier
            )
        }

        composable(Screen.KeyExchange.route) {
            KeyExchangeScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StealthXColors.Background,
                    titleContentColor = StealthXColors.OnSurface
                )
            )
        },
        containerColor = StealthXColors.Background
    ) { padding ->
        content(Modifier.padding(padding))
    }
}
