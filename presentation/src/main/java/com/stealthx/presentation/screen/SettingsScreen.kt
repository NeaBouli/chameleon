/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

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
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stealthx.presentation.composable.TierBadge
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.shared.model.IfrTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToIFR: () -> Unit,
    onNavigateToOverlay: () -> Unit,
    onNavigateToPrivateZone: () -> Unit,
    onNavigateToGeofencing: () -> Unit,
    onNavigateToDecoy: () -> Unit,
    currentTier: IfrTier = IfrTier.FREE
) {
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
            TierSection(tier = currentTier, onUpgradeClick = onNavigateToIFR)

            Spacer(modifier = Modifier.height(16.dp))

            // — Free features ——————————————————————————————————
            SettingsSection(title = "Free") {
                FeatureRow(
                    icon = Icons.Default.Shield,
                    title = "Overlay Encryption",
                    subtitle = "Always on — basic protection",
                    locked = false,
                    onClick = onNavigateToOverlay
                )
                FeatureRow(
                    icon = Icons.Default.MyLocation,
                    title = "Manual Geofencing",
                    subtitle = "3 rules max",
                    locked = false,
                    onClick = onNavigateToGeofencing
                )
                FeatureRow(
                    icon = Icons.Default.Storage,
                    title = "Private Zone",
                    subtitle = "100 MB encrypted vault",
                    locked = false,
                    onClick = onNavigateToPrivateZone
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // — Pro features ———————————————————————————————————
            SettingsSection(title = "Pro  ≥ 2,000 IFR") {
                FeatureRow(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Unlimited Automation Rules",
                    subtitle = "Context-aware triggers",
                    locked = currentTier < IfrTier.PRO,
                    onLockedClick = onNavigateToIFR
                )
                FeatureRow(
                    icon = Icons.Default.Storage,
                    title = "Private Zone — Unlimited",
                    subtitle = "No storage cap",
                    locked = currentTier < IfrTier.PRO,
                    onLockedClick = onNavigateToIFR,
                    onClick = onNavigateToPrivateZone
                )
                FeatureRow(
                    icon = Icons.Default.FaceRetouchingNatural,
                    title = "Decoy Profile",
                    subtitle = "Wrong PIN → decoy identity",
                    locked = currentTier < IfrTier.ELITE,
                    onLockedClick = onNavigateToIFR,
                    onClick = onNavigateToDecoy
                )
                FeatureRow(
                    icon = Icons.Default.MyLocation,
                    title = "Automatic Geofencing",
                    subtitle = "Location-triggered encryption",
                    locked = currentTier < IfrTier.PRO,
                    onLockedClick = onNavigateToIFR,
                    onClick = onNavigateToGeofencing
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // — Elite features —————————————————————————————————
            SettingsSection(title = "Elite  ≥ 6,000 IFR") {
                FeatureRow(
                    icon = Icons.Default.FaceRetouchingNatural,
                    title = "Multi-Decoy Profiles",
                    subtitle = "Multiple fake identities",
                    locked = currentTier < IfrTier.ELITE,
                    onLockedClick = onNavigateToIFR,
                    eliteTier = true
                )
                FeatureRow(
                    icon = Icons.Default.Security,
                    title = "Advanced Threat Detection",
                    subtitle = "Real-time behavioral analysis",
                    locked = currentTier < IfrTier.ELITE,
                    onLockedClick = onNavigateToIFR,
                    eliteTier = true
                )
                FeatureRow(
                    icon = Icons.Default.Shield,
                    title = "Zero Telemetry",
                    subtitle = "No analytics, no logs",
                    locked = currentTier < IfrTier.ELITE,
                    onLockedClick = onNavigateToIFR,
                    eliteTier = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // — About ——————————————————————————————————————————
            SettingsSection(title = "About") {
                SettingsItem(label = "Version", value = "0.1.0-alpha")
                SettingsItem(label = "License", value = "GPL-3.0")
                SettingsItem(label = "Platform", value = "StealthX")
            }
        }
    }
}

@Composable
private fun TierSection(tier: IfrTier, onUpgradeClick: () -> Unit) {
    val tierColor = when (tier) {
        IfrTier.FREE -> Color.Gray
        IfrTier.PRO -> StealthXColors.Primary
        IfrTier.ELITE -> Color(0xFFFFD700)
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
                if (tier == IfrTier.FREE) {
                    Spacer(Modifier.height(4.dp))
                    Text("Lock IFR tokens for Pro or Elite access", style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
                }
            }
            if (tier != IfrTier.ELITE) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onUpgradeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = tierColor.copy(alpha = 0.85f))
                ) {
                    Text(if (tier == IfrTier.FREE) "Upgrade" else "Upgrade to Elite", color = Color.Black)
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
    eliteTier: Boolean = false
) {
    val tintColor = when {
        locked -> Color.Gray.copy(alpha = 0.4f)
        eliteTier -> Color(0xFFFFD700)
        else -> StealthXColors.Primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!locked && onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tintColor)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (locked) Color.Gray else StealthXColors.OnSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
        }
        if (locked && onLockedClick != null) {
            TextButton(onClick = onLockedClick) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                Text("Unlock", color = StealthXColors.Primary, style = MaterialTheme.typography.labelSmall)
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
