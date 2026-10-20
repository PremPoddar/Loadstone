package com.prempoddar.loadstone.ui.drop

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.data.photo.PhotoStore
import com.prempoddar.loadstone.data.pin.PinCategory
import com.prempoddar.loadstone.ui.components.PhotoRow
import com.prempoddar.loadstone.ui.components.visual
import com.prempoddar.loadstone.ui.util.Haptics
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SavePinSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, note: String?, category: PinCategory, photoPaths: List<String>, folder: String?) -> Unit,
    initialName: String = "",
    initialNote: String = "",
    initialCategory: PinCategory = PinCategory.PIN,
    initialPhotos: List<String> = emptyList(),
    initialFolder: String? = null,
    /** Folders already in use, offered as one-tap choices. */
    folders: List<String> = emptyList(),
    title: String = "Save this spot",
    saveLabel: String = "Save pin",
    /** When set, offers to record the way walked from here, and reports the choice. */
    onRecordChoice: ((Boolean) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val view = LocalView.current
    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf(initialNote) }
    var category by remember { mutableStateOf(initialCategory) }
    var photos by remember { mutableStateOf(initialPhotos) }
    var folder by remember { mutableStateOf(initialFolder.orEmpty()) }
    val addPhotos = com.prempoddar.loadstone.ui.components.rememberPhotoAdder { added -> photos = photos + added }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("Behind the big oak, under the rock…") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Photos", style = MaterialTheme.typography.labelLarge)
            PhotoRow(
                paths = photos,
                onAdd = addPhotos,
                onRemove = { path ->
                    photos = photos - path
                    PhotoStore.delete(path)
                },
            )

            Text("Folder", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = folder, onValueChange = { folder = it.take(40) },
                placeholder = { Text("None — or type one, like Trip to Goa") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            if (folders.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    folders.forEach { f -> FilterChip(selected = folder == f, onClick = { folder = if (folder == f) "" else f }, label = { Text(f) }) }
                }
            }

            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PinCategory.entries.forEach { cat ->
                    val v = cat.visual()
                    FilterChip(
                        selected = category == cat,
                        onClick = { Haptics.select(view); category = cat },
                        label = { Text(v.label) },
                        leadingIcon = { Icon(v.icons.first(), contentDescription = null) },
                    )
                }
            }

            if (onRecordChoice != null) {
                var record by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Record my way from here", style = MaterialTheme.typography.bodyLarge)
                        Text("Retrace your exact route back later", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = record, onCheckedChange = { record = it; onRecordChoice(it) })
                }
            }

            Button(
                onClick = { Haptics.confirm(view); onSave(name, note, category, photos, folder.trim().ifBlank { null }) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(saveLabel)
            }
        }
    }
}
