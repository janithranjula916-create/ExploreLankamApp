package com.example.explorelanka.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.explorelanka.R
import com.example.explorelanka.data.Destination
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.ui.components.DestinationImage
import com.example.explorelanka.ui.components.FeaturedDestinationCard
import com.example.explorelanka.ui.theme.LankaPalette

@Composable
fun HomeScreen(
    onExploreClick: () -> Unit,
    onDestinationClick: (String) -> Unit,
    onMapClick: () -> Unit = onExploreClick,
    onJournalClick: () -> Unit = onExploreClick,
    onScanClick: () -> Unit = onExploreClick,
    onPlannerClick: () -> Unit = onExploreClick
) {
    // SnapshotStateList is observable; Firestore additions will update this screen.
    val places = DestinationData.places.toList()
    val feature = places.firstOrNull { it.id == "sigiriya" } ?: places.firstOrNull()
    val picks = places.filter { it.id != feature?.id }.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        item(key = "intro") {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp)) {
                // Your new Explore Lanka branding, optimized for smaller Android screens.
                // It replaces only the old status dot and heading, not the travel features.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.explore_lanka_mark),
                        contentDescription = "Explore Lanka logo",
                        modifier = Modifier.size(54.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Explore Lanka",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LankaPalette.Forest,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Discover Sri Lanka",
                            style = MaterialTheme.typography.labelSmall,
                            color = LankaPalette.SecondaryText,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = LankaPalette.Sage,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = LankaPalette.Forest
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Sri Lanka",
                                style = MaterialTheme.typography.labelSmall,
                                color = LankaPalette.Forest
                            )
                        }
                    }
                }
                Spacer(Modifier.height(21.dp))
                Text("Find your next", style = MaterialTheme.typography.headlineLarge)
                Text("great escape.", style = MaterialTheme.typography.headlineLarge,
                    color = LankaPalette.ForestRaised)
                Spacer(Modifier.height(8.dp))
                Text("Incredible places, unforgettable memories.",
                    color = LankaPalette.SecondaryText,
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(19.dp))
                Surface(
                    onClick = onExploreClick,
                    color = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp)
                        .padding(horizontal = 17.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, contentDescription = null,
                            tint = LankaPalette.Forest)
                        Spacer(Modifier.width(11.dp))
                        Text("Search destinations", Modifier.weight(1f),
                            color = LankaPalette.SecondaryText)
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null,
                            tint = LankaPalette.Forest)
                    }
                }
            }
        }

        if (feature != null) {
            item(key = "hero") {
                FeatureHero(feature) { onDestinationClick(feature.id) }
            }
        }

        item(key = "quickHeader") {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text("Make it yours", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("Everything you need for your next journey",
                    style = MaterialTheme.typography.bodySmall,
                    color = LankaPalette.SecondaryText)
            }
        }
        item(key = "plannerBanner") {
            Surface(
                onClick = onPlannerClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                color = LankaPalette.Forest,
                shape = RoundedCornerShape(26.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 19.dp, vertical = 19.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Explore, contentDescription = null,
                        tint = LankaPalette.Accent, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Build your dream trip", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = LankaPalette.White)
                        Text("Dates, places and budget in one plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = LankaPalette.White.copy(alpha = 0.8f))
                    }
                    Icon(Icons.Outlined.ArrowForward, contentDescription = "Open trip planner",
                        tint = LankaPalette.Accent)
                }
            }
        }
        item(key = "quickActions") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeActionCard("Map", Icons.Outlined.Map, Modifier.weight(1f), onMapClick)
                HomeActionCard("Journal", Icons.Outlined.PhotoLibrary, Modifier.weight(1f), onJournalClick)
                HomeActionCard("Scan QR", Icons.Outlined.QrCodeScanner, Modifier.weight(1f), onScanClick)
            }
        }

        if (picks.isNotEmpty()) {
            item(key = "picksHeader") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Places to fall for", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onExploreClick) { Text("View all") }
                }
            }
            item(key = "picks") {
                LazyRow(contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    items(picks, key = { it.id }) { place ->
                        FeaturedDestinationCard(place, onClick = { onDestinationClick(place.id) })
                    }
                }
            }
        }

        item(key = "footer") {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                color = LankaPalette.Sage,
                shape = RoundedCornerShape(26.dp)
            ) {
                Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Explore, contentDescription = null,
                        modifier = Modifier.size(30.dp), tint = LankaPalette.Forest)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("More waiting to be explored", fontWeight = FontWeight.SemiBold)
                        Text("Browse all destinations", style = MaterialTheme.typography.bodySmall,
                            color = LankaPalette.SecondaryText)
                    }
                    IconButton(onClick = onExploreClick) {
                        Icon(Icons.Outlined.ArrowForward, contentDescription = "Browse destinations")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHero(destination: Destination, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            .height(326.dp).clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
    ) {
        DestinationImage(destination, height = 326.dp)
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0x12000000), Color(0x35000000), Color(0xE0092119)))
        ))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(24.dp)) {
            Text("EDITOR'S PICK  /  ${destination.category.uppercase()}",
                color = LankaPalette.Accent, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(destination.name, style = MaterialTheme.typography.headlineLarge,
                color = Color.White)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(destination.district, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White)
            }
            Spacer(Modifier.height(15.dp))
            Surface(shape = RoundedCornerShape(50), color = LankaPalette.Accent) {
                Row(Modifier.padding(horizontal = 19.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Discover this place", color = LankaPalette.Forest,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(9.dp))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null,
                        modifier = Modifier.size(18.dp), tint = LankaPalette.Forest)
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 96.dp),
        shape = RoundedCornerShape(21.dp),
        color = Color.White
    ) {
        Column(Modifier.padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = LankaPalette.SoftSage) {
                Box(Modifier.size(39.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null,
                        tint = LankaPalette.Forest, modifier = Modifier.size(23.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}
