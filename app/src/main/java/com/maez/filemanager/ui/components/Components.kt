package com.maez.filemanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.maez.filemanager.ui.theme.FileManagerTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.filemanager.ui.theme.AccentBlueLight
import com.maez.filemanager.ui.theme.AccentGreen
import com.maez.filemanager.ui.theme.AccentGreenLight
import com.maez.filemanager.ui.theme.BgElevated
import com.maez.filemanager.ui.theme.BorderDark
import com.maez.filemanager.ui.theme.CardDark
import com.maez.filemanager.ui.theme.InputDark
import com.maez.filemanager.ui.theme.TextMuted
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.TextSecondary

val CardShape = RoundedCornerShape(16.dp)
val ControlShape = RoundedCornerShape(10.dp)

/** Tarjeta de sección con cabecera de icono tintado (idéntica a MediaRecoveryMobile). */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = AccentBlueLight,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = CardDark,
        shape = CardShape,
        border = BorderStroke(1.dp, BorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = icon, tint = iconTint, size = 34)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
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

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Int = 36) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
fun FieldLabel(text: String, trailing: String? = null, trailingColor: Color = AccentBlueLight) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(trailing, color = trailingColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Botón principal con degradado verde (idéntico a MediaRecoveryMobile). */
@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = listOf(AccentGreen, AccentGreenLight)
) {
    val brush = if (enabled) Brush.horizontalGradient(colors) else Brush.horizontalGradient(listOf(InputDark, InputDark))
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(ControlShape)
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
            val c = if (enabled) Color.White else TextMuted
            Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(text, color = c, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Botón secundario plano (idéntico a MediaRecoveryMobile). */
@Composable
fun SecondaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = TextPrimary
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = InputDark,
        shape = ControlShape,
        border = BorderStroke(1.dp, BorderDark),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            val c = if (enabled) tint else TextMuted
            Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = c, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Selector segmentado tipo iOS/Material You (idéntico a MediaRecoveryMobile). */
@Composable
fun SegmentedTabs(
    options: List<Pair<String, ImageVector?>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InputDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, (label, icon) ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(if (selected) BgElevated else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (selected) TextPrimary else TextSecondary, label = "segFg")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .then(if (selected) Modifier.border(1.dp, BorderDark, RoundedCornerShape(10.dp)) else Modifier)
                    .clickable { onSelect(index) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = if (selected) AccentGreenLight else fg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
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

/** Chip seleccionable con borde, fondo reactivo y opción de botón de cierre/reset (X). */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    caption: String? = null,
    accent: Color = AccentGreenLight,
    onClear: (() -> Unit)? = null
) {
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.14f) else InputDark, label = "chipBg")
    val border by animateColorAsState(if (selected) accent else BorderDark, label = "chipBorder")
    Column(
        modifier = modifier
            .clip(ControlShape)
            .background(bg)
            .border(1.dp, border, ControlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (selected) accent else TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                color = if (selected) accent else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onClear != null) {
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) accent.copy(alpha = 0.25f) else BorderDark)
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Volver a la raíz",
                        tint = if (selected) accent else TextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        if (caption != null) {
            Text(caption, color = TextSecondary, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

/** Caja de búsqueda compacta sin márgenes gigantes de Material3. */
@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(ControlShape)
            .background(InputDark)
            .border(1.dp, BorderDark, ControlShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TextMuted,
                    fontSize = 12.sp,
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(AccentBlueLight),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpiar",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGreen,
                checkedBorderColor = AccentGreen,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = InputDark,
                uncheckedBorderColor = BorderDark
            )
        )
    }
}

@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlueLight,
    unfocusedBorderColor = BorderDark,
    focusedContainerColor = InputDark,
    unfocusedContainerColor = InputDark,
    disabledContainerColor = InputDark,
    focusedLabelColor = AccentBlueLight,
    unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlueLight,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted,
    focusedLeadingIconColor = AccentBlueLight,
    unfocusedLeadingIconColor = TextSecondary,
    focusedTrailingIconColor = TextPrimary,
    unfocusedTrailingIconColor = TextSecondary
)

@Composable
fun appSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = AccentGreenLight,
    activeTrackColor = AccentGreenLight,
    inactiveTrackColor = InputDark,
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (expanded) AccentBlueLight else BorderDark, label = "ddBorder")
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(ControlShape)
                .background(InputDark)
                .border(1.dp, borderColor, ControlShape)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$label:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = optionLabel(value),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expanded) AccentBlueLight else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BgElevated
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel(opt),
                            color = if (opt == value) AccentGreenLight else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (opt == value) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AppProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = AccentGreenLight) {
    val animated by animateFloatAsState((progress / 100f).coerceIn(0f, 1f), label = "progress")
    LinearProgressIndicator(
        progress = { animated },
        color = color,
        trackColor = InputDark,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, tint: Color = TextMuted) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconBadge(icon = icon, tint = tint, size = 54)
        Spacer(Modifier.height(4.dp))
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(message, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
    }
}

@Composable
fun StatTile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(ControlShape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), ControlShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/**
 * Barra de navegación inferior principal con altura optimizada (78.dp) para M3,
 * evitando que los iconos/botones queden pegados al borde superior.
 */
@Composable
fun AppBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = AccentGreenLight,
        selectedTextColor = TextPrimary,
        indicatorColor = AccentGreenLight.copy(alpha = 0.16f),
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextSecondary
    )

    Column(modifier = modifier) {
        HorizontalDivider(color = BorderDark, thickness = 1.dp)
        NavigationBar(
            containerColor = CardDark,
            tonalElevation = 0.dp,
            modifier = Modifier.height(78.dp)
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                colors = navItemColors,
                icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp)) },
                label = { Text("Explorador", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                colors = navItemColors,
                icon = { Icon(Icons.AutoMirrored.Filled.ManageSearch, contentDescription = null, modifier = Modifier.size(20.dp)) },
                label = { Text("Buscador", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                colors = navItemColors,
                icon = { Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(20.dp)) },
                label = { Text("Red SMB / FTP", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
            )
        }
    }
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

@Preview(showBackground = true, backgroundColor = 0xFF121418)
@Composable
fun SectionCardPreview() {
    FileManagerTheme {
        SectionCard(
            title = "Almacenamiento Local",
            icon = Icons.Default.Folder,
            subtitle = "Muestra carpetas y archivos en la memoria interna.",
            iconTint = AccentBlueLight
        ) {
            PrimaryButton(
                text = "Explorar Archivos",
                icon = Icons.Default.Folder,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121418)
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

