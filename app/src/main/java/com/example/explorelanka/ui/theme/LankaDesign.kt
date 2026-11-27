package com.example.explorelanka.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** A warm, nature-inspired palette based on the user's reference, without new fonts. */
object LankaPalette {
    val Forest = Color(0xFF10291F)
    val ForestRaised = Color(0xFF214134)
    val Sage = Color(0xFFDCEADD)
    val SoftSage = Color(0xFFEAF1E9)
    val Ivory = Color(0xFFF6F7F1)
    val Canvas = Color(0xFFFCFCF8)
    val Accent = Color(0xFFDBE974)
    val Text = Color(0xFF17291F)
    val SecondaryText = Color(0xFF627468)
    val White = Color.White
}

val ExploreLankaColors = lightColorScheme(
    primary = LankaPalette.Forest,
    onPrimary = LankaPalette.White,
    primaryContainer = LankaPalette.Sage,
    onPrimaryContainer = LankaPalette.Forest,
    secondary = LankaPalette.ForestRaised,
    onSecondary = LankaPalette.White,
    secondaryContainer = LankaPalette.SoftSage,
    onSecondaryContainer = LankaPalette.Forest,
    tertiary = Color(0xFF66805F),
    background = LankaPalette.Ivory,
    onBackground = LankaPalette.Text,
    surface = LankaPalette.Canvas,
    onSurface = LankaPalette.Text,
    surfaceVariant = LankaPalette.SoftSage,
    onSurfaceVariant = LankaPalette.SecondaryText,
    outline = Color(0xFFBDCABB)
)

private val baseTypography = Typography()
val ExploreLankaTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 37.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = baseTypography.bodyLarge,
    bodyMedium = baseTypography.bodyMedium,
    bodySmall = baseTypography.bodySmall,
    labelLarge = baseTypography.labelLarge,
    labelMedium = baseTypography.labelMedium,
    labelSmall = baseTypography.labelSmall
)
