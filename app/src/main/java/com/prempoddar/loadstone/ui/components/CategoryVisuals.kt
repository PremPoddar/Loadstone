package com.prempoddar.loadstone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MapsHomeWork
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NightShelter
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory

/** Accent color and label for each pin category. Keeps the saved list scannable. */
data class CategoryVisual(val icons: List<ImageVector>, val accent: Color, val label: String)

/**
 * Every pin in a category used to wear the identical icon, so a list of eight parking spots was
 * eight copies of the same picture and the eye had nothing to catch on. Each category now owns a
 * small family of icons that all still *mean* that category — a camp is a tent or a campfire or a
 * backpack, never a cake — and a pin keeps one of them for life.
 */
fun PinCategory.visual(): CategoryVisual = when (this) {
    PinCategory.PIN -> CategoryVisual(
        listOf(Icons.Filled.Place, Icons.Filled.PushPin, Icons.Filled.LocationOn, Icons.Filled.Flag, Icons.Filled.NearMe),
        Color(0xFF3F51B5),
        "Pin",
    )
    PinCategory.CAR -> CategoryVisual(
        listOf(
            Icons.Filled.DirectionsCar, Icons.Filled.LocalParking, Icons.Filled.Garage,
            Icons.Filled.TwoWheeler, Icons.Filled.EvStation, Icons.Filled.Commute,
        ),
        Color(0xFF00796B),
        "Car",
    )
    PinCategory.HOME -> CategoryVisual(
        listOf(
            Icons.Filled.Home, Icons.Filled.Cottage, Icons.Filled.House,
            Icons.Filled.Villa, Icons.Filled.MapsHomeWork,
        ),
        Color(0xFF6D4C41),
        "Home",
    )
    PinCategory.TENT -> CategoryVisual(
        listOf(
            Icons.Filled.Cabin, Icons.Filled.Festival, Icons.Filled.Backpack,
            Icons.Filled.LocalFireDepartment, Icons.Filled.OutdoorGrill, Icons.Filled.NightShelter,
        ),
        Color(0xFF2E7D32),
        "Camp",
    )
    PinCategory.TRAIL -> CategoryVisual(
        listOf(
            Icons.Filled.Hiking, Icons.Filled.Terrain, Icons.Filled.Forest,
            Icons.Filled.Landscape, Icons.AutoMirrored.Filled.DirectionsWalk, Icons.Filled.NordicWalking,
            Icons.Filled.Park,
        ),
        Color(0xFFEF6C00),
        "Trail",
    )
    PinCategory.STAR -> CategoryVisual(
        listOf(
            Icons.Filled.Star, Icons.Filled.AutoAwesome, Icons.Filled.EmojiEvents,
            Icons.Filled.Verified,
        ),
        Color(0xFFF9A825),
        "Star",
    )
}

/**
 * The icon this pin wears, picked once and for all from its category's family.
 *
 * Derived from the pin's own identity rather than drawn at random or stored, so it survives a
 * restart, a reinstall from backup and a reordering of the list — a pin that changed its face
 * every time you scrolled past would be worse than all of them sharing one.
 */
fun Pin.icon(): ImageVector {
    val family = category.visual().icons
    return family[(scramble(id * 0x9E3779B9L + createdAt) % family.size).toInt()]
}

/**
 * Spread adjacent identifiers apart. Row ids and timestamps both count upward, so using either
 * directly would walk the icon family in order and hand consecutive pins consecutive icons.
 */
private fun scramble(seed: Long): Long {
    var x = seed
    x = (x xor (x ushr 33)) * -0xAE502812AA7333L
    x = (x xor (x ushr 29)) * -0x3B314601E57A13ADL
    return (x xor (x ushr 32)) and Long.MAX_VALUE
}

@Composable
fun CategoryBadge(
    category: PinCategory,
    modifier: Modifier = Modifier,
    size: Int = 44,
    icon: ImageVector = category.visual().icons.first(),
) {
    val v = category.visual()
    Box(
        modifier = modifier
            .size(size.dp)
            .background(v.accent.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = v.label,
            tint = v.accent,
            modifier = Modifier.size((size * 0.55).dp),
        )
    }
}
