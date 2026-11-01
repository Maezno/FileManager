package com.maez.filemanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.filemanager.ui.theme.Aqua
import com.maez.filemanager.ui.theme.BgBottom
import com.maez.filemanager.ui.theme.BgMid
import com.maez.filemanager.ui.theme.BgTop
import com.maez.filemanager.ui.theme.CardDark
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.GlassBorder
import com.maez.filemanager.ui.theme.GlassButtonShape
import com.maez.filemanager.ui.theme.GlassCardShape
import com.maez.filemanager.ui.theme.GlassChipShape
import com.maez.filemanager.ui.theme.GlassFill
import com.maez.filemanager.ui.theme.GlassFloatingBarShape
import com.maez.filemanager.ui.theme.GlassTint
import com.maez.filemanager.ui.theme.GlowAqua
import com.maez.filemanager.ui.theme.GlowViolet
import com.maez.filemanager.ui.theme.NavBg
import com.maez.filemanager.ui.theme.PopupBorder
import com.maez.filemanager.ui.theme.PopupSurface
import com.maez.filemanager.ui.theme.SelectedAqua
import com.maez.filemanager.ui.theme.SelectedAquaBorder
import com.maez.filemanager.ui.theme.SelectedViolet
import com.maez.filemanager.ui.theme.SelectedVioletBorder
import com.maez.filemanager.ui.theme.TextMuted
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.Violet
import android.os.Build
import android.view.WindowManager
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalBlurEnabled = staticCompositionLocalOf { true }

val CardShape = GlassCardShape
val ControlShape = GlassChipShape

/** Fondo global sobrio con degradado limpio sin orbes estridentes. */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hazeState = remember { HazeState() }
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BgTop, BgMid, BgBottom)
                    )
                )
        ) {
            content()
        }
    }
}

/** Superficie estilizada de alto contraste y aspecto moderno y sobrio. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    fillColor: Color = GlassTint,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    blur: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseModifier = modifier
        .clip(shape)
        .background(fillColor)
        .border(borderWidth, borderColor, shape)

    Box(modifier = baseModifier) {
        content()
    }
}

/** Tarjeta de vidrio tipo GlassCard. */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = Violet,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        shape = GlassCardShape,
        blur = true,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon = icon, tint = iconTint)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
            content()
        }
    }
}

/** Burbuja de icono (42dp, fondo al 18% del tinte, radio 13dp). */
@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color = Violet,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size.value * 0.55f).dp)
        )
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Int = 36) {
    IconBubble(icon = icon, tint = tint, size = size.dp)
}

