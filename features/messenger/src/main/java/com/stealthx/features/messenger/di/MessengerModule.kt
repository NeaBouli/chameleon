/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.messenger.di

import com.stealthx.features.messenger.transport.BluetoothTransport
import com.stealthx.features.messenger.transport.MessengerTransport
import com.stealthx.features.messenger.transport.MessengerTransportType
import com.stealthx.features.messenger.transport.ServerRelayTransport
import com.stealthx.features.messenger.transport.WifiDirectTransport
import com.stealthx.data.dao.ChatSessionDao
import com.stealthx.data.ChameleonDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessengerModule {

    @Provides
    @Singleton
    fun provideChatSessionDao(db: ChameleonDatabase): ChatSessionDao = db.chatSessionDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: ChameleonDatabase): com.stealthx.data.dao.MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideTransportMap(
        bluetoothTransport: BluetoothTransport,
        wifiDirectTransport: WifiDirectTransport,
        serverRelayTransport: ServerRelayTransport
    ): Map<MessengerTransportType, @JvmSuppressWildcards MessengerTransport> = mapOf(
        MessengerTransportType.BLUETOOTH to bluetoothTransport,
        MessengerTransportType.WIFI_DIRECT to wifiDirectTransport,
        MessengerTransportType.SERVER_RELAY to serverRelayTransport
    )
}
