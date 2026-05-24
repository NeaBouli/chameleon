/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val Gold = Color(0xFFFFD700)
private val Green = Color(0xFF00FF88)
private val Red = Color(0xFFFF6B6B)
private val Surface = Color(0xFF1A1A2E)
private val SurfaceVariant = Color(0xFF252540)

@Composable
fun MultiDecoyScreen(
    state: MultiDecoyUiState,
    onAddProfile: (name: String, realPin: String, decoyPin: String, confirmDecoyPin: String) -> Unit,
    onRemoveProfile: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddForm by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (!showAddForm) {
                ExtendedFloatingActionButton(
                    onClick = { showAddForm = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Profile") },
                    containerColor = Gold,
                    contentColor = Color.Black
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Surface
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = "Multi-decoy profiles" }
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Multi-Decoy Profiles",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Each decoy profile has its own PIN and shows a clean, empty app. " +
                    "Your real PIN is required to add a new profile, ensuring only you can configure this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                state.statusMessage?.let {
                    Text(it, color = Green, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                state.errorMessage?.let {
                    Text(it, color = Red, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                AnimatedVisibility(visible = showAddForm) {
                    AddProfileForm(
                        isSaving = state.isSaving,
                        onSave = { name, real, decoy, confirm ->
                            onAddProfile(name, real, decoy, confirm)
                            if (state.errorMessage == null) showAddForm = false
                        },
                        onCancel = { showAddForm = false }
                    )
                }
            }

            if (state.profiles.isEmpty() && !showAddForm) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            "No decoy profiles yet.\nTap Add Profile to create one.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    if (state.profiles.isNotEmpty()) {
                        Text(
                            "Active profiles (${state.profiles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Gold
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                items(state.profiles, key = { it.id }) { entry ->
                    ProfileCard(
                        entry = entry,
                        onRemove = { onRemoveProfile(entry.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ProfileCard(
    entry: DecoyProfileEntry,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Gold
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text("PIN configured", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Red)
            }
        }
    }
}

@Composable
private fun AddProfileForm(
    isSaving: Boolean,
    onSave: (name: String, realPin: String, decoyPin: String, confirmDecoyPin: String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var realPin by remember { mutableStateOf("") }
    var decoyPin by remember { mutableStateOf("") }
    var confirmDecoyPin by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "New Decoy Profile",
                style = MaterialTheme.typography.titleMedium,
                color = Gold
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(32) },
                label = { Text("Profile name") },
                placeholder = { Text("e.g. Work, Travel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            PinField(value = realPin, onValueChange = { realPin = it }, label = "Your real PIN (verification)")
            Spacer(Modifier.height(8.dp))
            PinField(value = decoyPin, onValueChange = { decoyPin = it }, label = "New decoy PIN")
            Spacer(Modifier.height(8.dp))
            PinField(value = confirmDecoyPin, onValueChange = { confirmDecoyPin = it }, label = "Confirm decoy PIN")

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel, enabled = !isSaving) {
                    Text("Cancel", color = Color.Gray)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(name, realPin, decoyPin, confirmDecoyPin) },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text(if (isSaving) "Saving..." else "Save", color = Color.Black)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PinField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(12)) },
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
