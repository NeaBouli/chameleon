/*
 * Chameleon — Server Relay Transport
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Routes encrypted RatchetMessages over wss://api.stealthx.tech/signal.
 * Wire format is identical to SecureChat's MESSAGE type — cross-app compatible.
 * Server sees only opaque ciphertext; no plaintext ever reaches the relay.
 */
package com.stealthx.features.messenger.transport

import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.network.StealthXApiTls
import com.stealthx.shared.model.RatchetMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import android.content.Context
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ServerRelayTransport"
private const val SIGNAL_URL = "wss://api.stealthx.tech/signal"

@Singleton
class ServerRelayTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : MessengerTransport {

    override val type = MessengerTransportType.SERVER_RELAY

    private val client = StealthXApiTls.newClientBuilder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var _isConnected = false

    private val _incoming = Channel<IncomingRatchetMessage>(Channel.BUFFERED)

    override val isAvailable: Boolean get() = _isConnected

    override suspend fun send(recipientId: String, message: RatchetMessage): MessengerTransportResult {
        val ws = webSocket ?: return MessengerTransportResult.Failed(type, "Not connected")
        val messageId = UUID.randomUUID().toString()
        val payload = ratchetMessageToJson(message)
        val json = JSONObject().apply {
            put("type", "MESSAGE")
            put("to", recipientId)
            put("payload", payload)
            put("messageId", messageId)
        }.toString()
        return if (ws.send(json)) {
            MessengerTransportResult.Queued(type)
        } else {
            MessengerTransportResult.Failed(type, "WebSocket send failed")
        }
    }

    override fun observeIncoming(): Flow<IncomingRatchetMessage> = _incoming.receiveAsFlow()

    override suspend fun startListening() {
        if (_isConnected) return
        val sxId = StealthXIdentity.get(context)?.raw ?: return
        val request = Request.Builder()
            .url("$SIGNAL_URL?sxId=$sxId")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _isConnected = true
                Log.d(TAG, "Connected to signaling relay")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncoming(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                _isConnected = false
                ws.close(1000, null)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _isConnected = false
                Log.w(TAG, "Relay connection failed: ${t.message}")
            }
        })
    }

    override fun stopListening() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _isConnected = false
    }

    private fun handleIncoming(text: String) {
        try {
            val json = JSONObject(text)
            if (json.optString("type") != "MESSAGE") return
            val from = json.optString("from").takeIf { it.isNotEmpty() } ?: return
            val payload = json.optString("payload").takeIf { it.isNotEmpty() } ?: return
            val message = ratchetMessageFromJson(payload) ?: return
            _incoming.trySend(IncomingRatchetMessage(from, message, MessengerTransportType.SERVER_RELAY))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse incoming relay message: ${e.message}")
        }
    }

    private fun ratchetMessageToJson(msg: RatchetMessage): String {
        return JSONObject().apply {
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

    private fun ratchetMessageFromJson(json: String): RatchetMessage? {
        return try {
            val obj = JSONObject(json)
            com.stealthx.shared.model.RatchetMessage(
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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize RatchetMessage: ${e.message}")
            null
        }
    }
}
