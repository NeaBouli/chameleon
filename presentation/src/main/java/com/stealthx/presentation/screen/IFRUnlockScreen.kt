/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stealthx.presentation.theme.StealthXColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IFRUnlockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

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
                .padding(16.dp)
                .semantics { contentDescription = "IFR unlock screen" }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StealthXColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Chameleon Access", color = StealthXColors.OnSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "WalletConnect was removed from the Android app. Buy normally or verify IFR in the browser for a 50% Stripe discount.",
                        color = StealthXColors.OnSurface.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { openUrl("https://chameleon.stealthx.tech/#ifr") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify IFR for 50% Stripe Discount", color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { openUrl("https://app.uniswap.org/explore/tokens/ethereum/0x77e99917Eca8539c62F509ED1193ac36580A6e7B") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buy IFR on Uniswap")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { openUrl("https://chameleon.stealthx.tech/#lifetime") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buy with Card (Stripe)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
