package com.prempoddar.loadstone.ui.components

import androidx.compose.runtime.setValue

import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private fun decodeSampled(path: String, targetPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    val largest = max(bounds.outWidth, bounds.outHeight)
    while (largest / sample > targetPx * 2) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
}.getOrNull()

/** Recently shown thumbnails, so scrolling back and reopening a pin shows them at once. */
private val thumbCache = object : android.util.LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

@Composable
private fun rememberPhoto(path: String, targetPx: Int): ImageBitmap? {
    val key = "$path@$targetPx"
    val bitmap by produceState(initialValue = thumbCache.get(key), path, targetPx) {
        if (value == null) value = withContext(Dispatchers.IO) { decodeSampled(path, targetPx)?.also { thumbCache.put(key, it) } }
    }
    return bitmap
}

@Composable
fun PhotoThumb(
    path: String,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val bitmap = rememberPhoto(path, with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx().toInt() })
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Pin photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove photo",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Horizontal strip of photos with an optional leading "add" button. */
@Composable
fun PhotoRow(
    paths: List<String>,
    modifier: Modifier = Modifier,
    thumbSize: Dp = 88.dp,
    onAdd: (() -> Unit)? = null,
    onRemove: ((String) -> Unit)? = null,
    onOpen: ((String) -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onAdd != null) {
            item {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onAdd() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.AddAPhoto,
                        contentDescription = "Add photo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        items(paths, key = { it }) { path ->
            PhotoThumb(
                path = path,
                size = thumbSize,
                onClick = onOpen?.let { { it(path) } },
                onRemove = onRemove?.let { { it(path) } },
            )
        }
    }
}

/**
 * Adding photos to a pin: take one with the camera, or pick several from the gallery at once
 * (the system picker lets you tick as many as you like, then confirm). Call the returned
 * function to offer the choice; [onAdded] gets the new photos' paths.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun rememberPhotoAdder(onAdded: (List<String>) -> Unit): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var choosing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<java.io.File?>(null) }
    val added by androidx.compose.runtime.rememberUpdatedState(onAdded)
    val camera = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { ok ->
        val file = pending
        if (ok && file != null) added(listOf(file.absolutePath)) else if (file != null) com.prempoddar.loadstone.data.photo.PhotoStore.delete(file.absolutePath)
        pending = null
    }
    val gallery = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val paths = withContext(Dispatchers.IO) { com.prempoddar.loadstone.data.photo.PhotoStore.importAll(context, uris) }
            if (paths.isNotEmpty()) added(paths)
        }
    }
    if (choosing) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { choosing = false }) {
            androidx.compose.foundation.layout.Column(Modifier.padding(bottom = 32.dp)) {
                androidx.compose.material3.ListItem(
                    headlineContent = { androidx.compose.material3.Text("Take a photo") },
                    leadingContent = { Icon(Icons.Filled.PhotoCamera, null) },
                    modifier = Modifier.clickable {
                        choosing = false
                        val file = com.prempoddar.loadstone.data.photo.PhotoStore.newPhotoFile(context)
                        pending = file
                        camera.launch(com.prempoddar.loadstone.data.photo.PhotoStore.uriFor(context, file))
                    },
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { androidx.compose.material3.Text("Choose from gallery") },
                    supportingContent = { androidx.compose.material3.Text("Pick as many as you like, then tap the tick") },
                    leadingContent = { Icon(Icons.Filled.PhotoLibrary, null) },
                    modifier = Modifier.clickable {
                        choosing = false
                        gallery.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            }
        }
    }
    return { choosing = true }
}
