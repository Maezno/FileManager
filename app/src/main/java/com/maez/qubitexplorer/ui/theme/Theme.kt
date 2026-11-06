package com.maez.qubitexplorer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
// PALETAS CLARA Y OSCURA
// ─────────────────────────────────────────────────────────────────────────────

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private data class AppPalette(
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val violet: Color,
    val aqua: Color,
    val danger: Color,
    val warning: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val surface: Color,
    val border: Color,
    val selectedViolet: Color,
    val selectedVioletBorder: Color,
    val selectedAqua: Color,
    val selectedAquaBorder: Color,
    val navBg: Color,
    val navSelectedPill: Color,
    val popupBorder: Color,
    val bgElevated: Color,
    val inputBg: Color,
    val buttonSecondaryBg: Color,
    val buttonSecondaryBorder: Color,
    val buttonDisabledBg: Color,
    val buttonDisabledBorder: Color,
    val onAccent: Color,
    val switchUncheckedThumb: Color
)

private val DarkPalette = AppPalette(
    bgTop = Color(0xFF0F1117),
    bgMid = Color(0xFF0D0F14),
    bgBottom = Color(0xFF090A0D),
    violet = Color(0xFF818CF8),        // Indigo sobrio / Acento principal
    aqua = Color(0xFF38BDF8),          // Sky Blue sobrio / Resaltes
    danger = Color(0xFFF87171),        // Rojo suave
    warning = Color(0xFFFBBF24),
    textPrimary = Color(0xFFF3F4F6),   // Blanco nítido
    textMuted = Color(0xFF9CA3AF),     // Gris suave legible
    surface = Color(0xFF161922),       // Superficie de tarjetas, menús y diálogos
    border = Color(0xFF262C38),        // Borde sutil y definido (1 dp)
    selectedViolet = Color(0xFF282D3D), // Fondo pestaña/chip seleccionado
    selectedVioletBorder = Color(0xFF4F46E5), // Borde activo indigo
    selectedAqua = Color(0xFF162B3D),
    selectedAquaBorder = Color(0xFF0284C7),
    navBg = Color(0xF212151D),         // Barra inferior oscura 95%
    navSelectedPill = Color(0xFF232938),
    popupBorder = Color(0xFF2E3545),   // Borde emergentes
    bgElevated = Color(0xFF1E232F),
    inputBg = Color(0xFF11141A),
    buttonSecondaryBg = Color(0xFF1E2330),
    buttonSecondaryBorder = Color(0xFF2E384D),
    buttonDisabledBg = Color(0xFF1F2430),
    buttonDisabledBorder = Color(0xFF2A303C),
    onAccent = Color(0xFF0D1424),
    switchUncheckedThumb = Color.White
)

private val LightPalette = AppPalette(
    bgTop = Color(0xFFF8F9FB),
    bgMid = Color(0xFFF6F7F9),
    bgBottom = Color(0xFFF1F3F6),
    violet = Color(0xFF4F46E5),
    aqua = Color(0xFF0369A1),
    danger = Color(0xFFDC2626),
    warning = Color(0xFFB45309),
    textPrimary = Color(0xFF111827),
    textMuted = Color(0xFF4B5563),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE3E6EC),
    selectedViolet = Color(0xFFE0E7FF),
    selectedVioletBorder = Color(0xFF4F46E5),
    selectedAqua = Color(0xFFE0F2FE),
    selectedAquaBorder = Color(0xFF0284C7),
    navBg = Color(0xF2FFFFFF),
    navSelectedPill = Color(0xFFE0E7FF),
    popupBorder = Color(0xFFD8DCE4),
    bgElevated = Color(0xFFEEF0F4),
    inputBg = Color(0xFFF1F3F6),
    buttonSecondaryBg = Color(0xFFF1F3F6),
    buttonSecondaryBorder = Color(0xFFD8DCE4),
    buttonDisabledBg = Color(0xFFE5E7EB),
    buttonDisabledBorder = Color(0xFFD1D5DB),
    onAccent = Color.White,
    switchUncheckedThumb = Color(0xFF6B7280)
)

// Al leerse desde un estado, cualquier uso de estos tokens se actualiza al cambiar de tema.
private var palette by mutableStateOf(DarkPalette)

// ─────────────────────────────────────────────────────────────────────────────
// TOKENS DE DISEÑO ELEGANTE Y SOBRIO
// ─────────────────────────────────────────────────────────────────────────────

val BgTop: Color get() = palette.bgTop
val BgMid: Color get() = palette.bgMid
val BgBottom: Color get() = palette.bgBottom

