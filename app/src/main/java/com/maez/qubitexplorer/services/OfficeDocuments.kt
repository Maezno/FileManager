package com.maez.qubitexplorer.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/*
 * Lectores simples de documentos Office Open XML (DOCX, PPTX, XLSX).
 * No pretenden ser fieles al 100 %: extraen texto, formato básico, tablas, imágenes
 * y posiciones para mostrar una vista previa legible sin depender de otra app.
 */

// ── Modelos ─────────────────────────────────────────────────────────────────────────

data class OfficeRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val sizePt: Float? = null,
    val color: Int? = null
)

enum class OfficeAlign { START, CENTER, END, JUSTIFY }

data class OfficeParagraph(
    val runs: List<OfficeRun>,
    val headingLevel: Int = 0,
    val bullet: Boolean = false,
    val indentLevel: Int = 0,
    val align: OfficeAlign = OfficeAlign.START
) {
    val plainText: String get() = runs.joinToString("") { it.text }
}

sealed class DocBlock {
    data class Paragraph(val p: OfficeParagraph) : DocBlock()
    data class Table(val rows: List<List<String>>) : DocBlock()
    data class Image(val bitmap: Bitmap) : DocBlock()
}

data class DocxDocument(val blocks: List<DocBlock>)

sealed class SlideElement {
    abstract val x: Long; abstract val y: Long; abstract val w: Long; abstract val h: Long
    data class TextBox(
        override val x: Long, override val y: Long, override val w: Long, override val h: Long,
        val paragraphs: List<OfficeParagraph>, val fill: Int?, val defaultSizePt: Float
    ) : SlideElement()
    data class Picture(
        override val x: Long, override val y: Long, override val w: Long, override val h: Long,
        val bitmap: Bitmap
    ) : SlideElement()
}

data class Slide(val elements: List<SlideElement>, val background: Int?, val notes: String?)
data class PptxDocument(val widthEmu: Long, val heightEmu: Long, val slides: List<Slide>)

data class SheetData(
    val name: String,
    val cells: Map<Int, Map<Int, String>>, // fila → (columna → valor)
    val maxRow: Int,
    val maxCol: Int,
    val truncated: Boolean
)
data class XlsxDocument(val sheets: List<SheetData>)

// ── Utilidades comunes ──────────────────────────────────────────────────────────────

private fun newParser(input: InputStream): XmlPullParser =
    Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(input, null)
    }

private fun ZipFile.text(name: String): String? =
    getEntry(name)?.let { e -> getInputStream(e).use { it.readBytes().toString(Charsets.UTF_8) } }

private fun ZipFile.parser(name: String): XmlPullParser? =
    getEntry(name)?.let { newParser(getInputStream(it)) }

/** Lee un archivo .rels → mapa Id → Target (resuelto respecto a la carpeta base). */
private fun ZipFile.rels(relsPath: String, baseDir: String): Map<String, String> {
    val p = parser(relsPath) ?: return emptyMap()
    val out = HashMap<String, String>()
    while (p.next() != XmlPullParser.END_DOCUMENT) {
        if (p.eventType == XmlPullParser.START_TAG && p.name == "Relationship") {
            val id = p.getAttributeValue(null, "Id") ?: continue
            val target = p.getAttributeValue(null, "Target") ?: continue
            out[id] = resolvePath(baseDir, target)
        }
    }
    return out
}

private fun resolvePath(baseDir: String, target: String): String {
    if (target.startsWith("/")) return target.trimStart('/')
    val parts = (if (baseDir.isEmpty()) emptyList() else baseDir.split('/')).toMutableList()
    for (seg in target.split('/')) {
        when (seg) {
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
            ".", "" -> {}
            else -> parts += seg
        }
    }
    return parts.joinToString("/")
}

