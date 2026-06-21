/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stealthx.presentation.composable.TierBadge
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.ActivationState
import com.stealthx.presentation.viewmodel.ActivationViewModel
import com.stealthx.presentation.viewmodel.SettingsViewModel
import com.stealthx.shared.model.AccessTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onNavigateToOverlay: () -> Unit,
    onNavigateToPrivateZone: () -> Unit,
    onNavigateToGeofencing: () -> Unit,
    onNavigateToDecoy: () -> Unit,
    onNavigateToAutomationRules: () -> Unit = {},
    onNavigateToMultiDecoy: () -> Unit = {},
    onNavigateToSetup: () -> Unit = {},
    currentTier: AccessTier = AccessTier.FREE,
    activationVm: ActivationViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activationState by activationVm.state.collectAsState()
    val backgroundListenerEnabled by settingsVm.backgroundListenerEnabled.collectAsState()
    val appVersion = remember(context.packageName) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")
    }
    var showActivationDialog by remember { mutableStateOf(false) }
    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "No browser available for this link", Toast.LENGTH_SHORT).show()
        }
    }

    if (showActivationDialog) {
        ActivationCodeDialog(
            state = activationState,
            onDismiss = { showActivationDialog = false; activationVm.reset() },
            onSubmit = activationVm::activate
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .semantics { contentDescription = "Settings screen" }
        ) {
            // — Tier Card ——————————————————————————————————————
            TierSection(tier = currentTier, onUpgradeClick = onNavigateToUpgrade)

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Connection") {
                ToggleRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Background Contact Listener",
                    subtitle = "Keep secure contact exchange active after leaving the app",
                    checked = backgroundListenerEnabled,
                    onCheckedChange = settingsVm::setBackgroundListenerEnabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // — Free features ——————————————————————————————————
            SettingsSection(title = "Free") {
                FeatureRow(
                    icon = Icons.Default.Shield,
                    title = "Overlay Encryption",
                    subtitle = "Always on — basic protection",
                    locked = false,
                    onClick = onNavigateToOverlay
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // — Pro features ———————————————————————————————————
            SettingsSection(title = "Pro") {
                FeatureRow(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Unlimited Automation Rules",
                    subtitle = "Context-aware triggers",
                    locked = currentTier < AccessTier.PRO,
                    onLockedClick = onNavigateToUpgrade,
                    onClick = onNavigateToAutomationRules
                )
                FeatureRow(
                    icon = Icons.Default.Storage,
                    title = "Private Zone",
                    subtitle = "100 MB encrypted vault",
                    locked = currentTier < AccessTier.PRO,
                    onLockedClick = onNavigateToUpgrade,
                    onClick = onNavigateToPrivateZone
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // — Elite features —————————————————————————————————
            SettingsSection(title = "Elite") {
                FeatureRow(
                    icon = Icons.Default.FaceRetouchingNatural,
                    title = "Decoy Profile",
                    subtitle = "Wrong PIN → decoy identity",
                    locked = currentTier < AccessTier.ELITE,
                    onLockedClick = onNavigateToUpgrade,
                    onClick = onNavigateToDecoy,
                    eliteTier = true
                )
                FeatureRow(
                    icon = Icons.Default.MyLocation,
                    title = "Geofencing",
                    subtitle = "Location-triggered encryption rules",
                    locked = currentTier < AccessTier.ELITE,
                    onLockedClick = onNavigateToUpgrade,
                    onClick = onNavigateToGeofencing,
                    eliteTier = true
                )
                FeatureRow(
                    icon = Icons.Default.FaceRetouchingNatural,
                    title = "Multi-Decoy Profiles",
                    subtitle = "Multiple fake identities",
                    locked = currentTier < AccessTier.ELITE,
                    onLockedClick = onNavigateToUpgrade,
                    onClick = onNavigateToMultiDecoy,
                    eliteTier = true
                )
                FeatureRow(
                    icon = Icons.Default.Security,
                    title = "Advanced Threat Detection",
                    subtitle = "Real-time behavioral analysis",
                    locked = currentTier < AccessTier.ELITE,
                    onLockedClick = onNavigateToUpgrade,
                    eliteTier = true,
                    comingSoon = true
                )
                FeatureRow(
                    icon = Icons.Default.Shield,
                    title = "Zero Telemetry",
                    subtitle = "No analytics, no logs",
                    locked = currentTier < AccessTier.ELITE,
                    onLockedClick = onNavigateToUpgrade,
                    eliteTier = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // — Access ——————————————————————————————————————————
            SettingsSection(title = "Access") {
                HelpLinkRow(
                    icon = Icons.Default.CreditCard,
                    title = "Buy Lifetime Access",
                    subtitle = "Pro EUR 9 · Elite EUR 19 · Stripe checkout",
                    onClick = { openUrl("https://chameleon.stealthx.tech/#lifetime") }
                )
                HelpLinkRow(
                    icon = Icons.Default.Key,
                    title = "Activation Code",
                    subtitle = "Enter code to unlock Pro or Elite tier",
                    onClick = { showActivationDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // — Help ———————————————————————————————————————————
            SettingsSection(title = "Help") {
                HelpLinkRow(
                    icon = Icons.Default.MenuBook,
                    title = "User Manual",
                    subtitle = "How Chameleon works + first setup",
                    onClick = { openUrl("https://chameleon.stealthx.tech/wiki/user-manual.html") }
                )
                HelpLinkRow(
                    icon = Icons.Default.RocketLaunch,
                    title = "Getting Started",
                    subtitle = "Activate overlay, set up Private Zone, configure rules",
                    onClick = onNavigateToSetup
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // — About ——————————————————————————————————————————
            SettingsSection(title = "About") {
                SettingsItem(label = "Version", value = appVersion)
                SettingsItem(label = "License", value = "GPL-3.0")
                SettingsItem(label = "Platform", value = "StealthX")
            }
        }
    }
}

@Composable
private fun TierSection(tier: AccessTier, onUpgradeClick: () -> Unit) {
    val tierColor = when (tier) {
        AccessTier.FREE -> Color.Gray
        AccessTier.PRO -> StealthXColors.Primary
        AccessTier.ELITE -> Color(0xFFFFD700)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StealthXColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Current Tier", style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                TierBadge(tier = tier)
                if (tier == AccessTier.FREE) {
                    Spacer(Modifier.height(4.dp))
                    Text("Buy on the website, then activate with your code.", style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
                }
            }
            if (tier != AccessTier.ELITE) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onUpgradeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = tierColor.copy(alpha = 0.85f))
                ) {
                    Text(if (tier == AccessTier.FREE) "Buy access" else "Buy Elite", color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    locked: Boolean,
    onLockedClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    eliteTier: Boolean = false,
    comingSoon: Boolean = false
) {
    val tintColor = when {
        locked || comingSoon -> Color.Gray.copy(alpha = 0.4f)
        eliteTier -> Color(0xFFFFD700)
        else -> StealthXColors.Primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!locked && !comingSoon && onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tintColor)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (locked || comingSoon) Color.Gray else StealthXColors.OnSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
        }
        if (comingSoon) {
            Text(
                "SOON",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF888888),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        } else if (locked && onLockedClick != null) {
            TextButton(onClick = onLockedClick) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                Text("Buy access", color = StealthXColors.Primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StealthXColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = StealthXColors.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SettingsItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = StealthXColors.OnSurface)
        Text(value, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
    }
}

@Composable
private fun HelpLinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = StealthXColors.Primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = StealthXColors.OnSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = StealthXColors.Primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = StealthXColors.OnSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActivationCodeDialog(
    state: ActivationState,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val isLoading = state is ActivationState.Loading
    val isDone = state is ActivationState.Success

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Enter Activation Code") },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Code") },
                    placeholder = { Text("XXXX-XXXX-XXXX") },
                    singleLine = true,
                    enabled = !isLoading && !isDone,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                when (state) {
                    is ActivationState.Error -> Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    is ActivationState.Success -> Text(
                        "Unlocked: ${state.tier.name}",
                        color = Color(0xFF00E676),
                        style = MaterialTheme.typography.bodySmall
                    )
                    is ActivationState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (isDone) {
                TextButton(onClick = onDismiss) { Text("Done") }
            } else {
                TextButton(
                    onClick = { onSubmit(code) },
                    enabled = code.isNotBlank() && !isLoading
                ) { Text("Activate") }
            }
        },
        dismissButton = {
            if (!isDone) {
                TextButton(onClick = { if (!isLoading) onDismiss() }) { Text("Cancel") }
            }
        }
    )
}
