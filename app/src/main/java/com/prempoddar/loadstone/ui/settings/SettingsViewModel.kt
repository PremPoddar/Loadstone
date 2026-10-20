package com.prempoddar.loadstone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prempoddar.loadstone.data.settings.Settings
import com.prempoddar.loadstone.data.settings.SettingsRepository
import com.prempoddar.loadstone.data.settings.ThemeMode
import com.prempoddar.loadstone.data.settings.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backup: com.prempoddar.loadstone.data.pin.PinBackup,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {
    private val _message = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** One-line results of backing up, restoring and moving pins, to show briefly. */
    val message: kotlinx.coroutines.flow.SharedFlow<String> = _message

    private fun files(work: suspend () -> String) {
        viewModelScope.launch { _message.emit(runCatching { work() }.getOrElse { "That didn't work: ${it.message ?: "the file couldn't be read"}" }) }
    }

    fun backupTo(uri: android.net.Uri) = files {
        val n = context.contentResolver.openOutputStream(uri)!!.use { backup.backup(it) }
        "Backed up $n ${if (n == 1) "pin" else "pins"} with their photos and voice notes"
    }

    fun restoreFrom(uri: android.net.Uri) = files {
        val r = context.contentResolver.openInputStream(uri)!!.use { backup.restore(it) }
        "Restored ${r.added} ${if (r.added == 1) "pin" else "pins"}" + if (r.alreadyHere > 0) " · ${r.alreadyHere} you already had" else ""
    }

    fun exportTo(uri: android.net.Uri, kml: Boolean) = files {
        val n = context.contentResolver.openOutputStream(uri, "wt")!!.use { backup.export(it, kml) }
        "Exported $n ${if (n == 1) "pin" else "pins"} as ${if (kml) "KML" else "GPX"}"
    }

    fun importFrom(uri: android.net.Uri) = files {
        val n = context.contentResolver.openInputStream(uri)!!.use { backup.import(it) }
        if (n == 0) "No places found in that file" else "Added $n ${if (n == 1) "place" else "places"}"
    }
    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(system) }
    }

    fun setAppearance(a: com.prempoddar.loadstone.ui.design.Appearance) {
        viewModelScope.launch { settingsRepository.setAppearance(a) }
    }

    fun setAccuracyLog(on: Boolean) {
        viewModelScope.launch { settingsRepository.setAccuracyLog(on) }
    }

    fun setAppLock(on: Boolean) {
        viewModelScope.launch { settingsRepository.setAppLock(on) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
