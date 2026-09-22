package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ForestGreenDark,
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF0B4624),
    onPrimaryContainer = MintLight,
    secondary = GoldenSun,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF5B3B00),
    onSecondaryContainer = HarvestAmberLight,
    background = DarkSurface,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurfaceVariant,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF263238),
    outline = Color(0xFF374151)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = MintLight,
    onPrimaryContainer = ForestGreenPrimary,
    secondary = HarvestAmber,
    onSecondary = Color.White,
    secondaryContainer = HarvestAmberLight,
    onSecondaryContainer = Color(0xFF78350F),
    background = LightBackground,
    onBackground = SlateText,
    surface = LightSurface,
    onSurface = SlateText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = BorderLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our tailored agricultural brand colors
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

