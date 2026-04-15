/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stealthx.presentation.theme.StealthXColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyExchangeScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Key Exchange") },
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
                .padding(24.dp)
                .semantics { contentDescription = "Key exchange screen" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Share Your Public Key",
                style = MaterialTheme.typography.headlineSmall,
                color = StealthXColors.OnSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Show this QR code to your contact.\nThey scan it to establish an encrypted session.",
                style = MaterialTheme.typography.bodyMedium,
                color = StealthXColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // QR Code placeholder — will use zxing in S-08
            Text(
                "[QR Code]",
                style = MaterialTheme.typography.headlineLarge,
                color = StealthXColors.Primary,
                modifier = Modifier.padding(32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* TODO: launch QR scanner */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StealthXColors.Primary)
            ) {
                Text("Scan Contact's QR Code", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* TODO: NFC tap */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exchange via NFC", color = StealthXColors.Primary)
            }
        }
    }
}
