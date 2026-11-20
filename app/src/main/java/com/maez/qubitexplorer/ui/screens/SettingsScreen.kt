package com.maez.qubitexplorer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.maez.qubitexplorer.R
import androidx.compose.ui.unit.sp
import com.maez.qubitexplorer.model.formatBytes
import com.maez.qubitexplorer.ui.components.AppProgressBar
import com.maez.qubitexplorer.ui.components.DropdownField
import com.maez.qubitexplorer.ui.components.GlassAlertDialog
import com.maez.qubitexplorer.ui.components.GlassPanel
import com.maez.qubitexplorer.ui.components.IconBadge
import com.maez.qubitexplorer.ui.components.PrimaryButton
import com.maez.qubitexplorer.ui.components.SecondaryButton
import com.maez.qubitexplorer.ui.components.SectionCard
import com.maez.qubitexplorer.ui.components.SegmentedTabs
import com.maez.qubitexplorer.ui.components.SettingSwitchRow
import com.maez.qubitexplorer.ui.theme.Aqua
import com.maez.qubitexplorer.ui.theme.Danger
import com.maez.qubitexplorer.ui.theme.QubitExplorerTheme
import com.maez.qubitexplorer.ui.theme.TextMuted
import com.maez.qubitexplorer.ui.theme.TextPrimary
import com.maez.qubitexplorer.ui.theme.ThemeMode
import com.maez.qubitexplorer.ui.theme.Violet
import com.maez.qubitexplorer.ui.theme.WarningAmber
import com.maez.qubitexplorer.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    hasAllFilesAccess: Boolean,
    onRequestStoragePermissions: () -> Unit
) {
    val context = LocalContext.current
    val appVersion = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    val localTotalBytes by viewModel.localTotalBytes.collectAsState()
    val localFreeBytes by viewModel.localFreeBytes.collectAsState()
    val remoteCacheSize by viewModel.remoteCacheSizeBytes.collectAsState()
    val autoClearRemoteCache by viewModel.autoClearRemoteCache.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val confirmBeforeDelete by viewModel.confirmBeforeDelete.collectAsState()
    val tabMemoryTimeoutMinutes by viewModel.tabMemoryTimeoutMinutes.collectAsState()
    val metadataCopyOptions by viewModel.metadataCopyOptions.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Título de pantalla
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                ) {
                    IconBadge(icon = Icons.Default.Settings, tint = Violet, size = 40)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 1. SECCIÓN: Apariencia
            item {
                SectionCard(
                    title = stringResource(R.string.settings_appearance),
                    icon = Icons.Default.Palette,
                    iconTint = Violet,
                    subtitle = stringResource(R.string.settings_appearance_sub)
                ) {
                    SegmentedTabs(
                        options = listOf(
                            stringResource(R.string.theme_system) to Icons.Default.BrightnessAuto,
                            stringResource(R.string.theme_light) to Icons.Default.LightMode,
                            stringResource(R.string.theme_dark) to Icons.Default.DarkMode
                        ),
                        selectedIndex = themeMode.ordinal,
                        onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) }
                    )
                }
            }

            // 2. SECCIÓN: Almacenamiento y Caché
            item {
                SectionCard(
                    title = stringResource(R.string.settings_storage),
                    icon = Icons.Default.Storage,
                    iconTint = Violet,
                    subtitle = stringResource(R.string.settings_storage_sub)
                ) {
                    val localUsedBytes = (localTotalBytes - localFreeBytes).coerceAtLeast(0L)
                    val storageProgress = if (localTotalBytes > 0) {
                        (localUsedBytes.toFloat() / localTotalBytes.toFloat()) * 100f
                    } else 0f

                    GlassPanel(padding = 14.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.storage_internal),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.storage_used_of, formatBytes(localUsedBytes), formatBytes(localTotalBytes)),
                                color = Aqua,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AppProgressBar(progress = storageProgress, color = Aqua)

                        Text(
                            text = stringResource(R.string.storage_free, formatBytes(localFreeBytes)),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.remote_cache_title),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.cache_used, formatBytes(remoteCacheSize)),
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        SecondaryButton(
                            text = stringResource(R.string.clean_now),
                            icon = Icons.Default.CleaningServices,
                            onClick = { viewModel.clearRemotePreviewCache() },
                            tint = Danger
                        )
                    }

                    SettingSwitchRow(
                        title = stringResource(R.string.clear_cache_on_exit),
                        subtitle = stringResource(R.string.clear_cache_on_exit_sub),
                        checked = autoClearRemoteCache,
                        onCheckedChange = { viewModel.setAutoClearRemoteCache(it) },
                        icon = Icons.Default.CleaningServices
                    )
                }
            }

            // SECCIÓN: Metadatos al copiar
            item {
                SectionCard(
                    title = stringResource(R.string.settings_metadata),
                    icon = Icons.Default.LocationOn,
                    iconTint = Aqua,
                    subtitle = stringResource(R.string.settings_metadata_sub)
                ) {
                    SettingSwitchRow(
                        title = stringResource(R.string.keep_mtime),
                        subtitle = stringResource(R.string.keep_mtime_sub),
                        checked = metadataCopyOptions.preserveDates,
                        onCheckedChange = { viewModel.setMetadataCopyOptions(metadataCopyOptions.copy(preserveDates = it)) },
                        icon = Icons.Default.Schedule
                    )
                    Text(
                        text = stringResource(R.string.exif_title),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    SegmentedTabs(
                        options = listOf(
                            stringResource(R.string.exif_all) to Icons.Default.PhotoCamera,
                            stringResource(R.string.exif_no_gps) to Icons.Default.LocationOff,
                            stringResource(R.string.exif_none) to Icons.Default.HideImage
                        ),
                        selectedIndex = metadataCopyOptions.exifMode.ordinal,
                        onSelect = {
                            viewModel.setMetadataCopyOptions(
                                metadataCopyOptions.copy(exifMode = com.maez.qubitexplorer.services.ExifCopyMode.entries[it])
                            )
                        }
                    )
                    Text(
                        text = when (metadataCopyOptions.exifMode) {
                            com.maez.qubitexplorer.services.ExifCopyMode.KEEP_ALL -> stringResource(R.string.exif_all_desc)
                            com.maez.qubitexplorer.services.ExifCopyMode.STRIP_GPS -> stringResource(R.string.exif_no_gps_desc)
                            com.maez.qubitexplorer.services.ExifCopyMode.STRIP_ALL -> stringResource(R.string.exif_none_desc)
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // 3. SECCIÓN: Explorador y Visualización
            item {
                SectionCard(
                    title = stringResource(R.string.settings_explorer),
                    icon = Icons.Default.FolderSpecial,
                    iconTint = Aqua,
                    subtitle = stringResource(R.string.settings_explorer_sub)
                ) {
                    SettingSwitchRow(
                        title = stringResource(R.string.show_hidden),
                        subtitle = stringResource(R.string.show_hidden_sub),
                        checked = showHiddenFiles,
                        onCheckedChange = { viewModel.toggleHiddenFiles() },
                        icon = Icons.Default.Visibility
                    )

                    SettingSwitchRow(
                        title = stringResource(R.string.grid_default),
                        subtitle = stringResource(R.string.grid_default_sub),
                        checked = isGridView,
                        onCheckedChange = { viewModel.toggleGridView() },
                        icon = Icons.Default.GridOn
                    )

                    SettingSwitchRow(
                        title = stringResource(R.string.confirm_delete),
                        subtitle = stringResource(R.string.confirm_delete_sub),
                        checked = confirmBeforeDelete,
                        onCheckedChange = { viewModel.setConfirmBeforeDelete(it) },
                        icon = Icons.Default.Delete
                    )

                    Spacer(Modifier.height(4.dp))

                    val timeoutOptions = listOf(0, 5, 20, -1)
                    val optionLabels = mapOf(
                        0 to stringResource(R.string.last_folder_never),
                        5 to stringResource(R.string.last_folder_5),
                        20 to stringResource(R.string.last_folder_20),
                        -1 to stringResource(R.string.last_folder_always)
                    )

                    DropdownField(
                        label = stringResource(R.string.last_folder_title),
                        value = tabMemoryTimeoutMinutes,
                        options = timeoutOptions,
                        optionLabel = { optionLabels[it] ?: context.getString(R.string.minutes_short, it) },
                        onSelect = { viewModel.setTabMemoryTimeoutMinutes(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. SECCIÓN: Permisos del Sistema
            item {
                SectionCard(
                    title = stringResource(R.string.settings_permissions),
                    icon = Icons.Default.Security,
                    iconTint = if (hasAllFilesAccess) Aqua else WarningAmber,
                    subtitle = stringResource(R.string.settings_permissions_sub)
                ) {
                    GlassPanel(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (hasAllFilesAccess) Aqua else WarningAmber)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.all_files_access),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hasAllFilesAccess) stringResource(R.string.permission_granted) else stringResource(R.string.permission_pending),
                                    color = if (hasAllFilesAccess) Aqua else WarningAmber,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (!hasAllFilesAccess) {
                        PrimaryButton(
                            text = stringResource(R.string.manage_permission),
                            icon = Icons.Default.Security,
                            onClick = onRequestStoragePermissions,
                            modifier = Modifier.fillMaxWidth(),
                            colors = listOf(WarningAmber.copy(alpha = 0.6f), Violet.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // 5. SECCIÓN: Acerca de y Privacidad
            item {
                SectionCard(
                    title = stringResource(R.string.settings_about),
                    icon = Icons.Default.Info,
                    iconTint = Violet,
                    subtitle = stringResource(R.string.app_version, appVersion)
                ) {
                    GlassPanel(padding = 14.dp) {
                        Text(
                            text = stringResource(R.string.app_version, appVersion),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.about_desc),
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryButton(
                            text = stringResource(R.string.privacy_policy),
                            icon = Icons.Default.PrivacyTip,
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            tint = Violet
                        )
                        SecondaryButton(
                            text = stringResource(R.string.oss_licenses),
                            icon = Icons.Default.Code,
                            onClick = { showLicensesDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            tint = Aqua
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Diálogo de Política de Privacidad
    if (showPrivacyDialog) {
        GlassAlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Default.Lock, tint = Violet, size = 32)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.privacy_policy),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.privacy_local_title),
                        color = Violet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.privacy_local_body),
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.privacy_storage_title),
                        color = Aqua,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.privacy_storage_body),
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.privacy_creds_title),
                        color = Violet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.privacy_creds_body),
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.got_it), color = Aqua, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo de Licencias Open Source
    if (showLicensesDialog) {
        GlassAlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Default.Code, tint = Aqua, size = 32)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.oss_licenses),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.oss_intro),
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    val libraries = listOf(
                        "AndroidX & Jetpack Compose" to "Apache License 2.0 (Google LLC)",
                        "SMBJ (com.hierynomus.smbj)" to "Apache License 2.0 (Jeroen van Erp)",
                        "JCIFS-NG (eu.agno3.jcifs)" to "LGPL v2.1+ (Agno3 & JCIFS Project)",
                        "Apache Commons Net" to "Apache License 2.0 (Apache Software Foundation)",
                        "Apache Commons Compress" to "Apache License 2.0 (Apache Software Foundation)",
                        "XZ for Java (org.tukaani)" to "0BSD (Lasse Collin)",
                        "junrar" to "UnRAR License (Alexander Roshal / junrar)"
                    )

                    libraries.forEach { (name, license) ->
                        GlassPanel(padding = 10.dp) {
                            Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(license, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text(stringResource(R.string.close), color = Violet, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun SettingsScreenPreview() {
    QubitExplorerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassPanel {
                Text(stringResource(R.string.settings_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Demostración de panel de cristal para ajustes.", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}
