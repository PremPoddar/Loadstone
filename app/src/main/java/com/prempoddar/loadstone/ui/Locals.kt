package com.prempoddar.loadstone.ui

import androidx.compose.runtime.compositionLocalOf
import com.prempoddar.loadstone.data.settings.UnitSystem

/** Current unit system, provided at the app root so any screen can format distances. */
val LocalUnitSystem = compositionLocalOf { UnitSystem.METRIC }
