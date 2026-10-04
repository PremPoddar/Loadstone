package com.prempoddar.loadstone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.data.pin.Pin

/**
 * The picture at the head of a pin card.
 *
 * Photos taken when saving a pin were only ever reachable by opening the pin and starting to
 * navigate, which is backwards: the photo of the parking level is what tells you which pin this
 * *is*. When a pin has photos the card leads with one, carrying its category icon as a small
 * badge, and tapping it opens the gallery without walking anywhere.
 */
@Composable
fun PinAvatar(
    pin: Pin,
    modifier: Modifier = Modifier,
    size: Int = 48,
    onOpenPhotos: (() -> Unit)? = null,
) {
    val visual = pin.category.visual()
    if (pin.photoPaths.isEmpty()) {
        CategoryBadge(pin.category, modifier = modifier, size = size, icon = pin.icon())
        return
    }

    Box(modifier = modifier.size(size.dp)) {
        PhotoThumb(
            path = pin.photoPaths.first(),
            size = size.dp,
            modifier = Modifier.semantics { contentDescription = "Photos of ${pin.name}" },
            onClick = onOpenPhotos,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size((size * 0.46).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = pin.icon(),
                contentDescription = visual.label,
                tint = visual.accent,
                modifier = Modifier.size((size * 0.28).dp),
            )
        }
        if (pin.photoPaths.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size((size * 0.36).dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    pin.photoPaths.size.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
