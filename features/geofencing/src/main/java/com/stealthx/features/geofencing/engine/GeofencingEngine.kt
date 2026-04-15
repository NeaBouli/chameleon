/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.geofencing.engine

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Geofencing Engine — Fused Location + Geofence API.
 *
 * RULES:
 * - Minimum radius: 100m (smaller causes excessive battery drain)
 * - Uses WorkManager for background processing, not foreground service
 * - Triggers RuleEngine via TriggerContext(latitude, longitude)
 * - Requires ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION (Android 11+)
 */
@Singleton
class GeofencingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val MIN_RADIUS_METERS = 100f
        const val GEOFENCE_EXPIRATION_MS = Geofence.NEVER_EXPIRE
    }

    private val geofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    data class GeofenceConfig(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
        val label: String
    )

    /**
     * Register a geofence with the system.
     * Minimum radius enforced at 100m.
     */
    @SuppressLint("MissingPermission")
    fun addGeofence(config: GeofenceConfig, pendingIntent: PendingIntent) {
        val radius = maxOf(config.radiusMeters, MIN_RADIUS_METERS)

        val geofence = Geofence.Builder()
            .setRequestId(config.id)
            .setCircularRegion(config.latitude, config.longitude, radius)
            .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, pendingIntent)
    }

    fun removeGeofence(geofenceId: String) {
        geofencingClient.removeGeofences(listOf(geofenceId))
    }

    fun removeAllGeofences() {
        geofencingClient.removeGeofences(emptyList<String>())
    }
}
