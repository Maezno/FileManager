package com.maez.filemanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta GitHub Dark inspirada en MediaRecovery Studio Desktop
val BgDark = Color(0xFF0D1117)
val CardDark = Color(0xFF161B22)
val BgElevated = Color(0xFF1C2128)
val InputDark = Color(0xFF21262D)
val BorderDark = Color(0xFF30363D)
val ConsoleBg = Color(0xFF090D13)
val ConsoleText = Color(0xFFA5D6FF)

val TextPrimary = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF8B949E)
val TextMuted = Color(0xFF6E7681)

val AccentGreen = Color(0xFF238636)
val AccentGreenLight = Color(0xFF3FB950)
val AccentBlue = Color(0xFF1F6FEB)
val AccentBlueLight = Color(0xFF58A6FF)
val AccentPurple = Color(0xFFA371F7)
val DangerRed = Color(0xFFDA3633)
val DangerRedLight = Color(0xFFF85149)
val WarningAmber = Color(0xFFD29922)

private val DarkColorPalette = darkColorScheme(
    primary = AccentBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF132C4E),
    onPrimaryContainer = AccentBlueLight,
    secondary = AccentGreenLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF12361F),
    onSecondaryContainer = AccentGreenLight,
    tertiary = AccentPurple,
    background = BgDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = InputDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = CardDark,
    surfaceContainerLow = CardDark,
    surfaceContainerHigh = BgElevated,
    surfaceContainerHighest = InputDark,
    surfaceContainerLowest = BgDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = DangerRedLight
)

@Composable
fun FileManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        content = content
    )
}
