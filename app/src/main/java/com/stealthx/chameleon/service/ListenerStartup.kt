package com.stealthx.chameleon.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import timber.log.Timber

object ListenerStartup {
    fun startSafely(context: Context) {
        runCatching {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, ContactListenerService::class.java)
            )
        }.onFailure {
            Timber.e(it, "ContactListenerService start failed; listener remains stopped")
        }
    }
}
