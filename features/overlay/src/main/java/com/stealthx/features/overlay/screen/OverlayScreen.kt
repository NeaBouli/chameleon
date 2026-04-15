/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.overlay.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OverlayScreen(modifier: Modifier = Modifier) {
    var overlayEnabled by remember { mutableStateOf(true) }

    val whitelistedApps = listOf(
        "WhatsApp" to "com.whatsapp",
        "Telegram" to "org.telegram.messenger",
        "Signal" to "org.thoughtcrime.securesms",
        "Discord" to "com.discord",
        "Gmail" to "com.google.android.gm"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = "Overlay settings screen" }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Overlay Active",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = overlayEnabled,
                onCheckedChange = { overlayEnabled = it },
                modifier = Modifier.semantics { contentDescription = "Toggle overlay" }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Whitelisted Apps",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF00D4FF)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(whitelistedApps) { (name, pkg) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A3B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            Text(pkg, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = true, onCheckedChange = {})
                    }
                }
            }
        }
    }
}
