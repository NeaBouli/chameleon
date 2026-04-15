/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core.accessibility

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.stealthx.core.ICryptoBridge
import com.stealthx.core.ProcessTextResult
import com.stealthx.crypto.SodiumInitializer

/**
 * CryptoService — runs in isolated :crypto process.
 *
 * Handles ALL crypto decisions: encryption, decryption, rule evaluation.
 * The AccessibilityService communicates with this via AIDL only.
 *
 * CRITICAL: SodiumInitializer.ensureInit() must be called here too —
 * each Android process needs its own JNI initialization.
 */
class CryptoService : Service() {

    override fun onCreate() {
        super.onCreate()
        // Each process needs its own JNI init
        SodiumInitializer.ensureInit()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val binder = object : ICryptoBridge.Stub() {

        override fun processText(text: String?, packageName: String?): ProcessTextResult {
            // TODO S-05: Implement encryption/decryption logic via EncryptionEngine
            // For now: passthrough — no replacement
            return ProcessTextResult(shouldReplace = false, text = null)
        }

        override fun notifySecurityLevel(level: Int) {
            // TODO S-05: Update current security level from RuleEngine
        }

        override fun isReady(): Boolean {
            return SodiumInitializer.isInitialized()
        }
    }
}
