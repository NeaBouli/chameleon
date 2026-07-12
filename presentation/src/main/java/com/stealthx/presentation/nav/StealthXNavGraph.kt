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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stealthx.features.decoy.screen.DecoySetupScreen
import com.stealthx.features.decoy.screen.DecoySetupViewModel
import com.stealthx.features.decoy.screen.MultiDecoyScreen
import com.stealthx.features.decoy.screen.MultiDecoyViewModel
import com.stealthx.features.geofencing.screen.GeofencingScreen
import com.stealthx.features.geofencing.screen.GeofencingViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stealthx.features.overlay.screen.OverlayScreen
import com.stealthx.features.privatezone.screen.PrivateZoneScreen
import com.stealthx.features.privatezone.screen.PrivateZoneViewModel
import com.stealthx.access.compose.TierGatedContent
import com.stealthx.presentation.screen.AddContactScreen
import com.stealthx.presentation.screen.AddRuleScreen
import com.stealthx.presentation.screen.AutomationRulesScreen
import com.stealthx.presentation.screen.DashboardScreen
import com.stealthx.presentation.screen.IntroScreen
import com.stealthx.presentation.screen.KeyExchangeScreen
import com.stealthx.presentation.screen.SettingsScreen
import com.stealthx.presentation.screen.SetupScreen
import com.stealthx.presentation.screen.UpgradeScreen
import com.stealthx.presentation.viewmodel.SetupViewModel
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.DashboardViewModel
import com.stealthx.presentation.viewmodel.SettingsViewModel
import com.stealthx.shared.model.AccessTier
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthXNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val dashboardVm: DashboardViewModel = hiltViewModel()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val setupVm: SetupViewModel = hiltViewModel()
    val currentTier by dashboardVm.currentTier.collectAsState()

    val startDestination = remember {
        when {
            setupVm.isInitiallySetup -> Screen.Dashboard.route
            else -> Screen.Intro.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardVm,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToKeyExchange = { navController.navigate(Screen.KeyExchange.route) },
                onNavigateToOverlay = { navController.navigate(Screen.Overlay.route) },
                onNavigateToMessenger = { navController.navigate(Screen.Messenger.route) }
            )
        }

        composable(Screen.Overlay.route) {
            FeatureScaffold(title = "Overlay", onBack = { navController.popBackStack() }) { modifier ->
                OverlayScreen(
                    modifier = modifier
                )
            }
        }

        composable(Screen.Messenger.route) {
            FeatureScaffold(title = "Messenger", onBack = { navController.popBackStack() }) { modifier ->
                Text(
                    "Cross-device messaging is unavailable until session establishment and transport identity are verified.",
                    modifier = modifier.padding(24.dp),
                    color = StealthXColors.OnSurface
                )
            }
        }

        composable(
            route = Screen.Conversation.ROUTE,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) {
            FeatureScaffold(title = "Messenger", onBack = { navController.popBackStack() }) { modifier ->
                Text(
                    "Cross-device messaging is unavailable in this release.",
                    modifier = modifier.padding(24.dp),
                    color = StealthXColors.OnSurface
                )
            }
        }

        composable(Screen.PrivateZone.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = AccessTier.PRO,
                featureName = "Private Zone",
                onUnlockClicked = { navController.navigate(Screen.Upgrade.route) }
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
                requiredTier = AccessTier.ELITE,
                featureName = "Geofencing",
                onUnlockClicked = { navController.navigate(Screen.Upgrade.route) }
            ) {
                val vm: GeofencingViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                FeatureScaffold(title = "Geofencing", onBack = { navController.popBackStack() }) { modifier ->
                    GeofencingScreen(
                        state = state,
                        onPermissionResult = vm::refreshPermissionState,
                        onAddGeofence = vm::addGeofence,
                        onRemoveZone = vm::removeZone,
                        modifier = modifier
                    )
                }
            }
        }

        composable(Screen.Decoy.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = AccessTier.ELITE,
                featureName = "Decoy Profile",
                onUnlockClicked = { navController.navigate(Screen.Upgrade.route) }
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

        composable(Screen.Upgrade.route) {
            UpgradeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Intro.route) {
            IntroScreen(
                onWatchIntro = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                }
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
                onNavigateToUpgrade = { navController.navigate(Screen.Upgrade.route) },
                onNavigateToOverlay = { navController.navigate(Screen.Overlay.route) },
                onNavigateToPrivateZone = { navController.navigate(Screen.PrivateZone.route) },
                onNavigateToGeofencing = { navController.navigate(Screen.Geofencing.route) },
                onNavigateToDecoy = { navController.navigate(Screen.Decoy.route) },
                onNavigateToAutomationRules = { navController.navigate(Screen.AutomationRules.route) },
                onNavigateToMultiDecoy = { navController.navigate(Screen.MultiDecoy.route) },
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
                currentTier = currentTier
            )
        }

        composable(Screen.KeyExchange.route) {
            KeyExchangeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddContact.route) {
            AddContactScreen(
                onBack = { navController.popBackStack() },
                onContactAdded = { navController.popBackStack() }
            )
        }

        composable(Screen.AutomationRules.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = AccessTier.PRO,
                featureName = "Automation Rules",
                onUnlockClicked = { navController.navigate(Screen.Upgrade.route) }
            ) {
                FeatureScaffold(title = "Automation Rules", onBack = { navController.popBackStack() }) { modifier ->
                    AutomationRulesScreen(
                        onAddRule = { navController.navigate(Screen.AddRule.route) },
                        modifier = modifier
                    )
                }
            }
        }

        composable(Screen.MultiDecoy.route) {
            TierGatedContent(
                currentTier = currentTier,
                requiredTier = AccessTier.ELITE,
                featureName = "Multi-Decoy Profiles",
                onUnlockClicked = { navController.navigate(Screen.Upgrade.route) }
            ) {
                val vm: MultiDecoyViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                FeatureScaffold(title = "Multi-Decoy Profiles", onBack = { navController.popBackStack() }) { modifier ->
                    MultiDecoyScreen(
                        state = state,
                        onAddProfile = vm::addProfile,
                        onRemoveProfile = vm::removeProfile,
                        modifier = modifier
                    )
                }
            }
        }

        composable(Screen.AddRule.route) {
            val vm: com.stealthx.presentation.viewmodel.AutomationRulesViewModel = hiltViewModel()
            FeatureScaffold(title = "New Rule", onBack = { navController.popBackStack() }) { modifier ->
                AddRuleScreen(
                    onSave = { name, type, value, level ->
                        vm.saveRule(name, type, value, level,
                            onSaved = { navController.popBackStack() }
                        )
                    },
                    modifier = modifier
                )
            }
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
