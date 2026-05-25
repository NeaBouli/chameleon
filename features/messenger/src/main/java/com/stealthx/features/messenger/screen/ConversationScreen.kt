/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stealthx.features.messenger.repository.DecryptedMessage
import com.stealthx.features.messenger.transport.MessengerTransportType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val contactName = uiState.contactName.ifEmpty { viewModel.contactId }
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) { viewModel.markRead() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(contactName, color = Color.White)
                        Text(
                            "E2E Encrypted · ${uiState.selectedTransport.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117)
                )
            )
        },
        containerColor = Color(0xFF0D1117)
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }

            if (uiState.sendError != null) {
                Text(
                    uiState.sendError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            MessageInput(
                text = inputText,
                onTextChange = { inputText = it },
                selectedTransport = uiState.selectedTransport,
                onTransportChange = viewModel::selectTransport,
                isSending = uiState.isSending,
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.send(inputText)
                        inputText = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: DecryptedMessage) {
    val isOutgoing = message.isOutgoing
    val bubbleColor = if (isOutgoing) Color(0xFF1C3A5C) else Color(0xFF1E1E2E)
    val transportIcon = when (message.transportType) {
        MessengerTransportType.BLUETOOTH -> "BT"
        MessengerTransportType.WIFI_DIRECT -> "WiFi"
        MessengerTransportType.SERVER_RELAY -> "Relay"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isOutgoing) 12.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(message.text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTime(message.timestamp),
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        " · $transportIcon",
                        color = Color(0xFF00D4FF),
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (isOutgoing) {
                        Text(
                            " · ${deliveryLabel(message.deliveryStatus)}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    selectedTransport: MessengerTransportType,
    onTransportChange: (MessengerTransportType) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22))
            .padding(8.dp)
    ) {
        // Transport picker
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransportChip(
                label = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                selected = selectedTransport == MessengerTransportType.BLUETOOTH,
                onClick = { onTransportChange(MessengerTransportType.BLUETOOTH) }
            )
            TransportChip(
                label = "WiFi Direct",
                icon = Icons.Default.Wifi,
                selected = selectedTransport == MessengerTransportType.WIFI_DIRECT,
                onClick = { onTransportChange(MessengerTransportType.WIFI_DIRECT) }
            )
            TransportChip(
                label = "Relay",
                icon = Icons.Default.CloudQueue,
                selected = selectedTransport == MessengerTransportType.SERVER_RELAY,
                onClick = { onTransportChange(MessengerTransportType.SERVER_RELAY) }
            )
        }
        // Message input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…", color = Color.Gray) },
                singleLine = false,
                maxLines = 4,
                enabled = !isSending,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00D4FF),
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = Color(0xFF00D4FF)
                )
            )
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isSending
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank() && !isSending) Color(0xFF00D4FF) else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun TransportChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF00D4FF).copy(alpha = 0.15f) else Color.Transparent
    val border = if (selected) Color(0xFF00D4FF) else Color(0xFF333333)
    val textColor = if (selected) Color(0xFF00D4FF) else Color.Gray

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.padding(end = 4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun deliveryLabel(status: String): String = when (status) {
    "QUEUED" -> "⏳"
    "SENT" -> "✓"
    "FAILED" -> "✗"
    else -> ""
}
