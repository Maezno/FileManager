package com.maez.filemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.filemanager.model.DeviceSummary
import com.maez.filemanager.model.DuplicateFileEntry
import com.maez.filemanager.model.FileCategory
import com.maez.filemanager.model.MediaOrientation
import com.maez.filemanager.model.MediaSearchItem
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import com.maez.filemanager.services.VfsManager
import com.maez.filemanager.ui.components.AppProgressBar
import com.maez.filemanager.ui.components.CompactSearchField
import com.maez.filemanager.ui.components.DropdownField
import com.maez.filemanager.ui.components.EmptyState
import com.maez.filemanager.ui.components.FieldLabel
import com.maez.filemanager.ui.components.FileThumbnailBox
import com.maez.filemanager.ui.components.IconBadge
import com.maez.filemanager.ui.components.PrimaryButton
import com.maez.filemanager.ui.components.SecondaryButton
import com.maez.filemanager.ui.components.SectionCard
import com.maez.filemanager.ui.components.SegmentedTabs
import com.maez.filemanager.ui.components.SelectChip
import com.maez.filemanager.ui.components.SettingSwitchRow
import com.maez.filemanager.ui.components.StatTile
import com.maez.filemanager.ui.components.Tag
import com.maez.filemanager.ui.components.appSliderColors
import com.maez.filemanager.ui.theme.AccentBlueLight
import com.maez.filemanager.ui.theme.AccentGreenLight
import com.maez.filemanager.ui.theme.AccentPurple
import com.maez.filemanager.ui.theme.BgElevated
import com.maez.filemanager.ui.theme.BorderDark
import com.maez.filemanager.ui.theme.CardDark
import com.maez.filemanager.ui.theme.DangerRed
import com.maez.filemanager.ui.theme.DangerRedLight
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.TextSecondary
import com.maez.filemanager.ui.theme.WarningAmber
import com.maez.filemanager.viewmodel.MainViewModel

@Composable
fun SmartSearchScreen(viewModel: MainViewModel) {
    val toolIndex by viewModel.searchToolIndex.collectAsState()
    val folderA by viewModel.folderA.collectAsState()
    val folderB by viewModel.folderB.collectAsState()
    val compareTwoFolders by viewModel.compareTwoFolders.collectAsState()
    val isRecursive by viewModel.isRecursiveScan.collectAsState()

    val searchWord by viewModel.searchWord.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedOrientation by viewModel.selectedOrientation.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val minSizeMB by viewModel.minSizeMB.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val statusText by viewModel.scanStatusText.collectAsState()
    val metricsText by viewModel.scanMetricsText.collectAsState()

    val searchResults by viewModel.searchResults.collectAsState()
    val deviceSummaries by viewModel.deviceSummaries.collectAsState()
    val selectedDeviceFilter by viewModel.selectedDeviceFilter.collectAsState()
    val duplicateResults by viewModel.duplicateResults.collectAsState()

    SmartSearchScreenContent(
        toolIndex = toolIndex,
        folderA = folderA,
        folderB = folderB,
        compareTwoFolders = compareTwoFolders,
        isRecursive = isRecursive,
        searchWord = searchWord,
        selectedCategory = selectedCategory,
        selectedOrientation = selectedOrientation,
        selectedMonth = selectedMonth,
        selectedYear = selectedYear,
        minSizeMB = minSizeMB,
        isScanning = isScanning,
        scanProgress = scanProgress,
        statusText = statusText,
        metricsText = metricsText,
        searchResults = searchResults,
        deviceSummaries = deviceSummaries,
        selectedDeviceFilter = selectedDeviceFilter,
        duplicateResults = duplicateResults,
        vfsManager = viewModel.vfsManager,
        onSetToolIndex = { viewModel.setSearchToolIndex(it) },
        onSetCompareTwoFolders = { viewModel.setCompareTwoFolders(it) },
        onSetRecursiveScan = { viewModel.setRecursiveScan(it) },
        onSetSearchWord = { viewModel.setSearchWord(it) },
        onSetSelectedCategory = { viewModel.setSelectedCategory(it) },
        onSetSelectedOrientation = { viewModel.setSelectedOrientation(it) },
        onSetSelectedMonth = { viewModel.setSelectedMonth(it) },
        onSetSelectedYear = { viewModel.setSelectedYear(it) },
        onSetMinSizeMB = { viewModel.setMinSizeMB(it) },
        onStartScan = { viewModel.startSmartScan() },
        onCancelScan = { viewModel.cancelSmartScan() },
        onExportCsv = { viewModel.exportCsvReport(it) },
        onSelectDeviceFilter = { viewModel.setSelectedDeviceFilter(it) },
        onOpenItem = { viewModel.openIntegratedPreview(it.item) },
        onShareItem = { viewModel.shareItem(it.item) },
        onOpenParentFolder = { item ->
            viewModel.selectTab(0)
            val conn = item.connectionId?.let { id ->
                viewModel.savedConnections.value.find { it.id == id }
            } ?: viewModel.activeConnection.value
            viewModel.loadDirectory(item.parentPath, item.protocol, conn)
        },
        onDeleteItem = { viewModel.deleteItem(it.item) },
        onOpenDuplicate = { viewModel.openIntegratedPreview(it.item) },
        onShareDuplicate = { viewModel.shareItem(it.item) },
        onDeleteDuplicate = { viewModel.deleteItem(it.item) }
    )
}