private fun ZipFile.bitmap(path: String, maxPx: Int = 1400): Bitmap? = try {
    getEntry(path)?.let {
        val bytes = getInputStream(it).use { s -> s.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }
} catch (_: Throwable) {
    null
}

private fun parseHexColor(hex: String?): Int? {
    if (hex == null || hex.length != 6 || hex.equals("auto", true)) return null
    return hex.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
}

private fun XmlPullParser.attr(name: String): String? = getAttributeValue(null, name)

private fun boolAttr(v: String?): Boolean = v == null || (v != "0" && v != "false" && v != "none")

// ── DOCX ────────────────────────────────────────────────────────────────────────────

object DocxReader {
    suspend fun read(file: File): DocxDocument = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val rels = zip.rels("word/_rels/document.xml.rels", "word")
            val p = zip.parser("word/document.xml") ?: throw IllegalArgumentException("No es un DOCX válido")
            val blocks = ArrayList<DocBlock>()
            var images = 0

            // Estado del párrafo actual
            var runs = ArrayList<OfficeRun>()
            var heading = 0; var bullet = false; var ilvl = 0; var align = OfficeAlign.START
            // Estado del run actual
            var bold = false; var italic = false; var underline = false; var size: Float? = null; var color: Int? = null
            var inRunProps = false
            // Tablas (se soportan anidadas aplanando el texto de la celda)
            var tableDepth = 0
            var tableRows = ArrayList<List<String>>()
            var currentRow = ArrayList<String>()
            val cellText = StringBuilder()
            var inText = false
            val pendingImages = ArrayList<Bitmap>()

            while (p.next() != XmlPullParser.END_DOCUMENT) {
                when (p.eventType) {
                    XmlPullParser.START_TAG -> when (p.name) {
                        "w:tbl" -> { tableDepth++; if (tableDepth == 1) tableRows = ArrayList() }
                        "w:tr" -> if (tableDepth == 1) currentRow = ArrayList()
                        "w:tc" -> if (tableDepth == 1) cellText.setLength(0)
                        "w:p" -> { runs = ArrayList(); heading = 0; bullet = false; ilvl = 0; align = OfficeAlign.START }
                        "w:pStyle" -> {
                            val v = p.attr("w:val")?.lowercase() ?: ""
                            Regex("""(?:heading|ttulo|titulo|título)\s*(\d)""").find(v)?.let { heading = it.groupValues[1].toInt() }
                            if (v == "title" || v == "titulo" || v == "título") heading = 1
                            if (v.contains("list")) bullet = true
                        }
                        "w:numPr" -> bullet = true
                        "w:ilvl" -> ilvl = p.attr("w:val")?.toIntOrNull() ?: 0
                        "w:jc" -> align = when (p.attr("w:val")) {
                            "center" -> OfficeAlign.CENTER
                            "right", "end" -> OfficeAlign.END
                            "both", "distribute" -> OfficeAlign.JUSTIFY
                            else -> OfficeAlign.START
                        }
                        "w:r" -> { bold = false; italic = false; underline = false; size = null; color = null }
                        "w:rPr" -> inRunProps = true
                        "w:b" -> if (inRunProps) bold = boolAttr(p.attr("w:val"))
                        "w:i" -> if (inRunProps) italic = boolAttr(p.attr("w:val"))
                        "w:u" -> if (inRunProps) underline = boolAttr(p.attr("w:val"))
                        "w:sz" -> if (inRunProps) size = p.attr("w:val")?.toFloatOrNull()?.div(2f)
                        "w:color" -> if (inRunProps) color = parseHexColor(p.attr("w:val"))
                        "w:t" -> inText = true
                        "w:tab" -> if (!inRunProps) runs += OfficeRun("\t")
                        "w:br", "w:cr" -> if (!inRunProps) runs += OfficeRun("\n")
                        "a:blip" -> {
                            val id = p.attr("r:embed")
                            if (id != null && images < 60) {
                                rels[id]?.let { zip.bitmap(it) }?.let { pendingImages += it; images++ }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> if (inText) {
                        val t = p.text ?: ""
                        if (tableDepth > 0) cellText.append(t)
                        else runs += OfficeRun(t, bold, italic, underline, size, color)
                    }
                    XmlPullParser.END_TAG -> when (p.name) {
                        "w:t" -> inText = false
                        "w:rPr" -> inRunProps = false
                        "w:p" -> {
                            if (tableDepth > 0) {
                                if (cellText.isNotEmpty()) cellText.append('\n')
                            } else {
                                blocks += DocBlock.Paragraph(OfficeParagraph(mergeRuns(runs), heading, bullet, ilvl, align))
                            }
                            pendingImages.forEach { blocks += DocBlock.Image(it) }
                            pendingImages.clear()
                        }
                        "w:tc" -> if (tableDepth == 1) currentRow += cellText.toString().trim()
                        "w:tr" -> if (tableDepth == 1) tableRows += currentRow
                        "w:tbl" -> {
                            if (tableDepth == 1 && tableRows.isNotEmpty()) blocks += DocBlock.Table(tableRows)
                            tableDepth--
                        }
                    }
                }
            }
            DocxDocument(trimEmpty(blocks))
        }
    }

    private fun mergeRuns(runs: List<OfficeRun>): List<OfficeRun> {
        val out = ArrayList<OfficeRun>()
        for (r in runs) {
            val last = out.lastOrNull()
            if (last != null && last.copy(text = "") == r.copy(text = "")) out[out.size - 1] = last.copy(text = last.text + r.text)
            else out += r
        }
        return out
    }

    /** Colapsa párrafos vacíos consecutivos. */
    private fun trimEmpty(blocks: List<DocBlock>): List<DocBlock> {
        val out = ArrayList<DocBlock>()
        var lastEmpty = false
        for (b in blocks) {
            val empty = b is DocBlock.Paragraph && b.p.plainText.isBlank()
            if (empty && lastEmpty) continue
            out += b
            lastEmpty = empty
        }
        return out
    }
}

// ── PPTX ────────────────────────────────────────────────────────────────────────────

object PptxReader {
    suspend fun read(file: File, maxSlides: Int = 200): PptxDocument = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            var cx = 12_192_000L; var cy = 6_858_000L
            val slideIds = ArrayList<String>()
            zip.parser("ppt/presentation.xml")?.let { p ->
                while (p.next() != XmlPullParser.END_DOCUMENT) {
                    if (p.eventType != XmlPullParser.START_TAG) continue
                    when (p.name) {
                        "p:sldSz" -> { cx = p.attr("cx")?.toLongOrNull() ?: cx; cy = p.attr("cy")?.toLongOrNull() ?: cy }
                        "p:sldId" -> p.attr("r:id")?.let { slideIds += it }
                    }
                }
            } ?: throw IllegalArgumentException("No es un PPTX válido")
            val presRels = zip.rels("ppt/_rels/presentation.xml.rels", "ppt")
            val slidePaths = slideIds.mapNotNull { presRels[it] }.ifEmpty {
                zip.entries().asSequence().map { it.name }
                    .filter { Regex("""ppt/slides/slide\d+\.xml""").matches(it) }
                    .sortedBy { Regex("""\d+""").find(it.substringAfterLast('/'))?.value?.toIntOrNull() ?: 0 }
                    .toList()
            }
            val slides = slidePaths.take(maxSlides).map { path -> readSlide(zip, path, cx, cy) }
            PptxDocument(cx, cy, slides)
        }
    }

    private fun readSlide(zip: ZipFile, path: String, cx: Long, cy: Long): Slide {
        val dir = path.substringBeforeLast('/')
        val rels = zip.rels("$dir/_rels/${path.substringAfterLast('/')}.rels", dir)
        val p = zip.parser(path) ?: return Slide(emptyList(), null, null)
        val elements = ArrayList<SlideElement>()
        var background: Int? = null

        // Estado de la forma actual
        var inShape = false; var inPic = false; var inBg = false
        var x = -1L; var y = -1L; var w = -1L; var h = -1L
        var phType: String? = null
        var fill: Int? = null; var inSpPr = false
        var blipId: String? = null
        val paragraphs = ArrayList<OfficeParagraph>()
        var runs = ArrayList<OfficeRun>()
        var align = OfficeAlign.START; var bullet = false; var lvl = 0
        var bold = false; var italic = false; var underline = false; var size: Float? = null; var color: Int? = null
        var inRPr = false; var inText = false; var defaultSize = 18f

        fun resetShape() {
            x = -1; y = -1; w = -1; h = -1; phType = null; fill = null; blipId = null; paragraphs.clear()
        }

        fun fallbackBox(): LongArray = when (phType) {
            "title", "ctrTitle" -> longArrayOf(cx / 20, cy / 20, cx * 9 / 10, cy / 5)
            "subTitle" -> longArrayOf(cx / 10, cy * 3 / 5, cx * 4 / 5, cy / 5)
            else -> longArrayOf(cx / 20, cy / 4, cx * 9 / 10, cy * 2 / 3)
        }

        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "p:bg" -> inBg = true
                    "p:sp" -> { inShape = true; resetShape() }
                    "p:pic" -> { inPic = true; resetShape() }
                    "p:ph" -> { phType = p.attr("type") ?: "body"; if (phType == "title" || phType == "ctrTitle") defaultSize = 40f else defaultSize = 20f }
                    "p:spPr" -> inSpPr = true
                    "a:off" -> if (x < 0) { x = p.attr("x")?.toLongOrNull() ?: -1; y = p.attr("y")?.toLongOrNull() ?: -1 }
                    "a:ext" -> if (w < 0) { w = p.attr("cx")?.toLongOrNull() ?: -1; h = p.attr("cy")?.toLongOrNull() ?: -1 }
                    "a:srgbClr" -> {
                        val c = parseHexColor(p.attr("val"))
                        when {
                            inBg -> background = c
                            inRPr -> color = c
                            inSpPr && (inShape) -> fill = c
                        }
                    }
                    "a:blip" -> blipId = p.attr("r:embed")
                    "a:p" -> { runs = ArrayList(); align = OfficeAlign.START; bullet = false; lvl = 0 }
                    "a:pPr" -> {
                        lvl = p.attr("lvl")?.toIntOrNull() ?: 0
                        align = when (p.attr("algn")) { "ctr" -> OfficeAlign.CENTER; "r" -> OfficeAlign.END; "just" -> OfficeAlign.JUSTIFY; else -> OfficeAlign.START }
                    }
                    "a:buChar", "a:buAutoNum" -> bullet = true
                    "a:r", "a:fld" -> { bold = false; italic = false; underline = false; size = null; color = null }
                    "a:rPr", "a:endParaRPr" -> {
                        inRPr = p.name == "a:rPr"
                        if (inRPr) {
                            bold = p.attr("b") == "1"; italic = p.attr("i") == "1"
                            underline = (p.attr("u") ?: "none") != "none"
                            size = p.attr("sz")?.toFloatOrNull()?.div(100f)
                        }
                    }
                    "a:t" -> inText = true
                    "a:br" -> runs += OfficeRun("\n")
                }
                XmlPullParser.TEXT -> if (inText) runs += OfficeRun(p.text ?: "", bold, italic, underline, size, color)
                XmlPullParser.END_TAG -> when (p.name) {
                    "p:bg" -> inBg = false
                    "p:spPr" -> inSpPr = false
                    "a:t" -> inText = false
                    "a:rPr" -> inRPr = false
                    "a:p" -> if (inShape) {
                        // Las viñetas por defecto de los cuerpos de texto se aproximan
                        val isBody = phType == "body" || phType == "obj"
                        paragraphs += OfficeParagraph(runs.toList(), 0, bullet || (isBody && runs.any { it.text.isNotBlank() }), lvl, align)
                    }
                    "p:sp" -> {
                        if (paragraphs.any { it.plainText.isNotBlank() } || fill != null) {
                            val box = if (x >= 0 && w > 0) longArrayOf(x, y, w, h) else fallbackBox()
                            elements += SlideElement.TextBox(box[0], box[1], box[2], box[3], paragraphs.toList(), fill, defaultSize)
                        }
                        inShape = false; defaultSize = 18f
                    }
                    "p:pic" -> {
                        val bmp = blipId?.let { rels[it] }?.let { zip.bitmap(it) }
                        if (bmp != null) {
                            val box = if (x >= 0 && w > 0) longArrayOf(x, y, w, h) else longArrayOf(cx / 10, cy / 10, cx * 4 / 5, cy * 4 / 5)
                            elements += SlideElement.Picture(box[0], box[1], box[2], box[3], bmp)
                        }
                        inPic = false
                    }
                }
            }
        }
        val notes = rels.values.firstOrNull { it.contains("notesSlide") }?.let { readNotes(zip, it) }
        return Slide(elements, background, notes)
    }

    private fun readNotes(zip: ZipFile, path: String): String? {
        val p = zip.parser(path) ?: return null
        val sb = StringBuilder()
        var inText = false; var isBody = false; var depth = 0
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "p:sp" -> { depth++; isBody = false }
                    "p:ph" -> isBody = p.attr("type") == "body"
                    "a:t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inText && isBody) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "a:t" -> inText = false
                    "a:p" -> if (isBody) sb.append('\n')
                }
            }
        }
        return sb.toString().trim().ifBlank { null }
    }
}

