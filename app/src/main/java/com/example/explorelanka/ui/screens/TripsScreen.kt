package com.example.explorelanka.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.data.local.entity.TripWithStops
import com.example.explorelanka.viewmodel.TripDraft
import com.example.explorelanka.viewmodel.TripsViewModel
import java.time.LocalDate

@Composable
fun TripsScreen(userId: String) {
    val appContext = LocalContext.current.applicationContext
    val factory = remember(appContext, userId) { TripsViewModel.factory(appContext, userId) }
    val viewModel: TripsViewModel = viewModel(key = "trips-$userId", factory = factory)
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showEditor by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripWithStops?>(null) }
    var pendingDelete by remember { mutableStateOf<TripWithStops?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(
                "My Trips",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Your trips are saved offline and synchronized with your account.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.trips.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.CalendarToday, contentDescription = null,
                            modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("No trips yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap New trip to create your first itinerary.")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { editingTrip = null; showEditor = true }) {
                            Text("Create a trip")
                        }
                    }
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    items(state.trips, key = { it.trip.id }) { item ->
                        TripCard(
                            item = item,
                            onEdit = { editingTrip = item; showEditor = true },
                            onDelete = { pendingDelete = item }
                        )
                    }
                }
            }
        }

        if (state.trips.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { editingTrip = null; showEditor = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("New trip") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)
            )
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showEditor) {
        TripEditorDialog(
            existing = editingTrip,
            onDismiss = { showEditor = false; editingTrip = null },
            onSave = { draft ->
                viewModel.saveTrip(draft)
                showEditor = false
                editingTrip = null
            }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete trip?") },
            text = { Text("Delete '${item.trip.title}' and all its saved stops? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTrip(item.trip.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TripCard(item: TripWithStops, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(item.trip.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${item.trip.startDate}  –  ${item.trip.endDate}")
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                val placeNames = item.stops.sortedBy { it.position }.map { stop ->
                    DestinationData.findById(stop.destinationId)?.name ?: "Unknown destination"
                }
                Text(
                    placeNames.joinToString(" • "),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (item.trip.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    item.trip.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun TripEditorDialog(
    existing: TripWithStops?,
    onDismiss: () -> Unit,
    onSave: (TripDraft) -> Unit
) {
    val key = existing?.trip?.id
    var title by remember(key) { mutableStateOf(existing?.trip?.title.orEmpty()) }
    var startDate by remember(key) {
        mutableStateOf(existing?.trip?.startDate ?: LocalDate.now().toString())
    }
    var endDate by remember(key) {
        mutableStateOf(existing?.trip?.endDate ?: LocalDate.now().plusDays(1).toString())
    }
    var notes by remember(key) { mutableStateOf(existing?.trip?.notes.orEmpty()) }
    val selectedIds: SnapshotStateList<String> = remember(key) {
        mutableStateListOf<String>().apply {
            addAll(existing?.stops?.sortedBy { it.position }?.map { it.destinationId }.orEmpty())
        }
    }
    var validationError by remember(key) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create trip" else "Edit trip") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Trip name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startDate, onValueChange = { startDate = it },
                    label = { Text("Start date (YYYY-MM-DD)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate, onValueChange = { endDate = it },
                    label = { Text("End date (YYYY-MM-DD)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth()
                )
                Text("Destinations", style = MaterialTheme.typography.titleMedium)
                DestinationData.places.forEach { place ->
                    val checked = place.id in selectedIds
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (checked) selectedIds.remove(place.id) else selectedIds.add(place.id)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (place.id !in selectedIds) selectedIds.add(place.id)
                                } else selectedIds.remove(place.id)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(place.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                validationError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val start = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
                val end = runCatching { LocalDate.parse(endDate.trim()) }.getOrNull()
                validationError = when {
                    title.trim().isEmpty() -> "Enter a trip name."
                    title.trim().length > 80 -> "Use at most 80 characters for the name."
                    start == null || end == null -> "Enter valid dates in YYYY-MM-DD format."
                    end.isBefore(start) -> "End date cannot be before start date."
                    selectedIds.isEmpty() -> "Select at least one destination."
                    else -> null
                }
                if (validationError == null) {
                    onSave(
                        TripDraft(
                            id = key,
                            title = title.trim(),
                            startDate = start!!.toString(),
                            endDate = end!!.toString(),
                            notes = notes.trim(),
                            destinationIds = selectedIds.toList()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
