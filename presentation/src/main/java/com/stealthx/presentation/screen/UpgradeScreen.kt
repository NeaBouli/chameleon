/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.UpgradeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    vm: UpgradeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.connect()
    }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "No browser available for this link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Access") },
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
                .semantics { contentDescription = "Access screen" }
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
                        "Buy Pro or Elite with Google Play. Activation codes still work from Settings.",
                        color = StealthXColors.OnSurface.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { vm.buy(activity, "chameleon_pro_lifetime") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PRO")
                                Text(state.products["chameleon_pro_lifetime"]?.price ?: "EUR 9", color = StealthXColors.Primary)
                                Text("lifetime", color = StealthXColors.OnSurface.copy(alpha = 0.72f))
                            }
                        }
                        OutlinedButton(
                            onClick = { vm.buy(activity, "chameleon_elite_lifetime") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ELITE")
                                Text(state.products["chameleon_elite_lifetime"]?.price ?: "EUR 19", color = Color(0xFFFFD700))
                                Text("lifetime", color = StealthXColors.OnSurface.copy(alpha = 0.72f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { vm.buy(activity, "chameleon_pro_monthly") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pro Monthly")
                        }
                        OutlinedButton(
                            onClick = { vm.buy(activity, "chameleon_elite_monthly") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Elite Monthly")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { vm.buy(activity, "chameleon_elite_activation_code") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buy Elite Activation Code")
                    }
                    TextButton(onClick = vm::restorePurchases, enabled = !state.isConnecting) {
                        Text("Restore Google Play purchases")
                    }
                    TextButton(onClick = { openUrl("https://chameleon.stealthx.tech/#lifetime") }) {
                        Text("Website checkout")
                    }
                    if (state.isConnecting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(state.status, color = StealthXColors.OnSurface.copy(alpha = 0.72f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
