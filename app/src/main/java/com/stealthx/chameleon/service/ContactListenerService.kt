/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.chameleon.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.prefs.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ContactListenerService : Service() {

    @Inject lateinit var contactExchangeManager: ContactExchangeManager
    @Inject lateinit var appPreferences: AppPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!appPreferences.backgroundListenerEnabled) {
            stopListeningService()
            return
        }
        contactExchangeManager.startListening()
        scope.launch {
            while (true) {
                delay(30_000)
                if (!appPreferences.backgroundListenerEnabled) {
                    stopListeningService()
                    return@launch
                }
                if (!contactExchangeManager.isConnected) contactExchangeManager.startListening()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (appPreferences.backgroundListenerEnabled) {
            START_STICKY
        } else {
            stopListeningService()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        scope.cancel()
        contactExchangeManager.stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): android.app.Notification {
        val channelId = "chameleon_listener"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Contact Exchange",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Chameleon")
            .setContentText("Secure channel active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun stopListeningService() {
        contactExchangeManager.stopListening()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private companion object {
        const val NOTIFICATION_ID = 7332
    }
}
