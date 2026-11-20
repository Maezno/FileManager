package com.maez.qubitexplorer.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maez.qubitexplorer.services.CodeLanguage
import com.maez.qubitexplorer.services.OutlineItem
import com.maez.qubitexplorer.services.SyntaxHighlighter
import com.maez.qubitexplorer.services.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Paleta del editor; se elige clara u oscura según el tema actual de la app. */
private data class EditorPalette(
    val bg: Color, val gutter: Color, val gutterText: Color, val text: Color, val bar: Color,
    val currentLine: Color, val match: Color,
    val tokens: Map<TokenType, Color>
)

private val DarkPalette = EditorPalette(
    bg = Color(0xFF1E1F26), gutter = Color(0xFF181920), gutterText = Color(0xFF5E6273), text = Color(0xFFD7DAE0),
    bar = Color(0xFF14151B), currentLine = Color(0x22FFFFFF), match = Color(0x66FFC857),
    tokens = mapOf(
        TokenType.KEYWORD to Color(0xFFC792EA), TokenType.TYPE to Color(0xFFFFCB6B),
        TokenType.STRING to Color(0xFFC3E88D), TokenType.NUMBER to Color(0xFFF78C6C),
        TokenType.COMMENT to Color(0xFF676E95), TokenType.FUNCTION to Color(0xFF82AAFF),
        TokenType.TAG to Color(0xFFF07178), TokenType.ATTRIBUTE to Color(0xFFFFCB6B),
        TokenType.PROPERTY to Color(0xFF89DDFF), TokenType.PUNCTUATION to Color(0xFF89DDFF),
        TokenType.META to Color(0xFFFF9CAC), TokenType.VARIABLE to Color(0xFFF78C6C)
    )
)

private val LightPalette = EditorPalette(
    bg = Color(0xFFFBFBFD), gutter = Color(0xFFF0F1F5), gutterText = Color(0xFFA0A4B2), text = Color(0xFF1F2330),
    bar = Color(0xFFE9EBF2), currentLine = Color(0x14000000), match = Color(0x66FFB400),
    tokens = mapOf(
        TokenType.KEYWORD to Color(0xFF8E24AA), TokenType.TYPE to Color(0xFFB26A00),
        TokenType.STRING to Color(0xFF2E7D32), TokenType.NUMBER to Color(0xFFD84315),
        TokenType.COMMENT to Color(0xFF8A8FA3), TokenType.FUNCTION to Color(0xFF1565C0),
        TokenType.TAG to Color(0xFFC62828), TokenType.ATTRIBUTE to Color(0xFFB26A00),
        TokenType.PROPERTY to Color(0xFF00838F), TokenType.PUNCTUATION to Color(0xFF546E7A),
        TokenType.META to Color(0xFFAD1457), TokenType.VARIABLE to Color(0xFFD84315)
    )
)

/** Resaltado de sintaxis + coincidencias de búsqueda, como transformación visual (no altera el texto). */
private class SyntaxTransformation(
    private val language: CodeLanguage,
    private val palette: EditorPalette,
    private val matches: List<IntRange>
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)
        for (span in SyntaxHighlighter.highlight(raw, language)) {
            val color = palette.tokens[span.type] ?: continue
            builder.addStyle(
                SpanStyle(
                    color = color,
                    fontStyle = if (span.type == TokenType.COMMENT) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = if (span.type == TokenType.KEYWORD) FontWeight.SemiBold else null
                ),
                span.start, span.end
            )
        }
        for (m in matches) {
            if (m.last < raw.length) builder.addStyle(SpanStyle(background = palette.match), m.first, m.last + 1)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private const val SYMBOLS = "⇥ { } ( ) [ ] < > / = \" ' ; : . , # $ _ - + * & | ! ? \\"

@Composable
fun TextEditorScreen(
    fileName: String,
    initialText: String?,
    loadError: String?,
    readOnlyReason: String?,
    onSave: suspend (String) -> Boolean,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, decorFitsSystemWindows = false)
    ) {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val palette = if (dark) DarkPalette else LightPalette

        Box(
            Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .padding(bottom = activityNavBarBottomPadding())
                .imePadding()
        ) {
            when {
                loadError != null -> CenterMessage(loadError, palette, onClose)
                initialText == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.tokens[TokenType.FUNCTION]!!)
                }
                else -> EditorBody(fileName, initialText, readOnlyReason, palette, onSave, onClose)
            }
        }
    }
}