// ── XLSX ────────────────────────────────────────────────────────────────────────────

object XlsxReader {
    suspend fun read(file: File, maxRows: Int = 5000, maxCols: Int = 200): XlsxDocument = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val shared = readSharedStrings(zip)
            val wbRels = zip.rels("xl/_rels/workbook.xml.rels", "xl")
            val sheetRefs = ArrayList<Pair<String, String>>() // nombre → ruta
            zip.parser("xl/workbook.xml")?.let { p ->
                while (p.next() != XmlPullParser.END_DOCUMENT) {
                    if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") {
                        val name = p.attr("name") ?: "Hoja"
                        val target = p.attr("r:id")?.let { wbRels[it] }
                        if (target != null) sheetRefs += name to target
                    }
                }
            } ?: throw IllegalArgumentException("No es un XLSX válido")
            XlsxDocument(sheetRefs.map { (name, path) -> readSheet(zip, name, path, shared, maxRows, maxCols) })
        }
    }

    private fun readSharedStrings(zip: ZipFile): List<String> {
        val p = zip.parser("xl/sharedStrings.xml") ?: return emptyList()
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inT = false; var inRPh = false
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> sb.setLength(0)
                    "t" -> inT = true
                    "rPh" -> inRPh = true // texto fonético: se ignora
                }
                XmlPullParser.TEXT -> if (inT && !inRPh) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "rPh" -> inRPh = false
                    "si" -> out += sb.toString()
                }
            }
        }
        return out
    }

    /** "BC12" → (fila 11, columna 54), base 0. */
    private fun cellRef(ref: String): Pair<Int, Int>? {
        var col = 0; var i = 0
        while (i < ref.length && ref[i].isLetter()) { col = col * 26 + (ref[i].uppercaseChar() - 'A' + 1); i++ }
        val row = ref.substring(i).toIntOrNull() ?: return null
        return (row - 1) to (col - 1)
    }

    private fun readSheet(zip: ZipFile, name: String, path: String, shared: List<String>, maxRows: Int, maxCols: Int): SheetData {
        val p = zip.parser(path) ?: return SheetData(name, emptyMap(), 0, 0, false)
        val cells = HashMap<Int, HashMap<Int, String>>()
        var maxRow = -1; var maxCol = -1; var truncated = false
        var ref: String? = null; var type: String? = null
        var inV = false; var inIs = false
        val value = StringBuilder()
        var rowIndex = -1; var colCursor = 0

        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> { rowIndex = (p.attr("r")?.toIntOrNull() ?: (rowIndex + 2)) - 1; colCursor = 0 }
                    "c" -> { ref = p.attr("r"); type = p.attr("t"); value.setLength(0) }
                    "v" -> inV = true
                    "is" -> inIs = true
                    "t" -> if (inIs) inV = true
                }
                XmlPullParser.TEXT -> if (inV) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v" -> inV = false
                    "t" -> if (inIs) inV = false
                    "is" -> inIs = false
                    "c" -> {
                        val (r, c) = ref?.let { cellRef(it) } ?: (rowIndex to colCursor)
                        colCursor = c + 1
                        val raw = value.toString()
                        val text = when (type) {
                            "s" -> raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: raw
                            "b" -> if (raw == "1") "VERDADERO" else "FALSO"
                            "e" -> raw
                            else -> formatNumber(raw)
                        }
                        if (text.isNotEmpty()) {
                            if (r < maxRows && c < maxCols) {
                                cells.getOrPut(r) { HashMap() }[c] = text
                                if (r > maxRow) maxRow = r
                                if (c > maxCol) maxCol = c
                            } else truncated = true
                        }
                    }
                }
            }
        }
        return SheetData(name, cells, maxRow + 1, maxCol + 1, truncated)
    }

    private fun formatNumber(raw: String): String {
        val d = raw.toDoubleOrNull() ?: return raw
        if (d == Math.floor(d) && kotlin.math.abs(d) < 1e15) return d.toLong().toString()
        return "%.10g".format(java.util.Locale.US, d).trimEnd('0').trimEnd('.')
    }

    fun columnName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) { val m = (n - 1) % 26; sb.insert(0, 'A' + m); n = (n - 1) / 26 }
        return sb.toString()
    }
}

object OfficeFormats {
    val DOCX = setOf("docx", "docm", "dotx")
    val PPTX = setOf("pptx", "pptm", "ppsx", "potx")
    val XLSX = setOf("xlsx", "xlsm", "xltx")
    val DOC = setOf("doc", "dot")
    val PPT = setOf("ppt", "pps", "pot")
    val XLS = setOf("xls", "xlt")
    val ODT = setOf("odt", "ott")
    val ODP = setOf("odp", "otp")
    val ODS = setOf("ods", "ots")

    val TEXT_DOCS = DOCX + DOC + ODT
    val PRESENTATIONS = PPTX + PPT + ODP
    val SPREADSHEETS = XLSX + XLS + ODS

    fun isSupported(ext: String) = ext in TEXT_DOCS || ext in PRESENTATIONS || ext in SPREADSHEETS
}
