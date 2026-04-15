/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stealthx.ifr.compose.IFRUnlockSheet
import com.stealthx.ifr.compose.TierStatusCard
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.IFRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IFRUnlockScreen(viewModel: IFRViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IFR Token") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StealthXColors.Background,
                    titleContentColor = StealthXColors.OnSurface
                )
            )
        },
        containerColor = StealthXColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .semantics { contentDescription = "IFR unlock screen" }
        ) {
            TierStatusCard(
                tier = state.tier,
                ifrBalance = state.lockedAmount,
                walletAddress = state.walletAddress,
                expiresIn = state.expiresIn
            )

            Spacer(modifier = Modifier.height(16.dp))

            IFRUnlockSheet(
                onWalletConnectClicked = { viewModel.connectWallet() },
                onManualAddressSubmit = { viewModel.verifyManualAddress(it) },
                isVerifying = state.isVerifying,
                error = state.error
            )
        }
    }
}
