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
package com.stealthx.core.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the floating overlay window for displaying encrypted/decrypted text.
 *
 * Uses TYPE_APPLICATION_OVERLAY (API 26+).
 * Requires SYSTEM_ALERT_WINDOW permission (checked before showing).
 *
 * SECURITY RULES:
 * - No overlay over lockscreen (FLAG_SHOW_WHEN_LOCKED not set)
 * - FLAG_NOT_FOCUSABLE: overlay does not steal input
 * - FLAG_NOT_TOUCH_MODAL: touches pass through to underlying app
 * - FLAG_SECURE: overlay content cannot be screenshot
 * - Overlay is dismissed when app loses accessibility focus
 */
@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var currentOverlayView: View? = null
    private var isOverlayShowing = false

    /**
     * Show the overlay with the given view.
     *
     * @param view    The view to display as overlay
     * @param gravity Gravity for positioning (default: bottom-center)
     * @param yOffset Y offset from gravity position in pixels
     */
    fun show(view: View, gravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, yOffset: Int = 200) {
        if (!canDrawOverlay()) return

        dismiss()

        val params = createLayoutParams(gravity, yOffset)

        try {
            windowManager.addView(view, params)
            currentOverlayView = view
            isOverlayShowing = true
        } catch (e: WindowManager.BadTokenException) {
            // Permission revoked between check and add
            isOverlayShowing = false
        }
    }

    /**
     * Dismiss the current overlay if visible.
     */
    fun dismiss() {
        val view = currentOverlayView ?: return
        try {
            if (isOverlayShowing) {
                windowManager.removeView(view)
            }
        } catch (e: IllegalArgumentException) {
            // View not attached — already removed
        } finally {
            currentOverlayView = null
            isOverlayShowing = false
        }
    }

    /**
     * Update the overlay position.
     */
    fun updatePosition(gravity: Int, yOffset: Int) {
        val view = currentOverlayView ?: return
        if (!isOverlayShowing) return

        val params = createLayoutParams(gravity, yOffset)
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            // View not attached
        }
    }

    fun isShowing(): Boolean = isOverlayShowing

    /**
     * Check if we have SYSTEM_ALERT_WINDOW permission.
     */
    fun canDrawOverlay(): Boolean {
        return android.provider.Settings.canDrawOverlays(context)
    }

    private fun createLayoutParams(gravity: Int, yOffset: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_FOCUSABLE: does not steal input
            // FLAG_NOT_TOUCH_MODAL: touches pass through
            // FLAG_SECURE: cannot be screenshot
            // NOT using FLAG_SHOW_WHEN_LOCKED: no overlay on lockscreen
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.y = yOffset
        }
    }
}
