package com.prempoddar.loadstone.ui.navigate

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prempoddar.loadstone.core.format.CoordinateFormat
import com.prempoddar.loadstone.core.format.UnitFormatter
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.ui.LocalUnitSystem
import com.prempoddar.loadstone.ui.components.CompassArrow
import com.prempoddar.loadstone.ui.permission.rememberLocationPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigateScreen(
    onBack: () -> Unit,
    viewModel: NavigateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val permission = rememberLocationPermissionState()
    val unit = LocalUnitSystem.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val view = LocalView.current

    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var wasArrived by remember { mutableStateOf(false) }

    LaunchedEffect(permission.hasPermission) {
        if (permission.hasPermission) viewModel.start()
    }
    LaunchedEffect(state.arrived) {
        if (state.arrived && !wasArrived) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        wasArrived = state.arrived
    }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.pin?.name ?: "Navigate") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val pin = state.pin
                    if (pin != null) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = { menuOpen = false; sharePin(context, pin) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { menuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!permission.hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Loadstone needs location access to point you to this pin.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = permission.request) { Text("Grant location access") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CompassArrow(angleDegrees = state.arrowAngle, arrived = state.arrived)
                if (state.arrived) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E9E5B),
                        modifier = Modifier.size(72.dp),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.arrived) {
                    Text(
                        "You've arrived",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E9E5B),
                    )
                } else {
                    Text(
                        state.distanceMeters?.let { UnitFormatter.formatDistance(it, unit) } ?: "—",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "to " + (state.pin?.name ?: "pin"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                state.currentAccuracyMeters?.let { acc ->
                    AssistChip(
                        onClick = {},
                        label = { Text("GPS ±${acc.toInt()} m") },
                        colors = AssistChipDefaults.assistChipColors(),
                    )
                }
                AnimatedVisibility(visible = state.compassAccuracyClass <= 1) {
                    Text(
                        "Compass needs calibration — wave your phone in a figure 8.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete pin?") },
            text = { Text("\"${state.pin?.name}\" will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun sharePin(context: android.content.Context, pin: Pin) {
    val coords = CoordinateFormat.format(pin.latitude, pin.longitude)
    val text = "${pin.name} — $coords\ngeo:${pin.latitude},${pin.longitude}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, pin.name)
    }
    context.startActivity(Intent.createChooser(intent, "Share pin"))
}
