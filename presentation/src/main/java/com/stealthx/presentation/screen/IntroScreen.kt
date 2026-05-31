/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stealthx.presentation.theme.StealthXColors
import kotlinx.coroutines.delay

private const val INTRO_CRAWL = """
CHAMELEON

In a world where every app you open, every permission you grant, every location ping you emit — is tracked, sold, and weaponised against you.

We built something different.

Chameleon is your personal privacy layer. It doesn't just protect you — it adapts with you.

Automated rules that respond to your environment.
Geofenced zones that lock down your most sensitive data.
Decoy profiles that shield your real identity.
An encrypted messenger with no central server.

No telemetry. No analytics. No compromise.

Your device. Your rules.

Welcome to Chameleon.
"""

// NEA-145: Intro or Skip choice on first registration
@Composable
fun IntroScreen(
    onWatchIntro: () -> Unit,
    onSkip: () -> Unit
) {
    var showCrawl by remember { mutableStateOf(false) }

    if (showCrawl) {
        IntroCrawlScreen(onFinished = onWatchIntro)
    } else {
        IntroChoiceScreen(
            onWatchIntro = { showCrawl = true },
            onSkip = onSkip
        )
    }
}

@Composable
private fun IntroChoiceScreen(
    onWatchIntro: () -> Unit,
    onSkip: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = LinearEasing),
        label = "fade_in"
    )

    LaunchedEffect(Unit) {
        delay(400)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CHAMELEON",
                color = StealthXColors.Primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onWatchIntro,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StealthXColors.Primary)
            ) {
                Text("Watch Intro", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StealthXColors.OnSurface)
            ) {
                Text("Skip → Register")
            }
        }
    }
}

@Composable
private fun IntroCrawlScreen(onFinished: () -> Unit) {
    val scrollState = rememberScrollState()
    var crawlStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600)
        crawlStarted = true
    }

    LaunchedEffect(crawlStarted) {
        if (!crawlStarted) return@LaunchedEffect
        // Animate scroll over ~14 seconds
        val totalDistance = 3000
        val steps = 140
        val stepDelay = 100L
        repeat(steps) {
            scrollState.animateScrollTo(
                value = (totalDistance * (it + 1) / steps),
                animationSpec = tween(stepDelay.toInt(), easing = LinearEasing)
            )
            delay(stepDelay)
        }
        delay(1500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top fade gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Black, Color.Transparent))
                )
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp)
                .padding(top = 600.dp, bottom = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = INTRO_CRAWL.trim(),
                color = StealthXColors.Primary.copy(alpha = 0.9f),
                fontSize = 16.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom fade gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                )
                .align(Alignment.BottomCenter)
        )
    }
}
