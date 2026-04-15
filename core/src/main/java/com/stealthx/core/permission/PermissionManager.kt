/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core.permission

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Permission state for Chameleon's required system permissions.
 */
data class PermissionState(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean
) {
    val allGranted: Boolean get() = accessibilityEnabled && overlayEnabled
}

/**
 * Manages and monitors system permissions required by Chameleon.
 *
 * Required permissions:
 * 1. BIND_ACCESSIBILITY_SERVICE — for text interception via AccessibilityService
 * 2. SYSTEM_ALERT_WINDOW — for floating overlay display
 *
 * Both require explicit user action in Settings — cannot be granted via runtime dialog.
 *
 * Emits a Flow<PermissionState> that polls every 2 seconds for changes.
 * UI subscribes to this flow to update the permission setup screen.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val POLL_INTERVAL_MS = 2000L
        private const val ACCESSIBILITY_SERVICE_CLASS =
            "com.stealthx.core.accessibility.ChameleonAccessibilityService"
    }

    /**
     * Emit current permission state, polling every 2 seconds.
     * Collect this in a ViewModel to drive the setup screen.
     */
    fun observePermissions(): Flow<PermissionState> = flow {
        while (true) {
            emit(currentState())
            delay(POLL_INTERVAL_MS)
        }
    }

    /**
     * Get current permission state (snapshot, not reactive).
     */
    fun currentState(): PermissionState {
        return PermissionState(
            accessibilityEnabled = isAccessibilityServiceEnabled(),
            overlayEnabled = isOverlayPermissionGranted()
        )
    }

    /**
     * Check if Chameleon's AccessibilityService is enabled.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false

        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )

        val chameleonComponent = ComponentName(context.packageName, ACCESSIBILITY_SERVICE_CLASS)

        return enabledServices.any { info ->
            val serviceComponent = info.resolveInfo?.serviceInfo?.let {
                ComponentName(it.packageName, it.name)
            }
            serviceComponent == chameleonComponent
        }
    }

    /**
     * Check if SYSTEM_ALERT_WINDOW overlay permission is granted.
     */
    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Create an intent to open Accessibility Settings.
     * User must manually enable the service.
     */
    fun accessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Create an intent to open Overlay Permission Settings.
     * User must manually grant SYSTEM_ALERT_WINDOW.
     */
    fun overlaySettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
