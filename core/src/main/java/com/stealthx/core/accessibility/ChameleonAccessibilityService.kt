/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.accessibility.AccessibilityEvent
import com.stealthx.core.ICryptoBridge
import timber.log.Timber

/**
 * ChameleonAccessibilityService — THE EYE, NOT THE BRAIN.
 *
 * This service:
 *   ✅ Reads text from accessibility events
 *   ✅ Writes modified text back via injectText()
 *   ✅ Delegates ALL decisions to CryptoService via AIDL
 *
 *   ❌ NO crypto logic here
 *   ❌ NO rule evaluation here
 *   ❌ NO key storage here
 *   ❌ NO UI decisions here
 *
 * IPC: All decisions made by CryptoService in isolated :crypto process.
 * If AIDL call fails → passthrough (never silently drop user text).
 *
 * Package whitelist in accessibility_service_config.xml — NO wildcards.
 */
class ChameleonAccessibilityService : AccessibilityService() {

    private var cryptoBridge: ICryptoBridge? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            cryptoBridge = ICryptoBridge.Stub.asInterface(service)
            isServiceBound = true
            Timber.d("CryptoService bound successfully")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cryptoBridge = null
            isServiceBound = false
            Timber.w("CryptoService disconnected — rebinding...")
            // Attempt rebind after disconnect
            bindCryptoService()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        bindCryptoService()
        Timber.d("AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val source = event.source ?: return
        val text = source.text?.toString() ?: return
        val packageName = event.packageName?.toString() ?: return

        // DELEGATE TO CRYPTO SERVICE — no decisions here
        val bridge = cryptoBridge
        if (bridge == null || !bridge.isReady) {
            // CryptoService not available — passthrough, never drop text
            return
        }

        try {
            val result = bridge.processText(text, packageName)
            if (result.shouldReplace && result.text != null) {
                injectText(source, result.text)
            }
        } catch (e: Exception) {
            // Remote exception — passthrough (fail open for UX, not security)
            Timber.e(e, "AIDL call to CryptoService failed")
        } finally {
            source.recycle()
        }
    }

    override fun onInterrupt() {
        Timber.w("AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun bindCryptoService() {
        val intent = Intent(this, CryptoService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun injectText(source: android.view.accessibility.AccessibilityNodeInfo, text: String) {
        val args = android.os.Bundle().apply {
            putCharSequence(
                android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        source.performAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )
    }
}
