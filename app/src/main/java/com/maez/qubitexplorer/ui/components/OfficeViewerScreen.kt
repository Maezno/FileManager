package com.maez.qubitexplorer.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maez.qubitexplorer.services.DocBlock
import com.maez.qubitexplorer.services.DocxDocument
import com.maez.qubitexplorer.services.DocxReader
import com.maez.qubitexplorer.services.OfficeAlign
import com.maez.qubitexplorer.services.OfficeFormats
import com.maez.qubitexplorer.services.OfficeParagraph
import com.maez.qubitexplorer.services.PptxDocument
import com.maez.qubitexplorer.services.PptxReader
import com.maez.qubitexplorer.services.SheetData
import com.maez.qubitexplorer.services.SlideElement
import com.maez.qubitexplorer.services.XlsxDocument
import com.maez.qubitexplorer.services.XlsxReader
import androidx.compose.foundation.layout.imePadding
import java.io.File

private val ViewerBg = Color(0xFF2A2C33)
private val BarBg = Color(0xFF1C1D22)
private val PaperText = Color(0xFF1F2330)

private sealed class OfficeLoad {
    data object Loading : OfficeLoad()
    data class Error(val msg: String) : OfficeLoad()
    data class Docx(val doc: DocxDocument) : OfficeLoad()
    data class Pptx(val doc: PptxDocument) : OfficeLoad()
    data class Xlsx(val doc: XlsxDocument) : OfficeLoad()
}

@Composable
fun OfficeViewerScreen(
    fileName: String,
    localPath: String?,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler { onClose() }
        var state by remember { mutableStateOf<OfficeLoad>(OfficeLoad.Loading) }
        val ext = fileName.substringAfterLast('.', "").lowercase()

        LaunchedEffect(localPath) {
            val path = localPath ?: return@LaunchedEffect
            state = try {
                val f = File(path)
                when (ext) {
                    in OfficeFormats.DOCX -> OfficeLoad.Docx(DocxReader.read(f))
                    in OfficeFormats.DOC -> OfficeLoad.Docx(com.maez.qubitexplorer.services.DocReader.read(f))
                    in OfficeFormats.ODT -> OfficeLoad.Docx(com.maez.qubitexplorer.services.OdtReader.read(f))
                    in OfficeFormats.PPTX -> OfficeLoad.Pptx(PptxReader.read(f))
                    in OfficeFormats.PPT -> OfficeLoad.Pptx(com.maez.qubitexplorer.services.PptReader.read(f))
                    in OfficeFormats.ODP -> OfficeLoad.Pptx(com.maez.qubitexplorer.services.OdpReader.read(f))
                    in OfficeFormats.XLSX -> OfficeLoad.Xlsx(XlsxReader.read(f))
                    in OfficeFormats.XLS -> OfficeLoad.Xlsx(com.maez.qubitexplorer.services.XlsReader.read(f))
                    in OfficeFormats.ODS -> OfficeLoad.Xlsx(com.maez.qubitexplorer.services.OdsReader.read(f))
                    else -> OfficeLoad.Error("Formato no soportado.")
                }
            } catch (e: Throwable) {
                OfficeLoad.Error("No se pudo leer el documento (${e.message ?: e.javaClass.simpleName}).")
            }
        }

        val subtitle = when (val s = state) {
            is OfficeLoad.Docx -> when (ext) { in OfficeFormats.ODT -> "Documento de texto (ODT)"; in OfficeFormats.DOC -> "Documento de Word 97-2003"; else -> "Documento de Word" }
            is OfficeLoad.Pptx -> "Presentación · ${s.doc.slides.size} diapositivas"
            is OfficeLoad.Xlsx -> "Hoja de cálculo · ${s.doc.sheets.size} hojas"
            else -> ""
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(ViewerBg)
                .statusBarsPadding()
                .padding(bottom = activityNavBarBottomPadding())
        ) {
            Row(
                Modifier.fillMaxWidth().background(BarBg).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(fileName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle.isNotEmpty()) Text(subtitle, color = Color(0xFFB0B4C0), fontSize = 11.sp)
                }
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Compartir", tint = Color.White) }
                IconButton(onClick = onOpenExternal) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "Abrir con otra app", tint = Color.White) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (val s = state) {
                    OfficeLoad.Loading -> CircularProgressIndicator(color = Color.White)
                    is OfficeLoad.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.msg, color = Color.White, modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center)
                        TextButton(onClick = onOpenExternal) { Text("Abrir con otra app") }
                    }
                    is OfficeLoad.Docx -> androidx.compose.foundation.text.selection.SelectionContainer { DocxView(s.doc) }
                    is OfficeLoad.Pptx -> androidx.compose.foundation.text.selection.SelectionContainer { PptxView(s.doc) }
                    is OfficeLoad.Xlsx -> XlsxView(s.doc)
                }
            }
        }
    }
}