@Composable
fun FieldLabel(text: String, trailing: String? = null, trailingColor: Color = Aqua) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.lowercase(),
            color = Violet,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(trailing, color = trailingColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Botón principal con diseño sólido y tipografía legible. */
@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = listOf(Color(0xFF4F46E5), Color(0xFF4338CA))
) {
    val bgColor = if (enabled) colors[0] else Color(0xFF1F2430)
    val contentColor = if (enabled) Color.White else TextMuted

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(GlassButtonShape)
            .background(bgColor)
            .border(1.dp, if (enabled) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFF2A303C), GlassButtonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PrimaryGlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PrimaryButton(text = text, icon = icon, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun SecondaryGlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Aqua
) {
    SecondaryButton(text = text, icon = icon, onClick = onClick, modifier = modifier, enabled = enabled, tint = tint)
}

@Composable
fun GlassSegmentedControl(
    options: List<Pair<String, ImageVector?>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedTabs(options = options, selectedIndex = selectedIndex, onSelect = onSelect, modifier = modifier)
}

/** Botón secundario sobrio con fondo sutil y borde nítido. */
@Composable
fun SecondaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Aqua
) {
    val bgColor = Color(0xFF1E2330)
    val contentColor = if (enabled) tint else TextMuted

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(GlassButtonShape)
            .background(bgColor)
            .border(1.dp, Color(0xFF2E384D), GlassButtonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Botón icono con marco de vidrio (48dp). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    contentDescription: String? = null
) {
    GlassSurface(
        shape = GlassButtonShape,
        fillColor = GlassTint,
        blur = false,
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/** Selector segmentado tipo vidrio. */
@Composable
fun SegmentedTabs(
    options: List<Pair<String, ImageVector?>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        shape = GlassCardShape,
        fillColor = GlassTint,
        blur = true,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, (label, icon) ->
                val selected = index == selectedIndex
                val bg = if (selected) Color(0xFF232938) else Color.Transparent
                val border = if (selected) Color(0xFF4F46E5) else Color.Transparent
                val fg = if (selected) Color(0xFFF3F4F6) else TextMuted

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(GlassChipShape)
                        .background(bg)
                        .border(if (selected) 1.dp else 0.dp, border, GlassChipShape)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        if (icon != null) {
                            Icon(icon, contentDescription = null, tint = if (selected) Aqua else fg, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            color = fg,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Chip seleccionable de vidrio. */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    caption: String? = null,
    accent: Color = Violet,
    onClear: (() -> Unit)? = null
) {
    val bg = if (selected) SelectedViolet else GlassTint
    val border = if (selected) SelectedVioletBorder else GlassBorder

    GlassSurface(
        shape = GlassChipShape,
        fillColor = bg,
        borderColor = border,
        blur = true,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = if (selected) accent else TextMuted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    color = if (selected) TextPrimary else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (onClear != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (selected) accent.copy(alpha = 0.25f) else GlassFill)
                            .clickable { onClear() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar",
                            tint = if (selected) accent else TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            if (caption != null) {
                Text(caption, color = TextMuted, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Campo de búsqueda de vidrio (48dp). */
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) Aqua else GlassBorder

    GlassSurface(
        shape = GlassChipShape,
        borderColor = borderColor,
        fillColor = GlassTint,
        blur = true,
        modifier = modifier
            .height(height)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) Aqua else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Aqua),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (value.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp
) {
    GlassSearchField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        height = height
    )
}

/** Fila con interruptor de vidrio. */
@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(GlassChipShape)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconBubble(icon = icon, tint = Violet, size = 40.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0D1424),
                checkedTrackColor = Aqua,
                checkedBorderColor = Aqua,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = GlassBorder
            )
        )
    }
}

@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Aqua,
    unfocusedBorderColor = GlassBorder,
    focusedContainerColor = GlassFill,
    unfocusedContainerColor = GlassFill,
    disabledContainerColor = GlassFill,
    focusedLabelColor = Aqua,
    unfocusedLabelColor = TextMuted,
    cursorColor = Aqua,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted,
    focusedLeadingIconColor = Aqua,
    unfocusedLeadingIconColor = TextMuted,
    focusedTrailingIconColor = TextPrimary,
    unfocusedTrailingIconColor = TextMuted
)

@Composable
fun appSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Aqua,
    activeTrackColor = Aqua,
    inactiveTrackColor = GlassFill,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent
)

@Composable
fun <T> DropdownField(
    label: String,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (expanded) Aqua else GlassBorder, label = "ddBorder")

    Column(modifier = modifier) {
        GlassSurface(
            shape = GlassChipShape,
            borderColor = borderColor,
            fillColor = GlassTint,
            blur = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (expanded) Aqua else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = "$label:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = optionLabel(value),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) Aqua else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            GlassSurface(
                blur = true,
                fillColor = GlassTint,
                shape = GlassChipShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(Modifier.padding(6.dp)) {
                    options.forEach { opt ->
                        val selected = opt == value
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clip(GlassChipShape)
                                .background(if (selected) SelectedAqua else Color.Transparent)
                                .border(if (selected) 1.dp else 0.dp, if (selected) SelectedAquaBorder else Color.Transparent, GlassChipShape)
                                .clickable {
                                    onSelect(opt)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = optionLabel(opt),
                                color = if (selected) Aqua else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Desenfoque de sistema detrás de un Dialog (API 31+). */
@Composable
fun DialogWindowBlur(radius: Int = 32, dim: Float = 0.45f) {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setDimAmount(dim)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = radius }
        }
    }
}

/** Diálogo emergente de vidrio opaco (PopupSurface) con desenfoque de ventana de sistema. */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = {
            DialogWindowBlur()
            text?.invoke()
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = PopupSurface,
        iconContentColor = Violet,
        titleContentColor = TextPrimary,
        textContentColor = TextMuted,
        tonalElevation = 0.dp
    )
}

/** Panel contenedor interno de vidrio suave. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        shape = GlassChipShape,
        fillColor = Color.White.copy(alpha = 0.05f),
        borderColor = GlassBorder,
        blur = false,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

/** Menú desplegable emergente de vidrio (PopupSurface). */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = PopupSurface,
        border = BorderStroke(1.dp, PopupBorder),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier = modifier,
        content = content
    )
}

