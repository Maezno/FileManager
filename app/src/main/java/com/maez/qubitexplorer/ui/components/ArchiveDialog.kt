package com.maez.qubitexplorer.ui.components

import com.maez.qubitexplorer.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maez.qubitexplorer.model.VfsItem
import com.maez.qubitexplorer.model.formatBytes
import com.maez.qubitexplorer.services.ArchiveExtractor
import com.maez.qubitexplorer.services.ArchiveListing
import com.maez.qubitexplorer.ui.theme.Aqua
import com.maez.qubitexplorer.ui.theme.Danger
import com.maez.qubitexplorer.ui.theme.TextMuted
import com.maez.qubitexplorer.ui.theme.TextPrimary
import com.maez.qubitexplorer.ui.theme.Violet

data class ArchiveUiState(
    val fileName: String,
    val listing: ArchiveListing? = null,
    val error: String? = null,
    val destinationLabel: String = "",
    val extracting: Boolean = false,
    val bytesWritten: Long = 0,
    val bytesTotal: Long = 0,
    val done: Boolean = false,
    val extractedCount: Int = 0,
    val openingEntry: String? = null
)

/** Nodo del árbol interno del comprimido. */
private data class ArchiveNode(
    val path: String,       // ruta completa normalizada
    val name: String,
    val isDir: Boolean,
    val size: Long,         // en carpetas: suma de su contenido (-1 si se desconoce)
    val fileCount: Int
)

private class ArchiveTree(listing: ArchiveListing) {
    val children = HashMap<String, MutableList<ArchiveNode>>()
    val fileSizes = HashMap<String, Long>()

    init {
        val dirs = HashSet<String>()
        for (e in listing.entries) {
            val n = ArchiveExtractor.normalizeName(e.name)
            if (n.isEmpty()) continue
            if (e.isDirectory) dirs += n else fileSizes[n] = e.size
            var parent = n.substringBeforeLast('/', "")
            while (parent.isNotEmpty()) { dirs += parent; parent = parent.substringBeforeLast('/', "") }
        }
        for (d in dirs) {
            val prefix = "$d/"
            val under = fileSizes.filterKeys { it.startsWith(prefix) }
            val size = if (under.values.any { it < 0 }) -1 else under.values.sum()
            add(ArchiveNode(d, d.substringAfterLast('/'), true, size, under.size))
        }
        for ((f, size) in fileSizes) add(ArchiveNode(f, f.substringAfterLast('/'), false, size, 1))
        children.values.forEach { list -> list.sortWith(compareByDescending<ArchiveNode> { it.isDir }.thenBy { it.name.lowercase() }) }
    }

    private fun add(node: ArchiveNode) {
        children.getOrPut(node.path.substringBeforeLast('/', "")) { mutableListOf() } += node
    }

    fun list(dir: String): List<ArchiveNode> = children[dir].orEmpty()
}

private fun isCoveredBySelection(path: String, selection: Set<String>): Boolean {
    if (path in selection) return true
    var p = path.substringBeforeLast('/', "")
    while (p.isNotEmpty()) { if (p in selection) return true; p = p.substringBeforeLast('/', "") }
    return false
}

/**
 * Explorador del comprimido: navegar carpetas, marcar archivos o carpetas y extraer solo eso,
 * o tocar un archivo para abrirlo sin extraer el resto.
 */
