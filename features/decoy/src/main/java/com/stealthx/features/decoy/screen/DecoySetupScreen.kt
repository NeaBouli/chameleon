/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.decoy.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun DecoySetupScreen(
    state: DecoySetupUiState,
    onSavePins: (realPin: String, decoyPin: String, confirmDecoyPin: String) -> Unit,
    onDisableDecoy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var realPin by remember { mutableStateOf("") }
    var decoyPin by remember { mutableStateOf("") }
    var confirmDecoyPin by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { contentDescription = "Decoy profile setup" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Decoy Profile",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Configure a decoy PIN that shows a clean, empty profile.\n" +
            "Anyone entering the decoy PIN will see no sensitive data.\n" +
            "Your real data is only accessible with your real PIN.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            if (state.isEnabled) "Status: Enabled" else "Status: Not configured",
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.isEnabled) Color(0xFF00FF88) else Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        PinField(
            value = realPin,
            onValueChange = { realPin = it },
            label = "Real PIN"
        )
        Spacer(modifier = Modifier.height(8.dp))
        PinField(
            value = decoyPin,
            onValueChange = { decoyPin = it },
            label = "Decoy PIN"
        )
        Spacer(modifier = Modifier.height(8.dp))
        PinField(
            value = confirmDecoyPin,
            onValueChange = { confirmDecoyPin = it },
            label = "Confirm decoy PIN"
        )

        state.statusMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Color(0xFF00FF88), style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onSavePins(realPin, decoyPin, confirmDecoyPin) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
        ) {
            Text(if (state.isSaving) "Saving..." else "Save Decoy Profile", color = Color.Black)
        }

        if (state.isEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDisableDecoy,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disable Decoy Profile", color = Color(0xFFFFB800))
            }
        }
    }
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
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
