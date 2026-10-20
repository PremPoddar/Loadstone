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
import com.prempoddar.loadstone.ui.design.SwitchRow
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Backup

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppearance: () -> Unit = {},
    onAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.message.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
    }
    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
    val backupTo = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { it?.let(viewModel::backupTo) }
    val restoreFrom = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { it?.let(viewModel::restoreFrom) }
    val gpxTo = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/gpx+xml")) { it?.let { u -> viewModel.exportTo(u, kml = false) } }
    val kmlTo = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/vnd.google-earth.kml+xml")) { it?.let { u -> viewModel.exportTo(u, kml = true) } }
    val importFrom = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importFrom) }
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
                    SettingsRow("Look & feel", "Theme, colours, font, shapes and haptics", Icons.Rounded.Palette, onClick = onAppearance)
                }
            }
            item {
                SettingsGroup("Your pins") {
                    SettingsRow("Back up", "Every pin with its photos, voice notes and paths, in one file", Icons.Rounded.Backup, onClick = { backupTo.launch("Loadstone backup $stamp.zip") })
                    SettingsRow("Restore a backup", "Adds the pins from a backup; ones you have stay as they are", Icons.Rounded.SettingsBackupRestore, onClick = { restoreFrom.launch(arrayOf("application/zip", "application/octet-stream")) })
                    SettingsRow("Export as GPX", "For hiking and map apps such as OsmAnd and Gaia", Icons.Rounded.Upload, onClick = { gpxTo.launch("Loadstone pins $stamp.gpx") })
                    SettingsRow("Export as KML", "For Google Earth and My Maps", Icons.Rounded.Public, onClick = { kmlTo.launch("Loadstone pins $stamp.kml") })
                    SettingsRow("Import GPX or KML", "Places from another app become pins", Icons.Rounded.Download, onClick = { importFrom.launch(arrayOf("*/*")) })
                }
            }
            item {
                SettingsGroup("Privacy") {
                    SwitchRow("App lock", "Fingerprint, face or screen lock to open Loadstone", Icons.Rounded.Lock, checked = current.appLock) { on ->
                        val ok = androidx.biometric.BiometricManager.from(context).canAuthenticate(com.prempoddar.loadstone.ui.lock.LOCK_AUTHENTICATORS) ==
                            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
                        if (!on || ok) viewModel.setAppLock(on)
                        else android.widget.Toast.makeText(context, "Set a screen lock on this phone first", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            item {
                SettingsGroup("Testing") {
                    SwitchRow("Accuracy log", "Records every GPS reading to a file on this phone, to compare and tune", Icons.Rounded.Insights, checked = current.accuracyLog) { viewModel.setAccuracyLog(it) }
                    if (current.accuracyLog || com.prempoddar.loadstone.data.location.FixLog.file(context).exists()) {
                        SettingsRow("Share the log", "Send it for checking", Icons.Rounded.Share, onClick = {
                            val f = com.prempoddar.loadstone.data.location.FixLog.file(context)
                            if (!f.exists()) { android.widget.Toast.makeText(context, "Nothing recorded yet", android.widget.Toast.LENGTH_SHORT).show(); return@SettingsRow }
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/csv").putExtra(android.content.Intent.EXTRA_STREAM, uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share the accuracy log"))
                        })
                        SettingsRow("Clear the log", null, Icons.Rounded.Delete, onClick = { com.prempoddar.loadstone.data.location.FixLog.clear(context); android.widget.Toast.makeText(context, "Log cleared", android.widget.Toast.LENGTH_SHORT).show() })
                    }
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
