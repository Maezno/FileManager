package com.maez.filemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.filemanager.model.ClipboardState
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.SortField
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import com.maez.filemanager.services.ConnectionStorage
import com.maez.filemanager.services.VfsManager
import com.maez.filemanager.ui.components.CardShape
import com.maez.filemanager.ui.components.ControlShape
import com.maez.filemanager.ui.components.EmptyState
import com.maez.filemanager.ui.components.FileThumbnailBox
import com.maez.filemanager.ui.components.GlassAlertDialog
import com.maez.filemanager.ui.components.GlassDropdownMenu
import com.maez.filemanager.ui.components.GlassIconButton
import com.maez.filemanager.ui.components.GlassSearchField
import com.maez.filemanager.ui.components.GlassSurface
import com.maez.filemanager.ui.components.IconBadge
import com.maez.filemanager.ui.components.IconBubble
import com.maez.filemanager.ui.components.PrimaryButton
import com.maez.filemanager.ui.components.SecondaryButton
import com.maez.filemanager.ui.components.SelectChip
import com.maez.filemanager.ui.components.appTextFieldColors
import com.maez.filemanager.ui.theme.Aqua
import com.maez.filemanager.ui.theme.BgElevated
import com.maez.filemanager.ui.theme.CardDark
import com.maez.filemanager.ui.theme.Danger
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.GlassBorder
import com.maez.filemanager.ui.theme.GlassCardShape
import com.maez.filemanager.ui.theme.GlassChipShape
import com.maez.filemanager.ui.theme.GlassFill
import com.maez.filemanager.ui.theme.SelectedViolet
import com.maez.filemanager.ui.theme.SelectedVioletBorder
import com.maez.filemanager.ui.theme.TextMuted
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.TextSecondary
import com.maez.filemanager.ui.theme.Violet
import com.maez.filemanager.viewmodel.MainViewModel

data class BreadcrumbSegment(val label: String, val fullPath: String)

fun parseBreadcrumbs(path: String, protocol: StorageProtocol): List<BreadcrumbSegment> {
    if (path.isEmpty() || path == "/") return listOf(BreadcrumbSegment("Raíz", "/"))
    val segments = mutableListOf<BreadcrumbSegment>()
    if (protocol == StorageProtocol.LOCAL) {
        if (path.startsWith("/storage/emulated/0")) {
            segments.add(BreadcrumbSegment("Interno", "/storage/emulated/0"))
            val rel = path.removePrefix("/storage/emulated/0").trim('/')
            if (rel.isNotEmpty()) {
                var acc = "/storage/emulated/0"
                rel.split('/').forEach { part ->
                    acc += "/$part"
                    segments.add(BreadcrumbSegment(part, acc))
                }
            }
            return segments
        }
    }
    var acc = ""
    path.trim('/').split('/').forEach { part ->
        acc += "/$part"
        segments.add(BreadcrumbSegment(if (acc == "/") "Raíz" else part, acc))
    }
    return segments.ifEmpty { listOf(BreadcrumbSegment("Raíz", "/")) }
}

