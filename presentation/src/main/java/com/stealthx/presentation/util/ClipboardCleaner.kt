/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-clears clipboard after 60 seconds to prevent sensitive data leakage.
 */
@Singleton
class ClipboardCleaner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CLEAR_DELAY_MS = 60_000L
        private val CLEAR_TOKEN = Any()
    }

    private val handler = Handler(Looper.getMainLooper())

    fun scheduleClean() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ clearClipboard() }, CLEAR_DELAY_MS)
    }

    fun clearNow() {
        handler.removeCallbacksAndMessages(null)
        clearClipboard()
    }

    private fun clearClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }
}
