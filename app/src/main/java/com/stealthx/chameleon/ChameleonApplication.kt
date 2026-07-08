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
package com.stealthx.chameleon

import android.app.Application
import android.content.Intent
import com.stealthx.chameleon.service.ContactListenerService
import com.stealthx.crypto.SodiumInitializer
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.shared.model.AccessTier
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ChameleonApplication : Application() {
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()

        // CRITICAL: Initialize libsodium FIRST before any crypto operations.
        // Must happen in Application.onCreate(), not lazily.
        SodiumInitializer.ensureInit()

        // Create per-device identity on first launch — idempotent on subsequent launches
        StealthXIdentity.getOrCreateWithSeed(this)

        val allowDevTierOverride = BuildConfig.DEBUG
        com.stealthx.shared.DevTierOverride.forcedTier =
            BuildConfig.FORCED_TIER
                .takeIf { allowDevTierOverride && it.isNotBlank() }
                ?.let { AccessTier.valueOf(it) }
        com.stealthx.shared.DevTierOverride.forceElite = allowDevTierOverride && BuildConfig.FORCE_ELITE

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        if (appPreferences.backgroundListenerEnabled) {
            startForegroundService(Intent(this, ContactListenerService::class.java))
        }
    }
}
