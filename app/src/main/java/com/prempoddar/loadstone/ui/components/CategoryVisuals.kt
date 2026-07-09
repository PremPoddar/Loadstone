package com.prempoddar.loadstone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.data.pin.PinCategory

/** Icon + accent color for each pin category. Keeps the saved list scannable. */
data class CategoryVisual(val icon: ImageVector, val accent: Color, val label: String)

fun PinCategory.visual(): CategoryVisual = when (this) {
    PinCategory.PIN -> CategoryVisual(Icons.Filled.Place, Color(0xFF3F51B5), "Pin")
    PinCategory.CAR -> CategoryVisual(Icons.Filled.DirectionsCar, Color(0xFF00796B), "Car")
    PinCategory.HOME -> CategoryVisual(Icons.Filled.Home, Color(0xFF6D4C41), "Home")
    PinCategory.TENT -> CategoryVisual(Icons.Filled.Cabin, Color(0xFF2E7D32), "Camp")
    PinCategory.TRAIL -> CategoryVisual(Icons.Filled.Hiking, Color(0xFFEF6C00), "Trail")
    PinCategory.STAR -> CategoryVisual(Icons.Filled.Star, Color(0xFFF9A825), "Star")
}

@Composable
fun CategoryBadge(
    category: PinCategory,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val v = category.visual()
    Box(
        modifier = modifier
            .size(size.dp)
            .background(v.accent.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = v.icon,
            contentDescription = v.label,
            tint = v.accent,
            modifier = Modifier.size((size * 0.55).dp),
        )
    }
}
