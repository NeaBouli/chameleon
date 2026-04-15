/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.nav

sealed class Screen(val route: String) {
    data object Dashboard    : Screen("dashboard")
    data object Overlay      : Screen("overlay")
    data object Messenger    : Screen("messenger")
    data object PrivateZone  : Screen("privatezone")
    data object IFRUnlock    : Screen("ifr_unlock")
    data object Settings     : Screen("settings")
    data object KeyExchange  : Screen("key_exchange")
}
