package com.prempoddar.loadstone.ui.navigate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prempoddar.loadstone.core.format.UnitFormatter
import com.prempoddar.loadstone.data.photo.PhotoStore
import com.prempoddar.loadstone.ui.LocalUnitSystem
import com.prempoddar.loadstone.ui.components.CategoryBadge
import com.prempoddar.loadstone.ui.components.icon
import com.prempoddar.loadstone.ui.components.CompassArrow
import com.prempoddar.loadstone.ui.components.PhotoRow
import com.prempoddar.loadstone.ui.components.PhotoViewerDialog
import com.prempoddar.loadstone.ui.permission.rememberLocationPermissionState
import com.prempoddar.loadstone.ui.util.Haptics
import java.io.File

@Composable
fun NavigateScreen(
    onBack: () -> Unit,
    viewModel: NavigateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val arcHintVisible by viewModel.arcHintVisible.collectAsState()
    val permission = rememberLocationPermissionState()
    val unit = LocalUnitSystem.current
    val view = LocalView.current
    val context = LocalContext.current

    var wasArrived by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    val addPhotos = com.prempoddar.loadstone.ui.components.rememberPhotoAdder { added ->
        Haptics.confirm(view)
        viewModel.addPhotos(added)
    }

    LaunchedEffect(permission.hasPermission) {
        if (permission.hasPermission) viewModel.start()
    }
    LaunchedEffect(state.arrived) {
        if (state.arrived && !wasArrived) Haptics.arrive(view)
        wasArrived = state.arrived
    }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!permission.hasPermission) {
            PermissionGate(onGrant = permission.request, onBack = onBack)
            return@Box
        }

        val pin = state.pin
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            if (arcHintVisible) {
                ArcHintBanner(onDismiss = {
                    Haptics.tap(view)
                    viewModel.dismissArcHint()
                })
                Spacer(Modifier.height(20.dp))
            }

            // Header — what you're finding.
            Text(
                "FINDING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                pin?.name ?: " ",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (pin != null) {
                Spacer(Modifier.height(16.dp))
                CategoryBadge(pin.category, size = 56, icon = pin.icon())
            }

            // Arrow.
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CompassArrow(
                    angleDegrees = state.arrowAngle,
                    arrived = state.arrived,
                    accuracyMeters = state.currentAccuracyMeters,
                )
            }

            if (!state.compassAvailable) {
                CompassNotice("This phone has no compass, so there's no arrow — walk and watch the distance.")
                Spacer(Modifier.height(16.dp))
            } else if (state.compassNeedsCalibration && !state.arrived) {
                CompassNotice("Compass needs calibrating — wave your phone in a figure 8.")
                Spacer(Modifier.height(16.dp))
            }

            // Distance.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.arrived) {
                    Text(
                        "You're here",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (state.waitingForFix) {
                    Text(
                        "Getting a fix",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "step outside for a clear view of the sky",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val parts = state.distanceMeters?.let { UnitFormatter.distanceParts(it, unit) }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            parts?.first ?: "—",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (parts != null) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                parts.second,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                    }
                    Text(
                        when {
                            state.searchHere -> "close — look around you"
                            state.arrowAngle != null -> "ahead"
                            else -> "away"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                state.uncertaintyMeters?.takeIf { state.arrived || state.searchHere }?.let { radius ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "the spot is within ${UnitFormatter.formatDistance(radius, unit)} of you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (state.canRefine) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { Haptics.confirm(view); viewModel.refineFromHere() }) {
                        Text("I'm standing on it — sharpen this pin")
                    }
                } else if (state.refined) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pin sharpened with this visit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                pin?.elevationMeters?.let { elev ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "~${UnitFormatter.formatElevation(elev, unit)} elevation · approximate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // Photos of this place — tap to view full screen, or add another.
            if (pin != null) {
                Spacer(Modifier.height(20.dp))
                PhotoRow(
                    paths = pin.photoPaths,
                    modifier = Modifier.fillMaxWidth(),
                    thumbSize = 72.dp,
                    onAdd = { Haptics.tap(view); addPhotos() },
                    onRemove = { path -> Haptics.tap(view); viewModel.removePhoto(path) },
                    onOpen = { path -> Haptics.tap(view); viewerIndex = pin.photoPaths.indexOf(path) },
                )
            }

            Spacer(Modifier.height(20.dp))

            // Close.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { Haptics.tap(view); onBack() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    viewerIndex?.let { index ->
        PhotoViewerDialog(
            paths = state.pin?.photoPaths.orEmpty(),
            startIndex = index,
            onDismiss = { viewerIndex = null },
        )
    }
}

@Composable
private fun ArcHintBanner(onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "The arc shows where your pin might be — the weaker the GPS, the wider the arc.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompassNotice(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
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
        TextButton(onClick = onGrant) { Text("Grant location access") }
        TextButton(onClick = onBack) { Text("Back") }
    }
}
