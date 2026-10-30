package com.maez.filemanager.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maez.filemanager.model.ClipboardState
import com.maez.filemanager.model.DeviceSummary
import com.maez.filemanager.model.DiscoveredHost
import com.maez.filemanager.model.DuplicateFileEntry
import com.maez.filemanager.model.FileCategory
import com.maez.filemanager.model.MediaOrientation
import com.maez.filemanager.model.MediaSearchItem
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.SortField
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import com.maez.filemanager.model.formatBytes
import com.maez.filemanager.services.ConnectionStorage
import com.maez.filemanager.services.SmartSearchEngine
import com.maez.filemanager.services.VfsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val vfsManager = VfsManager()
    private val searchEngine = SmartSearchEngine(application.applicationContext, vfsManager)
    private val connectionStorage = ConnectionStorage(application.applicationContext)

    // Navegación principal (0 = Explorador, 1 = Buscador Inteligente, 2 = Red SMB/FTP)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Vista previa integrada de Fotos y Videos (estilo MediaRecoveryMobile)
    private val _previewItem = MutableStateFlow<VfsItem?>(null)
    val previewItem: StateFlow<VfsItem?> = _previewItem.asStateFlow()

    private val _previewLocalPath = MutableStateFlow<String?>(null)
    val previewLocalPath: StateFlow<String?> = _previewLocalPath.asStateFlow()

    private val _isPreviewLoading = MutableStateFlow(false)
    val isPreviewLoading: StateFlow<Boolean> = _isPreviewLoading.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────────
    // ESTADO DEL EXPLORADOR DE ARCHIVOS (LOCAL + SMB + FTP)
    // ─────────────────────────────────────────────────────────────────────────────

    val defaultLocalRoot: String = Environment.getExternalStorageDirectory().absolutePath

    private val _currentPath = MutableStateFlow(defaultLocalRoot)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _currentProtocol = MutableStateFlow(StorageProtocol.LOCAL)
    val currentProtocol: StateFlow<StorageProtocol> = _currentProtocol.asStateFlow()

    private val _activeConnection = MutableStateFlow<NetworkConnection?>(null)
    val activeConnection: StateFlow<NetworkConnection?> = _activeConnection.asStateFlow()

    private val _directoryItems = MutableStateFlow<List<VfsItem>>(emptyList())
    val directoryItems: StateFlow<List<VfsItem>> = _directoryItems.asStateFlow()

    private val _isExplorerLoading = MutableStateFlow(false)
    val isExplorerLoading: StateFlow<Boolean> = _isExplorerLoading.asStateFlow()

    private val _explorerError = MutableStateFlow<String?>(null)
    val explorerError: StateFlow<String?> = _explorerError.asStateFlow()

    private val _quickFilterQuery = MutableStateFlow("")
    val quickFilterQuery: StateFlow<String> = _quickFilterQuery.asStateFlow()

    private val _sortField = MutableStateFlow(SortField.NAME)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _clipboard = MutableStateFlow(ClipboardState())
    val clipboard: StateFlow<ClipboardState> = _clipboard.asStateFlow()

    // Selección múltiple de archivos
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    // Pestañas rápidas con memoria de última carpeta visitada (y expiración de 20 min)
    private val _activeShortcutKey = MutableStateFlow("LOCAL_ROOT")
    val activeShortcutKey: StateFlow<String> = _activeShortcutKey.asStateFlow()

    private val _rememberedTabPaths = MutableStateFlow<Map<String, String>>(emptyMap())
    val rememberedTabPaths: StateFlow<Map<String, String>> = _rememberedTabPaths.asStateFlow()

    private val pathHistory = ArrayDeque<Triple<String, StorageProtocol, NetworkConnection?>>()

    // ─────────────────────────────────────────────────────────────────────────────
    // ESTADO DEL BUSCADOR INTELIGENTE (HERENCIA EVOLUTIVA DE MEDIARECOVERY)
    // ─────────────────────────────────────────────────────────────────────────────

    // 0 = Buscador & Orientación, 1 = Analizador Dispositivos/Prensa, 2 = Duplicados
    private val _searchToolIndex = MutableStateFlow(0)
    val searchToolIndex: StateFlow<Int> = _searchToolIndex.asStateFlow()

    private val _folderA = MutableStateFlow(
        VfsItem(
            path = defaultLocalRoot,
            name = "Almacenamiento Interno",
            parentPath = "",
            isDirectory = true,
            protocol = StorageProtocol.LOCAL
        )
    )
    val folderA: StateFlow<VfsItem> = _folderA.asStateFlow()

    private val _folderB = MutableStateFlow<VfsItem?>(null)
    val folderB: StateFlow<VfsItem?> = _folderB.asStateFlow()

    private val _compareTwoFolders = MutableStateFlow(false)
    val compareTwoFolders: StateFlow<Boolean> = _compareTwoFolders.asStateFlow()

    private val _isRecursiveScan = MutableStateFlow(true)
    val isRecursiveScan: StateFlow<Boolean> = _isRecursiveScan.asStateFlow()

    // Filtros de búsqueda
    private val _searchWord = MutableStateFlow("")
    val searchWord: StateFlow<String> = _searchWord.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FileCategory.ALL)
    val selectedCategory: StateFlow<FileCategory> = _selectedCategory.asStateFlow()

    private val _selectedOrientation = MutableStateFlow(MediaOrientation.ALL)
    val selectedOrientation: StateFlow<MediaOrientation> = _selectedOrientation.asStateFlow()

    private val _selectedMonth = MutableStateFlow("Todos")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow("Todos")
    val selectedYear: StateFlow<String> = _selectedYear.asStateFlow()

    private val _minSizeMB = MutableStateFlow(0f)
    val minSizeMB: StateFlow<Float> = _minSizeMB.asStateFlow()

    val monthsList = listOf(
        "Todos", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    val yearsList = listOf("Todos", "2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018")

    // Estado de progreso de escaneo
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStatusText = MutableStateFlow("Listo para buscar en carpetas locales o de red SMB/FTP.")
    val scanStatusText: StateFlow<String> = _scanStatusText.asStateFlow()

    private val _scanMetricsText = MutableStateFlow("")
    val scanMetricsText: StateFlow<String> = _scanMetricsText.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaSearchItem>>(emptyList())
    val searchResults: StateFlow<List<MediaSearchItem>> = _searchResults.asStateFlow()

    private val _deviceSummaries = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val deviceSummaries: StateFlow<List<DeviceSummary>> = _deviceSummaries.asStateFlow()

    private val _selectedDeviceFilter = MutableStateFlow<String?>(null)
    val selectedDeviceFilter: StateFlow<String?> = _selectedDeviceFilter.asStateFlow()

    private val _duplicateResults = MutableStateFlow<List<DuplicateFileEntry>>(emptyList())
    val duplicateResults: StateFlow<List<DuplicateFileEntry>> = _duplicateResults.asStateFlow()

    private var scanJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────────
    // ESTADO DE RED LOCAL (CONEXIONES SMB / FTP Y DESCUBRIMIENTO)
    // ─────────────────────────────────────────────────────────────────────────────

    private val _savedConnections = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val savedConnections: StateFlow<List<NetworkConnection>> = _savedConnections.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private val _isDiscoveringLan = MutableStateFlow(false)
    val isDiscoveringLan: StateFlow<Boolean> = _isDiscoveringLan.asStateFlow()

    private val _lanScanProgress = MutableStateFlow(0f)
    val lanScanProgress: StateFlow<Float> = _lanScanProgress.asStateFlow()

    init {
        _savedConnections.value = connectionStorage.loadConnections()
        _rememberedTabPaths.value = connectionStorage.loadAllValidTabPaths()
        val initialLocal = connectionStorage.getTabLastPath("LOCAL_ROOT", defaultLocalRoot)
        loadDirectory(initialLocal, StorageProtocol.LOCAL, null, pushHistory = false, shortcutKey = "LOCAL_ROOT")
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun getShortcutRootPath(shortcutKey: String, conn: NetworkConnection? = null): String {
        if (conn != null) return conn.initialPath.ifBlank { "/" }
        return when (shortcutKey) {
            "LOCAL_ROOT" -> defaultLocalRoot
            "LOCAL_Download" -> File(defaultLocalRoot, "Download").absolutePath
            "LOCAL_DCIM" -> File(defaultLocalRoot, "DCIM/Camera").absolutePath
            "LOCAL_Movies" -> File(defaultLocalRoot, "Movies").absolutePath
            "LOCAL_Pictures" -> File(defaultLocalRoot, "Pictures").absolutePath
            else -> defaultLocalRoot
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MÉTODOS DEL EXPLORADOR DE ARCHIVOS
    // ─────────────────────────────────────────────────────────────────────────────

    fun loadDirectory(
        path: String,
        protocol: StorageProtocol = _currentProtocol.value,
        connection: NetworkConnection? = _activeConnection.value,
        pushHistory: Boolean = true,
        shortcutKey: String? = null
    ) {
        if (pushHistory && (_currentPath.value != path || _currentProtocol.value != protocol)) {
            pathHistory.addLast(Triple(_currentPath.value, _currentProtocol.value, _activeConnection.value))
        }
        val resolvedShortcut = when {
            shortcutKey != null -> shortcutKey
            protocol != StorageProtocol.LOCAL && connection != null -> "NET_${connection.id}"
            else -> _activeShortcutKey.value.takeIf { !it.startsWith("NET_") } ?: "LOCAL_ROOT"
        }
        _activeShortcutKey.value = resolvedShortcut
        _currentPath.value = path
        _currentProtocol.value = protocol
        _activeConnection.value = connection
        _selectedPaths.value = emptySet()
        _explorerError.value = null
        _isExplorerLoading.value = true

        // Guardar última carpeta recordada en esta pestaña
        connectionStorage.saveTabLastPath(resolvedShortcut, path)
        _rememberedTabPaths.value = connectionStorage.loadAllValidTabPaths()

        // Si es una conexión de red, actualizar su marca de uso reciente para posicionarla primera
        if (protocol != StorageProtocol.LOCAL && connection != null) {
            markConnectionUsed(connection)
        }

        viewModelScope.launch {
            try {
                val rawItems = vfsManager.listDirectory(
                    path = path,
                    protocol = protocol,
                    connection = connection,
                    showHidden = _showHiddenFiles.value
                )
                _directoryItems.value = sortItems(rawItems)
            } catch (e: Exception) {
                _explorerError.value = e.localizedMessage ?: "No se pudo abrir la carpeta ($path)"
                _directoryItems.value = emptyList()
            } finally {
                _isExplorerLoading.value = false
            }
        }
    }

    private fun markConnectionUsed(conn: NetworkConnection) {
        val now = System.currentTimeMillis()
        val updated = _savedConnections.value.map {
            if (it.id == conn.id) it.copy(lastUsedAt = now) else it
        }.sortedByDescending { it.lastUsedAt }
        _savedConnections.value = updated
        connectionStorage.saveConnections(updated)
    }

    fun refreshCurrentDirectory() {
        _rememberedTabPaths.value = connectionStorage.loadAllValidTabPaths()
        loadDirectory(_currentPath.value, _currentProtocol.value, _activeConnection.value, pushHistory = false)
    }

    fun navigateUp(): Boolean {
        if (_selectedPaths.value.isNotEmpty()) {
            _selectedPaths.value = emptySet()
            return true
        }
        val current = _currentPath.value
        val proto = _currentProtocol.value

        if (proto == StorageProtocol.LOCAL) {
            val file = File(current)
            val parent = file.parentFile
            if (parent != null && parent.canRead() && current != defaultLocalRoot) {
                loadDirectory(parent.absolutePath, StorageProtocol.LOCAL, null, pushHistory = false)
                return true
            }
        } else {
            if (current != "/" && current.isNotEmpty()) {
                val parent = current.trimEnd('/').substringBeforeLast('/', "")
                val target = if (parent.isEmpty()) "/" else parent
                loadDirectory(target, proto, _activeConnection.value, pushHistory = false)
                return true
            }
        }

        if (pathHistory.isNotEmpty()) {
            val prev = pathHistory.removeLast()
            loadDirectory(prev.first, prev.second, prev.third, pushHistory = false)
            return true
        }
        return false
    }

    fun openQuickLocalFolder(subFolder: String?, forceRoot: Boolean = false) {
        val shortcutKey = when (subFolder) {
            null -> "LOCAL_ROOT"
            "DCIM/Camera" -> "LOCAL_DCIM"
            else -> "LOCAL_$subFolder"
        }
        val rootDir = if (subFolder == null) File(defaultLocalRoot) else File(defaultLocalRoot, subFolder)
        if (!rootDir.exists()) rootDir.mkdirs()
        val rootPath = rootDir.absolutePath

        val targetPath = if (forceRoot) {
            connectionStorage.saveTabLastPath(shortcutKey, rootPath)
            rootPath
        } else {
            val remembered = connectionStorage.getTabLastPath(shortcutKey, rootPath)
            if (File(remembered).exists()) remembered else rootPath
        }
        loadDirectory(targetPath, StorageProtocol.LOCAL, null, shortcutKey = shortcutKey)
    }

    fun connectAndBrowseNetwork(conn: NetworkConnection, forceRoot: Boolean = false) {
        _selectedTab.value = 0
        val shortcutKey = "NET_${conn.id}"
        val rootPath = conn.initialPath.ifBlank { "/" }
        val targetPath = if (forceRoot) {
            connectionStorage.saveTabLastPath(shortcutKey, rootPath)
            rootPath
        } else {
            connectionStorage.getTabLastPath(shortcutKey, rootPath)
        }
        loadDirectory(targetPath, conn.protocol, conn, shortcutKey = shortcutKey)
    }

    fun resetTabToRoot(shortcutKey: String, subFolder: String? = null, conn: NetworkConnection? = null) {
        if (conn != null) {
            val rootPath = conn.initialPath.ifBlank { "/" }
            connectionStorage.clearTabLastPath(shortcutKey)
            _rememberedTabPaths.value = connectionStorage.loadAllValidTabPaths()
            if (_activeConnection.value?.id == conn.id && _currentProtocol.value != StorageProtocol.LOCAL) {
                loadDirectory(rootPath, conn.protocol, conn, shortcutKey = shortcutKey)
            }
        } else {
            val rootPath = getShortcutRootPath(shortcutKey)
            connectionStorage.clearTabLastPath(shortcutKey)
            _rememberedTabPaths.value = connectionStorage.loadAllValidTabPaths()
            if (_activeShortcutKey.value == shortcutKey && _currentProtocol.value == StorageProtocol.LOCAL) {
                loadDirectory(rootPath, StorageProtocol.LOCAL, null, shortcutKey = shortcutKey)
            }
        }
    }

    fun setQuickFilter(query: String) {
        _quickFilterQuery.value = query
    }

    fun setSorting(field: SortField, ascending: Boolean = _sortAscending.value) {
        _sortField.value = field
        _sortAscending.value = ascending
        _directoryItems.value = sortItems(_directoryItems.value)
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun toggleHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
        refreshCurrentDirectory()
    }

    private fun sortItems(items: List<VfsItem>): List<VfsItem> {
        val field = _sortField.value
        val asc = _sortAscending.value

        val comparator = when (field) {
            SortField.NAME -> compareBy<VfsItem> { it.name.lowercase(Locale.ROOT) }
            SortField.DATE -> compareBy<VfsItem> { it.lastModified }
            SortField.SIZE -> compareBy<VfsItem> { it.sizeBytes }
            SortField.TYPE -> compareBy<VfsItem> { it.extension }.thenBy { it.name.lowercase(Locale.ROOT) }
        }
        val directed = if (asc) comparator else comparator.reversed()
        return items.sortedWith(compareByDescending<VfsItem> { it.isDirectory }.then(directed))
    }

    fun openOrNavigateItem(item: VfsItem) {
        if (item.isDirectory) {
            val conn = item.connectionId?.let { id -> _savedConnections.value.find { it.id == id } } ?: _activeConnection.value
            _selectedTab.value = 0
            loadDirectory(item.path, item.protocol, conn)
            return
        }

        // Si es Foto, Video o Audio, abrir primero en el diálogo de vista previa integrado (como MediaRecoveryMobile)
        if (item.category == FileCategory.IMAGE || item.category == FileCategory.VIDEO || item.category == FileCategory.AUDIO) {
            openIntegratedPreview(item)
        } else {
            openInExternalApp(item)
        }
    }

    fun openIntegratedPreview(item: VfsItem) {
        _previewItem.value = item
        if (item.protocol == StorageProtocol.LOCAL) {
            _isPreviewLoading.value = false
            _previewLocalPath.value = item.path
        } else {
            _isPreviewLoading.value = true
            _previewLocalPath.value = null
            viewModelScope.launch {
                val app = getApplication<Application>()
                val cacheDir = File(app.cacheDir, "remote_preview").apply { mkdirs() }
                val dest = File(cacheDir, item.name)
                val conn = item.connectionId?.let { id -> _savedConnections.value.find { it.id == id } } ?: _activeConnection.value
                val ok = vfsManager.downloadToLocalFile(item, conn, dest)
                _isPreviewLoading.value = false
                if (ok && dest.exists()) {
                    _previewLocalPath.value = dest.absolutePath
                }
            }
        }
    }

    fun closePreview() {
        _previewItem.value = null
        _previewLocalPath.value = null
        _isPreviewLoading.value = false
    }

    fun openInExternalApp(item: VfsItem) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val localFile = if (item.protocol == StorageProtocol.LOCAL) {
                File(item.path)
            } else {
                Toast.makeText(app, "Abriendo ${item.name}...", Toast.LENGTH_SHORT).show()
                val cacheDir = File(app.cacheDir, "remote_preview").apply { mkdirs() }
                val dest = File(cacheDir, item.name)
                val conn = item.connectionId?.let { id -> _savedConnections.value.find { it.id == id } } ?: _activeConnection.value
                if (!dest.exists() || dest.length() != item.sizeBytes) {
                    val ok = vfsManager.downloadToLocalFile(item, conn, dest)
                    if (!ok) {
                        Toast.makeText(app, "No se pudo descargar el archivo remoto.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }
                dest
            }

            try {
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", localFile)
                val ext = localFile.extension.lowercase(Locale.ROOT)
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(app, "No hay aplicación instalada para abrir ${item.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun createNewFolder(folderName: String) {
        if (folderName.isBlank()) return
        viewModelScope.launch {
            val ok = vfsManager.createFolder(
                parentPath = _currentPath.value,
                folderName = folderName.trim(),
                protocol = _currentProtocol.value,
                connection = _activeConnection.value
            )
            if (ok) {
                refreshCurrentDirectory()
            } else {
                Toast.makeText(getApplication(), "No se pudo crear la carpeta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun renameItem(item: VfsItem, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val conn = item.connectionId?.let { id -> _savedConnections.value.find { it.id == id } } ?: _activeConnection.value
            val ok = vfsManager.renameItem(item, newName.trim(), conn)
            if (ok) {
                refreshCurrentDirectory()
            } else {
                Toast.makeText(getApplication(), "No se pudo renombrar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteItem(item: VfsItem, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            val conn = item.connectionId?.let { id -> _savedConnections.value.find { it.id == id } } ?: _activeConnection.value
            val ok = vfsManager.deleteItem(item, conn)
            if (ok) {
                refreshCurrentDirectory()
                _searchResults.value = _searchResults.value.filterNot { it.item.path == item.path }
                _duplicateResults.value = _duplicateResults.value.filterNot { it.item.path == item.path }
                onDeleted?.invoke()
            } else {
                Toast.makeText(getApplication(), "No se pudo eliminar ${item.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SELECCIÓN MÚLTIPLE, COMPARTIR Y PORTAPAPELES
    // ─────────────────────────────────────────────────────────────────────────────

    fun toggleSelection(item: VfsItem) {
        val current = _selectedPaths.value.toMutableSet()
        if (current.contains(item.path)) {
            current.remove(item.path)
        } else {
            current.add(item.path)
        }
        _selectedPaths.value = current
    }

    fun setSelectionRange(itemsList: List<VfsItem>, startIndex: Int, endIndex: Int, baseSelection: Set<String>) {
        if (itemsList.isEmpty()) return
        val minIdx = minOf(startIndex, endIndex).coerceIn(0, itemsList.lastIndex)
        val maxIdx = maxOf(startIndex, endIndex).coerceIn(0, itemsList.lastIndex)
        val updated = baseSelection.toMutableSet()
        for (i in minIdx..maxIdx) {
            updated.add(itemsList[i].path)
        }
        _selectedPaths.value = updated
    }

    fun selectAllCurrent(itemsList: List<VfsItem>) {
        _selectedPaths.value = itemsList.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun copyToClipboard(item: VfsItem, isMove: Boolean) {
        _clipboard.value = ClipboardState(items = listOf(item), isMove = isMove)
        val msg = if (isMove) "Cortado al portapapeles: ${item.name}" else "Copiado al portapapeles: ${item.name}"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun copySelectedToClipboard(isMove: Boolean) {
        val selected = _directoryItems.value.filter { it.path in _selectedPaths.value }
        if (selected.isEmpty()) return
        _clipboard.value = ClipboardState(items = selected, isMove = isMove)
        _selectedPaths.value = emptySet()
        val msg = if (isMove) {
            "${selected.size} elemento(s) listos para mover"
        } else {
            "${selected.size} elemento(s) copiados al portapapeles"
        }
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun deleteSelectedItems() {
        val selected = _directoryItems.value.filter { it.path in _selectedPaths.value }
        if (selected.isEmpty()) return
        _isExplorerLoading.value = true
        viewModelScope.launch {
            var deletedCount = 0
            for (item in selected) {
                val conn = item.connectionId?.let { id -> _savedConnections.value.find { it.id == id } } ?: _activeConnection.value
                if (vfsManager.deleteItem(item, conn)) {
                    deletedCount++
                }
            }
            _selectedPaths.value = emptySet()
            refreshCurrentDirectory()
            Toast.makeText(getApplication(), "Eliminados $deletedCount de ${selected.size} elementos", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareItem(item: VfsItem) {
        if (item.isDirectory) {
            Toast.makeText(getApplication(), "Selecciona archivos para compartir", Toast.LENGTH_SHORT).show()
            return
        }
        shareItems(listOf(item))
    }

    fun shareSelectedItems() {
        val selectedFiles = _directoryItems.value.filter { it.path in _selectedPaths.value && !it.isDirectory }
        if (selectedFiles.isEmpty()) {
            Toast.makeText(getApplication(), "Selecciona al menos un archivo para compartir", Toast.LENGTH_SHORT).show()
            return
        }
        shareItems(selectedFiles)
    }

    fun shareItems(itemsToShare: List<VfsItem>) {
        val filesOnly = itemsToShare.filter { !it.isDirectory }
        if (filesOnly.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val uris = ArrayList<android.net.Uri>()
            var mimeType = "*/*"

            for (item in filesOnly) {
                val localFile = if (item.protocol == StorageProtocol.LOCAL) {
                    File(item.path)
                } else {
                    val cacheDir = File(app.cacheDir, "remote_preview").apply { mkdirs() }
                    val dest = File(cacheDir, item.name)
                    val conn = item.connectionId?.let { id -> _savedConnections.value.find { it.id == id } } ?: _activeConnection.value
                    if (!dest.exists() || dest.length() != item.sizeBytes) {
                        Toast.makeText(app, "Preparando ${item.name} para compartir...", Toast.LENGTH_SHORT).show()
                        val ok = vfsManager.downloadToLocalFile(item, conn, dest)
                        if (!ok) continue
                    }
                    dest
                }
                if (localFile.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", localFile)
                        uris.add(uri)
                        val ext = localFile.extension.lowercase(Locale.ROOT)
                        val itemMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                        if (uris.size == 1) mimeType = itemMime else if (mimeType != itemMime) mimeType = "*/*"
                    } catch (_: Exception) {
                    }
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(app, "No se pudo compartir el archivo.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val shareIntent = if (uris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uris.first())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = mimeType
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                val chooser = Intent.createChooser(shareIntent, "Compartir archivo").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(chooser)
                _selectedPaths.value = emptySet()
            } catch (_: Exception) {
                Toast.makeText(app, "No se encontró aplicación para compartir.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearClipboard() {
        _clipboard.value = ClipboardState()
    }

    fun pasteClipboardHere() {
        val clip = _clipboard.value
        if (clip.items.isEmpty()) return
        val targetPath = _currentPath.value
        val targetProto = _currentProtocol.value
        val targetConn = _activeConnection.value

        _isExplorerLoading.value = true
        viewModelScope.launch {
            var successCount = 0
            for (src in clip.items) {
                val srcConn = src.connectionId?.let { id -> _savedConnections.value.find { it.id == id } }
                val ok = vfsManager.transferFile(
                    source = src,
                    sourceConn = srcConn,
                    targetParentPath = targetPath,
                    targetProtocol = targetProto,
                    targetConn = targetConn,
                    deleteSourceAfter = clip.isMove
                )
                if (ok) successCount++
            }
            if (clip.isMove) _clipboard.value = ClipboardState()
            refreshCurrentDirectory()
            Toast.makeText(
                getApplication(),
                "Transferidos $successCount de ${clip.items.size} archivos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateActiveConnectionCredentials(
        username: String,
        password: String,
        anonymous: Boolean,
        shareName: String
    ) {
        val active = _activeConnection.value ?: return
        saveNetworkConnection(
            existingId = active.id,
            name = active.name,
            protocol = active.protocol,
            host = active.host,
            port = active.port,
            shareName = shareName,
            username = username,
            password = password,
            anonymous = anonymous,
            initialPath = active.initialPath
        )
        val updated = _savedConnections.value.find { it.id == active.id } ?: return
        _activeConnection.value = updated
        refreshCurrentDirectory()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MÉTODOS DEL BUSCADOR INTELIGENTE & DUPLICADOS
    // ─────────────────────────────────────────────────────────────────────────────

    fun setSearchToolIndex(index: Int) {
        _searchToolIndex.value = index
    }

    fun setCurrentFolderAsSearchFolderA() {
        val currentName = if (_currentProtocol.value == StorageProtocol.LOCAL) {
            File(_currentPath.value).name.ifBlank { "Raíz" }
        } else {
            "${_activeConnection.value?.name ?: "Red"}:${_currentPath.value}"
        }
        _folderA.value = VfsItem(
            path = _currentPath.value,
            name = currentName,
            parentPath = "",
            isDirectory = true,
            protocol = _currentProtocol.value,
            connectionId = _activeConnection.value?.id
        )
        _selectedTab.value = 1
        Toast.makeText(getApplication(), "Carpeta A fijada: ${_currentPath.value}", Toast.LENGTH_SHORT).show()
    }

    fun setCurrentFolderAsSearchFolderB() {
        val currentName = if (_currentProtocol.value == StorageProtocol.LOCAL) {
            File(_currentPath.value).name.ifBlank { "Raíz" }
        } else {
            "${_activeConnection.value?.name ?: "Red"}:${_currentPath.value}"
        }
        _folderB.value = VfsItem(
            path = _currentPath.value,
            name = currentName,
            parentPath = "",
            isDirectory = true,
            protocol = _currentProtocol.value,
            connectionId = _activeConnection.value?.id
        )
        _compareTwoFolders.value = true
        _searchToolIndex.value = 2
        _selectedTab.value = 1
        Toast.makeText(getApplication(), "Carpeta B fijada para comparar: ${_currentPath.value}", Toast.LENGTH_SHORT).show()
    }

    fun setFolderAsSearchTarget(folder: VfsItem, asFolderB: Boolean) {
        if (asFolderB) {
            _folderB.value = folder
            _compareTwoFolders.value = true
            _searchToolIndex.value = 2
        } else {
            _folderA.value = folder
        }
        _selectedTab.value = 1
    }

    fun setCompareTwoFolders(enabled: Boolean) {
        _compareTwoFolders.value = enabled
    }

    fun setRecursiveScan(enabled: Boolean) {
        _isRecursiveScan.value = enabled
    }

    fun setSearchWord(word: String) {
        _searchWord.value = word
    }

    fun setSelectedCategory(category: FileCategory) {
        _selectedCategory.value = category
    }

    fun setSelectedOrientation(orientation: MediaOrientation) {
        _selectedOrientation.value = orientation
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun setSelectedYear(year: String) {
        _selectedYear.value = year
    }

    fun setMinSizeMB(mb: Float) {
        _minSizeMB.value = mb
    }

    fun setSelectedDeviceFilter(deviceModel: String?) {
        _selectedDeviceFilter.value = if (_selectedDeviceFilter.value == deviceModel) null else deviceModel
    }

    fun startSmartScan() {
        if (_isScanning.value) return
        val targetA = _folderA.value
        val connMap = _savedConnections.value.associateBy { it.id }
        val connA = targetA.connectionId?.let { connMap[it] }

        _isScanning.value = true
        _scanProgress.value = 0f
        _scanStatusText.value = "Recolectando archivos en ${targetA.path}..."
        _scanMetricsText.value = ""

        scanJob = viewModelScope.launch {
            try {
                val filesA = vfsManager.collectFilesRecursive(
                    rootPath = targetA.path,
                    protocol = targetA.protocol,
                    connection = connA,
                    recursive = _isRecursiveScan.value,
                    includeDirectories = false
                ) { count, lastFile ->
                    _scanStatusText.value = "Descubiertos $count archivos ($lastFile)..."
                }

                if (filesA.isEmpty()) {
                    _scanStatusText.value = "No se encontraron archivos en la Carpeta A."
                    _isScanning.value = false
                    return@launch
                }

                val monthIdx = monthsList.indexOf(_selectedMonth.value).takeIf { it > 0 }
                val yearVal = _selectedYear.value.toIntOrNull()
                val minBytes = (_minSizeMB.value * 1024L * 1024L).toLong()

                when (_searchToolIndex.value) {
                    0, 1 -> {
                        val results = searchEngine.analyzeFiles(
                            files = filesA,
                            connectionMap = connMap,
                            filterMonth = monthIdx,
                            filterYear = yearVal,
                            categoryFilter = _selectedCategory.value,
                            minSizeBytes = minBytes,
                            analyzeMediaHeaders = true
                        ) { current, total, name ->
                            _scanProgress.value = if (total > 0) (current * 100f) / total else 0f
                            _scanStatusText.value = "Analizando [$current/$total] $name"
                        }

                        _searchResults.value = results
                        val summaries = searchEngine.buildDeviceSummaries(results)
                        _deviceSummaries.value = summaries
                        _selectedDeviceFilter.value = null

                        val vCount = results.count { it.orientation == "Vertical" }
                        val hCount = results.count { it.orientation == "Horizontal" }
                        val totalSize = results.sumOf { it.item.sizeBytes }
                        _scanStatusText.value = "Análisis completado."
                        _scanMetricsText.value = "${results.size} archivos (${formatBytes(totalSize)}) • $vCount Vert. • $hCount Horiz. • ${summaries.size} orígenes"
                    }

                    2 -> {
                        var filesB: List<VfsItem>? = null
                        if (_compareTwoFolders.value && _folderB.value != null) {
                            val targetB = _folderB.value!!
                            val connB = targetB.connectionId?.let { connMap[it] }
                            _scanStatusText.value = "Recolectando archivos en Carpeta B (${targetB.path})..."
                            filesB = vfsManager.collectFilesRecursive(
                                rootPath = targetB.path,
                                protocol = targetB.protocol,
                                connection = connB,
                                recursive = _isRecursiveScan.value,
                                includeDirectories = false
                            )
                        }

                        val dups = searchEngine.scanDuplicates(
                            folderAFiles = filesA,
                            folderBFiles = filesB,
                            connectionMap = connMap
                        ) { current, total, name ->
                            _scanProgress.value = if (total > 0) (current * 100f) / total else 0f
                            _scanStatusText.value = "Verificando SHA-256 [$current/$total] $name"
                        }

                        _duplicateResults.value = dups
                        val groups = dups.map { it.groupId }.distinct().size
                        val wastedBytes = dups.filter { !it.isPrimary }.sumOf { it.item.sizeBytes }
                        _scanStatusText.value = "Búsqueda de duplicados finalizada."
                        _scanMetricsText.value = "$groups grupos detectados (${dups.size} archivos) • Espacio recuperable: ${formatBytes(wastedBytes)}"
                    }
                }
            } catch (e: Exception) {
                _scanStatusText.value = "Escaneo interrumpido: ${e.localizedMessage ?: ""}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = 100f
            }
        }
    }

    fun cancelSmartScan() {
        scanJob?.cancel()
        _isScanning.value = false
        _scanStatusText.value = "Escaneo cancelado por el usuario."
    }

    fun exportCsvReport(exportDetails: Boolean) {
        val items = _searchResults.value
        val summaries = _deviceSummaries.value
        if (items.isEmpty()) {
            Toast.makeText(getApplication(), "Primero realiza un análisis para exportar.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            try {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                val suffix = if (exportDetails) "detalle" else "resumen_dispositivos"
                val file = File(downloads, "FileManager_${suffix}_${System.currentTimeMillis()}.csv")
                searchEngine.exportResultsToCsv(file, items, summaries, exportDetails)
                Toast.makeText(
                    getApplication(),
                    "CSV guardado en Descargas: ${file.name}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    getApplication(),
                    "Error al exportar CSV: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MÉTODOS DE GESTIÓN DE CONEXIONES SMB / FTP Y ESCÁNER DE RED
    // ─────────────────────────────────────────────────────────────────────────────

    fun saveNetworkConnection(
        existingId: String?,
        name: String,
        protocol: StorageProtocol,
        host: String,
        port: Int,
        shareName: String,
        username: String,
        password: String,
        anonymous: Boolean,
        initialPath: String
    ) {
        val cleanHost = host.trim().removePrefix("smb://").removePrefix("ftp://").trim('/')
        if (cleanHost.isEmpty()) return

        val existingLastUsed = existingId?.let { id -> _savedConnections.value.find { it.id == id }?.lastUsedAt } ?: 0L
        val newConn = NetworkConnection(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.ifBlank { "${protocol.name} ($cleanHost)" },
            protocol = protocol,
            host = cleanHost,
            port = port,
            shareName = shareName.trim('/', '\\'),
            username = username.trim(),
            password = password,
            anonymous = anonymous,
            initialPath = if (initialPath.isBlank()) "/" else initialPath.trim(),
            lastUsedAt = existingLastUsed
        )

        val updated = _savedConnections.value.toMutableList()
        val idx = updated.indexOfFirst { it.id == newConn.id }
        if (idx >= 0) updated[idx] = newConn else updated.add(newConn)

        val sorted = updated.sortedByDescending { it.lastUsedAt }
        _savedConnections.value = sorted
        connectionStorage.saveConnections(sorted)
    }

    fun deleteNetworkConnection(conn: NetworkConnection) {
        connectionStorage.clearTabLastPath("NET_${conn.id}")
        _rememberedTabPaths.value = connectionStorage.loadAllValidTabPaths()
        val updated = _savedConnections.value.filterNot { it.id == conn.id }
        _savedConnections.value = updated
        connectionStorage.saveConnections(updated)
    }

    fun scanLocalNetworkForServers() {
        if (_isDiscoveringLan.value) return
        _isDiscoveringLan.value = true
        _lanScanProgress.value = 0f
        _discoveredHosts.value = emptyList()

        viewModelScope.launch {
            try {
                val hosts = vfsManager.discoverLanServers { done, total ->
                    _lanScanProgress.value = (done * 100f) / total
                }
                _discoveredHosts.value = hosts
            } finally {
                _isDiscoveringLan.value = false
                _lanScanProgress.value = 100f
            }
        }
    }
}
