package com.prempoddar.loadstone.ui.drop

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prempoddar.loadstone.ui.components.AccuracyDial
import com.prempoddar.loadstone.ui.permission.rememberLocationPermissionState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropPinScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: DropPinViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val permission = rememberLocationPermissionState()
    val haptics = LocalHapticFeedback.current
    var showSheet by remember { mutableStateOf(false) }
    var wasStable by remember { mutableStateOf(false) }

    LaunchedEffect(permission.hasPermission) {
        if (permission.hasPermission) viewModel.startMeasuring()
    }
    LaunchedEffect(state.stabilized) {
        if (state.stabilized && !wasStable) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        wasStable = state.stabilized
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Drop a pin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (!permission.hasPermission) {
            PermissionPrompt(
                onGrant = permission.request,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(8.dp))
            AccuracyDial(
                accuracyMeters = state.fix?.estAccuracyMeters,
                stabilized = state.stabilized,
            ) {
                DialCenter(
                    accuracyMeters = state.fix?.estAccuracyMeters,
                    sampleCount = state.fix?.sampleCount ?: 0,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val statusColor by animateColorAsState(
                    if (state.stabilized) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "status",
                )
                Text(
                    text = when {
                        state.fix == null -> "Finding your position…"
                        state.stabilized -> "Great accuracy — ready to save"
                        else -> "Hold still — getting more precise…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The longer you wait, the sharper it gets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = { showSheet = true },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Save pin here")
            }
        }
    }

    if (showSheet) {
        SavePinSheet(
            onDismiss = { showSheet = false },
            onSave = { name, note, category ->
                showSheet = false
                viewModel.savePin(name, note, category, onSaved)
            },
        )
    }
}

@Composable
private fun DialCenter(accuracyMeters: Float?, sampleCount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (accuracyMeters == null) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "±${accuracyMeters.roundToInt()}",
                fontSize = 56.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text("meters", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "$sampleCount readings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Location needed", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Loadstone uses your location only while the app is open, to save and find your pins.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onGrant) { Text("Grant location access") }
    }
}
