/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.geofencing.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun GeofencingScreen(
    state: GeofencingUiState,
    onPermissionResult: () -> Unit,
    onAddGeofence: (label: String, latitude: String, longitude: String, radius: String) -> Unit,
    onRemoveZone: (id: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var label by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var pendingLocationRequest by remember { mutableStateOf<LocationPermissionRequest?>(null) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult()
    }
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult()
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onPermissionResult()
    }

    pendingLocationRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { pendingLocationRequest = null },
            title = { Text("Location privacy") },
            text = {
                Text(
                    "Chameleon uses location to activate privacy rules when you enter or leave " +
                        "geofence zones you create. For reliable zone changes, this can include " +
                        "location access while the app is closed or not in use. Location is " +
                        "optional, is not stored on our servers, and can be disabled at any time."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLocationRequest = null
                        when (request) {
                            LocationPermissionRequest.FOREGROUND ->
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                            LocationPermissionRequest.BACKGROUND ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    settingsLauncher.launch(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                    )
                                } else {
                                    backgroundPermissionLauncher.launch(
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    )
                                }
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLocationRequest = null }) {
                    Text("Not now")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { contentDescription = "Geofencing settings" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "Geofencing",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                "Automatically change security level based on your location.\nMinimum radius: 100 meters.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!state.hasFineLocationPermission) {
                Button(
                    onClick = {
                        pendingLocationRequest = LocationPermissionRequest.FOREGROUND
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
                ) {
                    Text("Allow Location", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.hasFineLocationPermission &&
                state.requiresBackgroundLocationPermission &&
                !state.hasBackgroundLocationPermission
            ) {
                Text(
                    "Background location required for reliable geofencing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB800),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        pendingLocationRequest = LocationPermissionRequest.BACKGROUND
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
                ) {
                    Text("Allow Background Location", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Zone name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = latitude,
                onValueChange = { latitude = it },
                label = { Text("Latitude") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = longitude,
                onValueChange = { longitude = it },
                label = { Text("Longitude") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text("Radius meters") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.statusMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Color(0xFF00FF88), style = MaterialTheme.typography.bodySmall)
            }
            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onAddGeofence(label, latitude, longitude, radius) },
                enabled = state.hasFineLocationPermission &&
                    (!state.requiresBackgroundLocationPermission || state.hasBackgroundLocationPermission),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
            ) {
                Text("Add Geofence Zone", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Zones",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.zones.isEmpty()) {
            item {
                Text(
                    "No geofence zones configured",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(state.zones, key = { it.id }) { zone ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(zone.label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${zone.latitude}, ${zone.longitude} • ${zone.radiusMeters.toInt()}m",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { onRemoveZone(zone.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove zone",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }
        }
    }
}

private enum class LocationPermissionRequest {
    FOREGROUND,
    BACKGROUND
}
