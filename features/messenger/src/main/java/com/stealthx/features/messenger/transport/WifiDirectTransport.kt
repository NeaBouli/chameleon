/*
 * Chameleon — WiFi Direct Transport
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * WiFi Direct P2P transport — proximity-based, no internet or router required.
 * Group Owner opens a TCP server on port 8742. Peers connect directly.
 * Contact discovery uses DNS-SD service records broadcast via WifiP2pDnsSdServiceInfo.
 */
package com.stealthx.features.messenger.transport

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
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
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WifiDirectTransport"
private const val SERVICE_TYPE = "_chameleon._tcp"
private const val TCP_PORT = 8742

@Singleton
class WifiDirectTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : MessengerTransport {

    override val type = MessengerTransportType.WIFI_DIRECT

    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }
    private var channel: WifiP2pManager.Channel? = null

    private val _incoming = Channel<IncomingRatchetMessage>(Channel.BUFFERED)
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // contactId → IP address discovered via DNS-SD
    private val peerAddresses = mutableMapOf<String, String>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            // State changes handled elsewhere; receiver is registered for completeness
        }
    }

    override val isAvailable: Boolean
        get() = manager != null

    @SuppressLint("MissingPermission")
    override suspend fun startListening() {
        val mgr = manager ?: return
        channel = mgr.initialize(context, context.mainLooper, null)
        val ch = channel ?: return

        // Register DNS-SD service so peers can discover us
        val record = mapOf("port" to TCP_PORT.toString())
        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("ChameleonMessenger", SERVICE_TYPE, record)
        mgr.addLocalService(ch, serviceInfo, noOpListener("addLocalService"))

        // Discover peers' services
        mgr.setDnsSdResponseListeners(ch,
            { instanceName, registrationType, device ->
                handleDnsSdTxtRecord(device, emptyMap())
            },
            { fullDomainName, record, device ->
                handleDnsSdTxtRecord(device, record)
            }
        )
        val request = WifiP2pDnsSdServiceRequest.newInstance()
        mgr.addServiceRequest(ch, request, noOpListener("addServiceRequest"))
        mgr.discoverServices(ch, noOpListener("discoverServices"))

        // TCP server loop
        serverJob = scope.launch {
            try {
                val serverSocket = ServerSocket(TCP_PORT)
                while (true) {
                    val socket = serverSocket.accept()
                    handleIncomingSocket(socket)
                }
            } catch (e: IOException) {
                Log.w(TAG, "TCP server ended: ${e.message}")
            }
        }

        context.registerReceiver(receiver, IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION))
    }

    override fun stopListening() {
        serverJob?.cancel()
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        channel?.let { ch ->
            manager?.clearLocalServices(ch, noOpListener("clearLocalServices"))
        }
    }

    override suspend fun send(recipientId: String, message: RatchetMessage): MessengerTransportResult {
        val ip = peerAddresses[recipientId]
            ?: return MessengerTransportResult.Failed(type, "Peer not discovered via WiFi Direct")
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, TCP_PORT), 5000)
            val json = ratchetMessageToJson(message, ownId = recipientId)
            socket.outputStream.write(json.toByteArray(Charsets.UTF_8))
            socket.outputStream.flush()
            socket.close()
            MessengerTransportResult.Sent(type)
        } catch (e: IOException) {
            Log.w(TAG, "WiFi Direct send failed: ${e.message}")
            MessengerTransportResult.Failed(type, e.message ?: "IO error")
        }
    }

    override fun observeIncoming(): Flow<IncomingRatchetMessage> = _incoming.receiveAsFlow()

    private fun handleDnsSdTxtRecord(device: WifiP2pDevice, record: Map<String, String>) {
        // Store device address mapped to a contact identifier
        // In practice, the sxId would be part of the DNS-SD record
        val sxId = record["sxId"] ?: device.deviceAddress
        peerAddresses[sxId] = device.deviceAddress
    }

    private fun handleIncomingSocket(socket: Socket) {
        scope.launch {
            try {
                val bytes = socket.inputStream.readBytes()
                socket.close()
                val text = bytes.toString(Charsets.UTF_8)
                val (fromId, message) = ratchetMessageFromJson(text) ?: return@launch
                _incoming.trySend(IncomingRatchetMessage(fromId, message, MessengerTransportType.WIFI_DIRECT))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read WiFi Direct message: ${e.message}")
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
            Log.w(TAG, "Failed to parse WiFi Direct message: ${e.message}")
            null
        }
    }

    private fun noOpListener(tag: String) = object : ActionListener {
        override fun onSuccess() { Log.d(TAG, "$tag succeeded") }
        override fun onFailure(reason: Int) { Log.w(TAG, "$tag failed: $reason") }
    }
}
