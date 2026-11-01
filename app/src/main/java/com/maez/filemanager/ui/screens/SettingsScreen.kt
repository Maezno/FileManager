package com.maez.filemanager.ui.screens

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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.sp
import com.maez.filemanager.model.formatBytes
import com.maez.filemanager.ui.components.AppProgressBar
import com.maez.filemanager.ui.components.DropdownField
import com.maez.filemanager.ui.components.GlassAlertDialog
import com.maez.filemanager.ui.components.GlassPanel
import com.maez.filemanager.ui.components.IconBadge
import com.maez.filemanager.ui.components.PrimaryButton
import com.maez.filemanager.ui.components.SecondaryButton
import com.maez.filemanager.ui.components.SectionCard
import com.maez.filemanager.ui.components.SettingSwitchRow
import com.maez.filemanager.ui.theme.Aqua
import com.maez.filemanager.ui.theme.Danger
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.TextMuted
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.Violet
import com.maez.filemanager.ui.theme.WarningAmber
import com.maez.filemanager.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    hasAllFilesAccess: Boolean,
    onRequestStoragePermissions: () -> Unit
) {
    val localTotalBytes by viewModel.localTotalBytes.collectAsState()
    val localFreeBytes by viewModel.localFreeBytes.collectAsState()
    val remoteCacheSize by viewModel.remoteCacheSizeBytes.collectAsState()
    val autoClearRemoteCache by viewModel.autoClearRemoteCache.collectAsState()

    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val confirmBeforeDelete by viewModel.confirmBeforeDelete.collectAsState()
    val tabMemoryTimeoutMinutes by viewModel.tabMemoryTimeoutMinutes.collectAsState()

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
                            text = "Ajustes y Configuración",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Opciones de almacenamiento, explorador y políticas de privacidad",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 1. SECCIÓN: Almacenamiento y Caché
            item {
                SectionCard(
                    title = "Almacenamiento y Caché",
                    icon = Icons.Default.Storage,
                    iconTint = Violet,
                    subtitle = "Espacio del dispositivo y caché de archivos de red"
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
                                text = "Almacenamiento Interno",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${formatBytes(localUsedBytes)} de ${formatBytes(localTotalBytes)}",
                                color = Aqua,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AppProgressBar(progress = storageProgress, color = Aqua)

                        Text(
                            text = "${formatBytes(localFreeBytes)} libres disponibles",
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
                                text = "Caché de vistas previas remotas",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Espacio ocupado: ${formatBytes(remoteCacheSize)}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        SecondaryButton(
                            text = "Limpiar ahora",
                            icon = Icons.Default.CleaningServices,
                            onClick = { viewModel.clearRemotePreviewCache() },
                            tint = Danger
                        )
                    }

                    SettingSwitchRow(
                        title = "Limpiar caché remota al salir",
                        subtitle = "Borra automáticamente las vistas previas descargadas de SMB/FTP al cerrar la app",
                        checked = autoClearRemoteCache,
                        onCheckedChange = { viewModel.setAutoClearRemoteCache(it) },
                        icon = Icons.Default.CleaningServices
                    )
                }
            }

            // 2. SECCIÓN: Explorador y Visualización
            item {
                SectionCard(
                    title = "Explorador y Visualización",
                    icon = Icons.Default.FolderSpecial,
                    iconTint = Aqua,
                    subtitle = "Comportamiento por defecto al explorar carpetas y archivos"
                ) {
                    SettingSwitchRow(
                        title = "Mostrar archivos ocultos",
                        subtitle = "Archivos que empiezan con '.' y recursos administrados '$'",
                        checked = showHiddenFiles,
                        onCheckedChange = { viewModel.toggleHiddenFiles() },
                        icon = Icons.Default.Visibility
                    )

                    SettingSwitchRow(
                        title = "Usar vista en cuadrícula (Grid) por defecto",
                        subtitle = "Muestra tarjetas con vista previa en lugar de la lista compacta",
                        checked = isGridView,
                        onCheckedChange = { viewModel.toggleGridView() },
                        icon = Icons.Default.GridOn
                    )

                    SettingSwitchRow(
                        title = "Confirmar antes de eliminar archivos",
                        subtitle = "Muestra un diálogo de advertencia previo a la eliminación (Recomendado)",
                        checked = confirmBeforeDelete,
                        onCheckedChange = { viewModel.setConfirmBeforeDelete(it) },
                        icon = Icons.Default.Delete
                    )

                    Spacer(Modifier.height(4.dp))

                    val timeoutOptions = listOf(0, 5, 20, -1)
                    val optionLabels = mapOf(
                        0 to "Nunca (0 min)",
                        5 to "5 minutos",
                        20 to "20 minutos (Default)",
                        -1 to "Siempre"
                    )

                    DropdownField(
                        label = "Memoria de última carpeta",
                        value = tabMemoryTimeoutMinutes,
                        options = timeoutOptions,
                        optionLabel = { optionLabels[it] ?: "$it min" },
                        onSelect = { viewModel.setTabMemoryTimeoutMinutes(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. SECCIÓN: Permisos del Sistema
            item {
                SectionCard(
                    title = "Permisos del Sistema",
                    icon = Icons.Default.Security,
                    iconTint = if (hasAllFilesAccess) Aqua else WarningAmber,
                    subtitle = "Acceso requerido para la lectura y gestión de archivos"
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
                                    text = "Acceso a todos los archivos (MANAGE_EXTERNAL_STORAGE)",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hasAllFilesAccess) "Estado: Concedido correctamente" else "Estado: Pendiente de conceder en ajustes del sistema",
                                    color = if (hasAllFilesAccess) Aqua else WarningAmber,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (!hasAllFilesAccess) {
                        PrimaryButton(
                            text = "Gestionar Permiso de Almacenamiento",
                            icon = Icons.Default.Security,
                            onClick = onRequestStoragePermissions,
                            modifier = Modifier.fillMaxWidth(),
                            colors = listOf(WarningAmber.copy(alpha = 0.6f), Violet.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // 4. SECCIÓN: Acerca de y Privacidad
            item {
                SectionCard(
                    title = "Acerca de y Privacidad",
                    icon = Icons.Default.Info,
                    iconTint = Violet,
                    subtitle = "FileManager Studio v1.0.0"
                ) {
                    GlassPanel(padding = 14.dp) {
                        Text(
                            text = "FileManager Studio v1.0.0",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gestor de archivos local y de red (SMB/FTP) diseñado para Android. Funciona 100% de manera local y en red privada.",
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
                            text = "Política de Privacidad",
                            icon = Icons.Default.PrivacyTip,
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            tint = Violet
                        )
                        SecondaryButton(
                            text = "Licencias Open Source",
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
                        text = "Política de Privacidad",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• Uso Local y Red Privada:",
                        color = Violet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "FileManager Studio opera de manera 100% local y en tu Red de Área Local (LAN). Ninguno de tus archivos, nombres, rutas ni credenciales de red (SMB/FTP) es enviado a servidores externos ni rastreadores.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "• Permiso de Almacenamiento Completo:",
                        color = Aqua,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "El permiso MANAGE_EXTERNAL_STORAGE se solicita con el único propósito de permitirte explorar, mover, transferir, buscar y organizar archivos en la memoria interna y tarjetas SD.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "• Cifrado de Credenciales:",
                        color = Violet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Las contraseñas de tus servidores locales SMB y FTP son almacenadas de forma ofuscada en el almacenamiento privado de la aplicación.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Entendido", color = Aqua, fontWeight = FontWeight.Bold)
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
                        text = "Licencias Open Source",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FileManager Studio utiliza las siguientes bibliotecas de código abierto:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    val libraries = listOf(
                        "AndroidX & Jetpack Compose" to "Apache License 2.0 (Google LLC)",
                        "SMBJ (com.hierynomus.smbj)" to "Apache License 2.0 (Jeroen van Erp)",
                        "JCIFS-NG (eu.agno3.jcifs)" to "LGPL v2.1+ (Agno3 & JCIFS Project)",
                        "Apache Commons Net" to "Apache License 2.0 (Apache Software Foundation)"
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
                    Text("Cerrar", color = Violet, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun SettingsScreenPreview() {
    FileManagerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassPanel {
                Text("Ajustes y Configuración", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Demostración de panel de cristal para ajustes.", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}