@Composable
private fun CenterMessage(msg: String, palette: EditorPalette, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(msg, color = palette.text, textAlign = TextAlign.Center)
        Spacer(Modifier.size(12.dp))
        TextButton(onClick = onClose) { Text("Cerrar") }
    }
}

@Composable
private fun EditorBody(
    fileName: String,
    original: String,
    readOnlyReason: String?,
    palette: EditorPalette,
    onSave: suspend (String) -> Boolean,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val usesCrlf = remember(original) { original.contains("\r\n") }
    val normalized = remember(original) { original.replace("\r\n", "\n") }
    val language = remember(fileName, normalized) { CodeLanguage.detect(fileName, normalized) }

    var value by remember { mutableStateOf(TextFieldValue(normalized)) }
    var savedText by remember { mutableStateOf(normalized) }
    val modified = value.text != savedText

    // Historial deshacer/rehacer (se agrupan cambios rápidos)
    val undo = remember { mutableStateListOf<TextFieldValue>() }
    val redo = remember { mutableStateListOf<TextFieldValue>() }
    var lastPush by remember { mutableStateOf(0L) }

    var fontSize by remember { mutableIntStateOf(13) }
    var wrap by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var matchIndex by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }
    var outline by remember { mutableStateOf<List<OutlineItem>>(emptyList()) }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val density = LocalDensity.current
    val lineHeightSp = fontSize * 1.45f
    val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }

    val matches = remember(value.text, query) {
        if (query.isEmpty()) emptyList()
        else Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(value.text).map { it.range }.take(5000).toList()
    }
    val lineCount = remember(value.text) { value.text.count { it == '\n' } + 1 }
    val caretLine = remember(value.selection, value.text) {
        value.text.substring(0, value.selection.start.coerceAtMost(value.text.length)).count { it == '\n' }
    }
    val caretCol = remember(value.selection, value.text) {
        val s = value.selection.start.coerceAtMost(value.text.length)
        s - (value.text.lastIndexOf('\n', s - 1) + 1) + 1
    }

    LaunchedEffect(value.text, showOutline) {
        if (!showOutline) return@LaunchedEffect
        delay(150)
        outline = withContext(Dispatchers.Default) { SyntaxHighlighter.outline(value.text, language) }
    }
    LaunchedEffect(status) { if (status != null) { delay(2200); status = null } }

    fun lineStartOffset(line: Int): Int {
        var idx = 0
        repeat(line) { idx = value.text.indexOf('\n', idx).let { if (it < 0) value.text.length else it + 1 } }
        return idx.coerceAtMost(value.text.length)
    }

    fun goToOffset(offset: Int, selectEnd: Int = offset) {
        value = value.copy(selection = TextRange(offset, selectEnd))
        val line = value.text.substring(0, offset.coerceAtMost(value.text.length)).count { it == '\n' }
        scope.launch { vScroll.animateScrollTo(((line - 3).coerceAtLeast(0) * lineHeightPx).toInt()) }
    }

    fun applyEdit(newValue: TextFieldValue) {
        if (readOnlyReason != null) return
        val old = value
        var nv = newValue
        // Auto-indentación: al insertar un salto de línea, copiar la sangría de la línea anterior
        if (nv.text.length == old.text.length + 1 && nv.selection.collapsed) {
            val pos = nv.selection.start
            if (pos > 0 && nv.text[pos - 1] == '\n') {
                val prevStart = nv.text.lastIndexOf('\n', pos - 2) + 1
                val prevLine = nv.text.substring(prevStart, pos - 1)
                var indent = prevLine.takeWhile { it == ' ' || it == '\t' }
                val trimmed = prevLine.trimEnd()
                val opensBlock = trimmed.endsWith("{") || trimmed.endsWith("[") || trimmed.endsWith("(") ||
                    (trimmed.endsWith(":") && language in setOf(CodeLanguage.PYTHON, CodeLanguage.YAML)) ||
                    (language.isMarkup && Regex("""<[A-Za-z][^/!?>]*>$""").containsMatchIn(trimmed) &&
                        !Regex("""</[^>]+>$""").containsMatchIn(trimmed))
                if (opensBlock) indent += "    "
                if (indent.isNotEmpty()) {
                    nv = TextFieldValue(
                        nv.text.substring(0, pos) + indent + nv.text.substring(pos),
                        TextRange(pos + indent.length)
                    )
                }
            }
        }
        if (nv.text != old.text) {
            val now = System.currentTimeMillis()
            if (now - lastPush > 700 || undo.isEmpty()) {
                undo.add(old); if (undo.size > 200) undo.removeAt(0)
                lastPush = now
            }
            redo.clear()
        }
        value = nv
    }

    fun insert(s: String) {
        val t = value.text
        val a = value.selection.min; val b = value.selection.max
        applyEdit(TextFieldValue(t.substring(0, a) + s + t.substring(b), TextRange(a + s.length)))
    }

    fun toggleLineComment() {
        val prefix = language.lineComment ?: run { status = "Este formato no tiene comentario de línea"; return }
        val t = value.text
        val start = t.lastIndexOf('\n', value.selection.min - 1) + 1
        val endIdx = t.indexOf('\n', value.selection.max).let { if (it < 0) t.length else it }
        val block = t.substring(start, endIdx)
        val lines = block.split('\n')
        val allCommented = lines.filter { it.isNotBlank() }.all { it.trimStart().startsWith(prefix.trim()) }
        val newBlock = lines.joinToString("\n") { line ->
            if (line.isBlank()) line
            else if (allCommented) {
                val p = prefix.trim()
                val i = line.indexOf(p)
                var end = i + p.length
                if (line.getOrNull(end) == ' ') end++
                line.removeRange(i, end)
            } else {
                val ws = line.takeWhile { it == ' ' || it == '\t' }
                ws + prefix.trimEnd() + " " + line.substring(ws.length)
            }
        }
        applyEdit(TextFieldValue(t.substring(0, start) + newBlock + t.substring(endIdx), TextRange(start, start + newBlock.length)))
    }

    fun save() {
        scope.launch {
            val out = if (usesCrlf) value.text.replace("\n", "\r\n") else value.text
            val ok = onSave(out)
            if (ok) savedText = value.text
            status = if (ok) "Guardado" else "No se pudo guardar"
        }
    }

    BackHandler {
        when {
            showSearch -> showSearch = false
            showOutline -> showOutline = false
            modified -> confirmExit = true
            else -> onClose()
        }
    }

    val transformation = remember(language, palette, matches) { SyntaxTransformation(language, palette, matches) }
    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, lineHeight = lineHeightSp.sp, color = palette.text
    )

    Column(Modifier.fillMaxSize()) {
        // ── Barra superior ──
        Row(
            Modifier.fillMaxWidth().background(palette.bar).padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (modified) confirmExit = true else onClose() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar", tint = palette.text)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    (if (modified) "● " else "") + fileName,
                    color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    language.label + (readOnlyReason?.let { " · solo lectura" } ?: ""),
                    color = palette.tokens[TokenType.FUNCTION]!!, fontSize = 11.sp
                )
            }
            IconButton(onClick = { undo.removeLastOrNull()?.let { redo.add(value); value = it } }, enabled = undo.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer", tint = palette.text.copy(alpha = if (undo.isNotEmpty()) 1f else 0.3f))
            }
            IconButton(onClick = { redo.removeLastOrNull()?.let { undo.add(value); value = it } }, enabled = redo.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Rehacer", tint = palette.text.copy(alpha = if (redo.isNotEmpty()) 1f else 0.3f))
            }
            IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Buscar", tint = palette.text) }
            IconButton(onClick = { showOutline = !showOutline }) { Icon(Icons.Default.AccountTree, "Esquema", tint = palette.text) }
            IconButton(onClick = { save() }, enabled = modified && readOnlyReason == null) {
                Icon(Icons.Default.Save, "Guardar", tint = if (modified) palette.tokens[TokenType.STRING]!! else palette.text.copy(alpha = 0.3f))
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Más", tint = palette.text) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (wrap) "Desactivar ajuste de línea" else "Ajuste de línea") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.WrapText, null) },
                        onClick = { wrap = !wrap; showMenu = false }
                    )
                    DropdownMenuItem(text = { Text("Agrandar texto") }, onClick = { fontSize = (fontSize + 1).coerceAtMost(24) })
                    DropdownMenuItem(text = { Text("Achicar texto") }, onClick = { fontSize = (fontSize - 1).coerceAtLeast(9) })
                    DropdownMenuItem(
                        text = { Text("Comentar / descomentar línea") },
                        enabled = readOnlyReason == null,
                        onClick = { toggleLineComment(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicar línea") },
                        enabled = readOnlyReason == null,
                        onClick = {
                            val t = value.text
                            val s = t.lastIndexOf('\n', value.selection.start - 1) + 1
                            val e = t.indexOf('\n', value.selection.start).let { if (it < 0) t.length else it }
                            val line = t.substring(s, e)
                            applyEdit(TextFieldValue(t.substring(0, e) + "\n" + line + t.substring(e), TextRange(e + 1 + (value.selection.start - s))))
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ir al inicio") },
                        onClick = { goToOffset(0); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Ir al final") },
                        onClick = { goToOffset(value.text.length); showMenu = false }
                    )
                }
            }
        }

        // ── Buscar y reemplazar ──
        if (showSearch) {
            Column(Modifier.fillMaxWidth().background(palette.gutter).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniField(query, { query = it; matchIndex = 0 }, "Buscar", palette, Modifier.weight(1f))
                    Text(
                        if (matches.isEmpty()) "0/0" else "${matchIndex + 1}/${matches.size}",
                        color = palette.gutterText, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = {
                        if (matches.isNotEmpty()) {
                            matchIndex = (matchIndex - 1 + matches.size) % matches.size
                            matches[matchIndex].let { goToOffset(it.first, it.last + 1) }
                        }
                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowUp, "Anterior", tint = palette.text) }
                    IconButton(onClick = {
                        if (matches.isNotEmpty()) {
                            matchIndex = (matchIndex + 1) % matches.size
                            matches[matchIndex].let { goToOffset(it.first, it.last + 1) }
                        }
                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowDown, "Siguiente", tint = palette.text) }
                    IconButton(onClick = { showSearch = false; query = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Cerrar búsqueda", tint = palette.text)
                    }
                }
                if (readOnlyReason == null) {
                    Spacer(Modifier.size(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniField(replacement, { replacement = it }, "Reemplazar por", palette, Modifier.weight(1f))
                        TextButton(onClick = {
                            val m = matches.getOrNull(matchIndex) ?: return@TextButton
                            val t = value.text
                            applyEdit(TextFieldValue(t.replaceRange(m, replacement), TextRange(m.first + replacement.length)))
                        }) { Text("Uno", fontSize = 12.sp) }
                        TextButton(onClick = {
                            if (query.isEmpty()) return@TextButton
                            val n = matches.size
                            applyEdit(TextFieldValue(
                                value.text.replace(Regex(Regex.escape(query), RegexOption.IGNORE_CASE), Regex.escapeReplacement(replacement)),
                                TextRange(0)
                            ))
                            status = "$n reemplazos"
                        }) { Text("Todos", fontSize = 12.sp) }
                    }
                }
            }
        }

        // ── Área de edición ──
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Row(Modifier.fillMaxSize().verticalScroll(vScroll)) {
                // Números de línea (alineados con la altura de línea fija del texto)
                if (!wrap) {
                    Text(
                        text = (1..lineCount).joinToString("\n"),
                        style = textStyle.copy(color = palette.gutterText, textAlign = TextAlign.End),
                        modifier = Modifier
                            .background(palette.gutter)
                            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 200.dp)
                            .widthIn(min = 24.dp)
                    )
                }
                val fieldModifier = if (wrap) Modifier.weight(1f) else Modifier.weight(1f).horizontalScroll(hScroll)
                Box(fieldModifier) {
                    BasicTextField(
                        value = value,
                        onValueChange = { applyEdit(it) },
                        readOnly = readOnlyReason != null,
                        textStyle = textStyle,
                        cursorBrush = SolidColor(palette.tokens[TokenType.FUNCTION]!!),
                        visualTransformation = transformation,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.None
                        ),
                        modifier = Modifier
                            .then(if (wrap) Modifier.fillMaxWidth() else Modifier.widthIn(min = 600.dp))
                            .padding(start = 8.dp, end = 24.dp, top = 6.dp, bottom = 200.dp)
                    )
                }
            }

            // Panel de esquema (funciones, clases, selectores…)
            if (showOutline) {
                Column(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .widthIn(max = 280.dp)
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.bar)
                ) {
                    Text(
                        "Esquema · ${language.label}",
                        color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                    if (outline.isEmpty()) {
                        Text("Sin elementos detectados", color = palette.gutterText, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                    }
                    LazyColumn {
                        items(outline) { o ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        goToOffset(lineStartOffset(o.line))
                                        showOutline = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    o.kind, color = kindColor(o.kind, palette), fontSize = 10.sp,
                                    modifier = Modifier.width(58.dp)
                                )
                                Text(
                                    o.label.trim(), color = palette.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                )
                                Text("${o.line + 1}", color = palette.gutterText, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Barra de símbolos para teclado táctil ──
        if (readOnlyReason == null) {
            Row(
                Modifier.fillMaxWidth().background(palette.bar).horizontalScroll(rememberScrollState()).padding(vertical = 2.dp)
            ) {
                IconButton(onClick = { insert("    ") }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, "Tabulación", tint = palette.text, modifier = Modifier.size(18.dp))
                }
                SYMBOLS.split(' ').drop(1).forEach { sym ->
                    Box(
                        Modifier
                            .size(width = 34.dp, height = 36.dp)
                            .clickable { insert(sym) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sym, color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                    }
                }
            }
        }

        // ── Barra de estado ──
        Row(
            Modifier.fillMaxWidth().background(palette.gutter).padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val small = Modifier
            Text("Ln ${caretLine + 1}, Col $caretCol", color = palette.gutterText, fontSize = 10.sp, modifier = small)
            Text("$lineCount líneas", color = palette.gutterText, fontSize = 10.sp)
            Text(if (usesCrlf) "CRLF" else "LF", color = palette.gutterText, fontSize = 10.sp)
            Text("UTF-8", color = palette.gutterText, fontSize = 10.sp)
            if (value.text.length > SyntaxHighlighter.MAX_HIGHLIGHT_CHARS) {
                Text("sin colores (archivo grande)", color = palette.gutterText, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            (status ?: readOnlyReason)?.let { Text(it, color = palette.tokens[TokenType.STRING]!!, fontSize = 10.sp) }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Cambios sin guardar") },
            text = { Text("¿Querés guardar los cambios en $fileName antes de salir?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    scope.launch {
                        val out = if (usesCrlf) value.text.replace("\n", "\r\n") else value.text
                        if (onSave(out)) onClose() else status = "No se pudo guardar"
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmExit = false; onClose() }) { Text("Descartar") }
                    TextButton(onClick = { confirmExit = false }) { Text("Cancelar") }
                }
            }
        )
    }
}

private fun kindColor(kind: String, p: EditorPalette): Color = when (kind) {
    "clase", "tipo" -> p.tokens[TokenType.TYPE]!!
    "función", "método" -> p.tokens[TokenType.FUNCTION]!!
    "selector", "etiqueta", "elemento", "#id" -> p.tokens[TokenType.TAG]!!
    "regla", "sección", "título" -> p.tokens[TokenType.KEYWORD]!!
    else -> p.tokens[TokenType.PROPERTY]!!
}

@Composable
private fun MiniField(value: String, onChange: (String) -> Unit, hint: String, palette: EditorPalette, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.bg)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        if (value.isEmpty()) Text(hint, color = palette.gutterText, fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = palette.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(palette.text),
            modifier = Modifier.fillMaxWidth()
        )
    }
}


/** Margen inferior de la barra de navegación leído de la ventana de la actividad (los Dialog no siempre lo reciben). */
@androidx.compose.runtime.Composable
internal fun activityNavBarBottomPadding(): androidx.compose.ui.unit.Dp {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var c: android.content.Context? = ctx
    while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
    val decor = (c as? android.app.Activity)?.window?.decorView ?: return 0.dp
    val insets = androidx.core.view.ViewCompat.getRootWindowInsets(decor) ?: return 0.dp
    val px = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
    return with(density) { px.toDp() }
}
