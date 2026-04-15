/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MessengerScreen(
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { contentDescription = "Encrypted messenger screen" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Encrypted Messenger",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Text(
            "All messages stored locally. No server. No internet.\nExchange keys via QR code or NFC.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onAddContact,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF))
        ) {
            Text("Add Contact via QR", color = Color.Black)
        }
    }
}
