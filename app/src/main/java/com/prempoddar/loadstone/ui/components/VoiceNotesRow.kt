package com.prempoddar.loadstone.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.prempoddar.loadstone.data.voice.VoiceRecorder
import kotlinx.coroutines.delay

private fun clock(ms: Long) = "%d:%02d".format(ms / 60_000, ms / 1000 % 60)

/** The pin's voice notes: tap one to hear it, the cross to delete it, the microphone to add one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceNotesRow(notes: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf<String?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { player.value?.release(); if (recorder.recording) recorder.stop()?.let(onAdd) } }
    LaunchedEffect(recording) { elapsed = 0; while (recording) { delay(250); elapsed += 250 } }

    fun play(path: String) {
        player.value?.release(); player.value = null
        if (playing == path) { playing = null; return }
        player.value = runCatching {
            MediaPlayer().apply { setDataSource(path); prepare(); setOnCompletionListener { playing = null }; start() }
        }.getOrNull()
        playing = if (player.value != null) path else null
    }
    fun toggle() {
        if (recording) { recording = false; recorder.stop()?.let(onAdd) }
        else recording = recorder.start()
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) toggle() }

    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notes.forEachIndexed { i, path ->
            val length = remember(path) { VoiceRecorder.durationMs(path) }
            InputChip(
                selected = playing == path,
                onClick = { play(path) },
                label = { Text("Note ${i + 1} · ${clock(length)}") },
                leadingIcon = { Icon(if (playing == path) Icons.Filled.Stop else Icons.Filled.PlayArrow, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    Icon(
                        Icons.Filled.Close, "Delete note ${i + 1}",
                        Modifier.size(18.dp).clickable { if (playing == path) play(path); onRemove(path) },
                    )
                },
            )
        }
        InputChip(
            selected = recording,
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) toggle()
                else ask.launch(Manifest.permission.RECORD_AUDIO)
            },
            label = { Text(if (recording) "Stop · ${clock(elapsed)}" else "Voice note") },
            leadingIcon = { Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic, null, Modifier.size(18.dp), tint = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
        )
    }
}
