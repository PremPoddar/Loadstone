package com.prempoddar.loadstone.ui.theme

import androidx.compose.runtime.Composable
import com.prempoddar.loadstone.ui.design.AppTheme
import com.prempoddar.loadstone.ui.design.Appearance

/** Loadstone's indigo, the accent when no other is chosen. */
const val LOADSTONE_BRAND = 0x3F51B5

/** Loadstone's look: the shared expressive theme with the person's chosen colours. */
@Composable
fun LoadstoneTheme(appearance: Appearance = Appearance(dynamic = true), content: @Composable () -> Unit) {
    AppTheme(appearance, LOADSTONE_BRAND, Typography, content)
}
