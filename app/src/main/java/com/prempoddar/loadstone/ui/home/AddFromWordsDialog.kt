package com.prempoddar.loadstone.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.core.format.CoordinateFormat
import com.prempoddar.loadstone.share.WordCode

/**
 * Types a shared sentence back into a pin.
 *
 * The whole point of the word form is that it travels as plain text, so the field accepts the
 * whole forwarded message rather than insisting on exactly seven words, and says what is wrong
 * with it as you type instead of waiting for you to press a button.
 */
@Composable
fun AddFromWordsDialog(
    onDismiss: () -> Unit,
    onAdd: (latitude: Double, longitude: Double, name: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val decoded = remember(text) { if (text.isBlank()) null else WordCode.decode(text) }
    val location = decoded as? WordCode.Decoded.Location

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add from words") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste the sentence someone sent you — the rest of their message can come too.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Sentence") },
                    placeholder = { Text("Grumpy walruses quietly juggle soggy waffles backwards") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Status(decoded)
                if (location != null) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Call it") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = location != null,
                onClick = {
                    location?.let { onAdd(it.latitude, it.longitude, name.trim().ifBlank { "Shared pin" }) }
                },
            ) { Text("Add pin") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Status(decoded: WordCode.Decoded?) {
    val (message, tone) = when (decoded) {
        null -> return
        is WordCode.Decoded.Location ->
            "Found it: " + CoordinateFormat.format(decoded.latitude, decoded.longitude) to
                MaterialTheme.colorScheme.primary
        is WordCode.Decoded.UnknownWord ->
            "\"${decoded.word}\" isn't one of the words — check it for typos." to MaterialTheme.colorScheme.error
        WordCode.Decoded.Garbled ->
            "Those are all real words, but one of them is wrong." to MaterialTheme.colorScheme.error
        WordCode.Decoded.NotFound ->
            "No sentence in there yet — it's seven words long." to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(message, style = MaterialTheme.typography.bodyMedium, color = tone)
}
