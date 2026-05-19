package com.stealthx.presentation.screen

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stealthx.core.permission.PermissionState
import com.stealthx.presentation.theme.StealthXColors
import com.stealthx.presentation.viewmodel.SetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onContinue: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.permissionState.collectAsState()

    // Auto-navigate when all permissions granted
    LaunchedEffect(state.allGranted) {
        if (state.allGranted) onContinue()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chameleon Setup") },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Chameleon needs two system permissions to protect your messages.",
                style = MaterialTheme.typography.bodyLarge,
                color = StealthXColors.OnSurface
            )

            // Android 12+ restricted settings warning
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Android 12+ — Required first step",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Before enabling Accessibility, go to:\n" +
                            "Settings → Apps → Chameleon → ⋮ (three dots) → Allow Restricted Settings\n\n" +
                            "Without this, Accessibility cannot be activated on Android 12+.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Step 1: Accessibility
            SetupStep(
                stepNumber = 1,
                title = "Accessibility Service",
                description = "Required to detect and encrypt text in supported apps. " +
                    "Go to Settings → Accessibility → Installed Services → Chameleon → Enable.",
                granted = state.accessibilityEnabled,
                onAction = {
                    context.startActivity(viewModel.accessibilitySettingsIntent())
                },
                actionLabel = "Open Accessibility Settings"
            )

            // Step 2: Overlay
            SetupStep(
                stepNumber = 2,
                title = "Display Over Other Apps",
                description = "Required to show the encryption overlay on top of chat apps.",
                granted = state.overlayEnabled,
                onAction = {
                    context.startActivity(viewModel.overlaySettingsIntent())
                },
                actionLabel = "Open Overlay Settings"
            )

            Spacer(Modifier.height(8.dp))

            // Continue button (always available as skip)
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = if (state.allGranted)
                    ButtonDefaults.buttonColors(containerColor = StealthXColors.Primary)
                else
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
            ) {
                Text(if (state.allGranted) "Continue to Chameleon" else "Skip for now")
            }

            if (!state.allGranted) {
                Text(
                    "You can enable permissions later in Settings → Getting Started.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StealthXColors.OnSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun SetupStep(
    stepNumber: Int,
    title: String,
    description: String,
    granted: Boolean,
    onAction: () -> Unit,
    actionLabel: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (granted) StealthXColors.Primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (granted) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                } else {
                    Text("$stepNumber", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (!granted) Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
                Text(description, style = MaterialTheme.typography.bodySmall, color = StealthXColors.OnSurface.copy(alpha = 0.7f))
                if (!granted) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onAction) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
