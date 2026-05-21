/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.AddContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onContactAdded: () -> Unit,
    vm: AddContactViewModel = hiltViewModel()
) {
    var qrContent by remember { mutableStateOf("") }
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            qrContent = content
            // Auto-trigger: scan intent == add intent, skip manual button press
            if (content.startsWith("stealthx://add/")) {
                vm.addFromQrContent(content)
            }
        }
    }

    LaunchedEffect(state.contactAdded) {
        if (state.contactAdded) {
            onContactAdded()
            vm.consumeContactAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Contact") },
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
                .semantics { contentDescription = "Add contact screen" },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionCard(
                icon = Icons.Default.QrCodeScanner,
                title = "Scan QR Code",
                subtitle = "Fastest way to add a contact",
                enabled = true,
                onClick = {
                    scanLauncher.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("Scan StealthX contact QR")
                            .setBeepEnabled(false)
                    )
                }
            )
            OptionCard(
                icon = Icons.Default.Nfc,
                title = "NFC Tap",
                subtitle = "Coming soon",
                enabled = false,
                onClick = {}
            )
            OptionCard(
                icon = Icons.Default.Edit,
                title = "Paste QR content",
                subtitle = "stealthx://add/... signed bundle",
                enabled = true,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                    if (pasted.isNotEmpty()) qrContent = pasted
                }
            )

            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = qrContent,
                onValueChange = { qrContent = it },
                label = { Text("Contact QR content") },
                placeholder = { Text("stealthx://add/sx_...?x=...&e=...&s=...&c=...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            state.statusMessage?.let {
                Text(it, color = Color(0xFF00C853), style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { vm.addFromQrContent(qrContent) },
                enabled = !state.isSaving && qrContent.startsWith("stealthx://add/"),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StealthXColors.Primary)
            ) {
                Text(
                    if (state.isSaving) "Adding..." else "Add Contact",
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = StealthXColors.Surface,
            disabledContainerColor = StealthXColors.Surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) StealthXColors.Primary
                       else StealthXColors.OnSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) StealthXColors.OnSurface
                            else StealthXColors.OnSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StealthXColors.OnSurfaceVariant
                )
            }
        }
    }
}
