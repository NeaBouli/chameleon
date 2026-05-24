/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stealthx.domain.rules.SecureRule
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.AutomationRulesViewModel

@Composable
fun AutomationRulesScreen(
    onAddRule: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AutomationRulesViewModel = hiltViewModel()
) {
    val rules by vm.rules.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Automation Rules",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    "Rules fire automatically based on context and apply the configured security level.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }

            if (rules.isEmpty()) {
                item {
                    Text(
                        "No rules yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onToggle = { vm.toggleRule(rule) },
                    onDelete = { vm.deleteRule(rule) }
                )
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = onAddRule,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = StealthXColors.Primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add rule", tint = Color.Black)
        }
    }
}

@Composable
private fun RuleCard(
    rule: SecureRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val levelColor = when (rule.securityLevel.colorHex) {
        "#4CAF50" -> Color(0xFF4CAF50)
        "#FFC107" -> Color(0xFFFFC107)
        "#FF5722" -> Color(0xFFFF5722)
        "#F44336" -> Color(0xFFF44336)
        else      -> StealthXColors.Primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StealthXColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${rule.triggerType.name} → ${rule.securityLevel.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = levelColor
                )
                if (rule.lastTriggered != null) {
                    Text(
                        "Triggered ${rule.triggerCount}×",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = StealthXColors.Primary)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete rule", tint = Color(0xFFFF6B6B))
            }
        }
    }
}