@Composable
fun ArchiveBrowserScreen(
    state: ArchiveUiState,
    onExtract: (include: ((String) -> Boolean)?) -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenDestination: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.extracting) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val bg = MaterialTheme.colorScheme.background
        var currentDir by remember { mutableStateOf("") }
        var selection by remember { mutableStateOf(setOf<String>()) }
        val tree = remember(state.listing) { state.listing?.let { ArchiveTree(it) } }

        BackHandler {
            when {
                state.extracting -> onCancel()
                currentDir.isNotEmpty() -> currentDir = currentDir.substringBeforeLast('/', "")
                else -> onClose()
            }
        }

        val selectedFiles = remember(selection, tree) {
            tree?.fileSizes?.keys?.filter { isCoveredBySelection(it, selection) } ?: emptyList()
        }
        val selectedBytes = remember(selectedFiles, tree) { selectedFiles.sumOf { (tree?.fileSizes?.get(it) ?: 0L).coerceAtLeast(0) } }

        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .padding(bottom = activityNavBarBottomPadding())
        ) {
            // Barra superior
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentDir.isNotEmpty()) currentDir = currentDir.substringBeforeLast('/', "") else if (!state.extracting) onClose()
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.archive_volver), tint = TextPrimary) }
                Column(Modifier.weight(1f)) {
                    Text(state.fileName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    state.listing?.let {
                        Text(stringResource(R.string.archive_archivos, it.format, it.fileCount) + (if (it.totalSize > 0) " · ${formatBytes(it.totalSize)}" else ""),
                            color = Aqua, fontSize = 12.sp
                        )
                    }
                }
                if (tree != null && !state.extracting) {
                    val visible = tree.list(currentDir)
                    val allHere = visible.isNotEmpty() && visible.all { isCoveredBySelection(it.path, selection) }
                    TextButton(onClick = {
                        selection = if (allHere) selection - visible.map { it.path }.toSet()
                        else selection + visible.map { it.path }
                    }) { Text(if (allHere) stringResource(R.string.exif_none) else stringResource(R.string.text_editor_todos), color = Violet, fontSize = 13.sp) }
                }
            }

            // Ruta dentro del comprimido (migas)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val parts = if (currentDir.isEmpty()) emptyList() else currentDir.split('/')
                Crumb(state.fileName.substringBeforeLast('.'), currentDir.isEmpty()) { currentDir = "" }
                parts.forEachIndexed { i, part ->
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Crumb(part, i == parts.lastIndex) { currentDir = parts.take(i + 1).joinToString("/") }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.error != null && tree == null -> Text(state.error, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(20.dp))
                    tree == null -> Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Violet)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.archive_leyendo_contenido), color = TextMuted, fontSize = 13.sp)
                    }
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        state.listing?.note?.let { note -> item { Text(note, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(12.dp)) } }
                        items(tree.list(currentDir), key = { it.path }) { node ->
                            val checked = isCoveredBySelection(node.path, selection)
                            val partial = !checked && node.isDir && selection.any { it.startsWith(node.path + "/") }
                            val style = fileTypeStyle(
                                VfsItem(path = node.path, name = node.name, parentPath = "", isDirectory = node.isDir, sizeBytes = node.size.coerceAtLeast(1))
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.extracting) {
                                        if (node.isDir) currentDir = node.path else onOpenEntry(node.path)
                                    }
                                    .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    enabled = !state.extracting,
                                    onClick = {
                                        selection = if (checked) {
                                            // Desmarcar: si lo cubría una carpeta padre, se marcan sus hermanos en su lugar
                                            if (node.path in selection) selection - node.path
                                            else expandWithout(tree, selection, node.path)
                                        } else {
                                            (selection.filterNot { it.startsWith(node.path + "/") } + node.path).toSet()
                                        }
                                    }
                                ) {
                                    Icon(
                                        when { checked -> Icons.Default.CheckBox; partial -> Icons.Default.IndeterminateCheckBox; else -> Icons.Default.CheckBoxOutlineBlank },
                                        contentDescription = stringResource(R.string.a11y_select),
                                        tint = if (checked || partial) Violet else TextMuted
                                    )
                                }
                                Box(
                                    Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(style.color.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(style.icon, null, tint = style.color, modifier = Modifier.size(20.dp)) }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(node.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (node.isDir) stringResource(R.string.archive_archivos_2, node.fileCount) + (if (node.size > 0) " · ${formatBytes(node.size)}" else "")
                                        else if (node.size >= 0) formatBytes(node.size) else stringResource(R.string.archive_tamano_desconocido),
                                        color = TextMuted, fontSize = 11.sp
                                    )
                                }
                                if (state.openingEntry == node.path) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Violet)
                                else if (node.isDir) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
                            }
                        }
                    }
                }
            }

            // Barra inferior: progreso / acciones
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                if (state.destinationLabel.isNotEmpty()) Text(stringResource(R.string.archive_destino, state.destinationLabel), color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state.extracting) {
                    if (state.bytesTotal > 0) LinearProgressIndicator(progress = { (state.bytesWritten.toFloat() / state.bytesTotal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Violet)
                    else LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Violet)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.archive_extrayendo, formatBytes(state.bytesWritten)), color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.explorer_cancelar), color = Danger) }
                    }
                } else {
                    if (state.done) Text(stringResource(R.string.archive_listo_archivos_extraidos, state.extractedCount), color = Aqua, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (state.error != null && tree != null) Text(state.error, color = Danger, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (selectedFiles.isEmpty()) stringResource(R.string.archive_nada_seleccionado)
                            else stringResource(R.string.archive_seleccionados, selectedFiles.size, formatBytes(selectedBytes)),
                            color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f)
                        )
                        if (state.done) TextButton(onClick = onOpenDestination) { Text(stringResource(R.string.archive_abrir_carpeta), color = Aqua, fontWeight = FontWeight.Bold) }
                        if (tree != null) {
                            if (selectedFiles.isNotEmpty()) {
                                val chosen = selection
                                TextButton(onClick = { onExtract { name -> isCoveredBySelection(name, chosen) } }) {
                                    Text(stringResource(R.string.archive_extraer_seleccion), color = Violet, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                TextButton(onClick = { onExtract(null) }) { Text(stringResource(R.string.archive_extraer_todo), color = Violet, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Quita [path] de una selección donde lo cubre una carpeta padre: reemplaza esa carpeta por sus otros hijos. */
private fun expandWithout(tree: ArchiveTree, selection: Set<String>, path: String): Set<String> {
    var ancestor = path.substringBeforeLast('/', "")
    while (ancestor.isNotEmpty() && ancestor !in selection) ancestor = ancestor.substringBeforeLast('/', "")
    if (ancestor.isEmpty()) return selection - path
    val result = selection.toMutableSet().apply { remove(ancestor) }
    // Bajar desde el ancestro hasta el nodo, marcando los hermanos de cada nivel
    var level = ancestor
    while (level != path) {
        val next = path.substring(0, path.indexOf('/', level.length + 1).let { if (it < 0) path.length else it })
        tree.list(level).forEach { if (it.path != next) result += it.path }
        level = next
    }
    return result
}

@Composable
private fun Crumb(label: String, current: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (current) TextPrimary else Violet,
        fontSize = 13.sp,
        fontWeight = if (current) FontWeight.SemiBold else null,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
