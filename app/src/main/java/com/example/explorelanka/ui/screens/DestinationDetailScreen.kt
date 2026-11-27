package com.example.explorelanka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.explorelanka.data.Destination
import com.example.explorelanka.ui.components.DestinationImage
import com.example.explorelanka.ui.components.WeatherPanel
import com.example.explorelanka.ui.theme.LankaPalette

@Composable
fun DestinationDetailScreen(
    destination: Destination?,
    onBackClick: () -> Unit,
    onMapClick: (String) -> Unit,
    onTakePhotoClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (destination == null) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text("Destination not found", style = MaterialTheme.typography.titleLarge)
            }
            Text("This destination is no longer available.", Modifier.padding(20.dp))
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(365.dp)
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                DestinationImage(destination, height = 365.dp)
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x55000000),
                        0.45f to Color.Transparent,
                        1f to Color(0xD70C261B)
                    )
                ))
                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    onClick = onBackClick
                ) {
                    Box(Modifier.size(45.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ArrowBack,
                            contentDescription = "Back to previous screen",
                            tint = LankaPalette.Forest)
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(23.dp)) {
                    Text(destination.category.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = LankaPalette.Accent, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(destination.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null,
                            modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(5.dp))
                        Text(destination.district, color = Color.White,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text("A place worth exploring", style = MaterialTheme.typography.titleLarge)
                Text(destination.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LankaPalette.SecondaryText)

                // Keep live Open-Meteo + Room caching completely unchanged.
                Text("Weather & conditions", style = MaterialTheme.typography.titleLarge)
                WeatherPanel(destinationId = destination.id)

                Text("Plan your visit", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { onMapClick(destination.id) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp)
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("Show on map")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                }
                FilledTonalButton(
                    onClick = { onTakePhotoClick(destination.id) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("Add a travel memory")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                }

                HorizontalDivider(color = LankaPalette.Sage)
                Text(
                    "Photo credit: ${destination.photoCreator} · ${destination.photoLicense}",
                    color = LankaPalette.SecondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(
                    onClick = { uriHandler.openUri(destination.photoSourceUrl) },
                    modifier = Modifier.align(Alignment.Start)
                ) { Text("Original photo and licence") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
