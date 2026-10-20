package com.prempoddar.loadstone.ui.navigate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
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
                modifier = Modifier.fillMaxWidth().height(310.dp),
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
                // Where you are and where the pin is, so a climb or a descent ahead is no surprise.
                val here = state.elevationMeters
                val there = pin?.elevationMeters
                if (here != null || there != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOfNotNull(
                            here?.let { "You ~${UnitFormatter.formatElevation(it, unit)}" },
                            there?.let { "Pin ~${UnitFormatter.formatElevation(it, unit)}" },
                            if (here != null && there != null && kotlin.math.abs(there - here) >= 3) {
                                val d = there - here
                                (if (d > 0) "climb " else "descend ") + UnitFormatter.formatElevation(kotlin.math.abs(d), unit)
                            } else null,
                        ).joinToString(" · ") + " · above sea level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            if (pin != null) {
                Spacer(Modifier.height(16.dp))
                WaysBack(pin, state, onRetrace = viewModel::setRetrace, onReload = viewModel::reloadPin)
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

/**
 * How to get back: straight at the pin or along the recorded path (drawn, with you on it), and the
 * screen-off helpers: guiding by notification, and recording the way you walk from here.
 */
@Composable
private fun WaysBack(
    pin: com.prempoddar.loadstone.data.pin.Pin,
    state: NavigateUiState,
    onRetrace: (Boolean) -> Unit,
    onReload: () -> Unit,
) {
    val context = LocalContext.current
    val unit = LocalUnitSystem.current
    val active by com.prempoddar.loadstone.guide.GuideService.active.collectAsState()
    val mine = active?.takeIf { it.pinId == pin.id }
    // A finished recording changes the pin's path: show it as soon as it's saved.
    LaunchedEffect(mine?.recording) { if (mine?.recording != true) { kotlinx.coroutines.delay(600); onReload() } }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { pending?.invoke(); pending = null }
    // The notification is how these work with the screen off, so ask to show it first (Android 13+).
    fun withNotifications(go: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) { pending = go; askNotify.launch(android.Manifest.permission.POST_NOTIFICATIONS) } else go()
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (pin.trail.size >= 2) {
            val length = com.prempoddar.loadstone.core.geo.Trail.length(pin.trail)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !state.retracing, onClick = { onRetrace(false) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Straight line") }
                SegmentedButton(selected = state.retracing, onClick = { onRetrace(true) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text("My path · ${UnitFormatter.formatDistance(length, unit)}")
                }
            }
            if (state.retracing) {
                PathPicture(pin, state.here, Modifier.fillMaxWidth().height(150.dp))
                state.offTrailMeters?.takeIf { it > com.prempoddar.loadstone.core.geo.Trail.STRAYED_METERS }?.let {
                    Text(
                        "You're ${UnitFormatter.formatDistance(it, unit)} off your path — the arrow leads back to it.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mine?.guiding == true) {
                FilledTonalButton(onClick = { com.prempoddar.loadstone.guide.GuideService.stop(context) }, modifier = Modifier.weight(1f)) { Text("Stop guiding") }
            } else {
                FilledTonalButton(
                    onClick = { withNotifications { com.prempoddar.loadstone.guide.GuideService.guide(context, pin.id) } },
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Notifications, null, Modifier.size(18.dp)); Text("  Screen off", maxLines = 1) }
            }
            if (mine?.recording == true) {
                FilledTonalButton(onClick = { com.prempoddar.loadstone.guide.GuideService.stop(context) }, modifier = Modifier.weight(1f)) { Text("Stop recording") }
            } else {
                OutlinedButton(
                    onClick = { withNotifications { com.prempoddar.loadstone.guide.GuideService.record(context, pin.id) } },
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Route, null, Modifier.size(18.dp)); Text(if (pin.trail.size >= 2) "  Record again" else "  Record my way", maxLines = 1) }
            }
        }
        Text(
            when {
                mine?.recording == true -> "Recording the way you walk from here. Turn the screen off; stop when you've arrived where you're going."
                mine?.guiding == true -> "Guiding from the notification: turn the screen off and it buzzes when you're there."
                else -> "Screen off: distance and direction in a notification, with a buzz on arrival. Record my way: save your route from here to retrace it later."
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** The recorded path from the pin, with the pin and you marked, fitted to the box with north up. */
@Composable
private fun PathPicture(pin: com.prempoddar.loadstone.data.pin.Pin, here: com.prempoddar.loadstone.core.geo.TrailPoint?, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val pinColor = MaterialTheme.colorScheme.tertiary
    val youColor = MaterialTheme.colorScheme.onSurface
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    androidx.compose.foundation.Canvas(modifier.clip(MaterialTheme.shapes.large).background(bg)) {
        val points = pin.trail + listOfNotNull(here)
        val lat0 = points.minOf { it.latitude }; val lat1 = points.maxOf { it.latitude }
        val lon0 = points.minOf { it.longitude }; val lon1 = points.maxOf { it.longitude }
        val k = kotlin.math.cos(Math.toRadians((lat0 + lat1) / 2))
        val spanX = ((lon1 - lon0) * k).coerceAtLeast(1e-6); val spanY = (lat1 - lat0).coerceAtLeast(1e-6)
        val pad = 16.dp.toPx()
        val scale = minOf((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY).toFloat()
        val ox = (size.width - (spanX * scale).toFloat()) / 2; val oy = (size.height - (spanY * scale).toFloat()) / 2
        fun at(p: com.prempoddar.loadstone.core.geo.TrailPoint) =
            Offset(ox + ((p.longitude - lon0) * k * scale).toFloat(), size.height - oy - ((p.latitude - lat0) * scale).toFloat())
        val path = androidx.compose.ui.graphics.Path().apply {
            val first = at(pin.trail.first()); moveTo(first.x, first.y)
            pin.trail.drop(1).forEach { val o = at(it); lineTo(o.x, o.y) }
        }
        drawPath(path, line, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawCircle(pinColor, 7.dp.toPx(), at(pin.trail.first()))
        here?.let { drawCircle(youColor, 6.dp.toPx(), at(it)); drawCircle(bg, 3.dp.toPx(), at(it)) }
    }
}
