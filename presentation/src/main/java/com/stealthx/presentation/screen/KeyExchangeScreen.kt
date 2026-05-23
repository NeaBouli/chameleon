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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.stealthx.presentation.theme.StealthXColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyExchangeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var identity by remember { mutableStateOf<StealthXId?>(null) }
    var qrUri by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val nfcState by NfcWriteRelay.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { NfcWriteRelay.reset() }
    }

    LaunchedEffect(Unit) {
        val (id, uri, bitmap) = withContext(Dispatchers.IO) {
            val id = runCatching { StealthXIdentity.getOrCreateWithSeed(context) }.getOrNull()
            val uri = if (id != null) {
                runCatching { StealthXIdentity.createQrContent(context) }.getOrNull()
            } else null
            val bitmap = if (uri != null) {
                runCatching { generateQrBitmap(uri) }.getOrNull()
            } else null
            Triple(id, uri, bitmap)
        }
        identity = id
        qrUri = uri
        qrBitmap = bitmap
        isLoading = false
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
                    val link = qrUri ?: return@Button
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share StealthX ID"))
                },
                enabled = qrUri != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StealthXColors.Primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share Identity Link", color = Color.Black)
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