@Composable
fun AppProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Aqua) {
    val animated by animateFloatAsState((progress / 100f).coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(GlassFill)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Violet, color)))
        )
    }
}

/** Estado vacío con burbuja de icono de 64dp en aqua. */
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, tint: Color = Aqua) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBubble(icon = icon, tint = tint, size = 64.dp)
        Spacer(Modifier.height(4.dp))
        Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}

@Composable
fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    GlassSurface(
        shape = GlassChipShape,
        fillColor = color.copy(alpha = 0.12f),
        borderColor = color.copy(alpha = 0.35f),
        blur = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = TextMuted, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private data class BottomTabItem(
    val index: Int,
    val label: String,
    val icon: ImageVector
)

/** Barra de navegación inferior flotante tipo vidrio con animación de píldora de traslación. */
@Composable
fun GlassBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            BottomTabItem(0, "Explorador", Icons.Default.Folder),
            BottomTabItem(1, "Buscador", Icons.AutoMirrored.Filled.ManageSearch),
            BottomTabItem(2, "Red LAN", Icons.Default.Lan),
            BottomTabItem(3, "Ajustes", Icons.Default.Settings)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        GlassSurface(
            shape = GlassFloatingBarShape,
            fillColor = NavBg,
            borderColor = GlassBorder,
            blur = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            ) {
                // Asignamos proporcionalmente un poco más de ancho a la pestaña seleccionada que muestra texto
                val totalWeight = (items.size - 1) * 1.0f + 1.45f
                val baseInactiveWidth = maxWidth / totalWeight
                val activeWidth = baseInactiveWidth * 1.45f

                // Cálculo de la posición x para cada pestaña
                fun getTabOffset(index: Int): Dp {
                    return if (index <= selectedTab) {
                        baseInactiveWidth * index
                    } else {
                        baseInactiveWidth * (index - 1) + activeWidth
                    }
                }

                val indicatorOffset by animateDpAsState(
                    targetValue = getTabOffset(selectedTab),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "indicatorOffset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(activeWidth)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF232938))
                        .border(1.dp, Color(0xFF4F46E5), RoundedCornerShape(16.dp))
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val selected = item.index == selectedTab
                        val tabItemWidth = if (selected) activeWidth else baseInactiveWidth
                        val iconColor by animateColorAsState(if (selected) Aqua else TextMuted, label = "iconColor")
                        val textColor by animateColorAsState(if (selected) Color.White else TextMuted, label = "textColor")

                        Box(
                            modifier = Modifier
                                .width(tabItemWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onTabSelected(item.index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(19.dp)
                                )
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = item.label,
                                            color = textColor,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBottomBar(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AppBottomNavigationBarPreview() {
    FileManagerTheme {
        AppBottomNavigationBar(
            selectedTab = 0,
            onTabSelected = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun SectionCardPreview() {
    FileManagerTheme {
        SectionCard(
            title = "Almacenamiento Local",
            icon = Icons.Default.Folder,
            subtitle = "Muestra carpetas y archivos en la memoria interna.",
            iconTint = Violet
        ) {
            PrimaryButton(
                text = "Explorar Archivos",
                icon = Icons.Default.Folder,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun EmptyStatePreview() {
    FileManagerTheme {
        EmptyState(
            icon = Icons.Default.Folder,
            title = "Carpeta vacía",
            message = "No hay archivos en esta ubicación."
        )
    }
}
