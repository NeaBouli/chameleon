/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.shared.model.SecurityLevel

@Composable
fun SecurityLevelIndicator(
    level: SecurityLevel,
    modifier: Modifier = Modifier
) {
    val targetColor = when (level) {
        SecurityLevel.PUBLIC -> StealthXColors.LevelPublic
        SecurityLevel.PROTECTED -> StealthXColors.LevelProtected
        SecurityLevel.PRIVATE -> StealthXColors.LevelPrivate
        SecurityLevel.CAMOUFLAGE -> StealthXColors.LevelCamouflage
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600),
        label = "security_level_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics {
            contentDescription = "Security level: ${level.displayName}"
        }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = level.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = animatedColor
        )
    }
}
