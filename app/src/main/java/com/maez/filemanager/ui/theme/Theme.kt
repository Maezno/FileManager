package com.maez.filemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// TOKENS DE DISEÑO ELEGANTE Y SOBRIO (ESTILO GITHUB DARK / CHARCOAL)
// ─────────────────────────────────────────────────────────────────────────────

val BgTop = Color(0xFF0F1117)
val BgMid = Color(0xFF0D0F14)
val BgBottom = Color(0xFF090A0D)

val GlowAqua = Color(0x1538BDF8)
val GlowViolet = Color(0x18818CF8)

val Violet = Color(0xFF818CF8)        // Indigo sobrio / Acento principal
val Aqua = Color(0xFF38BDF8)          // Sky Blue sobrio / Resaltes
val Danger = Color(0xFFF87171)        // Rojo suave

val TextPrimary = Color(0xFFF3F4F6)   // Blanco nítido
val TextMuted = Color(0xFF9CA3AF)     // Gris suave legible

val GlassFill = Color(0xFF161922)      // Superficie secundaria sólida y limpia
val GlassBorder = Color(0xFF262C38)    // Borde sutil y definido (1 dp)

val SelectedViolet = Color(0xFF282D3D) // Fondo pestaña/chip seleccionado
val SelectedVioletBorder = Color(0xFF4F46E5) // Borde activo indigo

val SelectedAqua = Color(0xFF162B3D)
val SelectedAquaBorder = Color(0xFF0284C7)

val NavBg = Color(0xF212151D)          // Barra inferior oscura 95%

val GlassTint = Color(0xFF161922)      // Superficie de tarjetas / contenedores
val PopupSurface = Color(0xFF161922)   // Menús y diálogos sólidos
val PopupBorder = Color(0xFF2E3545)    // Borde emergentes

// ─────────────────────────────────────────────────────────────────────────────
// ALIAS DE COLOR PARA COMPATIBILIDAD CON CÓDIGO EXISTENTE
// ─────────────────────────────────────────────────────────────────────────────

val BgDark = BgMid
val CardDark = Color(0xFF161922)
val BgElevated = Color(0xFF1E232F)
val InputDark = Color(0xFF11141A)
val BorderDark = GlassBorder
val ConsoleBg = Color(0xFF0B0D12)
val ConsoleText = Aqua

val TextSecondary = TextMuted

val AccentGreen = Aqua
val AccentGreenLight = Aqua
val AccentBlue = Violet
val AccentBlueLight = Violet
val AccentPurple = Violet
val DangerRed = Danger
val DangerRedLight = Danger
val WarningAmber = Color(0xFFFBBF24)

// Formas de bordes (Tarjetas 16dp, Chips 12dp, Botones 14dp, Floating Bar 20dp)
val GlassCardShape = RoundedCornerShape(16.dp)
val GlassChipShape = RoundedCornerShape(12.dp)
val GlassButtonShape = RoundedCornerShape(14.dp)
val GlassFloatingBarShape = RoundedCornerShape(20.dp)

private val AppColorScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color(0xFF0D0B1A),
    primaryContainer = SelectedViolet,
    onPrimaryContainer = TextPrimary,
    secondary = Aqua,
    onSecondary = Color(0xFF081C19),
    secondaryContainer = SelectedAqua,
    onSecondaryContainer = Aqua,
    tertiary = Violet,
    background = BgMid,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = InputDark,
    onSurfaceVariant = TextMuted,
    outline = GlassBorder,
    outlineVariant = GlassBorder,
    error = Danger
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = TextMuted
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = Violet
    )
)

@Composable
fun FileManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content
    )
}
