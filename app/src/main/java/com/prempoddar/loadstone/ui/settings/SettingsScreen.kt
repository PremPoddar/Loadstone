package com.prempoddar.loadstone.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prempoddar.loadstone.BuildConfig
import com.prempoddar.loadstone.data.settings.UnitSystem
import com.prempoddar.loadstone.share.AppLinks
import com.prempoddar.loadstone.ui.design.ChoiceChips
import com.prempoddar.loadstone.ui.design.ExpressiveScaffold
import com.prempoddar.loadstone.ui.design.SettingsGroup
import com.prempoddar.loadstone.ui.design.SettingsRow

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppearance: () -> Unit = {},
    onAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    ExpressiveScaffold("Settings", onBack = onBack) { pad ->
        val current = settings ?: return@ExpressiveScaffold
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item {
                SettingsGroup("Distances") {
                    ChoiceChips(
                        listOf(UnitSystem.METRIC to "Metres and kilometres", UnitSystem.IMPERIAL to "Feet and miles"),
                        current.unitSystem, viewModel::setUnitSystem, Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
            item {
                SettingsGroup("Look") {
                    SettingsRow("Appearance", "Light or dark, colours", Icons.Rounded.Palette, onClick = onAppearance)
                }
            }
            item {
                SettingsGroup("Loadstone") {
                    SettingsRow("About", "Version ${BuildConfig.VERSION_NAME}, who made it, licences", Icons.Rounded.Info, onClick = onAbout)
                    SettingsRow("Privacy policy", "Location is used only while the app is open", Icons.Rounded.Shield, onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.privacyUrl()))) }
                    })
                }
            }
        }
    }
}
