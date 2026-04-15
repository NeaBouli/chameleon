/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stealthx.presentation.theme.StealthXColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToIFR: () -> Unit) {
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
                .semantics { contentDescription = "Settings screen" }
        ) {
            SettingsSection(title = "Security") {
                SettingsItem(label = "Default Security Level", value = "Protected")
                SettingsItem(label = "Biometric Authentication", value = "Enabled")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "IFR Token") {
                TextButton(onClick = onNavigateToIFR) {
                    Text("Manage IFR Tier", color = StealthXColors.Primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "About") {
                SettingsItem(label = "Version", value = "0.1.0-alpha")
                SettingsItem(label = "License", value = "GPL-3.0")
                SettingsItem(label = "Platform", value = "StealthX")
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
