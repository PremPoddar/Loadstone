package com.prempoddar.loadstone.ui.drop

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.prempoddar.loadstone.data.photo.PhotoStore
import com.prempoddar.loadstone.data.pin.PinCategory
import com.prempoddar.loadstone.ui.components.PhotoRow
import com.prempoddar.loadstone.ui.components.visual
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SavePinSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, note: String?, category: PinCategory, photoPaths: List<String>) -> Unit,
    initialName: String = "",
    initialNote: String = "",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf(initialNote) }
    var category by remember { mutableStateOf(PinCategory.PIN) }
    var photos by remember { mutableStateOf(listOf<String>()) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingFile
        if (success && file != null) photos = photos + file.absolutePath
        else if (file != null) PhotoStore.delete(file.absolutePath)
        pendingFile = null
    }

    fun launchCamera() {
        val file = PhotoStore.newPhotoFile(context)
        pendingFile = file
        cameraLauncher.launch(PhotoStore.uriFor(context, file))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Save this spot", style = MaterialTheme.typography.headlineSmall)

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
                onAdd = { launchCamera() },
                onRemove = { path ->
                    photos = photos - path
                    PhotoStore.delete(path)
                },
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PinCategory.entries.forEach { cat ->
                    val v = cat.visual()
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(v.label) },
                        leadingIcon = { Icon(v.icon, contentDescription = null) },
                    )
                }
            }

            Button(
                onClick = { onSave(name, note, category, photos) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Save pin")
            }
        }
    }
}
