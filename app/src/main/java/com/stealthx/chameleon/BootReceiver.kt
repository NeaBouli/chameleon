/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.chameleon

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.stealthx.chameleon.service.ContactListenerService
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.features.geofencing.engine.GeofenceTransitionReceiver
import com.stealthx.features.geofencing.engine.GeofencingEngine
import com.stealthx.shared.model.IfrTier
import com.stealthx.domain.tier.TierGate
import dagger.hilt.android.AndroidEntryPoint
import java.util.Base64
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var geofencingEngine: GeofencingEngine
    @Inject lateinit var tierGate: TierGate

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        // S-05: Restart ContactListenerService (WS keep-alive)
        val serviceIntent = Intent(context, ContactListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // S-08: Re-register geofences if Elite tier (GMS loses them after reboot)
        if (tierGate.getTierSync() >= IfrTier.ELITE) {
            reRegisterGeofences(context)
        }
    }

    private fun reRegisterGeofences(context: Context) {
        val pendingIntent = geofencePendingIntent(context)
        appPreferences.geofenceZones
            .mapNotNull { decodeZone(it) }
            .forEach { zone ->
                geofencingEngine.addGeofenceSilent(
                    config = GeofencingEngine.GeofenceConfig(
                        id = zone.id,
                        latitude = zone.latitude,
                        longitude = zone.longitude,
                        radiusMeters = zone.radiusMeters,
                        label = zone.label
                    ),
                    pendingIntent = pendingIntent
                )
            }
    }

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceTransitionReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    private data class ZoneData(
        val id: String,
        val label: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float
    )

    private fun decodeZone(value: String): ZoneData? {
        val parts = value.split("|")
        if (parts.size != 5) return null
        return runCatching {
            ZoneData(
                id = parts[0],
                label = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8),
                latitude = parts[2].toDouble(),
                longitude = parts[3].toDouble(),
                radiusMeters = parts[4].toFloat()
            )
        }.getOrNull()
    }
}
