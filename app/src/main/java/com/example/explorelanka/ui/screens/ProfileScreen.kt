package com.example.explorelanka.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.repository.TripRepository
import com.example.explorelanka.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: AuthViewModel, uid: String) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val repository = remember(uid) {
        TripRepository(ExploreLankaDatabase.getInstance(context), uid)
    }
    var name by remember(uid, viewModel.displayName) { mutableStateOf(viewModel.displayName) }
    var legacyCount by remember(uid) { mutableIntStateOf(0) }
    var legacyMessage by remember(uid) { mutableStateOf<String?>(null) }
    var busyImport by remember(uid) { mutableStateOf(false) }

    LaunchedEffect(uid) { legacyCount = repository.countLegacyTrips() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("My Profile", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Icon(Icons.Outlined.AccountCircle, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(viewModel.user?.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Display name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { viewModel.updateDisplayName(name) }, enabled = !viewModel.busy) {
            Text("Save profile")
        }
        viewModel.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider()
        Text("Offline and cloud trips", style = MaterialTheme.typography.titleLarge)
        Text("Trip changes are saved to Room first, then synchronized with Firestore when online.")
        if (legacyCount > 0) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Old local trips found: $legacyCount", fontWeight = FontWeight.SemiBold)
                    Text("Import these Step 3 trips into this signed-in account. This assigns them to the current account; they will no longer appear as unclaimed local trips.")
                    Button(enabled = !busyImport, onClick = {
                        busyImport = true
                        scope.launch {
                            try {
                                repository.importLegacyTrips()
                                legacyCount = repository.countLegacyTrips()
                                legacyMessage = "Local trips imported. They will sync when connected."
                            } catch (e: Exception) {
                                legacyMessage = e.localizedMessage ?: "Import failed."
                            } finally { busyImport = false }
                        }
                    }) { Text("Import my old trips") }
                }
            }
        }
        legacyMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider()
        OutlinedButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }
    }
}
