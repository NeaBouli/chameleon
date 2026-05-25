/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.transport

import com.stealthx.shared.model.RatchetMessage
import kotlinx.coroutines.flow.Flow

enum class MessengerTransportType(val label: String) {
    BLUETOOTH("Bluetooth"),
    WIFI_DIRECT("WiFi Direct"),
    SERVER_RELAY("Server Relay")
}

sealed class MessengerTransportResult {
    data class Sent(val transportType: MessengerTransportType) : MessengerTransportResult()
    data class Queued(val transportType: MessengerTransportType) : MessengerTransportResult()
    data class Failed(val transportType: MessengerTransportType, val reason: String) : MessengerTransportResult()
}

data class IncomingRatchetMessage(
    val senderContactId: String,
    val message: RatchetMessage,
    val transport: MessengerTransportType
)

interface MessengerTransport {
    val type: MessengerTransportType
    val isAvailable: Boolean

    suspend fun send(recipientId: String, message: RatchetMessage): MessengerTransportResult
    fun observeIncoming(): Flow<IncomingRatchetMessage>
    suspend fun startListening()
    fun stopListening()
}