val GlowAqua = Color(0x1538BDF8)
val GlowViolet = Color(0x18818CF8)

val Violet: Color get() = palette.violet
val Aqua: Color get() = palette.aqua
val Danger: Color get() = palette.danger

val TextPrimary: Color get() = palette.textPrimary
val TextMuted: Color get() = palette.textMuted

val GlassFill: Color get() = palette.surface
val GlassBorder: Color get() = palette.border

val SelectedViolet: Color get() = palette.selectedViolet
val SelectedVioletBorder: Color get() = palette.selectedVioletBorder

val SelectedAqua: Color get() = palette.selectedAqua
val SelectedAquaBorder: Color get() = palette.selectedAquaBorder

val NavBg: Color get() = palette.navBg
val NavSelectedPill: Color get() = palette.navSelectedPill

val GlassTint: Color get() = palette.surface
val PopupSurface: Color get() = palette.surface
val PopupBorder: Color get() = palette.popupBorder

val ButtonSecondaryBg: Color get() = palette.buttonSecondaryBg
val ButtonSecondaryBorder: Color get() = palette.buttonSecondaryBorder
val ButtonDisabledBg: Color get() = palette.buttonDisabledBg
val ButtonDisabledBorder: Color get() = palette.buttonDisabledBorder

val OnAccent: Color get() = palette.onAccent                     // Contenido sobre fondos de acento
val SwitchUncheckedThumb: Color get() = palette.switchUncheckedThumb

// Fondo de fotos y video: sigue oscuro en ambos temas, igual que su contenido
val ConsoleBg = Color(0xFF0B0D12)
val ConsoleText = Color(0xFF38BDF8)
val MediaTextMuted = Color(0xFF9CA3AF)
val MediaDanger = Color(0xFFF87171)
val MediaAccent = Color(0xFF38BDF8)

// ─────────────────────────────────────────────────────────────────────────────
// ALIAS DE COLOR PARA COMPATIBILIDAD CON CÓDIGO EXISTENTE
// ─────────────────────────────────────────────────────────────────────────────

val BgDark: Color get() = BgMid
val CardDark: Color get() = palette.surface
val BgElevated: Color get() = palette.bgElevated
val InputDark: Color get() = palette.inputBg
val BorderDark: Color get() = GlassBorder

val TextSecondary: Color get() = TextMuted

val AccentGreen: Color get() = Aqua
val AccentGreenLight: Color get() = Aqua
val AccentBlue: Color get() = Violet
val AccentBlueLight: Color get() = Violet
val AccentPurple: Color get() = Violet
val DangerRed: Color get() = Danger
val DangerRedLight: Color get() = Danger
val WarningAmber: Color get() = palette.warning

// Formas de bordes (Tarjetas 16dp, Chips 12dp, Botones 14dp, Floating Bar 20dp)
val GlassCardShape = RoundedCornerShape(16.dp)
val GlassChipShape = RoundedCornerShape(12.dp)
val GlassButtonShape = RoundedCornerShape(14.dp)
val GlassFloatingBarShape = RoundedCornerShape(20.dp)

private val DarkColors = with(DarkPalette) {
    darkColorScheme(
        primary = violet,
        onPrimary = Color(0xFF0D0B1A),
        primaryContainer = selectedViolet,
        onPrimaryContainer = textPrimary,
        secondary = aqua,
        onSecondary = Color(0xFF081C19),
        secondaryContainer = selectedAqua,
        onSecondaryContainer = aqua,
        tertiary = violet,
        background = bgMid,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = inputBg,
        onSurfaceVariant = textMuted,
        outline = border,
        outlineVariant = border,
        error = danger
    )
}

private val LightColors = with(LightPalette) {
    lightColorScheme(
        primary = violet,
        onPrimary = onAccent,
        primaryContainer = selectedViolet,
        onPrimaryContainer = textPrimary,
        secondary = aqua,
        onSecondary = onAccent,
        secondaryContainer = selectedAqua,
        onSecondaryContainer = aqua,
        tertiary = violet,
        onTertiary = onAccent,
        background = bgMid,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = inputBg,
        onSurfaceVariant = textMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = bgElevated,
        outline = border,
        outlineVariant = border,
        error = danger
    )
}

private fun appTypography() = Typography(
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
fun QubitExplorerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Antes de componer el contenido, para no dibujar un cuadro con la paleta anterior
    palette = if (dark) DarkPalette else LightPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).run {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = remember(dark) { appTypography() },
        content = content
    )
}