@Composable
fun SmartSearchScreenContent(
    toolIndex: Int,
    folderA: VfsItem,
    folderB: VfsItem?,
    compareTwoFolders: Boolean,
    isRecursive: Boolean,
    searchWord: String,
    selectedCategory: FileCategory,
    selectedOrientation: MediaOrientation,
    selectedMonth: String,
    selectedYear: String,
    minSizeMB: Float,
    isScanning: Boolean,
    scanProgress: Float,
    statusText: String,
    metricsText: String,
    searchResults: List<MediaSearchItem>,
    deviceSummaries: List<DeviceSummary>,
    selectedDeviceFilter: String?,
    duplicateResults: List<DuplicateFileEntry>,
    modifier: Modifier = Modifier,
    vfsManager: VfsManager? = null,
    onSetToolIndex: (Int) -> Unit = {},
    onSetCompareTwoFolders: (Boolean) -> Unit = {},
    onSetRecursiveScan: (Boolean) -> Unit = {},
    onSetSearchWord: (String) -> Unit = {},
    onSetSelectedCategory: (FileCategory) -> Unit = {},
    onSetSelectedOrientation: (MediaOrientation) -> Unit = {},
    onSetSelectedMonth: (String) -> Unit = {},
    onSetSelectedYear: (String) -> Unit = {},
    onSetMinSizeMB: (Float) -> Unit = {},
    onStartScan: () -> Unit = {},
    onCancelScan: () -> Unit = {},
    onExportCsv: (Boolean) -> Unit = {},
    onSelectDeviceFilter: (String?) -> Unit = {},
    onOpenItem: (MediaSearchItem) -> Unit = {},
    onShareItem: (MediaSearchItem) -> Unit = {},
    onOpenParentFolder: (VfsItem) -> Unit = {},
    onDeleteItem: (MediaSearchItem) -> Unit = {},
    onOpenDuplicate: (DuplicateFileEntry) -> Unit = {},
    onShareDuplicate: (DuplicateFileEntry) -> Unit = {},
    onDeleteDuplicate: (DuplicateFileEntry) -> Unit = {}
) {
    val filteredSearchResults = remember(
        searchResults,
        searchWord,
        selectedCategory,
        selectedOrientation,
        selectedDeviceFilter,
        minSizeMB
    ) {
        val minBytes = (minSizeMB * 1024L * 1024L).toLong()
        searchResults.filter { entry ->
            val matchWord = searchWord.isBlank() ||
                entry.item.name.contains(searchWord, ignoreCase = true) ||
                entry.item.parentPath.contains(searchWord, ignoreCase = true) ||
                entry.deviceModel.contains(searchWord, ignoreCase = true)

            val matchCat = selectedCategory == FileCategory.ALL || entry.item.category == selectedCategory
            val matchOrient = when (selectedOrientation) {
                MediaOrientation.ALL -> true
                MediaOrientation.VERTICAL -> entry.orientation.equals("Vertical", ignoreCase = true)
                MediaOrientation.HORIZONTAL -> entry.orientation.equals("Horizontal", ignoreCase = true)
            }
            val matchDevice = selectedDeviceFilter == null || entry.deviceModel.equals(selectedDeviceFilter, ignoreCase = true)
            val matchSize = entry.item.sizeBytes >= minBytes

            matchWord && matchCat && matchOrient && matchDevice && matchSize
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Selector segmentado
        item {
            SegmentedTabs(
                options = listOf(
                    "Buscador" to Icons.Default.Search,
                    "Dispositivos" to Icons.Default.Devices,
                    "Duplicados" to Icons.Default.ContentCopy
                ),
                selectedIndex = toolIndex,
                onSelect = onSetToolIndex
            )
        }

        // 2. Tarjeta de Configuración
        item {
            val sectionTitle = when (toolIndex) {
                0 -> "Buscador Avanzado de Archivos"
                1 -> "Estadísticas por Cámara y Dispositivo"
                else -> "Detector de Archivos Duplicados"
            }
            val sectionIcon = when (toolIndex) {
                0 -> Icons.Default.Search
                1 -> Icons.Default.Devices
                else -> Icons.Default.ContentCopy
            }

            SectionCard(
                title = sectionTitle,
                icon = sectionIcon,
                subtitle = "Analiza carpetas de tu dispositivo, tarjetas SD o servidores SMB/FTP de red local."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Carpeta A: ${folderA.path}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (toolIndex == 2) {
                        SettingSwitchRow(
                            title = "Comparar dos carpetas distintas (Carpeta A vs B)",
                            subtitle = if (compareTwoFolders) "Carpeta B: ${folderB?.path ?: "Sin seleccionar"}" else "Busca duplicados dentro de la Carpeta A únicamente",
                            checked = compareTwoFolders,
                            onCheckedChange = onSetCompareTwoFolders
                        )
                    }

                    SettingSwitchRow(
                        title = "Escanear subcarpetas recursivamente",
                        subtitle = if (isRecursive) "Incluye todo el árbol de directorios" else "Escanea solo el nivel raíz indicado",
                        checked = isRecursive,
                        onCheckedChange = onSetRecursiveScan
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isScanning) {
                        PrimaryButton(
                            text = "Detener Escaneo",
                            icon = Icons.Default.Stop,
                            onClick = onCancelScan,
                            colors = listOf(DangerRed, DangerRedLight),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val btnText = when (toolIndex) {
                            0 -> "Buscar Archivos"
                            1 -> "Analizar Cámaras"
                            else -> "Detectar Duplicados"
                        }
                        PrimaryButton(
                            text = btnText,
                            icon = Icons.Default.Search,
                            onClick = onStartScan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isScanning) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppProgressBar(progress = scanProgress)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = statusText, color = TextSecondary, fontSize = 11.sp)
                            Text(text = metricsText, color = AccentGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Panel de Filtros Rápidos
        if (toolIndex == 0) {
            item {
                SectionCard(
                    title = "Filtros de Búsqueda",
                    icon = Icons.Default.Search,
                    iconTint = AccentBlueLight
                ) {
                    CompactSearchField(
                        value = searchWord,
                        onValueChange = onSetSearchWord,
                        placeholder = "Nombre, extensión o modelo de cámara..."
                    )

                    FieldLabel(text = "Tipo de Contenido")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val catIcons = listOf(
                            FileCategory.ALL to (Icons.Default.Apps to AccentBlueLight),
                            FileCategory.IMAGE to (Icons.Default.Image to AccentGreenLight),
                            FileCategory.VIDEO to (Icons.Default.Movie to AccentPurple),
                            FileCategory.AUDIO to (Icons.Default.AudioFile to WarningAmber),
                            FileCategory.DOCUMENT to (Icons.Default.Description to AccentBlueLight),
                            FileCategory.ARCHIVE to (Icons.Default.Archive to WarningAmber),
                            FileCategory.APK to (Icons.Default.Android to AccentGreenLight)
                        )
                        for ((cat, style) in catIcons) {
                            SelectChip(
                                text = cat.label,
                                icon = style.first,
                                selected = selectedCategory == cat,
                                onClick = { onSetSelectedCategory(cat) },
                                accent = style.second
                            )
                        }
                    }

                    FieldLabel(text = "Orientación de Fotos y Videos")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SelectChip(
                            text = "Todas",
                            selected = selectedOrientation == MediaOrientation.ALL,
                            onClick = { onSetSelectedOrientation(MediaOrientation.ALL) },
                            modifier = Modifier.weight(1f)
                        )
                        SelectChip(
                            text = "Vertical",
                            icon = Icons.Default.Smartphone,
                            selected = selectedOrientation == MediaOrientation.VERTICAL,
                            onClick = { onSetSelectedOrientation(MediaOrientation.VERTICAL) },
                            accent = AccentGreenLight,
                            modifier = Modifier.weight(1f)
                        )
                        SelectChip(
                            text = "Horizontal",
                            icon = Icons.Default.ScreenRotation,
                            selected = selectedOrientation == MediaOrientation.HORIZONTAL,
                            onClick = { onSetSelectedOrientation(MediaOrientation.HORIZONTAL) },
                            accent = AccentPurple,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val months = listOf("Todos", "01 - Ene", "02 - Feb", "03 - Mar", "04 - Abr", "05 - May", "06 - Jun", "07 - Jul", "08 - Ago", "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dic")
                        DropdownField(
                            label = "Mes",
                            value = selectedMonth,
                            options = months,
                            optionLabel = { it },
                            onSelect = onSetSelectedMonth,
                            modifier = Modifier.weight(1f)
                        )
                        val years = listOf("Todos", "2026", "2025", "2024", "2023", "2022", "2021", "2020")
                        DropdownField(
                            label = "Año",
                            value = selectedYear,
                            options = years,
                            optionLabel = { it },
                            onSelect = onSetSelectedYear,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FieldLabel(
                            text = "Tamaño mínimo de archivo",
                            trailing = if (minSizeMB == 0f) "Cualquier tamaño" else "≥ ${minSizeMB.toInt()} MB"
                        )
                        Slider(
                            value = minSizeMB,
                            onValueChange = onSetMinSizeMB,
                            valueRange = 0f..500f,
                            colors = appSliderColors()
                        )
                    }
                }
            }
        }

        // 4. Muestra de Resultados según Herramienta
        when (toolIndex) {
            0 -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RESULTADOS HALLADOS (${filteredSearchResults.size})",
                            color = AccentGreenLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                        if (filteredSearchResults.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SecondaryButton(text = "Exportar CSV", icon = Icons.Default.Description, onClick = { onExportCsv(true) }, tint = AccentBlueLight)
                            }
                        }
                    }
                }

                if (filteredSearchResults.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "Sin resultados de búsqueda",
                            message = "Pulsa 'Buscar Archivos' para escanear la carpeta A seleccionada o ajusta los criterios de filtro."
                        )
                    }
                } else {
                    items(filteredSearchResults, key = { "${it.item.protocol}:${it.item.path}" }) { entry ->
                        MediaSearchResultCard(
                            entry = entry,
                            vfsManager = vfsManager,
                            onOpen = { onOpenItem(entry) },
                            onShare = { onShareItem(entry) },
                            onOpenParentFolder = { onOpenParentFolder(entry.item) },
                            onDelete = { onDeleteItem(entry) }
                        )
                    }
                }
            }

            1 -> {
                item {
                    Text(
                        text = "RESUMEN DE DISPOSITIVOS DE ORIGEN (${deviceSummaries.size})",
                        color = AccentPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (deviceSummaries.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Devices,
                            title = "Sin datos de dispositivos",
                            message = "Pulsa 'Analizar Cámaras' para leer la metainformación EXIF/media de fotos y videos."
                        )
                    }
                } else {
                    item {
                        SelectChip(
                            text = "Mostrar Todos (${searchResults.size} archivos)",
                            selected = selectedDeviceFilter == null,
                            onClick = { onSelectDeviceFilter(null) },
                            accent = AccentGreenLight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(deviceSummaries, key = { it.deviceModel }) { summary ->
                        DeviceSummaryCard(
                            summary = summary,
                            isSelected = selectedDeviceFilter == summary.deviceModel,
                            onClick = {
                                if (selectedDeviceFilter == summary.deviceModel) {
                                    onSelectDeviceFilter(null)
                                } else {
                                    onSelectDeviceFilter(summary.deviceModel)
                                }
                            }
                        )
                    }
                }
            }

            2 -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DUPLICADOS DETECTADOS (${duplicateResults.size})",
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                        if (duplicateResults.isNotEmpty()) {
                            SecondaryButton(text = "Reporte CSV", icon = Icons.Default.Description, onClick = { onExportCsv(false) }, tint = WarningAmber)
                        }
                    }
                }

                if (duplicateResults.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.ContentCopy,
                            title = "Sin duplicados encontrados",
                            message = "Pulsa 'Detectar Duplicados' para comparar contenido hash exacto de los archivos."
                        )
                    }
                } else {
                    items(duplicateResults, key = { "${it.groupId}:${it.item.path}" }) { dup ->
                        DuplicateFileCard(
                            entry = dup,
                            vfsManager = vfsManager,
                            onOpen = { onOpenDuplicate(dup) },
                            onShare = { onShareDuplicate(dup) },
                            onDelete = { onDeleteDuplicate(dup) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaSearchResultCard(
    entry: MediaSearchItem,
    vfsManager: VfsManager? = null,
    onOpen: () -> Unit = {},
    onShare: () -> Unit = {},
    onOpenParentFolder: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileThumbnailBox(
                item = entry.item,
                vfsManager = vfsManager,
                cornerRadius = 9.dp,
                showBadge = true,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.item.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = entry.item.sizeFormatted,
                        color = AccentBlueLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.deviceModel.isNotBlank() && entry.deviceModel != "Desconocido") {
                        Tag(text = entry.deviceModel, color = AccentBlueLight)
                    }
                    if (entry.orientation == "Vertical") {
                        Tag(text = "Vertical", color = AccentGreenLight)
                    } else if (entry.orientation == "Horizontal") {
                        Tag(text = "Horizontal", color = AccentPurple)
                    }
                    if (entry.resolution.isNotBlank()) {
                        Tag(text = entry.resolution, color = TextSecondary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${entry.item.protocol.name} • ${entry.item.parentPath}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(onClick = onShare, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = AccentGreenLight, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = onOpenParentFolder, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Ir a la carpeta", tint = AccentBlueLight, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DangerRedLight, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSummaryCard(
    summary: DeviceSummary,
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        color = if (isSelected) BgElevated else CardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) AccentGreenLight else BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconBadge(
                    icon = Icons.Default.CameraAlt,
                    tint = if (isSelected) AccentGreenLight else AccentBlueLight,
                    size = 34
                )
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = summary.deviceModel,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = AccentGreenLight, modifier = Modifier.size(13.dp))
                            Text("Vert: ${summary.verticalCount}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Monitor, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(13.dp))
                            Text("Horiz: ${summary.horizontalCount}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Tag(text = "${summary.totalCount} arch.", color = AccentBlueLight)
                Tag(text = "${summary.pctVertical} Vert.", color = AccentGreenLight)
            }
        }
    }
}

@Composable
private fun DuplicateFileCard(
    entry: DuplicateFileEntry,
    vfsManager: VfsManager? = null,
    onOpen: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (entry.isPrimary) AccentGreenLight.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileThumbnailBox(
                item = entry.item,
                vfsManager = vfsManager,
                cornerRadius = 9.dp,
                showBadge = false,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (entry.isPrimary) Icons.Default.Star else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (entry.isPrimary) AccentGreenLight else WarningAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "[Grupo #${entry.groupId} • ${entry.sourceLabel}] ${if (entry.isPrimary) "Original" else "Copia Duplicada"}",
                        color = if (entry.isPrimary) AccentGreenLight else WarningAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = entry.item.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.item.protocol.name} • ${entry.item.parentPath} • ${entry.item.sizeFormatted}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir", tint = AccentBlueLight, modifier = Modifier.size(16.dp))
                }
                if (!entry.isPrimary) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar duplicado", tint = DangerRedLight, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121418)
@Composable
fun SmartSearchScreenPreview() {
    val sampleFolder = VfsItem(
        name = "DCIM",
        path = "/storage/emulated/0/DCIM",
        parentPath = "/storage/emulated/0",
        sizeBytes = 0,
        lastModified = System.currentTimeMillis(),
        isDirectory = true,
        protocol = StorageProtocol.LOCAL
    )
    FileManagerTheme {
        SmartSearchScreenContent(
            toolIndex = 0,
            folderA = sampleFolder,
            folderB = null,
            compareTwoFolders = false,
            isRecursive = true,
            searchWord = "",
            selectedCategory = FileCategory.ALL,
            selectedOrientation = MediaOrientation.ALL,
            selectedMonth = "Todos",
            selectedYear = "Todos",
            minSizeMB = 0f,
            isScanning = false,
            scanProgress = 0f,
            statusText = "",
            metricsText = "",
            searchResults = listOf(
                MediaSearchItem(
                    item = VfsItem(
                        name = "IMG_20260330_120000.jpg",
                        path = "/storage/emulated/0/DCIM/Camera/IMG_20260330_120000.jpg",
                        parentPath = "/storage/emulated/0/DCIM/Camera",
                        sizeBytes = 3_200_000,
                        lastModified = System.currentTimeMillis(),
                        isDirectory = false,
                        protocol = StorageProtocol.LOCAL
                    ),
                    deviceModel = "Samsung Galaxy S24",
                    orientation = "Vertical",
                    resolution = "1080x1920"
                )
            ),
            deviceSummaries = emptyList(),
            selectedDeviceFilter = null,
            duplicateResults = emptyList()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121418)
@Composable
fun MediaSearchResultCardPreview() {
    FileManagerTheme {
        MediaSearchResultCard(
            entry = MediaSearchItem(
                item = VfsItem(
                    name = "DSC_0042.jpg",
                    path = "/storage/emulated/0/DCIM/Camera/DSC_0042.jpg",
                    parentPath = "/storage/emulated/0/DCIM/Camera",
                    sizeBytes = 4_800_000,
                    lastModified = System.currentTimeMillis(),
                    isDirectory = false,
                    protocol = StorageProtocol.LOCAL
                ),
                deviceModel = "Sony Alpha 7",
                orientation = "Horizontal",
                resolution = "3840x2160"
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121418)
@Composable
fun DuplicateFileCardPreview() {
    FileManagerTheme {
        DuplicateFileCard(
            entry = DuplicateFileEntry(
                groupId = 1,
                item = VfsItem(
                    name = "Copia_Documento.pdf",
                    path = "/storage/emulated/0/Download/Copia_Documento.pdf",
                    parentPath = "/storage/emulated/0/Download",
                    sizeBytes = 1_500_000,
                    lastModified = System.currentTimeMillis(),
                    isDirectory = false,
                    protocol = StorageProtocol.LOCAL
                ),
                isPrimary = false,
                sourceLabel = "Carpeta B"
            )
        )
    }
}
