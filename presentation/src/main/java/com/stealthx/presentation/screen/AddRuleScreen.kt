/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stealthx.domain.rules.TriggerType
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.shared.model.SecurityLevel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRuleScreen(
    onSave: (name: String, type: TriggerType, value: String, level: SecurityLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var triggerType by remember { mutableStateOf(TriggerType.APP) }
    var appPackage by rememberSaveable { mutableStateOf("") }
    var wifiSsid by rememberSaveable { mutableStateOf("") }
    var bluetoothId by rememberSaveable { mutableStateOf("") }
    var timeStart by rememberSaveable { mutableIntStateOf(9) }
    var timeEnd by rememberSaveable { mutableIntStateOf(17) }
    var timeDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var securityLevel by remember { mutableStateOf(SecurityLevel.PRIVATE) }
    var nameError by remember { mutableStateOf(false) }
    var valueError by remember { mutableStateOf(false) }

    fun buildTriggerValue(): String = when (triggerType) {
        TriggerType.APP       -> appPackage.trim()
        TriggerType.WIFI      -> wifiSsid.trim()
        TriggerType.BLUETOOTH -> bluetoothId.trim()
        TriggerType.TIME      -> """{"startHour":$timeStart,"endHour":$timeEnd,"days":[${timeDays.sorted().joinToString(",")}]}"""
        TriggerType.LOCATION  -> ""
    }

    fun validate(): Boolean {
        nameError = name.isBlank()
        valueError = when (triggerType) {
            TriggerType.APP       -> appPackage.isBlank()
            TriggerType.WIFI      -> wifiSsid.isBlank()
            TriggerType.BLUETOOTH -> bluetoothId.isBlank()
            TriggerType.TIME      -> timeDays.isEmpty() || timeStart == timeEnd
            TriggerType.LOCATION  -> false
        }
        return !nameError && !valueError
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("New Rule", style = MaterialTheme.typography.headlineSmall, color = Color.White)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Rule name") },
            isError = nameError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SectionLabel("Trigger")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(TriggerType.APP, TriggerType.WIFI, TriggerType.BLUETOOTH, TriggerType.TIME).forEach { type ->
                FilterChip(
                    selected = triggerType == type,
                    onClick = { triggerType = type; valueError = false },
                    label = { Text(type.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StealthXColors.Primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        when (triggerType) {
            TriggerType.APP -> OutlinedTextField(
                value = appPackage,
                onValueChange = { appPackage = it; valueError = false },
                label = { Text("Package name (e.g. com.whatsapp)") },
                isError = valueError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TriggerType.WIFI -> OutlinedTextField(
                value = wifiSsid,
                onValueChange = { wifiSsid = it; valueError = false },
                label = { Text("Wi-Fi SSID") },
                isError = valueError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TriggerType.BLUETOOTH -> OutlinedTextField(
                value = bluetoothId,
                onValueChange = { bluetoothId = it; valueError = false },
                label = { Text("Bluetooth device address") },
                isError = valueError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TriggerType.TIME -> TimeRuleInput(
                startHour = timeStart,
                endHour = timeEnd,
                days = timeDays,
                isError = valueError,
                onStartChange = { timeStart = it },
                onEndChange = { timeEnd = it },
                onDaysChange = { timeDays = it }
            )
            TriggerType.LOCATION -> Text(
                "Location rules are managed via Geofencing.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        SectionLabel("Security Level")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecurityLevel.entries.forEach { level ->
                val color = when (level) {
                    SecurityLevel.PUBLIC     -> Color(0xFF4CAF50)
                    SecurityLevel.PROTECTED  -> Color(0xFFFFC107)
                    SecurityLevel.PRIVATE    -> Color(0xFFFF5722)
                    SecurityLevel.CAMOUFLAGE -> Color(0xFFF44336)
                }
                FilterChip(
                    selected = securityLevel == level,
                    onClick = { securityLevel = level },
                    label = { Text(level.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (validate()) {
                    onSave(name, triggerType, buildTriggerValue(), securityLevel)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StealthXColors.Primary)
        ) {
            Text("Save Rule", color = Color.Black)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = StealthXColors.OnSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeRuleInput(
    startHour: Int,
    endHour: Int,
    days: Set<Int>,
    isError: Boolean,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onDaysChange: (Set<Int>) -> Unit
) {
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = startHour.toString(),
                onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) onStartChange(h) } },
                label = { Text("Start hour (0-23)") },
                isError = isError,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endHour.toString(),
                onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) onEndChange(h) } },
                label = { Text("End hour (0-23)") },
                isError = isError,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Text("Active days", style = MaterialTheme.typography.labelMedium, color = StealthXColors.OnSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dayLabels.forEachIndexed { idx, label ->
                val day = idx + 1
                FilterChip(
                    selected = day in days,
                    onClick = {
                        onDaysChange(if (day in days) days - day else days + day)
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StealthXColors.Primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }
}
