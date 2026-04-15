/*
 * Chameleon — Core Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core

import android.content.Context
import android.provider.Settings
import com.stealthx.core.permission.PermissionManager
import com.stealthx.core.permission.PermissionState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PermissionManager")
class PermissionManagerTest {

    @Test
    @DisplayName("PermissionState.allGranted is true when both granted")
    fun `allGranted requires both permissions`() {
        val state = PermissionState(accessibilityEnabled = true, overlayEnabled = true)
        assertTrue(state.allGranted)
    }

    @Test
    @DisplayName("PermissionState.allGranted is false when accessibility missing")
    fun `allGranted false without accessibility`() {
        val state = PermissionState(accessibilityEnabled = false, overlayEnabled = true)
        assertFalse(state.allGranted)
    }

    @Test
    @DisplayName("PermissionState.allGranted is false when overlay missing")
    fun `allGranted false without overlay`() {
        val state = PermissionState(accessibilityEnabled = true, overlayEnabled = false)
        assertFalse(state.allGranted)
    }

    @Test
    @DisplayName("PermissionState.allGranted is false when both missing")
    fun `allGranted false when nothing granted`() {
        val state = PermissionState(accessibilityEnabled = false, overlayEnabled = false)
        assertFalse(state.allGranted)
    }

    @Test
    @DisplayName("accessibilitySettingsIntent has correct action")
    fun `accessibility intent opens settings`() {
        assumeTrue(isAndroidAvailable(), "Intent.setFlags not available in JVM")
        val context: Context = mockk(relaxed = true)
        val manager = PermissionManager(context)
        val intent = manager.accessibilitySettingsIntent()
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, intent.action)
    }

    @Test
    @DisplayName("overlaySettingsIntent has correct action")
    fun `overlay intent has correct action`() {
        assumeTrue(isAndroidAvailable(), "Uri.parse needs Android framework")
        val context: Context = mockk(relaxed = true)
        every { context.packageName } returns "com.stealthx.chameleon"
        val manager = PermissionManager(context)
        val intent = manager.overlaySettingsIntent()
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
    }

    private fun isAndroidAvailable(): Boolean {
        return try {
            android.net.Uri.parse("package:test")
            true
        } catch (e: Exception) {
            false
        }
    }
}
