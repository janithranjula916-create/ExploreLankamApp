package com.example.explorelanka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.explorelanka.data.Destination
import com.example.explorelanka.ui.theme.LankaPalette

/** Shared image component: keep this signature for the Step 5–7 detail screen. */
@Composable
fun DestinationImage(
    destination: Destination,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFC8DCCB), Color(0xFF7A9F8A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Landscape,
            contentDescription = null,
            tint = LankaPalette.Forest.copy(alpha = 0.48f),
            modifier = Modifier.size(52.dp)
        )
        AsyncImage(
            model = destination.imageUrl,
            contentDescription = "Photograph of ${destination.name}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/** Immersive cover card for Explore: full-photo background and readable gradient. */
@Composable
fun DestinationCard(
    destination: Destination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
    ) {
        DestinationImage(destination, height = 236.dp)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.40f to Color(0x22000000),
                    1f to Color(0xDD0B211B)
                )
            )
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            shape = RoundedCornerShape(50),
            color = LankaPalette.White.copy(alpha = 0.94f)
        ) {
            Text(
                destination.category.uppercase(),
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = LankaPalette.Forest
            )
        }
        Row(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    destination.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = LankaPalette.White.copy(alpha = 0.9f))
                    Spacer(Modifier.width(4.dp))
                    Text(destination.district, color = LankaPalette.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Surface(shape = RoundedCornerShape(18.dp), color = LankaPalette.Accent) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ArrowForward, contentDescription = "Explore ${destination.name}",
                        tint = LankaPalette.Forest)
                }
            }
        }
    }
}

/** Smaller editorial tile for the horizontally-scrolling home collection. */
@Composable
fun FeaturedDestinationCard(
    destination: Destination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(218.dp),
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        DestinationImage(destination, height = 164.dp)
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Text(destination.name, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null,
                    modifier = Modifier.size(15.dp), tint = LankaPalette.SecondaryText)
                Spacer(Modifier.width(4.dp))
                Text(destination.district, color = LankaPalette.SecondaryText,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
