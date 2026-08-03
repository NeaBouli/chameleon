/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.exchange

import android.content.Context
import com.stealthx.crypto.ChameleonCrypto
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.shared.SxIdValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactExchangeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactKeyDao: ContactKeyDao,
) {
    private companion object {
        const val SIGNAL_URL = "wss://api.stealthx.tech/signal"
    }

    // Primary api.stealthx.tech SPKI plus the Let's Encrypt R12 intermediate backup.
    private val certPinner = CertificatePinner.Builder()
        .add("api.stealthx.tech", "sha256/1e85xNSEj+dcImOJS0iNkfMZOrZdvJJzzPCqT1/CZDc=")
        .add("api.stealthx.tech", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=")
        .build()

    private val listenClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .certificatePinner(certPinner)
        .build()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile private var listenerWs: WebSocket? = null
    @Volatile private var identified = false
    private val pendingFrames = ConcurrentLinkedQueue<String>()
    private val incomingLock = Mutex()
    private val listenerStateLock = Any()
    private val listenerGeneration = AtomicLong(0L)
    @Volatile private var recoveryPaused = false

    val isConnected: Boolean get() = listenerWs != null

    private fun sendOrQueue(frame: String) {
        if (identified) listenerWs?.send(frame) else pendingFrames.add(frame)
    }

    private fun drainPending(ws: WebSocket) {
        var frame = pendingFrames.poll()
        while (frame != null) { ws.send(frame); frame = pendingFrames.poll() }
    }

    fun sendExchange(toSxId: String) {
        ioScope.launch {
            runCatching {
                val bundle = StealthXIdentity.createQrContent(context)
                sendOrQueue(JSONObject().apply {
                    put("type", "CONTACT_EXCHANGE")
                    put("to", toSxId)
                    put("bundle", bundle)
                }.toString())
            }
        }
    }

    fun startListening() = synchronized(listenerStateLock) {
        if (recoveryPaused) return@synchronized
        if (listenerWs != null) return
        val mySxId = StealthXIdentity.get(context)?.raw ?: return
        val connectionGeneration = listenerGeneration.get()
        val req = Request.Builder().url(SIGNAL_URL).build()
        listenerWs = listenClient.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                if (!isCurrentConnection(ws, connectionGeneration)) return
                ws.send(JSONObject().apply {
                    put("type", "IDENTIFY")
                    put("sxId", mySxId)
                }.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                if (!isCurrentConnection(ws, connectionGeneration)) return
                runCatching {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "IDENTIFY_ACK" -> { identified = true; drainPending(ws) }
                        "CONTACT_EXCHANGE" -> {
                            val bundle = json.optString("bundle")
                            if (bundle.startsWith("stealthx://add/")) {
                                ioScope.launch {
                                    runCatching { parseAndSave(bundle, connectionGeneration) }
                                }
                            }
                        }
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                clearListenerIfCurrent(ws)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                clearListenerIfCurrent(ws)
            }
        })
    }

    private fun isCurrentConnection(ws: WebSocket, generation: Long): Boolean =
        listenerWs === ws && generation == listenerGeneration.get()

    private fun clearListenerIfCurrent(ws: WebSocket) = synchronized(listenerStateLock) {
        if (listenerWs === ws) {
            listenerWs = null
            identified = false
        }
    }

    fun stopListening() = synchronized(listenerStateLock) {
        stopListeningLocked()
    }

    private fun stopListeningLocked() {
        listenerGeneration.incrementAndGet()
        listenerWs?.close(1000, "listener disabled")
        listenerWs = null
        identified = false
        pendingFrames.clear()
    }

    suspend fun <T> withIncomingPaused(block: suspend () -> T): T =
        incomingLock.withLock {
            synchronized(listenerStateLock) {
                recoveryPaused = true
                stopListeningLocked()
            }
            try {
                block()
            } finally {
                // Invalidate frames delivered by a socket while its close was in flight.
                listenerGeneration.incrementAndGet()
            }
        }

    fun resumeAfterRecovery(startListener: Boolean) = synchronized(listenerStateLock) {
        listenerGeneration.incrementAndGet()
        recoveryPaused = false
        if (startListener) startListening()
    }

    private suspend fun parseAndSave(content: String, generation: Long) =
        incomingLock.withLock {
            if (generation != listenerGeneration.get()) return@withLock
            val uri = URI(content)
            val sxId = uri.path.substringAfterLast('/')
            SxIdValidator.requireValid(sxId)

            val params = uri.rawQuery
                ?.split("&")
                ?.associate { part ->
                    val eq = part.indexOf('=')
                    if (eq < 0) part to ""
                    else part.substring(0, eq) to URLDecoder.decode(part.substring(eq + 1), "UTF-8")
                } ?: emptyMap()

            val decoder = Base64.getUrlDecoder()
            val x25519 = decoder.decode(params["x"] ?: return@withLock)
            val ed25519 = decoder.decode(params["e"] ?: return@withLock)
            val signature = decoder.decode(params["s"] ?: return@withLock)
            val createdAt = params["c"]?.toLongOrNull() ?: return@withLock
            val handle = params["h"]?.takeIf { it.isNotEmpty() }

            if (x25519.size != 32 || ed25519.size != 32 || signature.size != 64) return@withLock
            if (!StealthXIdentity.isIdBoundToPublicKey(sxId, ed25519)) return@withLock

            val payload = buildString {
                append(sxId); append("|")
                append(handle ?: ""); append("|")
                append(x25519.joinToString("") { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
                append(ed25519.joinToString("") { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
                append(createdAt.toString())
            }.toByteArray(Charsets.UTF_8)

            val isVerified = runCatching {
                ChameleonCrypto.verify(payload, signature, ed25519)
            }.getOrDefault(false)
            if (!isVerified) return@withLock

            if (contactKeyDao.getById(sxId) != null) return@withLock
            if (generation != listenerGeneration.get()) return@withLock

            contactKeyDao.upsert(
                ContactKeyEntity(
                    id = sxId,
                    displayName = handle ?: sxId,
                    identityKey = ed25519,
                    dhPublicKey = x25519,
                    signature = signature,
                    isVerified = true,
                    createdAt = createdAt,
                    lastUsedAt = null
                )
            )
        }
}
