package com.example.explorelanka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.data.DestinationSearch
import com.example.explorelanka.ui.components.AppEmptyState
import com.example.explorelanka.ui.components.DestinationCard
import com.example.explorelanka.ui.theme.LankaPalette

@Composable
fun ExploreScreen(
    onDestinationClick: (String) -> Unit,
    onMapClick: () -> Unit,
    onJournalClick: () -> Unit,
    onScanClick: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val destinations = DestinationData.places.toList()
    val categories = listOf("All") + destinations.map { it.category }
        .filter { it.isNotBlank() }.distinct().sorted()
    val matches = DestinationSearch.filter(destinations, search, category)

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("explore.list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item("header") {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                Text("DESTINATION COLLECTION", style = MaterialTheme.typography.labelMedium,
                    color = LankaPalette.ForestRaised, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Explore destinations", style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(5.dp))
                Text("Choose your kind of adventure.",
                    color = LankaPalette.SecondaryText,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        item("search") {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                placeholder = { Text("Places, cities, experiences...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = LankaPalette.Forest,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().testTag("explore.search")
            )
        }
        item("categories") {
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item ->
                    val selected = item == category
                    Surface(
                        onClick = { category = item },
                        shape = RoundedCornerShape(50),
                        color = if (selected) LankaPalette.Forest else Color.White,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center) {
                            Text(item,
                                color = if (selected) LankaPalette.White else LankaPalette.Forest,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item("quickActions") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExplorerAction("Map", Icons.Outlined.Map,
                    onMapClick, Modifier.weight(1f))
                ExplorerAction("Journal", Icons.Outlined.PhotoLibrary,
                    onJournalClick, Modifier.weight(1f))
                ExplorerAction("Scan QR", Icons.Outlined.QrCodeScanner,
                    onScanClick, Modifier.weight(1f))
            }
        }
        // Keep the exact button text for the Step 8 navigation UI test.
        item("mapTestAction") {
            TextButton(onClick = onMapClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 45.dp)) {
                Text("View destination map and nearby places")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(18.dp))
            }
        }
        item("listTitle") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Discover more", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
                Text("${matches.size} places", style = MaterialTheme.typography.bodySmall,
                    color = LankaPalette.SecondaryText)
            }
        }
        if (matches.isEmpty()) {
            item("empty") {
                AppEmptyState("No destinations found",
                    "Try a different name, district, or category.")
            }
        } else {
            items(matches, key = { it.id }) { place ->
                DestinationCard(place, onClick = { onDestinationClick(place.id) })
            }
        }
    }
}

@Composable
private fun ExplorerAction(
    title: String,
    image: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 84.dp),
        shape = RoundedCornerShape(23.dp),
        color = LankaPalette.Sage
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(image, contentDescription = null, modifier = Modifier.size(23.dp),
                tint = LankaPalette.Forest)
            Text(title, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = LankaPalette.Forest)
        }
    }
}
