/*
 * Chameleon — Bluetooth RFCOMM Transport
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Classic Bluetooth RFCOMM — proximity-based, no internet required.
 * Operates as both server (listening) and client (connecting to paired contacts).
 * UUID is app-specific to avoid conflicts with other Bluetooth services.
 */
package com.stealthx.features.messenger.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Base64
import android.util.Log
import com.stealthx.shared.model.RatchetMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BluetoothTransport"
private val APP_UUID: UUID = UUID.fromString("5E6D7C8A-4B3F-2E1D-0C9B-8A7F6E5D4C3B")
private const val SERVICE_NAME = "ChameleonMessenger"

@Singleton
class BluetoothTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : MessengerTransport {

    override val type = MessengerTransportType.BLUETOOTH

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private val _incoming = Channel<IncomingRatchetMessage>(Channel.BUFFERED)
    private var serverSocket: BluetoothServerSocket? = null
    private var listenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override val isAvailable: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override suspend fun send(recipientId: String, message: RatchetMessage): MessengerTransportResult {
        val adapter = bluetoothAdapter ?: return MessengerTransportResult.Failed(type, "Bluetooth not available")
        // Find paired device matching recipientId (stored as device name or address)
        val device = adapter.bondedDevices.firstOrNull {
            it.name == recipientId || it.address == recipientId
        } ?: return MessengerTransportResult.Failed(type, "Contact not paired via Bluetooth")

        return try {
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(APP_UUID)
            adapter.cancelDiscovery()
            socket.connect()
            val json = ratchetMessageToJson(message, ownId = adapter.address ?: "unknown")
            socket.outputStream.write(json.toByteArray(Charsets.UTF_8))
            socket.outputStream.flush()
            socket.close()
            MessengerTransportResult.Sent(type)
        } catch (e: IOException) {
            Log.w(TAG, "Bluetooth send failed: ${e.message}")
            MessengerTransportResult.Failed(type, e.message ?: "IO error")
        }
    }

    override fun observeIncoming(): Flow<IncomingRatchetMessage> = _incoming.receiveAsFlow()

    @SuppressLint("MissingPermission")
    override suspend fun startListening() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return
        serverSocket = try {
            adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, APP_UUID)
        } catch (e: IOException) {
            Log.w(TAG, "Failed to create server socket: ${e.message}")
            return
        }
        listenJob = scope.launch {
            while (true) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    handleConnection(socket)
                } catch (e: IOException) {
                    Log.w(TAG, "Accept loop ended: ${e.message}")
                    break
                }
            }
        }
    }

    override fun stopListening() {
        listenJob?.cancel()
        try { serverSocket?.close() } catch (_: IOException) {}
        serverSocket = null
    }

    private fun handleConnection(socket: BluetoothSocket) {
        scope.launch {
            try {
                val bytes = socket.inputStream.readBytes()
                socket.close()
                val text = bytes.toString(Charsets.UTF_8)
                val (fromId, message) = ratchetMessageFromJson(text) ?: return@launch
                _incoming.trySend(IncomingRatchetMessage(fromId, message, MessengerTransportType.BLUETOOTH))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read Bluetooth message: ${e.message}")
            }
        }
    }

    private fun ratchetMessageToJson(msg: RatchetMessage, ownId: String): String {
        return JSONObject().apply {
            put("from", ownId)
            put("dhPublicKey", Base64.encodeToString(msg.dhPublicKey, Base64.NO_WRAP))
            put("counter", msg.counter)
            put("prevCounter", msg.prevCounter)
            put("ciphertext", Base64.encodeToString(msg.payload.ciphertext, Base64.NO_WRAP))
            put("nonce", Base64.encodeToString(msg.payload.nonce, Base64.NO_WRAP))
            put("aad", Base64.encodeToString(msg.payload.aad, Base64.NO_WRAP))
            put("paddedLength", msg.payload.paddedLength)
            put("algorithm", msg.payload.algorithm)
            put("version", msg.payload.version)
        }.toString()
    }

    private fun ratchetMessageFromJson(json: String): Pair<String, RatchetMessage>? {
        return try {
            val obj = JSONObject(json)
            val from = obj.getString("from")
            val message = com.stealthx.shared.model.RatchetMessage(
                dhPublicKey = Base64.decode(obj.getString("dhPublicKey"), Base64.NO_WRAP),
                counter = obj.getInt("counter"),
                prevCounter = obj.getInt("prevCounter"),
                payload = com.stealthx.shared.model.EncryptedPayload(
                    ciphertext = Base64.decode(obj.getString("ciphertext"), Base64.NO_WRAP),
                    nonce = Base64.decode(obj.getString("nonce"), Base64.NO_WRAP),
                    aad = Base64.decode(obj.getString("aad"), Base64.NO_WRAP),
                    paddedLength = obj.getInt("paddedLength"),
                    algorithm = obj.getString("algorithm"),
                    version = obj.getInt("version")
                )
            )
            Pair(from, message)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Bluetooth message: ${e.message}")
            null
        }
    }
}
