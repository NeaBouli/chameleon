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
import com.stealthx.security.SodiumInitializer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ChameleonApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // CRITICAL: Initialize libsodium FIRST before any crypto operations.
        // Must happen in Application.onCreate(), not lazily.
        SodiumInitializer.ensureInit()

        // Logging — debug builds only. NEVER log in release.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
