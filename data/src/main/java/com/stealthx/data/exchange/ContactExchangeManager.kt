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
import java.util.concurrent.TimeUnit
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

    val isConnected: Boolean get() = listenerWs != null

    fun sendExchange(toSxId: String) {
        ioScope.launch {
            runCatching {
                val bundle = StealthXIdentity.createQrContent(context)
                listenerWs?.send(JSONObject().apply {
                    put("type", "CONTACT_EXCHANGE")
                    put("to", toSxId)
                    put("bundle", bundle)
                }.toString())
            }
        }
    }

    fun startListening() {
        if (listenerWs != null) return
        val mySxId = StealthXIdentity.get(context)?.raw ?: return
        val req = Request.Builder().url(SIGNAL_URL).build()
        listenerWs = listenClient.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send(JSONObject().apply {
                    put("type", "IDENTIFY")
                    put("sxId", mySxId)
                }.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    if (json.optString("type") == "CONTACT_EXCHANGE") {
                        val bundle = json.optString("bundle")
                        if (bundle.startsWith("stealthx://add/")) {
                            ioScope.launch { parseAndSave(bundle) }
                        }
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                listenerWs = null
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                listenerWs = null
            }
        })
    }

    private suspend fun parseAndSave(content: String) {
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
        val x25519   = decoder.decode(params["x"] ?: return)
        val ed25519  = decoder.decode(params["e"] ?: return)
        val signature = decoder.decode(params["s"] ?: return)
        val createdAt = params["c"]?.toLongOrNull() ?: return
        val handle    = params["h"]?.takeIf { it.isNotEmpty() }

        if (x25519.size != 32 || ed25519.size != 32 || signature.size != 64) return

        val payload = buildString {
            append(sxId); append("|")
            append(handle ?: ""); append("|")
            append(x25519.joinToString("")  { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
            append(ed25519.joinToString("") { b: Byte -> "%02x".format(b.toInt() and 0xFF) }); append("|")
            append(createdAt.toString())
        }.toByteArray(Charsets.UTF_8)

        val isVerified = runCatching { ChameleonCrypto.verify(payload, signature, ed25519) }.getOrDefault(false)
        if (!isVerified) return

        if (contactKeyDao.getById(sxId) != null) return

        contactKeyDao.upsert(
            ContactKeyEntity(
                id          = sxId,
                displayName = handle ?: sxId,
                identityKey = ed25519,
                dhPublicKey = x25519,
                signature   = signature,
                isVerified  = true,
                createdAt   = createdAt,
                lastUsedAt  = null
            )
        )
    }
}
