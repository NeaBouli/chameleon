/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.geofencing.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val location = event.triggeringLocation
        val requests = event.triggeringGeofences.orEmpty()
        requests.forEach { geofence ->
            val input = Data.Builder()
                .putString("geofence_id", geofence.requestId)
                .putInt("transition_type", event.geofenceTransition)
                .putDouble("latitude", location?.latitude ?: 0.0)
                .putDouble("longitude", location?.longitude ?: 0.0)
                .build()

            val request = OneTimeWorkRequestBuilder<GeofenceWorker>()
                .setInputData(input)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
