package com.maez.filemanager.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.filemanager.model.DeviceSummary
import com.maez.filemanager.model.DuplicateFileEntry
import com.maez.filemanager.model.FileCategory
import com.maez.filemanager.model.MediaOrientation
import com.maez.filemanager.model.MediaSearchItem
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.SearchSortField
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import com.maez.filemanager.model.formatBytes
import com.maez.filemanager.services.ConnectionStorage
import com.maez.filemanager.services.VfsManager
import com.maez.filemanager.ui.components.AppProgressBar
import com.maez.filemanager.ui.components.CompactSearchField
import com.maez.filemanager.ui.components.ControlShape
import com.maez.filemanager.ui.components.EmptyState
import com.maez.filemanager.ui.components.FileThumbnailBox
import com.maez.filemanager.ui.components.GlassAlertDialog
import com.maez.filemanager.ui.components.GlassDropdownMenu
import com.maez.filemanager.ui.components.GlassIconButton
import com.maez.filemanager.ui.components.GlassSearchField
import com.maez.filemanager.ui.components.GlassSegmentedControl
import com.maez.filemanager.ui.components.GlassSurface
import com.maez.filemanager.ui.components.IconBadge
import com.maez.filemanager.ui.components.IconBubble
import com.maez.filemanager.ui.components.PrimaryButton
import com.maez.filemanager.ui.components.PrimaryGlassButton
import com.maez.filemanager.ui.components.SecondaryButton
import com.maez.filemanager.ui.components.SecondaryGlassButton
import com.maez.filemanager.ui.components.SectionCard
import com.maez.filemanager.ui.components.SegmentedTabs
import com.maez.filemanager.ui.components.SelectChip
import com.maez.filemanager.ui.components.SettingSwitchRow
import com.maez.filemanager.ui.components.Tag
import com.maez.filemanager.ui.components.appTextFieldColors
import com.maez.filemanager.ui.theme.Aqua
import com.maez.filemanager.ui.theme.BgElevated
import com.maez.filemanager.ui.theme.BorderDark
import com.maez.filemanager.ui.theme.CardDark
import com.maez.filemanager.ui.theme.Danger
import com.maez.filemanager.ui.theme.DangerRed
import com.maez.filemanager.ui.theme.DangerRedLight
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.GlassBorder
import com.maez.filemanager.ui.theme.GlassCardShape
import com.maez.filemanager.ui.theme.GlassChipShape
import com.maez.filemanager.ui.theme.GlassFill
import com.maez.filemanager.ui.theme.InputDark
import com.maez.filemanager.ui.theme.SelectedViolet
import com.maez.filemanager.ui.theme.TextMuted
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.TextSecondary
import com.maez.filemanager.ui.theme.Violet
import com.maez.filemanager.ui.theme.WarningAmber
import com.maez.filemanager.viewmodel.MainViewModel

