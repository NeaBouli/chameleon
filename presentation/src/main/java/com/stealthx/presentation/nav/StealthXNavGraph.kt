/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stealthx.ifr.compose.TierGatedContent
import com.stealthx.presentation.screen.DashboardScreen
import com.stealthx.presentation.screen.IFRUnlockScreen
import com.stealthx.presentation.screen.KeyExchangeScreen
import com.stealthx.presentation.screen.SettingsScreen
import com.stealthx.presentation.viewmodel.DashboardViewModel
import com.stealthx.presentation.viewmodel.IFRViewModel
import com.stealthx.shared.model.IfrTier

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
            // Free tier — always accessible
            // TODO S-08: OverlayFeatureScreen
        }

        composable(Screen.Messenger.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = IfrTier.PRO,
                featureName = "Encrypted Messenger",
                onUnlockClicked = { navController.navigate(Screen.IFRUnlock.route) }
            ) {
                // TODO S-08: MessengerFeatureScreen
            }
        }

        composable(Screen.PrivateZone.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = IfrTier.PRO,
                featureName = "Private Zone",
                onUnlockClicked = { navController.navigate(Screen.IFRUnlock.route) }
            ) {
                // TODO S-08: PrivateZoneFeatureScreen
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
                onNavigateToIFR = { navController.navigate(Screen.IFRUnlock.route) }
            )
        }

        composable(Screen.KeyExchange.route) {
            KeyExchangeScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
