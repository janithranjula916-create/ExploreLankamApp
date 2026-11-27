package com.example.explorelanka.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.data.local.entity.JournalPhotoEntity
import com.example.explorelanka.viewmodel.JournalViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun JournalScreen(
    userId: String,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit
) {
    val vm: JournalViewModel = viewModel()
    val flow = remember(userId) { vm.photosFor(userId) }
    val photos by flow.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<JournalPhotoEntity?>(null) }
    var removing by remember { mutableStateOf<JournalPhotoEntity?>(null) }
    var caption by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Travel journal", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold)
        }
        Text("Your private, offline travel memories",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onCaptureClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Capture a photograph")
        }
        Spacer(Modifier.height(12.dp))
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null,
                        modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("No journal photographs yet", fontWeight = FontWeight.SemiBold)
                    Text("Capture your first travel memory.")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                items(photos, key = { it.id }) { photo ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = File(photo.localPath),
                            contentDescription = "Travel photo",
                            modifier = Modifier.fillMaxWidth().height(210.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Column(Modifier.padding(14.dp)) {
                            Text(DestinationData.findById(photo.destinationId)?.name
                                ?: "Travel destination", fontWeight = FontWeight.SemiBold)
                            if (photo.caption.isNotBlank()) Text(photo.caption)
                            Text(
                                DateFormat.getDateTimeInstance().format(Date(photo.capturedAtMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { caption = photo.caption; editing = photo }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                TextButton(onClick = { removing = photo }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { photo ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit photo caption") },
            text = { OutlinedTextField(caption, { caption = it.take(240) },
                label = { Text("Caption") }, maxLines = 3) },
            confirmButton = {
                TextButton(onClick = {
                    vm.editCaption(userId, photo.id, caption); editing = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Cancel") }
            }
        )
    }
    removing?.let { photo ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Delete photograph?") },
            text = { Text("This removes the photo from this device's journal.") },
            confirmButton = {
                TextButton(onClick = { vm.deletePhoto(userId, photo.id); removing = null }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancel") }
            }
        )
    }
    vm.errorMessage?.let { message ->
        AlertDialog(onDismissRequest = vm::dismissError, title = { Text("Journal error") },
            text = { Text(message) }, confirmButton = {
                TextButton(onClick = vm::dismissError) { Text("OK") }
            })
    }
}