data class FolderPickerState(
    val title: String,
    val initialPath: String,
    val protocol: StorageProtocol,
    val connection: NetworkConnection?,
    val isFolderB: Boolean
)

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
    val minSizeMB by viewModel.minSizeMB.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val statusText by viewModel.scanStatusText.collectAsState()
    val metricsText by viewModel.scanMetricsText.collectAsState()

    val searchResults by viewModel.sortedSearchResults.collectAsState()
    val searchSortField by viewModel.searchSortField.collectAsState()
    val searchSortAscending by viewModel.searchSortAscending.collectAsState()
    val selectedSearchPaths by viewModel.selectedSearchPaths.collectAsState()
    val deviceSummaries by viewModel.deviceSummaries.collectAsState()
    val selectedDeviceFilter by viewModel.selectedDeviceFilter.collectAsState()
    val duplicateResults by viewModel.duplicateResults.collectAsState()
    val selectedDuplicatePaths by viewModel.selectedDuplicatePaths.collectAsState()

    val savedConnections by viewModel.savedConnections.collectAsState()
    val defaultLocalRoot = remember { viewModel.defaultLocalRoot }

    var itemToDeleteConfirm by remember { mutableStateOf<MediaSearchItem?>(null) }
    var showConfirmDeleteDuplicates by remember { mutableStateOf(false) }
    var showConfirmDeleteSelectedSearch by remember { mutableStateOf(false) }

    SmartSearchScreenContent(
        toolIndex = toolIndex,
        folderA = folderA,
        folderB = folderB,
        compareTwoFolders = compareTwoFolders,
        isRecursive = isRecursive,
        searchWord = searchWord,
        selectedCategory = selectedCategory,
        selectedOrientation = selectedOrientation,
        minSizeMB = minSizeMB,
        searchSortField = searchSortField,
        searchSortAscending = searchSortAscending,
        selectedSearchPaths = selectedSearchPaths,
        isScanning = isScanning,
        scanProgress = scanProgress,
        statusText = statusText,
        metricsText = metricsText,
        searchResults = searchResults,
        deviceSummaries = deviceSummaries,
        selectedDeviceFilter = selectedDeviceFilter,
        duplicateResults = duplicateResults,
        selectedDuplicatePaths = selectedDuplicatePaths,
        savedConnections = savedConnections,
        defaultLocalRoot = defaultLocalRoot,
        vfsManager = viewModel.vfsManager,
        onSetToolIndex = { viewModel.setSearchToolIndex(it) },
        onSetFolderAItem = { viewModel.setFolderAsSearchTarget(it, asFolderB = false) },
        onSetFolderBItem = { viewModel.setFolderAsSearchTarget(it, asFolderB = true) },
        onSetCompareTwoFolders = { viewModel.setCompareTwoFolders(it) },
        onSetDuplicateTargetShortcut = { viewModel.setDuplicateTargetShortcut(it) },
        onSetSearchWord = { viewModel.setSearchWord(it) },
        onSetSelectedCategory = { viewModel.setSelectedCategory(it) },
        onSetSelectedOrientation = { viewModel.setSelectedOrientation(it) },
        onSetMinSizeMB = { viewModel.setMinSizeMB(it) },
        onSetSearchSort = { field, asc -> viewModel.setSearchSort(field, asc) },
        onToggleSearchSelection = { viewModel.toggleSearchSelection(it) },
        onSelectAllSearchResults = { viewModel.selectAllSearchResults() },
        onClearSearchSelection = { viewModel.clearSearchSelection() },
        onRequestDeleteSelectedSearch = { showConfirmDeleteSelectedSearch = true },
        onShareSelectedSearch = { viewModel.shareSelectedSearchResults() },
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
        onDeleteItem = { itemToDeleteConfirm = it },
        onToggleDuplicateSelection = { viewModel.toggleDuplicateSelection(it) },
        onSelectDuplicateCopiesOnly = { viewModel.selectDuplicateCopiesOnly() },
        onRequestDeleteDuplicates = { showConfirmDeleteDuplicates = true },
        onOpenDuplicate = { viewModel.openIntegratedPreview(it.item) },
        onShareDuplicate = { viewModel.shareItem(it.item) },
        onDeleteDuplicate = { itemToDeleteConfirm = MediaSearchItem(it.item) }
    )

    // Diálogo de Confirmar Eliminación para Un Elemento
    if (itemToDeleteConfirm != null) {
        val item = itemToDeleteConfirm!!
        GlassAlertDialog(
            onDismissRequest = { itemToDeleteConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon = Icons.Default.Warning, tint = Danger, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Confirmar eliminación",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "¿Deseas eliminar permanentemente '${item.item.name}'? Esta acción liberará espacio pero no se puede deshacer.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = itemToDeleteConfirm
                        itemToDeleteConfirm = null
                        if (target != null) viewModel.deleteItem(target.item)
                    }
                ) {
                    Text("Eliminar", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteConfirm = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    // Diálogo de Confirmar Eliminación para Duplicados Seleccionados
    if (showConfirmDeleteDuplicates) {
        GlassAlertDialog(
            onDismissRequest = { showConfirmDeleteDuplicates = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon = Icons.Default.Warning, tint = Danger, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Confirmar eliminación de duplicados",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "¿Deseas eliminar permanentemente los ${selectedDuplicatePaths.size} archivos duplicados seleccionados? Esta acción liberará almacenamiento en tu dispositivo y no se puede deshacer.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteDuplicates = false
                        viewModel.deleteSelectedDuplicates()
                    }
                ) {
                    Text("Eliminar", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDuplicates = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    // Diálogo de Confirmar Eliminación para Resultados de Búsqueda Seleccionados
    if (showConfirmDeleteSelectedSearch) {
        val selectedItems = searchResults.filter { it.item.path in selectedSearchPaths }
        val totalBytesToFree = selectedItems.sumOf { it.item.sizeBytes }
        GlassAlertDialog(
            onDismissRequest = { showConfirmDeleteSelectedSearch = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon = Icons.Default.Warning, tint = Danger, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Confirmar eliminación",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "¿Deseas eliminar permanentemente los ${selectedSearchPaths.size} archivos seleccionados? Se liberarán ${formatBytes(totalBytesToFree)} en tu dispositivo y esta acción no se puede deshacer.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteSelectedSearch = false
                        viewModel.deleteSelectedSearchResults()
                    }
                ) {
                    Text("Eliminar", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteSelectedSearch = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
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
    minSizeMB: Float,
    searchSortField: SearchSortField = SearchSortField.DATE,
    searchSortAscending: Boolean = false,
    selectedSearchPaths: Set<String> = emptySet(),
    isScanning: Boolean,
    scanProgress: Float,
    statusText: String,
    metricsText: String,
    searchResults: List<MediaSearchItem>,
    deviceSummaries: List<DeviceSummary>,
    selectedDeviceFilter: String?,
    duplicateResults: List<DuplicateFileEntry>,
    selectedDuplicatePaths: Set<String>,
    savedConnections: List<NetworkConnection> = emptyList(),
    defaultLocalRoot: String = "/storage/emulated/0",
    modifier: Modifier = Modifier,
    vfsManager: VfsManager? = null,
    onSetToolIndex: (Int) -> Unit = {},
    onSetFolderAItem: (VfsItem) -> Unit = {},
    onSetFolderBItem: (VfsItem) -> Unit = {},
    onSetCompareTwoFolders: (Boolean) -> Unit = {},
    onSetDuplicateTargetShortcut: (String) -> Unit = {},
    onSetSearchWord: (String) -> Unit = {},
    onSetSelectedCategory: (FileCategory) -> Unit = {},
    onSetSelectedOrientation: (MediaOrientation) -> Unit = {},
    onSetMinSizeMB: (Float) -> Unit = {},
    onSetSearchSort: (SearchSortField, Boolean) -> Unit = { _, _ -> },
    onToggleSearchSelection: (String) -> Unit = {},
    onSelectAllSearchResults: () -> Unit = {},
    onClearSearchSelection: () -> Unit = {},
    onRequestDeleteSelectedSearch: () -> Unit = {},
    onShareSelectedSearch: () -> Unit = {},
    onStartScan: () -> Unit = {},
    onCancelScan: () -> Unit = {},
    onExportCsv: (Boolean) -> Unit = {},
    onSelectDeviceFilter: (String?) -> Unit = {},
    onOpenItem: (MediaSearchItem) -> Unit = {},
    onShareItem: (MediaSearchItem) -> Unit = {},
    onOpenParentFolder: (VfsItem) -> Unit = {},
    onDeleteItem: (MediaSearchItem) -> Unit = {},
    onToggleDuplicateSelection: (String) -> Unit = {},
    onSelectDuplicateCopiesOnly: () -> Unit = {},
    onRequestDeleteDuplicates: () -> Unit = {},
    onOpenDuplicate: (DuplicateFileEntry) -> Unit = {},
    onShareDuplicate: (DuplicateFileEntry) -> Unit = {},
    onDeleteDuplicate: (DuplicateFileEntry) -> Unit = {}
) {
    var folderPickerState by remember { mutableStateOf<FolderPickerState?>(null) }

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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Selector de Modo (Búsqueda Inteligente vs Limpiar Duplicados)
            item {
                GlassSegmentedControl(
                    options = listOf(
                        "Búsqueda Inteligente" to Icons.Default.Search,
                        "Limpiar Duplicados" to Icons.Default.ContentCopy
                    ),
                    selectedIndex = toolIndex,
                    onSelect = onSetToolIndex
                )
            }

            if (toolIndex == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GlassSearchField(
                            value = searchWord,
                            onValueChange = onSetSearchWord,
                            placeholder = "Buscar por nombre o formato...",
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )

                        StorageSelectorIconDropdown(
                            currentFolder = folderA,
                            savedConnections = savedConnections,
                            defaultLocalRoot = defaultLocalRoot,
                            onSelectFolder = onSetFolderAItem,
                            isFolderB = false,
                            onRequestPicker = { item, isFolderB ->
                                val conn = savedConnections.find { it.id == item.connectionId }
                                folderPickerState = FolderPickerState(
                                    title = "Seleccionar Carpeta A en ${item.name}",
                                    initialPath = item.path.ifBlank { "/" },
                                    protocol = item.protocol,
                                    connection = conn,
                                    isFolderB = isFolderB
                                )
                            }
                        )

                        val categoryOptions = listOf(
                            FileCategory.ALL,
                            FileCategory.IMAGE,
                            FileCategory.VIDEO,
                            FileCategory.AUDIO,
                            FileCategory.DOCUMENT,
                            FileCategory.APK
                        )
                        IconOnlyDropdownFilter(
                            icon = Icons.Default.Category,
                            title = "Tipo de archivo",
                            value = selectedCategory,
                            options = categoryOptions,
                            optionLabel = { it.label },
                            onSelect = onSetSelectedCategory,
                            activeColor = Aqua
                        )

                        val orientationOptions = listOf(
                            MediaOrientation.ALL,
                            MediaOrientation.VERTICAL,
                            MediaOrientation.HORIZONTAL
                        )
                        val orientLabels = mapOf(
                            MediaOrientation.ALL to "Todas",
                            MediaOrientation.VERTICAL to "Vertical",
                            MediaOrientation.HORIZONTAL to "Horizontal"
                        )
                        IconOnlyDropdownFilter(
                            icon = Icons.Default.ScreenRotation,
                            title = "Orientación de imagen",
                            value = selectedOrientation,
                            options = orientationOptions,
                            optionLabel = { orientLabels[it] ?: "Todas" },
                            onSelect = onSetSelectedOrientation,
                            activeColor = Violet
                        )

                        // Selector de Orden
                        val sortOptions = listOf(
                            SearchSortField.DATE,
                            SearchSortField.SIZE,
                            SearchSortField.NAME,
                            SearchSortField.TYPE
                        )
                        IconOnlyDropdownFilter(
                            icon = Icons.AutoMirrored.Filled.Sort,
                            title = "Ordenar por",
                            value = searchSortField,
                            options = sortOptions,
                            optionLabel = { it.label },
                            onSelect = { onSetSearchSort(it, searchSortAscending) },
                            activeColor = Aqua
                        )

                        // Alternar Ascendente / Descendente
                        GlassIconButton(
                            icon = if (searchSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            onClick = { onSetSearchSort(searchSortField, !searchSortAscending) },
                            tint = Aqua,
                            contentDescription = if (searchSortAscending) "Ascendente" else "Descendente"
                        )
                    }
                }

                // Fila de Chips de Archivos Pesados y Tamaño
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pesados:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val heavySizes = listOf(100f, 500f, 1000f)
                        val heavyLabels = listOf("> 100 MB", "> 500 MB", "> 1 GB")
                        heavySizes.forEachIndexed { idx, sizeMB ->
                            val isSelected = minSizeMB == sizeMB
                            SelectChip(
                                text = heavyLabels[idx],
                                selected = isSelected,
                                onClick = {
                                    val newSize = if (isSelected) 0f else sizeMB
                                    onSetMinSizeMB(newSize)
                                    onSetSearchSort(SearchSortField.SIZE, false)
                                },
                                accent = WarningAmber
                            )
                        }

                        Spacer(Modifier.width(4.dp))
                        // Estado actual de orden visible
                        Text(
                            text = "${searchSortField.label} ${if (searchSortAscending) "↑" else "↓"}",
                            color = Aqua,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Fila de Acciones de Escaneo
                item {
                    if (isScanning) {
                        GlassSurface(
                            shape = GlassCardShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = statusText, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    SecondaryButton(
                                        text = "Detener",
                                        icon = Icons.Default.Stop,
                                        onClick = onCancelScan,
                                        tint = Danger
                                    )
                                }
                                AppProgressBar(progress = scanProgress)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PrimaryGlassButton(
                                text = "Escanear carpeta",
                                icon = Icons.AutoMirrored.Filled.ManageSearch,
                                onClick = onStartScan,
                                modifier = Modifier.weight(1f)
                            )
                            if (searchResults.isNotEmpty()) {
                                SecondaryGlassButton(
                                    text = "Exportar CSV",
                                    icon = Icons.Default.Description,
                                    onClick = { onExportCsv(true) },
                                    tint = Violet
                                )
                            }
                        }
                    }
                }

                if (metricsText.isNotEmpty() && !isScanning) {
                    item {
                        Text(
                            text = metricsText,
                            color = Aqua,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                // Resumen de archivos y tamaño
                if (filteredSearchResults.isNotEmpty() && !isScanning) {
                    item {
                        val totalBytes = filteredSearchResults.sumOf { it.item.sizeBytes }
                        Text(
                            text = "${filteredSearchResults.size} archivos · ${formatBytes(totalBytes)} en total",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                // Barra de Acciones de Selección Múltiple (Búsqueda)
                if (selectedSearchPaths.isNotEmpty() && !isScanning) {
                    item {
                        val selectedItems = filteredSearchResults.filter { it.item.path in selectedSearchPaths }
                        val selectedBytes = selectedItems.sumOf { it.item.sizeBytes }

                        GlassSurface(
                            shape = GlassCardShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedSearchPaths.size} seleccionados",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${formatBytes(selectedBytes)} seleccionados",
                                        color = WarningAmber,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SecondaryGlassButton(
                                        text = if (selectedSearchPaths.size == filteredSearchResults.size) "Deseleccionar" else "Todo",
                                        icon = Icons.Default.SelectAll,
                                        onClick = {
                                            if (selectedSearchPaths.size == filteredSearchResults.size) {
                                                onClearSearchSelection()
                                            } else {
                                                onSelectAllSearchResults()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        tint = Aqua
                                    )
                                    SecondaryGlassButton(
                                        text = "Compartir",
                                        icon = Icons.Default.Share,
                                        onClick = onShareSelectedSearch,
                                        modifier = Modifier.weight(1f),
                                        tint = Violet
                                    )
                                    PrimaryGlassButton(
                                        text = "Eliminar",
                                        icon = Icons.Default.Delete,
                                        onClick = onRequestDeleteSelectedSearch,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SecondaryButton(
                                        text = "Cancelar",
                                        icon = Icons.Default.Close,
                                        onClick = onClearSearchSelection,
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Lista de Resultados
                if (filteredSearchResults.isEmpty() && !isScanning) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = if (searchResults.isEmpty()) "Aún no hay resultados de búsqueda" else "Sin coincidencias con los filtros",
                            message = if (searchResults.isEmpty()) "Ingresa tus criterios y pulsa 'Escanear carpeta' para iniciar la búsqueda en el almacenamiento." else "Intenta ajustar los filtros de tipo, orientación o cámara arriba."
                        )
                    }
                } else {
                    items(filteredSearchResults, key = { it.item.path }) { entry ->
                        val isSelected = entry.item.path in selectedSearchPaths
                        val isSelectionMode = selectedSearchPaths.isNotEmpty()
                        MediaSearchResultCard(
                            entry = entry,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            vfsManager = vfsManager,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSearchSelection(entry.item.path)
                                } else {
                                    onOpenItem(entry)
                                }
                            },
                            onLongClick = {
                                onToggleSearchSelection(entry.item.path)
                            },
                            onToggleSelect = {
                                onToggleSearchSelection(entry.item.path)
                            },
                            onShare = { onShareItem(entry) },
                            onOpenFolder = { onOpenParentFolder(entry.item) },
                            onDelete = { onDeleteItem(entry) }
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StorageSelectorIconDropdown(
                                currentFolder = folderA,
                                savedConnections = savedConnections,
                                defaultLocalRoot = defaultLocalRoot,
                                onSelectFolder = onSetFolderAItem,
                                isFolderB = false,
                                onRequestPicker = { item, isFolderB ->
                                    val conn = savedConnections.find { it.id == item.connectionId }
                                    folderPickerState = FolderPickerState(
                                        title = "Seleccionar Carpeta A en ${item.name}",
                                        initialPath = item.path.ifBlank { "/" },
                                        protocol = item.protocol,
                                        connection = conn,
                                        isFolderB = isFolderB
                                    )
                                }
                            )

                            GlassSurface(
                                shape = GlassChipShape,
                                fillColor = GlassFill,
                                blur = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetCompareTwoFolders(!compareTwoFolders) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (compareTwoFolders) "Modo: Carpeta A vs B" else "Modo: Solo Carpeta A",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (compareTwoFolders) {
                            GlassSurface(
                                shape = GlassChipShape,
                                fillColor = GlassFill,
                                blur = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("B:", color = WarningAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = folderB?.name ?: "Elegir B",
                                            color = if (folderB != null) TextPrimary else TextMuted,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    StorageSelectorIconDropdown(
                                        currentFolder = folderB ?: folderA,
                                        savedConnections = savedConnections,
                                        defaultLocalRoot = defaultLocalRoot,
                                        onSelectFolder = onSetFolderBItem,
                                        isFolderB = true,
                                        onRequestPicker = { item, isFolderB ->
                                            val conn = savedConnections.find { it.id == item.connectionId }
                                            folderPickerState = FolderPickerState(
                                                title = "Seleccionar Carpeta B en ${item.name}",
                                                initialPath = item.path.ifBlank { "/" },
                                                protocol = item.protocol,
                                                connection = conn,
                                                isFolderB = true
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón Principal de Búsqueda
                item {
                    if (isScanning) {
                        GlassSurface(
                            shape = GlassCardShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = statusText, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    SecondaryButton(
                                        text = "Detener",
                                        icon = Icons.Default.Stop,
                                        onClick = onCancelScan,
                                        tint = Danger
                                    )
                                }
                                AppProgressBar(progress = scanProgress)
                            }
                        }
                    } else {
                        PrimaryGlassButton(
                            text = "Buscar archivos repetidos",
                            icon = Icons.Default.FindReplace,
                            onClick = onStartScan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (duplicateResults.isNotEmpty() && !isScanning) {
                    item {
                        val wastedBytes = duplicateResults.filter { !it.isPrimary }.sumOf { it.item.sizeBytes }
                        val groupCount = duplicateResults.map { it.groupId }.distinct().size

                        GlassSurface(
                            shape = GlassCardShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$groupCount grupos de duplicados",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${formatBytes(wastedBytes)} ocupados",
                                        color = WarningAmber,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SecondaryGlassButton(
                                        text = "Marcar copias sobrantes",
                                        icon = Icons.Default.DoneAll,
                                        onClick = onSelectDuplicateCopiesOnly,
                                        modifier = Modifier.weight(1f),
                                        tint = Violet
                                    )
                                    PrimaryGlassButton(
                                        text = "Eliminar (${selectedDuplicatePaths.size})",
                                        icon = Icons.Default.Delete,
                                        onClick = onRequestDeleteDuplicates,
                                        enabled = selectedDuplicatePaths.isNotEmpty()
                                    )
                                }
                            }
                        }
                    }
                }

                // Lista de Duplicados
                if (duplicateResults.isEmpty() && !isScanning) {
                    item {
                        EmptyState(
                            icon = Icons.Default.ContentCopy,
                            title = "Sin duplicados analizados",
                            message = "Selecciona una ubicación y pulsa 'Buscar archivos repetidos' para detectar copias exactas por contenido."
                        )
                    }
                } else {
                    items(duplicateResults, key = { "${it.groupId}_${it.item.path}" }) { entry ->
                        val isSelected = entry.item.path in selectedDuplicatePaths
                        DuplicateFileCard(
                            entry = entry,
                            isSelected = isSelected,
                            vfsManager = vfsManager,
                            onClick = { onOpenDuplicate(entry) },
                            onToggleSelect = { onToggleDuplicateSelection(entry.item.path) },
                            onShare = { onShareDuplicate(entry) },
                            onDelete = { onDeleteDuplicate(entry) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo Selector de Carpetas
    if (folderPickerState != null && vfsManager != null) {
        val state = folderPickerState!!
        FolderPickerDialog(
            title = state.title,
            initialPath = state.initialPath,
            protocol = state.protocol,
            connection = state.connection,
            vfsManager = vfsManager,
            onFolderSelected = { selectedSubfolder ->
                if (state.isFolderB) {
                    onSetFolderBItem(selectedSubfolder)
                } else {
                    onSetFolderAItem(selectedSubfolder)
                }
                folderPickerState = null
            },
            onDismiss = { folderPickerState = null }
        )
    }
}

@Composable
private fun StorageSelectorIconDropdown(
    currentFolder: VfsItem,
    savedConnections: List<NetworkConnection>,
    defaultLocalRoot: String,
    onSelectFolder: (VfsItem) -> Unit,
    isFolderB: Boolean,
    onRequestPicker: (VfsItem, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        GlassIconButton(
            icon = when (currentFolder.protocol) {
                StorageProtocol.LOCAL -> Icons.Default.PhoneAndroid
                StorageProtocol.SMB -> Icons.Default.Lan
                StorageProtocol.FTP -> Icons.Default.Lan
            },
            onClick = { expanded = true },
            tint = Violet,
            contentDescription = "Elegir ubicación"
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = CardDark,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            DropdownMenuItem(
                text = { Text("Almacenamiento interno", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Aqua) },
                onClick = {
                    expanded = false
                    val rootItem = VfsItem(
                        path = defaultLocalRoot,
                        name = "Almacenamiento Interno",
                        parentPath = "/",
                        isDirectory = true,
                        protocol = StorageProtocol.LOCAL
                    )
                    onSelectFolder(rootItem)
                }
            )

            val downloadsItem = VfsItem(
                path = "$defaultLocalRoot/Download",
                name = "Descargas",
                parentPath = defaultLocalRoot,
                isDirectory = true,
                protocol = StorageProtocol.LOCAL
            )
            DropdownMenuItem(
                text = { Text("Carpeta Descargas", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Violet) },
                onClick = {
                    expanded = false
                    onSelectFolder(downloadsItem)
                }
            )

            val cameraItem = VfsItem(
                path = "$defaultLocalRoot/DCIM/Camera",
                name = "Cámara",
                parentPath = "$defaultLocalRoot/DCIM",
                isDirectory = true,
                protocol = StorageProtocol.LOCAL
            )
            DropdownMenuItem(
                text = { Text("Carpeta Cámara (DCIM)", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Aqua) },
                onClick = {
                    expanded = false
                    onSelectFolder(cameraItem)
                }
            )

            if (savedConnections.isNotEmpty()) {
                HorizontalDivider(color = GlassBorder)
                savedConnections.forEach { conn ->
                    DropdownMenuItem(
                        text = { Text("${conn.name} (${conn.protocol.name})", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null, tint = Violet) },
                        onClick = {
                            expanded = false
                            val netItem = VfsItem(
                                path = conn.initialPath.ifBlank { "/" },
                                name = conn.name,
                                parentPath = "/",
                                isDirectory = true,
                                protocol = conn.protocol,
                                connectionId = conn.id
                            )
                            onSelectFolder(netItem)
                        }
                    )
                }
            }

            HorizontalDivider(color = GlassBorder)
            DropdownMenuItem(
                text = { Text("Elegir subcarpeta específica...", color = Aqua, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Aqua) },
                onClick = {
                    expanded = false
                    onRequestPicker(currentFolder, isFolderB)
                }
            )
        }
    }
}

@Composable
private fun <T> IconOnlyDropdownFilter(
    icon: ImageVector,
    title: String,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    activeColor: Color = Aqua
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        GlassIconButton(
            icon = icon,
            onClick = { expanded = true },
            tint = activeColor,
            contentDescription = title
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = CardDark,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel(opt),
                            color = if (opt == value) activeColor else TextPrimary,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaSearchResultCard(
    entry: MediaSearchItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    vfsManager: VfsManager? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onShare: () -> Unit = {},
    onOpenFolder: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    GlassSurface(
        shape = GlassChipShape,
        fillColor = if (isSelected) SelectedViolet else GlassFill,
        borderColor = if (isSelected) Aqua.copy(alpha = 0.6f) else GlassBorder,
        blur = false,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                IconButton(onClick = onToggleSelect, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Seleccionar",
                        tint = if (isSelected) Aqua else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            FileThumbnailBox(
                item = entry.item,
                vfsManager = vfsManager,
                cornerRadius = 9.dp,
                showBadge = true,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
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
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = entry.item.sizeFormatted,
                        color = Aqua,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
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
                        Tag(text = entry.deviceModel, color = Violet)
                    }
                    if (entry.orientation == "Vertical") {
                        Tag(text = "Vertical", color = Aqua)
                    } else if (entry.orientation == "Horizontal") {
                        Tag(text = "Horizontal", color = Violet)
                    }
                    if (entry.resolution.isNotBlank()) {
                        Tag(text = entry.resolution, color = TextMuted)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${entry.item.protocol.name} • ${entry.item.parentPath}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSelectionMode) {
                        Row {
                            IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Violet, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onOpenFolder, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Ir a carpeta", tint = Aqua, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Danger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateFileCard(
    entry: DuplicateFileEntry,
    isSelected: Boolean,
    vfsManager: VfsManager? = null,
    onClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    GlassSurface(
        shape = GlassChipShape,
        fillColor = if (isSelected) SelectedViolet else GlassFill,
        borderColor = if (entry.isPrimary) Aqua.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.6f),
        blur = false,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleSelect, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = "Marcar duplicado",
                    tint = if (isSelected) Aqua else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(6.dp))

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
                    Text(
                        text = "[Grupo #${entry.groupId} • ${entry.sourceLabel}] ${if (entry.isPrimary) "Original" else "Copia Duplicada"}",
                        color = if (entry.isPrimary) Aqua else WarningAmber,
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
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Violet, modifier = Modifier.size(16.dp))
                }
                if (!entry.isPrimary) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar duplicado", tint = Danger, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPickerDialog(
    title: String,
    initialPath: String,
    protocol: StorageProtocol,
    connection: NetworkConnection?,
    vfsManager: VfsManager,
    onFolderSelected: (VfsItem) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPath by remember { mutableStateOf(initialPath.ifBlank { "/" }) }
    var folderItems by remember { mutableStateOf<List<VfsItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPath) {
        isLoading = true
        errorMsg = null
        try {
            val list = vfsManager.listDirectory(
                path = currentPath,
                protocol = protocol,
                connection = connection
            )
            folderItems = list.filter { it.isDirectory }
            isLoading = false
        } catch (e: Exception) {
            errorMsg = e.localizedMessage ?: "Error al listar directorios"
            isLoading = false
        }
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = currentPath, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentPath != "/" && currentPath != "/storage/emulated/0") {
                    Surface(
                        onClick = {
                            val parent = currentPath.substringBeforeLast('/', "/")
                            currentPath = if (parent.isBlank()) "/" else parent
                        },
                        color = GlassFill,
                        shape = GlassChipShape,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Violet, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(".. (Subir carpeta)", color = Violet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Aqua, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    }
                } else if (errorMsg != null) {
                    Text(text = errorMsg!!, color = Danger, fontSize = 12.sp)
                } else if (folderItems.isEmpty()) {
                    Text("No hay subcarpetas en este nivel", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(folderItems, key = { it.path }) { item ->
                            Surface(
                                onClick = { currentPath = item.path },
                                color = GlassFill,
                                shape = GlassChipShape,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Aqua, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.name, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedItem = VfsItem(
                        path = currentPath,
                        name = currentPath.substringAfterLast('/', currentPath),
                        parentPath = currentPath.substringBeforeLast('/', "/"),
                        isDirectory = true,
                        protocol = protocol,
                        connectionId = connection?.id
                    )
                    onFolderSelected(selectedItem)
                }
            ) {
                Text("Usar esta carpeta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
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
            duplicateResults = emptyList(),
            selectedDuplicatePaths = emptySet()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
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

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
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
            ),
            isSelected = false
        )
    }
}
