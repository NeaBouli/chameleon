/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.stealthx.data.NfcWriteRelay
import com.stealthx.data.NfcWriteState
import com.stealthx.data.identity.StealthXId
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.identity.IdentityIntegrityException
import com.stealthx.data.identity.IdentityIntegrityReason
import com.stealthx.data.identity.isUserRecoverable
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.IdentityRecoveryUiState
import com.stealthx.presentation.viewmodel.IdentityRecoveryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyExchangeScreen(
    onBack: () -> Unit,
    onAddContact: () -> Unit,
    recoveryViewModel: IdentityRecoveryViewModel
) {
    val context = LocalContext.current
    var identity by remember { mutableStateOf<StealthXId?>(null) }
    var qrUri by remember { mutableStateOf<String?>(null) }
    var inviteUrl by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var identityUnavailable by remember { mutableStateOf(false) }
    var integrityReason by remember { mutableStateOf<IdentityIntegrityReason?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var identityGeneration by remember { mutableStateOf(0) }
    var showRecoveryWarning by remember { mutableStateOf(false) }
    var showRecoveryConfirmation by remember { mutableStateOf(false) }
    var warningAccepted by remember { mutableStateOf(false) }
    var confirmationText by remember { mutableStateOf("") }
    val nfcState by NfcWriteRelay.state.collectAsState()
    val recoveryState by recoveryViewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { NfcWriteRelay.reset() }
    }

    LaunchedEffect(identityGeneration) {
        data class IdState(
            val id: StealthXId?,
            val uri: String?,
            val url: String?,
            val bitmap: Bitmap?,
            val unavailable: Boolean,
            val reason: IdentityIntegrityReason?
        )
        val result = withContext(Dispatchers.IO) {
            val identityResult = runCatching { StealthXIdentity.getOrCreateWithSeed(context) }
            val id = identityResult.getOrNull()
            val uri = if (id != null) runCatching { StealthXIdentity.createQrContent(context) }.getOrNull() else null
            val url = if (id != null) runCatching { StealthXIdentity.createInviteUrl(context) }.getOrNull() else null
            val bitmap = if (uri != null) runCatching { generateQrBitmap(uri) }.getOrNull() else null
            val reason = (identityResult.exceptionOrNull() as? IdentityIntegrityException)?.reason
            IdState(id, uri, url, bitmap, identityResult.isFailure || (id != null && uri == null), reason)
        }
        identity = result.id
        qrUri = result.uri
        inviteUrl = result.url
        qrBitmap = result.bitmap
        identityUnavailable = result.unavailable
        integrityReason = result.reason
        isLoading = false
    }

    LaunchedEffect(recoveryState) {
        if (recoveryState is IdentityRecoveryUiState.Complete) {
            showRecoveryWarning = false
            showRecoveryConfirmation = false
            warningAccepted = false
            confirmationText = ""
            identityGeneration += 1
            recoveryViewModel.consumeResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Identity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onAddContact,
                        enabled = !identityUnavailable && !isLoading
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add contact")
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
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Your StealthX ID",
                style = MaterialTheme.typography.labelLarge,
                color = StealthXColors.OnSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                identity?.displayId ?: if (isLoading) "…" else "not initialized",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = StealthXColors.Primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            if (identityUnavailable) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "This identity cannot be used safely. It has been preserved and all sharing is disabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "A confirmed reset creates a new ID and permanently removes the custom handle, contacts, messages, secure sessions, and the activation bound to this ID. PINs, Private Zone data, rules, geofences, and app settings stay on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StealthXColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                if (integrityReason?.isUserRecoverable == true) {
                    Button(
                        onClick = { showRecoveryWarning = true },
                        enabled = recoveryState !is IdentityRecoveryUiState.Running,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (recoveryState is IdentityRecoveryUiState.Running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (recoveryState is IdentityRecoveryUiState.Running) "Resetting…" else "Reset identity")
                    }
                } else {
                    Text(
                        "Storage could not be updated safely. Contact support; resetting is disabled.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                if (recoveryState is IdentityRecoveryUiState.Failed) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        (recoveryState as IdentityRecoveryUiState.Failed).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    "Show this QR code to your contact.\nThey scan it in any StealthX app to add you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StealthXColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                Surface(
                    modifier = Modifier.size(220.dp),
                    color = Color.White,
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        when {
                            isLoading -> CircularProgressIndicator(color = StealthXColors.Primary)
                            qrBitmap != null -> Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "StealthX ID QR Code",
                                modifier = Modifier.size(196.dp)
                            )
                            else -> Text(
                                "QR unavailable",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val link = inviteUrl ?: qrUri ?: return@Button
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, link)
                        }
                        context.startActivity(Intent.createChooser(intent, "Invite to Chameleon"))
                    },
                    enabled = qrUri != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StealthXColors.Primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Invite via Secure Link", color = Color.Black)
                }

                Spacer(Modifier.height(8.dp))

                val nfcLabel = when (nfcState) {
                    is NfcWriteState.Pending -> "Hold phone to NFC tag…"
                    is NfcWriteState.Success -> "Written! Tap again to write another"
                    is NfcWriteState.Failure -> "Write failed — tap again to retry"
                    is NfcWriteState.Idle -> "Share via NFC"
                }
                val nfcActive = nfcState is NfcWriteState.Pending
                Button(
                    onClick = {
                        if (nfcState is NfcWriteState.Idle || nfcState is NfcWriteState.Success) {
                            NfcWriteRelay.post(qrUri)
                        } else {
                            NfcWriteRelay.reset()
                        }
                    },
                    enabled = qrUri != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (nfcActive) StealthXColors.Primary.copy(alpha = 0.6f)
                                        else StealthXColors.Surface
                    )
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null,
                        tint = if (nfcActive) Color.Black else StealthXColors.Primary)
                    Spacer(Modifier.width(8.dp))
                    Text(nfcLabel,
                        color = if (nfcActive) Color.Black else StealthXColors.OnSurface)
                }

                if (nfcState is NfcWriteState.Failure) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        (nfcState as NfcWriteState.Failure).reason,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showRecoveryWarning) {
        AlertDialog(
            onDismissRequest = { showRecoveryWarning = false },
            title = { Text("Identity cannot be repaired") },
            text = {
                Column {
                    Text("Reset permanently removes this ID, its custom handle, contacts, messages, secure sessions, and its activation. Your PINs, Private Zone, rules, and settings remain.")
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = warningAccepted, onCheckedChange = { warningAccepted = it })
                        Text("I understand that this data cannot be restored.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRecoveryWarning = false
                        showRecoveryConfirmation = true
                    },
                    enabled = warningAccepted
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryWarning = false }) { Text("Cancel") }
            }
        )
    }

    if (showRecoveryConfirmation) {
        AlertDialog(
            onDismissRequest = { showRecoveryConfirmation = false },
            title = { Text("Confirm permanent reset") },
            text = {
                Column {
                    Text("Type RESET to create a new identity. Your contacts must scan your new code, and paid access must be activated again.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it },
                        label = { Text("Type RESET") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRecoveryConfirmation = false
                        integrityReason?.let(recoveryViewModel::recover)
                    },
                    enabled = confirmationText == "RESET"
                ) { Text("Reset permanently", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

private fun generateQrBitmap(content: String): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
    val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
