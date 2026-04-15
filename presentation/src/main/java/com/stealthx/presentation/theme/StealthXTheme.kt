/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = StealthXColors.Primary,
    onPrimary = StealthXColors.OnPrimary,
    primaryContainer = StealthXColors.PrimaryDark,
    secondary = StealthXColors.TierElite,
    secondaryContainer = StealthXColors.SurfaceVariant,
    background = StealthXColors.Background,
    surface = StealthXColors.Surface,
    surfaceVariant = StealthXColors.SurfaceVariant,
    onBackground = StealthXColors.OnSurface,
    onSurface = StealthXColors.OnSurface,
    onSurfaceVariant = StealthXColors.OnSurfaceVariant,
    error = StealthXColors.Error,
    onError = StealthXColors.OnError
)

/**
 * StealthX Design System Theme.
 * Dark mode is the ONLY mode — privacy-first UX.
 */
@Composable
fun StealthXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = StealthXTypography,
        content = content
    )
}