@Composable
fun ExplorerScreen(
    viewModel: MainViewModel,
    hasAllFilesAccess: Boolean = true,
    onRequestStoragePermissions: () -> Unit = {}
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val currentProtocol by viewModel.currentProtocol.collectAsState()
    val activeConnection by viewModel.activeConnection.collectAsState()
    val activeShortcutKey by viewModel.activeShortcutKey.collectAsState()
    val rememberedTabPaths by viewModel.rememberedTabPaths.collectAsState()
    val items by viewModel.directoryItems.collectAsState()
    val isLoading by viewModel.isExplorerLoading.collectAsState()
    val errorText by viewModel.explorerError.collectAsState()
    val quickFilter by viewModel.quickFilterQuery.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortAsc by viewModel.sortAscending.collectAsState()
    val isGrid by viewModel.isGridView.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val savedConnections by viewModel.savedConnections.collectAsState()
    val confirmBeforeDelete by viewModel.confirmBeforeDelete.collectAsState()

    ExplorerScreenContent(
        currentPath = currentPath,
        currentProtocol = currentProtocol,
        activeConnection = activeConnection,
        activeShortcutKey = activeShortcutKey,
        rememberedTabPaths = rememberedTabPaths,
        items = items,
        isLoading = isLoading,
        errorText = errorText,
        quickFilter = quickFilter,
        sortField = sortField,
        sortAsc = sortAsc,
        isGrid = isGrid,
        clipboard = clipboard,
        selectedPaths = selectedPaths,
        savedConnections = savedConnections,
        confirmBeforeDelete = confirmBeforeDelete,
        hasAllFilesAccess = hasAllFilesAccess,
        onRequestStoragePermissions = onRequestStoragePermissions,
        vfsManager = viewModel.vfsManager,
        getShortcutRootPath = { key -> viewModel.getShortcutRootPath(key) },
        onConnectAndBrowseNetwork = { viewModel.connectAndBrowseNetwork(it) },
        onResetTabToRoot = { key, conn, subFolder -> viewModel.resetTabToRoot(key, subFolder = subFolder, conn = conn) },
        onOpenQuickLocalFolder = { viewModel.openQuickLocalFolder(it) },
        onNavigateUp = { viewModel.navigateUp() },
        onNavigateToPath = { viewModel.loadDirectory(it, currentProtocol, activeConnection) },
        onRefreshDirectory = { viewModel.refreshCurrentDirectory() },
        onCreateFolder = { viewModel.createNewFolder(it) },
        onSetFolderA = { viewModel.setCurrentFolderAsSearchFolderA() },
        onSetFolderB = { viewModel.setCurrentFolderAsSearchFolderB() },
        onClearSelection = { viewModel.clearSelection() },
        onSelectAll = { viewModel.selectAllCurrent(it) },
        onCopySelected = { viewModel.copySelectedToClipboard(isMove = false) },
        onCutSelected = { viewModel.copySelectedToClipboard(isMove = true) },
        onShareSelected = { viewModel.shareSelectedItems() },
        onDeleteSelected = { viewModel.deleteSelectedItems() },
        onPasteClipboard = { viewModel.pasteClipboardHere() },
        onClearClipboard = { viewModel.clearClipboard() },
        onSetQuickFilter = { viewModel.setQuickFilter(it) },
        onSetSorting = { field, asc -> viewModel.setSorting(field, asc) },
        onToggleGridView = { viewModel.toggleGridView() },
        onItemClick = { item ->
            if (selectedPaths.isNotEmpty()) {
                viewModel.toggleSelection(item)
            } else if (item.isDirectory) {
                viewModel.loadDirectory(item.path, item.protocol, activeConnection)
            } else {
                viewModel.openIntegratedPreview(item)
            }
        },
        onItemLongClick = { item -> viewModel.toggleSelection(item) },
        onToggleSelectPath = { item -> viewModel.toggleSelection(item) },
        onOpenExternal = { viewModel.openInExternalApp(it) },
        onShareItem = { viewModel.shareItem(it) },
        onCopySingle = { viewModel.copyToClipboard(it, isMove = false) },
        onMoveSingle = { viewModel.copyToClipboard(it, isMove = true) },
        onRenameItem = { item, newName -> viewModel.renameItem(item, newName) },
        onDeleteItem = { viewModel.deleteItem(it) },
        onSetItemFolderA = { viewModel.setFolderAsSearchTarget(it, asFolderB = false) },
        onSetItemFolderB = { viewModel.setFolderAsSearchTarget(it, asFolderB = true) },
        onPromptCredentials = { host, port, user, pass, isAnon ->
            viewModel.updateActiveConnectionCredentials(user, pass, isAnon, activeConnection?.shareName ?: "")
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerScreenContent(
    currentPath: String,
    currentProtocol: StorageProtocol,
    activeConnection: NetworkConnection?,
    activeShortcutKey: String?,
    rememberedTabPaths: Map<String, String>,
    items: List<VfsItem>,
    isLoading: Boolean,
    errorText: String?,
    quickFilter: String,
    sortField: SortField,
    sortAsc: Boolean,
    isGrid: Boolean,
    clipboard: ClipboardState,
    selectedPaths: Set<String>,
    savedConnections: List<NetworkConnection>,
    confirmBeforeDelete: Boolean = true,
    hasAllFilesAccess: Boolean = true,
    onRequestStoragePermissions: () -> Unit = {},
    modifier: Modifier = Modifier,
    vfsManager: VfsManager? = null,
    getShortcutRootPath: (String) -> String = { "/" },
    onConnectAndBrowseNetwork: (NetworkConnection) -> Unit = {},
    onResetTabToRoot: (String, NetworkConnection?, String?) -> Unit = { _, _, _ -> },
    onOpenQuickLocalFolder: (String?) -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onNavigateToPath: (String) -> Unit = {},
    onRefreshDirectory: () -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onSetFolderA: () -> Unit = {},
    onSetFolderB: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: (List<VfsItem>) -> Unit = {},
    onCopySelected: () -> Unit = {},
    onCutSelected: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onPasteClipboard: () -> Unit = {},
    onClearClipboard: () -> Unit = {},
    onSetQuickFilter: (String) -> Unit = {},
    onSetSorting: (SortField, Boolean) -> Unit = { _, _ -> },
    onToggleGridView: () -> Unit = {},
    onItemClick: (VfsItem) -> Unit = {},
    onItemLongClick: (VfsItem) -> Unit = {},
    onToggleSelectPath: (VfsItem) -> Unit = {},
    onOpenExternal: (VfsItem) -> Unit = {},
    onShareItem: (VfsItem) -> Unit = {},
    onCopySingle: (VfsItem) -> Unit = {},
    onMoveSingle: (VfsItem) -> Unit = {},
    onRenameItem: (VfsItem, String) -> Unit = { _, _ -> },
    onDeleteItem: (VfsItem) -> Unit = {},
    onSetItemFolderA: (VfsItem) -> Unit = {},
    onSetItemFolderB: (VfsItem) -> Unit = {},
    onPromptCredentials: (String, Int, String, String, Boolean) -> Unit = { _, _, _, _, _ -> }
) {
    val isSelectionMode = selectedPaths.isNotEmpty()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var renameTarget by remember { mutableStateOf<VfsItem?>(null) }
    var renameText by remember { mutableStateOf("") }

    var propertyItem by remember { mutableStateOf<VfsItem?>(null) }
    var itemToDeleteConfirm by remember { mutableStateOf<VfsItem?>(null) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showCredentialsDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(items, quickFilter) {
        if (quickFilter.isBlank()) items
        else items.filter { it.name.contains(quickFilter, ignoreCase = true) }
    }

    val now = System.currentTimeMillis()
    val (recentNetConns, olderNetConns) = remember(savedConnections, activeConnection, currentProtocol, now) {
        savedConnections.partition { conn ->
            (activeConnection?.id == conn.id && currentProtocol != StorageProtocol.LOCAL) ||
                (conn.lastUsedAt > 0L && (now - conn.lastUsedAt) <= ConnectionStorage.TAB_MEMORY_TIMEOUT_MS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Accesos rápidos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            @Composable
            fun renderNetworkChip(conn: NetworkConnection) {
                val key = "NET_${conn.id}"
                val rootPath = conn.initialPath.ifBlank { "/" }
                val isSelected = activeConnection?.id == conn.id && currentProtocol != StorageProtocol.LOCAL
                val remembered = rememberedTabPaths[key]
                val isNotAtRoot = if (isSelected) {
                    currentPath != rootPath
                } else {
                    remembered != null && remembered != rootPath
                }
                SelectChip(
                    text = "${conn.name} (${conn.protocol.name})",
                    icon = Icons.Default.Lan,
                    selected = isSelected,
                    onClick = { onConnectAndBrowseNetwork(conn) },
                    accent = Violet,
                    onClear = if (isNotAtRoot) {
                        { onResetTabToRoot(key, conn, null) }
                    } else null
                )
            }

            for (conn in recentNetConns) {
                renderNetworkChip(conn)
            }

            val localShortcuts = listOf(
                Triple("LOCAL_ROOT", "Interno", null as String?) to (Icons.Default.PhoneAndroid to Aqua),
                Triple("LOCAL_Download", "Descargas", "Download") to (Icons.Default.Download to Violet),
                Triple("LOCAL_DCIM", "Cámara", "DCIM/Camera") to (Icons.Default.CameraAlt to Aqua),
                Triple("LOCAL_Movies", "Videos", "Movies") to (Icons.Default.Movie to Violet),
                Triple("LOCAL_Pictures", "Imágenes", "Pictures") to (Icons.Default.Image to Aqua)
            )

            for ((info, style) in localShortcuts) {
                val (key, label, subFolder) = info
                val (icon, accent) = style
                val rootPath = getShortcutRootPath(key)
                val isSelected = currentProtocol == StorageProtocol.LOCAL && activeShortcutKey == key
                val remembered = rememberedTabPaths[key]
                val isNotAtRoot = if (isSelected) {
                    currentPath != rootPath
                } else {
                    remembered != null && remembered != rootPath
                }
                SelectChip(
                    text = label,
                    icon = icon,
                    selected = isSelected,
                    onClick = { onOpenQuickLocalFolder(subFolder) },
                    accent = accent,
                    onClear = if (isNotAtRoot) {
                        { onResetTabToRoot(key, null, subFolder) }
                    } else null
                )
            }

            for (conn in olderNetConns) {
                renderNetworkChip(conn)
            }
        }

        // 2. Tarjeta de Carpeta Actual (Segmentos de ruta)
        GlassSurface(
            shape = GlassCardShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(GlassChipShape)
                        .background(Violet.copy(alpha = 0.18f))
                        .clickable { onNavigateUp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Subir nivel",
                        tint = Violet,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val badge = when (currentProtocol) {
                        StorageProtocol.LOCAL -> "almacenamiento local"
                        StorageProtocol.SMB -> "red smb • ${activeConnection?.name ?: activeConnection?.host}"
                        StorageProtocol.FTP -> "red ftp • ${activeConnection?.name ?: activeConnection?.host}"
                    }
                    Text(
                        text = badge,
                        color = Violet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val segments = remember(currentPath, currentProtocol) { parseBreadcrumbs(currentPath, currentProtocol) }
                        segments.forEachIndexed { idx, segment ->
                            if (idx > 0) {
                                Text(" › ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            val isLast = idx == segments.lastIndex
                            Text(
                                text = segment.label,
                                color = if (isLast) TextPrimary else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (!isLast) Modifier.clickable { onNavigateToPath(segment.fullPath) }
                                        else Modifier
                                    )
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(icon = Icons.Default.Search, onClick = onSetFolderA, tint = Violet, contentDescription = "Buscar aquí (A)")
                    GlassIconButton(icon = Icons.AutoMirrored.Filled.CompareArrows, onClick = onSetFolderB, tint = Aqua, contentDescription = "Comparar Dups (B)")
                }
            }
        }

        // 3. Barra de Selección Múltiple
        if (isSelectionMode) {
            GlassSurface(
                shape = GlassChipShape,
                borderColor = Violet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onClearSelection, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar selección", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${selectedPaths.size} sel.",
                            color = Violet,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (selectedPaths.size == filteredItems.size) {
                                    onClearSelection()
                                } else {
                                    onSelectAll(filteredItems)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Seleccionar todo", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onCopySelected, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Violet, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onCutSelected, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Mover", tint = Aqua, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onShareSelected, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Violet, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                if (confirmBeforeDelete) {
                                    showDeleteSelectedConfirm = true
                                } else {
                                    onDeleteSelected()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Danger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 4. Portapapeles flotante
        if (clipboard.items.isNotEmpty()) {
            GlassSurface(
                shape = GlassChipShape,
                borderColor = Aqua,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val actionLabel = if (clipboard.isMove) "Mover" else "Copiar"
                    val summaryLabel = if (clipboard.items.size == 1) {
                        clipboard.items.first().name
                    } else {
                        "${clipboard.items.size} elementos"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (clipboard.isMove) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Aqua,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$actionLabel: $summaryLabel",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SecondaryButton(
                            text = "Pegar aquí",
                            icon = Icons.Default.ContentPaste,
                            onClick = onPasteClipboard,
                            tint = Aqua
                        )
                        IconButton(onClick = onClearClipboard, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Danger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 5. Barra compacta de herramientas con GlassSearchField y GlassIconButton
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassSearchField(
                value = quickFilter,
                onValueChange = onSetQuickFilter,
                placeholder = "Filtrar en esta carpeta...",
                height = 48.dp,
                modifier = Modifier.weight(1f)
            )

            Box {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = { showSortMenu = true },
                    tint = Violet,
                    contentDescription = "Ordenar"
                )
                GlassDropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortField.entries.forEach { field ->
                        DropdownMenuItem(
                            text = {
                                val arrow = if (sortField == field) (if (sortAsc) " ↑" else " ↓") else ""
                                Text("Ordenar por ${field.label}$arrow", color = TextPrimary, fontSize = 13.sp)
                            },
                            onClick = {
                                val nextAsc = if (sortField == field) !sortAsc else true
                                onSetSorting(field, nextAsc)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            GlassIconButton(
                icon = Icons.Default.Refresh,
                onClick = onRefreshDirectory,
                tint = TextMuted,
                contentDescription = "Actualizar"
            )

            GlassIconButton(
                icon = Icons.Default.CreateNewFolder,
                onClick = { showNewFolderDialog = true },
                tint = Aqua,
                contentDescription = "Nueva carpeta"
            )

            GlassIconButton(
                icon = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                onClick = onToggleGridView,
                tint = Violet,
                contentDescription = "Cambiar vista"
            )
        }

        Text(
            text = "${filteredItems.size} elementos",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Aqua, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
        } else if (errorText != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Folder,
                    title = "No se pudo acceder a la carpeta",
                    message = errorText,
                    tint = Danger
                )
                if (currentProtocol != StorageProtocol.LOCAL && (errorText.contains("autenticaci", ignoreCase = true) || errorText.contains("acceso", ignoreCase = true) || errorText.contains("inválid", ignoreCase = true))) {
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        text = "Ingresar usuario y contraseña",
                        icon = Icons.Default.Key,
                        onClick = { showCredentialsDialog = true }
                    )
                }
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Folder,
                    title = if (quickFilter.isBlank()) "Carpeta vacía" else "Sin coincidencias",
                    message = if (quickFilter.isBlank()) "No hay elementos en esta ubicación." else "Prueba con otra palabra clave en el filtro rápido."
                )
            }
        } else {
            val lazyListState = rememberLazyListState()
            val lazyGridState = rememberLazyGridState()

            var isDraggingSelection by remember { mutableStateOf(false) }
            var dragStartIndex by remember { mutableIntStateOf(-1) }
            var initialSelectionState by remember { mutableStateOf(false) }

            val updatedItems by rememberUpdatedState(filteredItems)
            val updatedSelectedPaths by rememberUpdatedState(selectedPaths)

            val dragGestureModifier = Modifier.pointerInput(filteredItems, isGrid) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val hitIndex = if (isGrid) {
                            val layoutInfo = lazyGridState.layoutInfo
                            layoutInfo.visibleItemsInfo.firstOrNull {
                                offset.x >= it.offset.x && offset.x <= (it.offset.x + it.size.width) &&
                                    offset.y >= it.offset.y && offset.y <= (it.offset.y + it.size.height)
                            }?.index
                        } else {
                            val layoutInfo = lazyListState.layoutInfo
                            layoutInfo.visibleItemsInfo.firstOrNull {
                                offset.y >= it.offset && offset.y <= (it.offset + it.size)
                            }?.index
                        }

                        if (hitIndex != null && hitIndex in updatedItems.indices) {
                            isDraggingSelection = true
                            dragStartIndex = hitIndex
                            val startItem = updatedItems[hitIndex]
                            initialSelectionState = startItem.path !in updatedSelectedPaths
                            onToggleSelectPath(startItem)
                        }
                    },
                    onDrag = { change, _ ->
                        if (isDraggingSelection && dragStartIndex != -1) {
                            val offset = change.position
                            val currentIndex = if (isGrid) {
                                val layoutInfo = lazyGridState.layoutInfo
                                layoutInfo.visibleItemsInfo.firstOrNull {
                                    offset.x >= it.offset.x && offset.x <= (it.offset.x + it.size.width) &&
                                        offset.y >= it.offset.y && offset.y <= (it.offset.y + it.size.height)
                                }?.index
                            } else {
                                val layoutInfo = lazyListState.layoutInfo
                                layoutInfo.visibleItemsInfo.firstOrNull {
                                    offset.y >= it.offset && offset.y <= (it.offset + it.size)
                                }?.index
                            }

                            if (currentIndex != null && currentIndex in updatedItems.indices) {
                                val minIdx = minOf(dragStartIndex, currentIndex)
                                val maxIdx = maxOf(dragStartIndex, currentIndex)
                                for (i in minIdx..maxIdx) {
                                    val item = updatedItems[i]
                                    val currentlySel = item.path in updatedSelectedPaths
                                    if (initialSelectionState && !currentlySel) {
                                        onToggleSelectPath(item)
                                    } else if (!initialSelectionState && currentlySel) {
                                        onToggleSelectPath(item)
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        isDraggingSelection = false
                        dragStartIndex = -1
                    },
                    onDragCancel = {
                        isDraggingSelection = false
                        dragStartIndex = -1
                    }
                )
            }

            if (isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    state = lazyGridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .then(dragGestureModifier),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredItems, key = { _, item -> item.path }) { _, item ->
                        val isSelected = item.path in selectedPaths
                        ExplorerGridCard(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            vfsManager = vfsManager,
                            connection = activeConnection,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) },
                            onToggleSelect = { onToggleSelectPath(item) },
                            onOpenExternal = { onOpenExternal(item) },
                            onShare = { onShareItem(item) },
                            onCopy = { onCopySingle(item) },
                            onMove = { onMoveSingle(item) },
                            onRename = {
                                renameTarget = item
                                renameText = item.name
                            },
                            onDelete = {
                                if (confirmBeforeDelete) {
                                    itemToDeleteConfirm = item
                                } else {
                                    onDeleteItem(item)
                                }
                            },
                            onSetFolderA = { onSetItemFolderA(item) },
                            onSetFolderB = { onSetItemFolderB(item) },
                            onShowProperties = { propertyItem = item }
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .then(dragGestureModifier),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredItems, key = { _, item -> item.path }) { _, item ->
                        val isSelected = item.path in selectedPaths
                        ExplorerListRow(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            vfsManager = vfsManager,
                            connection = activeConnection,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) },
                            onToggleSelect = { onToggleSelectPath(item) },
                            onOpenExternal = { onOpenExternal(item) },
                            onShare = { onShareItem(item) },
                            onCopy = { onCopySingle(item) },
                            onMove = { onMoveSingle(item) },
                            onRename = {
                                renameTarget = item
                                renameText = item.name
                            },
                            onDelete = {
                                if (confirmBeforeDelete) {
                                    itemToDeleteConfirm = item
                                } else {
                                    onDeleteItem(item)
                                }
                            },
                            onSetFolderA = { onSetItemFolderA(item) },
                            onSetFolderB = { onSetItemFolderB(item) },
                            onShowProperties = { propertyItem = item }
                        )
                    }
                }
            }
        }
    }

    // Modal Crear Nueva Carpeta
    if (showNewFolderDialog) {
        GlassAlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Crear nueva carpeta", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nombre de carpeta") },
                    singleLine = true,
                    colors = appTextFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    // Modal Renombrar Elemento
    renameTarget?.let { item ->
        GlassAlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renombrar elemento", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    colors = appTextFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank() && renameText != item.name) {
                            onRenameItem(item, renameText.trim())
                            renameTarget = null
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    // Diálogo de Propiedades
    propertyItem?.let { item ->
        val formattedDate = remember(item.lastModified) {
            if (item.lastModified > 0) {
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(item.lastModified))
            } else "Desconocida"
        }
        GlassAlertDialog(
            onDismissRequest = { propertyItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileThumbnailBox(item = item, modifier = Modifier.size(36.dp), vfsManager = vfsManager)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Propiedades",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text("nombre completo", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("ruta completa", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.path, color = TextMuted, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("protocolo", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.protocol.name, color = Violet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("tipo", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (item.isDirectory) "Carpeta de archivos" else "${item.extension.uppercase()} (${item.category.name})",
                                color = Aqua,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (!item.isDirectory) {
                        Column {
                            Text("tamaño", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${item.sizeFormatted} (${item.sizeBytes} bytes)", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                    Column {
                        Text("última modificación", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(formattedDate, color = TextMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { propertyItem = null }) {
                    Text("Cerrar", color = Violet, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo de Confirmación Eliminar Un Elemento
    itemToDeleteConfirm?.let { item ->
        GlassAlertDialog(
            onDismissRequest = { itemToDeleteConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon = Icons.Default.Delete, tint = Danger, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmar eliminación", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar '${item.name}'? Esta acción es permanente y no se podrá deshacer.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    itemToDeleteConfirm = null
                    onDeleteItem(item)
                }) {
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

    // Diálogo de Confirmación Eliminar Selección Múltiple
    if (showDeleteSelectedConfirm) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon = Icons.Default.Delete, tint = Danger, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar elementos seleccionados", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar los ${selectedPaths.size} elementos seleccionados? Esta acción es permanente y no se podrá deshacer.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteSelectedConfirm = false
                    onDeleteSelected()
                }) {
                    Text("Eliminar ${selectedPaths.size} elementos", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    // Modal para Credenciales cuando falla acceso
    if (showCredentialsDialog) {
        var userVal by remember { mutableStateOf(activeConnection?.username ?: "") }
        var passVal by remember { mutableStateOf(activeConnection?.password ?: "") }
        var isAnonVal by remember { mutableStateOf(activeConnection?.anonymous ?: false) }

        GlassAlertDialog(
            onDismissRequest = { showCredentialsDialog = false },
            title = { Text("Credenciales de acceso a red", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "La conexión a '${activeConnection?.name ?: currentPath}' requiere autenticación.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Switch(
                        checked = isAnonVal,
                        onCheckedChange = { isAnonVal = it }
                    )

                    if (!isAnonVal) {
                        OutlinedTextField(
                            value = userVal,
                            onValueChange = { userVal = it },
                            label = { Text("Usuario") },
                            singleLine = true,
                            colors = appTextFieldColors()
                        )
                        OutlinedTextField(
                            value = passVal,
                            onValueChange = { passVal = it },
                            label = { Text("Contraseña") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = appTextFieldColors()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val host = activeConnection?.host ?: ""
                        val port = activeConnection?.port ?: 445
                        onPromptCredentials(host, port, userVal, passVal, isAnonVal)
                        showCredentialsDialog = false
                    }
                ) {
                    Text("Reintentar acceso")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialsDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerListRow(
    item: VfsItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    vfsManager: VfsManager? = null,
    connection: NetworkConnection? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onOpenExternal: () -> Unit = {},
    onShare: () -> Unit = {},
    onCopy: () -> Unit = {},
    onMove: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSetFolderA: () -> Unit = {},
    onSetFolderB: () -> Unit = {},
    onShowProperties: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    GlassSurface(
        shape = GlassChipShape,
        fillColor = if (isSelected) SelectedViolet else GlassFill,
        borderColor = if (isSelected) SelectedVioletBorder else GlassBorder,
        blur = false,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                        contentDescription = null,
                        tint = if (isSelected) Violet else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            FileThumbnailBox(
                item = item,
                vfsManager = vfsManager,
                connection = connection,
                cornerRadius = 10.dp,
                showBadge = false,
                modifier = Modifier.size(42.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.isDirectory) "Carpeta" else item.sizeFormatted,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                ItemContextMenu(
                    item = item,
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onToggleSelect = onToggleSelect,
                    onOpenExternal = onOpenExternal,
                    onShare = onShare,
                    onCopy = onCopy,
                    onMove = onMove,
                    onRename = onRename,
                    onDelete = onDelete,
                    onSetFolderA = onSetFolderA,
                    onSetFolderB = onSetFolderB,
                    onShowProperties = onShowProperties
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerGridCard(
    item: VfsItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    vfsManager: VfsManager? = null,
    connection: NetworkConnection? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onOpenExternal: () -> Unit = {},
    onShare: () -> Unit = {},
    onCopy: () -> Unit = {},
    onMove: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSetFolderA: () -> Unit = {},
    onSetFolderB: () -> Unit = {},
    onShowProperties: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    GlassSurface(
        shape = GlassCardShape,
        fillColor = if (isSelected) SelectedViolet else GlassFill,
        borderColor = if (isSelected) SelectedVioletBorder else GlassBorder,
        blur = false,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                FileThumbnailBox(
                    item = item,
                    vfsManager = vfsManager,
                    connection = connection,
                    cornerRadius = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                )

                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Seleccionar",
                        tint = if (isSelected) Violet else TextMuted,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(20.dp)
                            .clickable { onToggleSelect() }
                    )
                }

                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .padding(2.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    ItemContextMenu(
                        item = item,
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onToggleSelect = onToggleSelect,
                        onOpenExternal = onOpenExternal,
                        onShare = onShare,
                        onCopy = onCopy,
                        onMove = onMove,
                        onRename = onRename,
                        onDelete = onDelete,
                        onSetFolderA = onSetFolderA,
                        onSetFolderB = onSetFolderB,
                        onShowProperties = onShowProperties
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.isDirectory) "Carpeta" else item.sizeFormatted,
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ItemContextMenu(
    item: VfsItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onToggleSelect: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSetFolderA: () -> Unit,
    onSetFolderB: () -> Unit,
    onShowProperties: () -> Unit
) {
    GlassDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Seleccionar", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Violet) },
            onClick = { onDismiss(); onToggleSelect() }
        )
        DropdownMenuItem(
            text = { Text("Detalles / Propiedades", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Aqua) },
            onClick = { onDismiss(); onShowProperties() }
        )
        if (item.isDirectory) {
            DropdownMenuItem(
                text = { Text("Analizar en Buscador (Carpeta A)", color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Violet) },
                onClick = { onDismiss(); onSetFolderA() }
            )
            DropdownMenuItem(
                text = { Text("Comparar Duplicados (Carpeta B)", color = TextPrimary) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = Aqua) },
                onClick = { onDismiss(); onSetFolderB() }
            )
        } else {
            DropdownMenuItem(
                text = { Text("Abrir con app externa", color = TextPrimary) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Aqua) },
                onClick = { onDismiss(); onOpenExternal() }
            )
            DropdownMenuItem(
                text = { Text("Compartir", color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Violet) },
                onClick = { onDismiss(); onShare() }
            )
            DropdownMenuItem(
                text = { Text("Copiar", color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Violet) },
                onClick = { onDismiss(); onCopy() }
            )
            DropdownMenuItem(
                text = { Text("Cortar / Mover", color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, tint = Aqua) },
                onClick = { onDismiss(); onMove() }
            )
        }
        DropdownMenuItem(
            text = { Text("Renombrar", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = TextMuted) },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text("Eliminar", color = Danger) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Danger) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun ExplorerScreenPreview() {
    FileManagerTheme {
        ExplorerScreenContent(
            currentPath = "/storage/emulated/0/Download",
            currentProtocol = StorageProtocol.LOCAL,
            activeConnection = null,
            activeShortcutKey = "LOCAL_Download",
            rememberedTabPaths = emptyMap(),
            items = listOf(
                VfsItem(
                    name = "Proyectos",
                    path = "/storage/emulated/0/Download/Proyectos",
                    parentPath = "/storage/emulated/0/Download",
                    sizeBytes = 0,
                    lastModified = System.currentTimeMillis(),
                    isDirectory = true,
                    protocol = StorageProtocol.LOCAL
                ),
                VfsItem(
                    name = "Foto_Vacaciones.jpg",
                    path = "/storage/emulated/0/Download/Foto_Vacaciones.jpg",
                    parentPath = "/storage/emulated/0/Download",
                    sizeBytes = 2_450_000,
                    lastModified = System.currentTimeMillis(),
                    isDirectory = false,
                    protocol = StorageProtocol.LOCAL
                ),
                VfsItem(
                    name = "Video_Presentacion.mp4",
                    path = "/storage/emulated/0/Download/Video_Presentacion.mp4",
                    parentPath = "/storage/emulated/0/Download",
                    sizeBytes = 15_800_000,
                    lastModified = System.currentTimeMillis(),
                    isDirectory = false,
                    protocol = StorageProtocol.LOCAL
                )
            ),
            isLoading = false,
            errorText = null,
            quickFilter = "",
            sortField = SortField.NAME,
            sortAsc = true,
            isGrid = false,
            clipboard = ClipboardState(),
            selectedPaths = emptySet(),
            savedConnections = emptyList()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun ExplorerListRowPreview() {
    FileManagerTheme {
        ExplorerListRow(
            item = VfsItem(
                name = "Manual_Usuario.pdf",
                path = "/storage/emulated/0/Manual_Usuario.pdf",
                parentPath = "/storage/emulated/0",
                sizeBytes = 3_400_000,
                lastModified = System.currentTimeMillis(),
                isDirectory = false,
                protocol = StorageProtocol.LOCAL
            ),
            isSelected = false,
            isSelectionMode = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun ExplorerGridCardPreview() {
    FileManagerTheme {
        Box(modifier = Modifier.width(130.dp)) {
            ExplorerGridCard(
                item = VfsItem(
                    name = "Foto_Atardecer.png",
                    path = "/storage/emulated/0/Foto_Atardecer.png",
                    parentPath = "/storage/emulated/0",
                    sizeBytes = 1_800_000,
                    lastModified = System.currentTimeMillis(),
                    isDirectory = false,
                    protocol = StorageProtocol.LOCAL
                ),
                isSelected = false,
                isSelectionMode = false
            )
        }
    }
}
