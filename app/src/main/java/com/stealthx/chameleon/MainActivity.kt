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

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.stealthx.data.NfcUriRelay
import com.stealthx.data.NfcWriteRelay
import com.stealthx.features.decoy.screen.DecoyAuthViewModel
import com.stealthx.features.decoy.screen.DecoyModeScreen
import com.stealthx.features.decoy.screen.DecoyUnlockScreen
import com.stealthx.presentation.nav.StealthXNavGraph
import com.stealthx.presentation.theme.StealthXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val decoyAuthViewModel: DecoyAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        handleNfcIntent(intent)

        setContent {
            StealthXTheme {
                val authState by decoyAuthViewModel.uiState.collectAsState()
                when {
                    authState.isDecoyMode -> DecoyModeScreen(onLock = decoyAuthViewModel::lock)
                    authState.requiresUnlock && !authState.isUnlocked -> DecoyUnlockScreen(
                        state = authState,
                        onSubmitPin = decoyAuthViewModel::submitPin
                    )
                    else -> {
                        val navController = rememberNavController()
                        StealthXNavGraph(navController = navController)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        NfcAdapter.getDefaultAdapter(this)?.enableForegroundDispatch(
            this,
            android.app.PendingIntent.getActivity(
                this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        android.app.PendingIntent.FLAG_IMMUTABLE else 0
            ),
            null, null
        )
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
    }

    private fun handleNfcIntent(intent: Intent?) {
        when (intent?.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
                val writeUri = NfcWriteRelay.pendingUri
                if (writeUri != null) {
                    val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                    }
                    if (tag != null) {
                        val ok = tryWriteNdefTag(tag, writeUri)
                        if (ok) NfcWriteRelay.reportSuccess()
                        else NfcWriteRelay.reportFailure(writeUri, "Tag write failed — tag may be read-only or too small")
                    } else {
                        NfcWriteRelay.reportFailure(writeUri, "No writable NFC tag detected")
                    }
                    return
                }
                // Read mode: parse NDEF and route URI to AddContactScreen
                val rawMsgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                }
                val msgs = rawMsgs?.mapNotNull { it as? NdefMessage }
                val uri = msgs?.firstOrNull()?.records?.firstOrNull()
                    ?.let { record ->
                        if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(NdefRecord.RTD_URI)) {
                            record.toUri()?.toString()
                        } else if (record.tnf == NdefRecord.TNF_ABSOLUTE_URI) {
                            String(record.payload, Charsets.UTF_8)
                        } else null
                    }
                if (uri?.startsWith("stealthx://add/") == true) {
                    NfcUriRelay.post(uri)
                }
            }
        }
    }

    private fun tryWriteNdefTag(tag: Tag, uri: String): Boolean {
        val record = NdefRecord.createUri(uri)
        val msg = NdefMessage(arrayOf(record))
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return runCatching {
                ndef.connect()
                try {
                    if (!ndef.isWritable) return@runCatching false
                    if (ndef.maxSize < msg.toByteArray().size) return@runCatching false
                    ndef.writeNdefMessage(msg)
                    true
                } finally {
                    runCatching { ndef.close() }
                }
            }.getOrDefault(false)
        }
        val formatable = NdefFormatable.get(tag) ?: return false
        return runCatching {
            formatable.connect()
            try {
                formatable.format(msg)
                true
            } finally {
                runCatching { formatable.close() }
            }
        }.getOrDefault(false)
    }
}
