package com.prempoddar.loadstone.ui.home

import com.prempoddar.loadstone.ui.design.tap
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Radar
import com.prempoddar.loadstone.ui.components.QrDialog
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prempoddar.loadstone.core.format.UnitFormatter
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.share.WordCode
import com.prempoddar.loadstone.ui.LocalUnitSystem
import com.prempoddar.loadstone.ui.components.PhotoViewerDialog
import com.prempoddar.loadstone.ui.components.PinAvatar
import com.prempoddar.loadstone.ui.drop.SavePinSheet
import com.prempoddar.loadstone.ui.permission.rememberLocationPermissionState
import com.prempoddar.loadstone.ui.util.Haptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDropPin: () -> Unit,
    onOpenPin: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRadar: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val permission = rememberLocationPermissionState()
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(permission.hasPermission) {
        if (permission.hasPermission) viewModel.startLocationUpdates()
    }
    var pendingDelete by remember { mutableStateOf<Pin?>(null) }
    var editing by remember { mutableStateOf<Pin?>(null) }
    var viewingPhotosOf by remember { mutableStateOf<Pin?>(null) }
    var addingFromWords by remember { mutableStateOf(false) }
    var showingQr by remember { mutableStateOf<Pin?>(null) }
    // The home-screen widget shows the pinned spot, or the newest, from here.
    val unitNow = com.prempoddar.loadstone.ui.LocalUnitSystem.current
    LaunchedEffect(state.items, state.here?.let { (it.first * 2000).toInt() to (it.second * 2000).toInt() }) {
        val top = state.items.firstOrNull { it.pin.isPinned }?.pin ?: state.items.maxByOrNull { it.pin.createdAt }?.pin
        com.prempoddar.loadstone.widget.PinWidget.update(context, top, state.here, unitNow)
    }
    var folderFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    // Scanning goes through Google Play services, so Loadstone itself needs no camera permission.
    fun scanQr() {
        val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build()
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { code ->
                val shared = com.prempoddar.loadstone.share.QrCode.read(code.rawValue)
                if (shared == null) {
                    android.widget.Toast.makeText(context, "That code isn't a Loadstone pin", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    Haptics.confirm(view)
                    val name = shared.name ?: "Shared pin"
                    viewModel.addFromWords(shared.lat, shared.lon, name)
                    android.widget.Toast.makeText(context, "Added $name", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(context, "The scanner isn't available on this phone", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Loadstone", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { Haptics.tap(view); onOpenRadar() }) {
                        Icon(Icons.Filled.Radar, contentDescription = "Pins around you")
                    }
                    IconButton(onClick = { Haptics.tap(view); scanQr() }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan a pin's QR code")
                    }
                    IconButton(onClick = { Haptics.tap(view); addingFromWords = true }) {
                        Icon(Icons.Filled.Abc, contentDescription = "Add from words")
                    }
                    IconButton(onClick = tap(onOpenSettings)) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = tap { Haptics.tap(view); onDropPin() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Drop pin") },
            )
        },
    ) { padding ->
        val folders = remember(state.items) { state.items.mapNotNull { it.pin.folder }.distinct().sorted() }
        if (folderFilter != null && folderFilter !in folders) folderFilter = null
        val shown = if (folderFilter == null) state.items else state.items.filter { it.pin.folder == folderFilter }
        if (state.items.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
            ) {
                // Folders, when any are in use: tap one to see just its pins.
                if (folders.isNotEmpty()) item(key = "folders") {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        item { androidx.compose.material3.FilterChip(selected = folderFilter == null, onClick = tap { folderFilter = null }, label = { Text("All") }) }
                        items(folders) { f ->
                            androidx.compose.material3.FilterChip(
                                selected = folderFilter == f, onClick = tap { folderFilter = if (folderFilter == f) null else f },
                                label = { Text(f) }, leadingIcon = { Icon(Icons.Filled.Folder, null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                }
                items(shown, key = { it.pin.id }) { item ->
                    SwipeablePinRow(
                        pin = item.pin,
                        onSetPinned = { pinned ->
                            Haptics.confirm(view)
                            viewModel.setPinned(item.pin.id, pinned)
                        },
                    ) {
                    PinRow(
                        pin = item.pin,
                        distanceMeters = item.distanceMeters,
                        onClick = { Haptics.tap(view); onOpenPin(item.pin.id) },
                        onOpenMaps = { Haptics.tap(view); openInMaps(context, item.pin) },
                        onShare = { Haptics.tap(view); sharePin(context, item.pin) },
                        onShowQr = { Haptics.tap(view); showingQr = item.pin },
                        onEdit = { Haptics.tap(view); editing = item.pin },
                        onDelete = { pendingDelete = item.pin },
                        onOpenPhotos = { Haptics.tap(view); viewingPhotosOf = item.pin },
                    )
                    }
                }
            }
        }
    }

    pendingDelete?.let { pin ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete pin?") },
            text = { Text("\"${pin.name}\" will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = tap {
                    Haptics.confirm(view); viewModel.delete(pin.id); com.prempoddar.loadstone.ui.util.PinShortcuts.removed(context, pin.id); pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = tap { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (addingFromWords) {
        AddFromWordsDialog(
            onDismiss = { addingFromWords = false },
            onAdd = { lat, lon, name ->
                Haptics.confirm(view)
                viewModel.addFromWords(lat, lon, name)
                addingFromWords = false
            },
        )
    }

    showingQr?.let { QrDialog(it, onDismiss = { showingQr = null }) }

    viewingPhotosOf?.let { pin ->
        PhotoViewerDialog(
            paths = pin.photoPaths,
            onDismiss = { viewingPhotosOf = null },
        )
    }

    editing?.let { pin ->
        SavePinSheet(
            onDismiss = { editing = null },
            initialFolder = pin.folder,
            folders = state.items.mapNotNull { it.pin.folder }.distinct().sorted(),
            onSave = { name, note, category, photos, folder ->
                viewModel.updatePin(pin, name, note, category, photos, folder)
                editing = null
            },
            initialName = pin.name,
            initialNote = pin.note.orEmpty(),
            initialCategory = pin.category,
            initialPhotos = pin.photoPaths,
            title = "Edit pin",
            saveLabel = "Save changes",
        )
    }
}

private fun openInMaps(context: android.content.Context, pin: Pin) {
    val label = android.net.Uri.encode(pin.name)
    val uri = android.net.Uri.parse("geo:${pin.latitude},${pin.longitude}?q=${pin.latitude},${pin.longitude}($label)")
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
    runCatching { context.startActivity(intent) }
}

/**
 * Share a pin as a sentence rather than as a link.
 *
 * A link is only useful if the thing receiving it can open it, and a Loadstone link lands in
 * whatever browser the recipient happens to have. Seven words need nothing: they can be read
 * aloud, written on a receipt, or pasted into any chat app, and typed back in by hand. The
 * coordinates ride along on the last line for anyone who would rather use a map app.
 */
private fun sharePin(context: android.content.Context, pin: Pin) {
    // The shared text is the sentence and nothing else. Every target of the share sheet gets the
    // same string, including its Copy button, so anything wrapped around the words — a name, an
    // explanation of what to do with them — is something the recipient then has to delete before
    // pasting. The sentence is self-contained by design; the pin's name travels as the subject,
    // where mail apps use it and chat apps ignore it.
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, WordCode.sentenceFor(pin.latitude, pin.longitude))
        putExtra(android.content.Intent.EXTRA_SUBJECT, pin.name)
        putExtra(android.content.Intent.EXTRA_TITLE, pin.name)
    }
    runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Share ${pin.name}")) }
}

/**
 * Pin and unpin by swiping the card.
 *
 * Swiping right pins, swiping left unpins, and the card always springs back: there is nothing to
 * dismiss here, so [SwipeToDismissBox] is used purely for its gesture and the settle is refused
 * rather than confirmed. The background says which of the two is about to happen, because a swipe
 * with no label on it is a guess.
 */
@Composable
private fun SwipeablePinRow(
    pin: Pin,
    onSetPinned: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onSetPinned(true)
                SwipeToDismissBoxValue.EndToStart -> onSetPinned(false)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd ->
                    SwipeAction(Alignment.CenterStart, Icons.Filled.PushPin, "Pin to top", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                SwipeToDismissBoxValue.EndToStart ->
                    SwipeAction(Alignment.CenterEnd, Icons.Filled.PushPin, "Unpin", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        content = { content() },
    )
}

@Composable
private fun SwipeAction(
    alignment: Alignment,
    icon: ImageVector,
    label: String,
    background: Color,
    foreground: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = foreground)
            Spacer(Modifier.size(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = foreground)
        }
    }
}

@Composable
private fun PinRow(
    pin: Pin,
    distanceMeters: Double?,
    onClick: () -> Unit,
    onOpenMaps: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenPhotos: () -> Unit,
    onShowQr: () -> Unit,
) {
    val unit = LocalUnitSystem.current
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PinAvatar(pin = pin, onOpenPhotos = onOpenPhotos)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pin.isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.size(5.dp))
                    }
                    Text(
                        pin.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val subtitle = pin.note?.takeIf { it.isNotBlank() }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (distanceMeters != null) {
                Text(
                    UnitFormatter.formatDistance(distanceMeters, unit),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box {
                IconButton(onClick = tap { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Open in Maps") },
                        leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        onClick = tap { menuOpen = false; onOpenMaps() },
                    )
                    DropdownMenuItem(
                        text = { Text("Share as words") },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        onClick = tap { menuOpen = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text("Show QR code") },
                        leadingIcon = { Icon(Icons.Filled.QrCode2, contentDescription = null) },
                        onClick = tap { menuOpen = false; onShowQr() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = tap { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = tap { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("No pins yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap Drop pin to save your first spot. Loadstone remembers the exact place so you can walk right back to it.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
