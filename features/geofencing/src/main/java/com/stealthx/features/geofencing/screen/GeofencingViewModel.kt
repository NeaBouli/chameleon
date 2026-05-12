/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.geofencing.screen

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.features.geofencing.engine.GeofenceTransitionReceiver
import com.stealthx.features.geofencing.engine.GeofencingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Base64
import java.util.UUID
import javax.inject.Inject

data class GeofenceZoneUi(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
)

data class GeofencingUiState(
    val zones: List<GeofenceZoneUi> = emptyList(),
    val hasFineLocationPermission: Boolean = false,
    val hasBackgroundLocationPermission: Boolean = false,
    val requiresBackgroundLocationPermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class GeofencingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: GeofencingEngine,
    private val prefs: AppPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GeofencingUiState(
            zones = prefs.geofenceZones.mapNotNull(::decodeZone),
            hasFineLocationPermission = hasFineLocationPermission(),
            hasBackgroundLocationPermission = hasBackgroundLocationPermission()
        )
    )
    val uiState: StateFlow<GeofencingUiState> = _uiState.asStateFlow()

    fun refreshPermissionState() {
        _uiState.value = _uiState.value.copy(
            hasFineLocationPermission = hasFineLocationPermission(),
            hasBackgroundLocationPermission = hasBackgroundLocationPermission()
        )
    }

    fun addGeofence(label: String, latitudeText: String, longitudeText: String, radiusText: String) {
        val cleanLabel = label.trim()
        val latitude = latitudeText.toDoubleOrNull()
        val longitude = longitudeText.toDoubleOrNull()
        val radius = radiusText.toFloatOrNull()

        val error = when {
            !hasFineLocationPermission() -> "Location permission required"
            !hasBackgroundLocationPermission() -> "Background location required for geofencing"
            cleanLabel.isBlank() -> "Name is required"
            latitude == null || latitude !in -90.0..90.0 -> "Latitude must be between -90 and 90"
            longitude == null || longitude !in -180.0..180.0 -> "Longitude must be between -180 and 180"
            radius == null || radius <= 0f -> "Radius is required"
            else -> null
        }
        if (error != null) {
            _uiState.value = _uiState.value.copy(errorMessage = error, statusMessage = null)
            return
        }

        val zone = GeofenceZoneUi(
            id = "geo_${UUID.randomUUID()}",
            label = cleanLabel,
            latitude = latitude!!,
            longitude = longitude!!,
            radiusMeters = maxOf(radius!!, GeofencingEngine.MIN_RADIUS_METERS)
        )

        try {
            engine.addGeofence(
                config = GeofencingEngine.GeofenceConfig(
                    id = zone.id,
                    latitude = zone.latitude,
                    longitude = zone.longitude,
                    radiusMeters = zone.radiusMeters,
                    label = zone.label
                ),
                pendingIntent = geofencePendingIntent()
            )
            val updated = _uiState.value.zones + zone
            prefs.geofenceZones = updated.map(::encodeZone).toSet()
            _uiState.value = GeofencingUiState(
                zones = updated,
                hasFineLocationPermission = true,
                hasBackgroundLocationPermission = hasBackgroundLocationPermission(),
                statusMessage = "Geofence added"
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = e.message ?: "Could not add geofence",
                statusMessage = null
            )
        }
    }

    private fun geofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceTransitionReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    private fun hasFineLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasBackgroundLocationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun encodeZone(zone: GeofenceZoneUi): String =
        listOf(
            zone.id,
            Base64.getUrlEncoder().withoutPadding().encodeToString(zone.label.toByteArray(Charsets.UTF_8)),
            zone.latitude.toString(),
            zone.longitude.toString(),
            zone.radiusMeters.toString()
        ).joinToString("|")

    private fun decodeZone(value: String): GeofenceZoneUi? {
        val parts = value.split("|")
        if (parts.size != 5) return null
        return runCatching {
            GeofenceZoneUi(
                id = parts[0],
                label = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8),
                latitude = parts[2].toDouble(),
                longitude = parts[3].toDouble(),
                radiusMeters = parts[4].toFloat()
            )
        }.getOrNull()
    }
}