// ── Word ──

private fun OfficeAlign.toTextAlign() = when (this) {
    OfficeAlign.CENTER -> TextAlign.Center
    OfficeAlign.END -> TextAlign.End
    OfficeAlign.JUSTIFY -> TextAlign.Justify
    OfficeAlign.START -> TextAlign.Start
}

private fun paragraphText(p: OfficeParagraph, defaultSize: TextUnit, scaleSize: (Float) -> TextUnit, forceBold: Boolean = false): AnnotatedString =
    buildAnnotatedString {
        if (p.bullet) append("•  ")
        for (r in p.runs) {
            withStyle(
                SpanStyle(
                    fontWeight = if (r.bold || forceBold) FontWeight.Bold else null,
                    fontStyle = if (r.italic) FontStyle.Italic else null,
                    textDecoration = if (r.underline) TextDecoration.Underline else null,
                    fontSize = r.sizePt?.let(scaleSize) ?: defaultSize,
                    color = r.color?.let { Color(it) } ?: Color.Unspecified
                )
            ) { append(r.text) }
        }
    }

@Composable
private fun DocxView(doc: DocxDocument) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp)
    ) {
        item { Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(Color.White)) }
        items(doc.blocks) { block ->
            Box(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 3.dp)) {
                when (block) {
                    is DocBlock.Paragraph -> {
                        val p = block.p
                        val headingSize = when (p.headingLevel) { 1 -> 22.sp; 2 -> 19.sp; 3 -> 17.sp; 0 -> 14.sp; else -> 15.sp }
                        Text(
                            // En títulos se usa el tamaño de título, no el del run
                            paragraphText(p, headingSize, { if (p.headingLevel > 0) headingSize else (it * 1.15f).coerceIn(11f, 40f).sp }, forceBold = p.headingLevel > 0),
                            color = PaperText,
                            textAlign = p.align.toTextAlign(),
                            lineHeight = (headingSize.value * 1.35f).sp,
                            modifier = Modifier.fillMaxWidth().padding(
                                start = (if (p.bullet) 12 + p.indentLevel * 14 else 0).dp,
                                top = if (p.headingLevel > 0) 8.dp else 0.dp
                            )
                        )
                    }
                    is DocBlock.Table -> TableView(block.rows)
                    is DocBlock.Image -> Image(
                        block.bitmap.asImageBitmap(), null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    )
                }
            }
        }
        item { Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).background(Color.White)) }
    }
}

