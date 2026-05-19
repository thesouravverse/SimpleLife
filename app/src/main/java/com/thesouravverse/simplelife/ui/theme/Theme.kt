package com.thesouravverse.simplelife.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// SimpleLife palette: warm, kind, kid-friendly.
// Cream paper + soft ink, hero indigo, success green, gentle red.
object DqPalette {
    val Cream = Color(0xFFFAF6EE)
    val CreamDeep = Color(0xFFF1EADA)
    val Ink = Color(0xFF1F1B16)
    val InkSoft = Color(0xFF5F574B)
    val InkMuted = Color(0xFF8E8576)
    val Indigo = Color(0xFF4C5BD4)
    val IndigoDeep = Color(0xFF2E3A9E)
    val Success = Color(0xFF2BB673)
    val Danger = Color(0xFFE05A4B)
    val Gold = Color(0xFFE9B949)

    // Dark mode
    val Night = Color(0xFF14120F)
    val NightSurface = Color(0xFF1E1B17)
    val NightSurfaceVariant = Color(0xFF2A2620)
    val NightInk = Color(0xFFF3EEDF)
    val NightInkSoft = Color(0xFFB6AC97)
}

private val LightScheme = lightColorScheme(
    primary = DqPalette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E4FF),
    onPrimaryContainer = DqPalette.IndigoDeep,
    secondary = DqPalette.Gold,
    onSecondary = Color.Black,
    background = DqPalette.Cream,
    onBackground = DqPalette.Ink,
    surface = DqPalette.Cream,
    onSurface = DqPalette.Ink,
    surfaceVariant = DqPalette.CreamDeep,
    onSurfaceVariant = DqPalette.InkSoft,
    outline = DqPalette.InkMuted,
    error = DqPalette.Danger,
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8A96FF),
    onPrimary = Color.Black,
    primaryContainer = DqPalette.IndigoDeep,
    onPrimaryContainer = Color.White,
    secondary = DqPalette.Gold,
    onSecondary = Color.Black,
    background = DqPalette.Night,
    onBackground = DqPalette.NightInk,
    surface = DqPalette.NightSurface,
    onSurface = DqPalette.NightInk,
    surfaceVariant = DqPalette.NightSurfaceVariant,
    onSurfaceVariant = DqPalette.NightInkSoft,
    outline = DqPalette.NightInkSoft,
    error = DqPalette.Danger,
    onError = Color.White
)

private val DqTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
)

@Composable
fun SimpleLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = DqTypography,
        content = content
    )
}
