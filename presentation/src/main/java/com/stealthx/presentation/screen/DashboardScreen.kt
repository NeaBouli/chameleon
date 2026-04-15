/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stealthx.presentation.composable.SecurityLevelIndicator
import com.stealthx.presentation.composable.TierBadge
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToKeyExchange: () -> Unit,
    onNavigateToIFR: () -> Unit,
    onNavigateToOverlay: () -> Unit,
    onNavigateToMessenger: () -> Unit
) {
    val tier by viewModel.currentTier.collectAsState()
    val securityLevel by viewModel.securityLevel.collectAsState()
    val activeRules by viewModel.activeRules.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureChat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StealthXColors.Background,
                    titleContentColor = StealthXColors.OnSurface
                ),
                actions = {
                    TierBadge(tier = tier, modifier = Modifier.semantics {
                        contentDescription = "Current tier: ${tier.name}"
                    })
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        containerColor = StealthXColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SecurityLevelIndicator(level = securityLevel)

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(icon = Icons.Default.Shield, label = "Overlay", onClick = onNavigateToOverlay)
                QuickAction(icon = Icons.Default.Message, label = "Messenger", onClick = onNavigateToMessenger)
                QuickAction(icon = Icons.Default.Key, label = "Keys", onClick = onNavigateToKeyExchange)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active Rules", style = MaterialTheme.typography.titleMedium, color = StealthXColors.OnSurface)
                TextButton(onClick = onNavigateToIFR) {
                    Text("IFR Status", color = StealthXColors.Primary)
                }
            }

            LazyColumn {
                items(activeRules) { rule ->
                    RuleCard(ruleName = rule.name, triggerType = rule.triggerType.name, securityLevel = rule.securityLevel.displayName)
                }
                if (activeRules.isEmpty()) {
                    item {
                        Text(
                            "No active rules. Add rules in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StealthXColors.OnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = StealthXColors.Primary)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
    }
}

@Composable
private fun RuleCard(ruleName: String, triggerType: String, securityLevel: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = StealthXColors.SurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(ruleName, style = MaterialTheme.typography.bodyLarge, color = StealthXColors.OnSurface)
                Text(triggerType, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurfaceVariant)
            }
            Text(securityLevel, style = MaterialTheme.typography.labelLarge, color = StealthXColors.Primary)
        }
    }
}
