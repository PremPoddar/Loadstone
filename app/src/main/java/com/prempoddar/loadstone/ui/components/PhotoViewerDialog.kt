package com.prempoddar.loadstone.ui.components

import com.prempoddar.loadstone.ui.design.tap
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen viewer for a pin's photos: swipe between them, pinch or double-tap to zoom.
 *
 * It takes the whole list rather than one path because a photo of a parking level is rarely the
 * only one you want — you took the row marker too, and the way back to the stairs.
 */
@Composable
fun PhotoViewerDialog(
    paths: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    if (paths.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, paths.lastIndex),
        pageCount = { paths.size },
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ZoomablePhoto(paths[page])
            }

            if (paths.size > 1) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${paths.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            IconButton(
                onClick = tap(onDismiss),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            if (onDelete != null) {
                IconButton(
                    onClick = tap { onDelete(paths[pagerState.currentPage]) },
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete photo", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomablePhoto(path: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Pin photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    )
                    // canPan is what makes swiping between photos work. A transformable that
                    // accepts pans consumes every drag that reaches it, zoomed in or not, so the
                    // pager underneath never saw the gesture and the only way to the next photo
                    // was to close the viewer and tap it. Panning is meaningless at rest anyway —
                    // there is nothing off screen to pan to — so at scale 1 the drag is left
                    // alone and the pager takes it. Pinching still zooms, because that is two
                    // fingers and not a pan.
                    .transformable(state = transform, canPan = { scale > 1f })
                    .pointerInput(path) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                                offset = Offset.Zero
                            },
                        )
                    },
            )
        }
    }
}

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
