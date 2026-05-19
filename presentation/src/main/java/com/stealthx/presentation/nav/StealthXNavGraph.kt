/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.nav

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stealthx.features.decoy.screen.DecoySetupScreen
import com.stealthx.features.decoy.screen.DecoySetupViewModel
import com.stealthx.features.geofencing.screen.GeofencingScreen
import com.stealthx.features.geofencing.screen.GeofencingViewModel
import com.stealthx.features.messenger.screen.MessengerScreen
import com.stealthx.features.overlay.screen.OverlayScreen
import com.stealthx.features.privatezone.screen.PrivateZoneScreen
import com.stealthx.features.privatezone.screen.PrivateZoneViewModel
import com.stealthx.ifr.compose.TierGatedContent
import com.stealthx.presentation.screen.DashboardScreen
import com.stealthx.presentation.screen.IFRUnlockScreen
import com.stealthx.presentation.screen.KeyExchangeScreen
import com.stealthx.presentation.screen.SettingsScreen
import com.stealthx.presentation.screen.SetupScreen
import com.stealthx.presentation.viewmodel.SetupViewModel
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.DashboardViewModel
import com.stealthx.presentation.viewmodel.IFRViewModel
import com.stealthx.presentation.viewmodel.SettingsViewModel
import com.stealthx.shared.model.IfrTier
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthXNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val dashboardVm: DashboardViewModel = hiltViewModel()
    val ifrVm: IFRViewModel = hiltViewModel()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val setupVm: SetupViewModel = hiltViewModel()
    val currentTier by dashboardVm.currentTier.collectAsState()
    val overlayEnabled by settingsVm.overlayEnabled.collectAsState()
    val overlayWhitelist by settingsVm.overlayWhitelist.collectAsState()

    val startDestination = remember {
        if (setupVm.isInitiallySetup) Screen.Dashboard.route else Screen.Setup.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

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
                OverlayScreen(
                    overlayEnabled = overlayEnabled,
                    overlayWhitelistPackages = overlayWhitelist,
                    onOverlayEnabledChange = settingsVm::setOverlayEnabled,
                    onPackageEnabledChange = settingsVm::setOverlayPackageEnabled,
                    modifier = modifier
                )
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
                val vm: PrivateZoneViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) {
                        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "import_${System.currentTimeMillis()}"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null) {
                            vm.importFile(name, bytes)
                        } else {
                            Toast.makeText(context, "Could not read selected file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        vm.storePhoto(out.toByteArray())
                    } else {
                        Toast.makeText(context, "No photo captured", Toast.LENGTH_SHORT).show()
                    }
                }

                FeatureScaffold(title = "Private Zone", onBack = { navController.popBackStack() }) { modifier ->
                    PrivateZoneScreen(
                        fileCount = state.fileCount,
                        files = state.files,
                        statusMessage = state.statusMessage,
                        errorMessage = state.errorMessage,
                        onImportFile = { importLauncher.launch("*/*") },
                        onTakePhoto = { cameraLauncher.launch(null) },
                        modifier = modifier
                    )
                }
            }
        }

        composable(Screen.Geofencing.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = IfrTier.ELITE,
                featureName = "Geofencing",
                onUnlockClicked = { navController.navigate(Screen.IFRUnlock.route) }
            ) {
                val vm: GeofencingViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                FeatureScaffold(title = "Geofencing", onBack = { navController.popBackStack() }) { modifier ->
                    GeofencingScreen(
                        state = state,
                        onPermissionResult = vm::refreshPermissionState,
                        onAddGeofence = vm::addGeofence,
                        modifier = modifier
                    )
                }
            }
        }

        composable(Screen.Decoy.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = IfrTier.ELITE,
                featureName = "Decoy Profile",
                onUnlockClicked = { navController.navigate(Screen.IFRUnlock.route) }
            ) {
                val vm: DecoySetupViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                FeatureScaffold(title = "Decoy Profile", onBack = { navController.popBackStack() }) { modifier ->
                    DecoySetupScreen(
                        state = state,
                        onSavePins = vm::savePins,
                        onDisableDecoy = vm::disableDecoy,
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

        composable(Screen.Setup.route) {
            SetupScreen(onContinue = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Setup.route) { inclusive = true }
                }
            }, viewModel = setupVm)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToIFR = { navController.navigate(Screen.IFRUnlock.route) },
                onNavigateToOverlay = { navController.navigate(Screen.Overlay.route) },
                onNavigateToPrivateZone = { navController.navigate(Screen.PrivateZone.route) },
                onNavigateToGeofencing = { navController.navigate(Screen.Geofencing.route) },
                onNavigateToDecoy = { navController.navigate(Screen.Decoy.route) },
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
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