@Composable
private fun TableView(rows: List<List<String>>) {
    val cols = rows.maxOf { it.size }
    Column(Modifier.padding(vertical = 6.dp).horizontalScroll(rememberScrollState()).border(1.dp, Color(0xFFBFC3CC))) {
        rows.forEachIndexed { ri, row ->
            Row(Modifier.background(if (ri == 0) Color(0xFFEEF0F5) else Color.White)) {
                for (c in 0 until cols) {
                    Text(
                        row.getOrElse(c) { "" },
                        color = PaperText, fontSize = 12.sp,
                        fontWeight = if (ri == 0) FontWeight.SemiBold else null,
                        modifier = Modifier
                            .width(120.dp)
                            .border(0.5.dp, Color(0xFFD5D8DF))
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

// ── PowerPoint ──

@Composable
private fun PptxView(doc: PptxDocument) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(doc.slides) { index, slide ->
            Column {
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(doc.widthEmu.toFloat() / doc.heightEmu)
                        .clip(RoundedCornerShape(3.dp))
                        .background(slide.background?.let { Color(it) } ?: Color.White)
                ) {
                    val density = LocalDensity.current
                    val widthPx = with(density) { maxWidth.toPx() }
                    val scale = widthPx / doc.widthEmu // px por EMU
                    fun emuToDp(v: Long) = with(density) { (v * scale).toDp() }
                    fun ptToSp(pt: Float) = with(density) { (pt * 12700f * scale).toSp() }

                    for (el in slide.elements) {
                        val mod = Modifier
                            .offset(emuToDp(el.x), emuToDp(el.y))
                            .size(emuToDp(el.w), emuToDp(el.h))
                        when (el) {
                            is SlideElement.Picture -> Image(el.bitmap.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = mod)
                            is SlideElement.TextBox -> Column(
                                mod
                                    .then(if (el.fill != null) Modifier.background(Color(el.fill)) else Modifier)
                                    .padding(horizontal = emuToDp(91440), vertical = emuToDp(45720)),
                                verticalArrangement = Arrangement.Top
                            ) {
                                for (p in el.paragraphs) {
                                    val size = ptToSp(el.defaultSizePt)
                                    Text(
                                        paragraphText(p, size, { ptToSp(it) }),
                                        color = PaperText,
                                        textAlign = p.align.toTextAlign(),
                                        lineHeight = (size.value * 1.15f).sp,
                                        modifier = Modifier.fillMaxWidth().padding(start = emuToDp(p.indentLevel * 342900L))
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    "Diapositiva ${index + 1}" + (slide.notes?.let { " · Notas: $it" } ?: ""),
                    color = Color(0xFFB0B4C0), fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
        }
    }
}

// ── Excel ──

@Composable
private fun XlsxView(doc: XlsxDocument) {
    var selected by remember { mutableIntStateOf(0) }
    val sheet = doc.sheets.getOrNull(selected)
    Column(Modifier.fillMaxSize()) {
        if (doc.sheets.size > 1) {
            Row(Modifier.fillMaxWidth().background(BarBg).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp)) {
                doc.sheets.forEachIndexed { i, s ->
                    Text(
                        s.name,
                        color = if (i == selected) Color.White else Color(0xFFB0B4C0),
                        fontSize = 12.sp,
                        fontWeight = if (i == selected) FontWeight.Bold else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (i == selected) Color(0xFF1E9E5A) else Color.Transparent)
                            .clickable { selected = i }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        if (sheet == null || sheet.maxRow == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Hoja vacía", color = Color.White) }
        } else {
            SheetGrid(sheet)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SheetGrid(sheet: SheetData) {
    val colW = 96.dp
    val rowHeaderW = 44.dp
    val cols = sheet.maxCol.coerceAtLeast(1)
    val rowsCount = sheet.maxRow.coerceAtLeast(1)
    val grid = Color(0xFFD5D8DF)
    val selFill = Color(0x331E9E5A)
    val selBorder = Color(0xFF1E9E5A)
    val hScroll = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Selección: celda ancla + celda foco → rango rectangular
    var anchor by remember(sheet) { mutableStateOf<Pair<Int, Int>?>(null) }
    var focus by remember(sheet) { mutableStateOf<Pair<Int, Int>?>(null) }
    var extendMode by remember { mutableStateOf(false) }
    val range = anchor?.let { a ->
        val f = focus ?: a
        (minOf(a.first, f.first)..maxOf(a.first, f.first)) to (minOf(a.second, f.second)..maxOf(a.second, f.second))
    }
    fun inRange(r: Int, c: Int) = range != null && r in range.first && c in range.second
    fun tapCell(r: Int, c: Int) {
        if (extendMode && anchor != null) focus = r to c else { anchor = r to c; focus = r to c }
    }
    fun refOf(r: Int, c: Int) = XlsxReader.columnName(c) + (r + 1)
    fun copySelection() {
        val rg = range ?: return
        val tsv = rg.first.joinToString("\n") { r -> rg.second.joinToString("\t") { c -> sheet.cells[r]?.get(c) ?: "" } }
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Celdas", tsv))
        android.widget.Toast.makeText(context, "Celdas copiadas", android.widget.Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // Barra de referencia y contenido de la celda (estilo barra de fórmulas)
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF4F5F8)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label = when {
                range == null -> "—"
                range.first.first == range.first.last && range.second.first == range.second.last -> refOf(range.first.first, range.second.first)
                else -> refOf(range.first.first, range.second.first) + ":" + refOf(range.first.last, range.second.last)
            }
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = selBorder, modifier = Modifier.width(78.dp), maxLines = 1)
            Box(Modifier.weight(1f)) { androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    anchor?.let { (r, c) -> sheet.cells[r]?.get(c) } ?: "Tocá una celda · mantené presionado para elegir un rango",
                    fontSize = 12.sp,
                    color = if (anchor == null) Color(0xFF8A8FA3) else PaperText,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            } }
            Text(
                "Rango",
                fontSize = 11.sp,
                color = if (extendMode) Color.White else selBorder,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (extendMode) selBorder else Color.Transparent)
                    .border(1.dp, selBorder, RoundedCornerShape(6.dp))
                    .clickable { extendMode = !extendMode }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            IconButton(onClick = { copySelection() }, enabled = range != null) {
                Icon(Icons.Default.ContentCopy, "Copiar celdas", tint = if (range != null) selBorder else Color(0xFFBFC3CC))
            }
        }
        // Encabezado de columnas: tocar selecciona la columna; la esquina selecciona todo
        Row(Modifier.background(Color(0xFFEEF0F5))) {
            Box(
                Modifier.width(rowHeaderW).height(26.dp).border(0.5.dp, grid)
                    .clickable { anchor = 0 to 0; focus = (rowsCount - 1) to (cols - 1) }
            )
            Row(Modifier.horizontalScroll(hScroll)) {
                for (c in 0 until cols) {
                    val colSelected = range != null && c in range.second && range.first.first == 0 && range.first.last == rowsCount - 1
                    Box(
                        Modifier.width(colW).height(26.dp).background(if (colSelected) selFill else Color.Transparent).border(0.5.dp, grid)
                            .clickable { anchor = 0 to c; focus = (rowsCount - 1) to c },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(XlsxReader.columnName(c), fontSize = 11.sp, color = Color(0xFF5E6273), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(rowsCount) { r ->
                val row = sheet.cells[r]
                Row {
                    Box(
                        Modifier.width(rowHeaderW).height(26.dp).background(Color(0xFFEEF0F5)).border(0.5.dp, grid)
                            .clickable { anchor = r to 0; focus = r to (cols - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${r + 1}", fontSize = 11.sp, color = Color(0xFF5E6273))
                    }
                    Row(Modifier.horizontalScroll(hScroll)) {
                        for (c in 0 until cols) {
                            val v = row?.get(c) ?: ""
                            val numeric = v.isNotEmpty() && v.replace(".", "").replace(",", ".").removeSuffix("%").trim().toDoubleOrNull() != null
                            val selected = inRange(r, c)
                            val isAnchor = anchor == (r to c)
                            Box(
                                Modifier
                                    .width(colW).height(26.dp)
                                    .background(if (selected && !isAnchor) selFill else Color.Transparent)
                                    .border(if (isAnchor) 2.dp else 0.5.dp, if (isAnchor) selBorder else grid)
                                    .combinedClickable(
                                        onClick = { tapCell(r, c) },
                                        onLongClick = { if (anchor == null) anchor = r to c; focus = r to c }
                                    )
                                    .padding(horizontal = 4.dp),
                                contentAlignment = if (numeric) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Text(v, fontSize = 12.sp, color = PaperText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (sheet.truncated) {
                item { Text("Se muestran hasta 5000 filas y 200 columnas.", fontSize = 11.sp, color = Color(0xFF8A8FA3), modifier = Modifier.padding(8.dp)) }
            }
        }
    }
}
