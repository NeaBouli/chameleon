/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.shared.model.IfrTier

@Composable
fun TierBadge(tier: IfrTier, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (tier) {
        IfrTier.FREE -> StealthXColors.Disabled to StealthXColors.TierFree
        IfrTier.PRO -> StealthXColors.PrimaryDark to Color.White
        IfrTier.ELITE -> Color(0xFF3D2E00) to StealthXColors.TierElite
    }

    Text(
        text = tier.name,
        style = MaterialTheme.typography.labelLarge,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = "IFR Tier: ${tier.name}" }
    )
}
