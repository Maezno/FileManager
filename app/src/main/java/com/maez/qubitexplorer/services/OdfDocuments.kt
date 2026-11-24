package com.maez.qubitexplorer.services

import com.maez.qubitexplorer.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile

/*
 * Lectores de OpenDocument (LibreOffice / OpenOffice): ODT, ODS y ODP.
 * Producen los mismos modelos que los lectores OOXML para reutilizar el visor.
 */

private data class OdfTextStyle(
    val bold: Boolean? = null, val italic: Boolean? = null, val underline: Boolean? = null,
    val sizePt: Float? = null, val color: Int? = null, val align: OfficeAlign? = null,
    val parent: String? = null
)

private fun odfParser(zip: ZipFile, name: String): XmlPullParser? =
    zip.getEntry(name)?.let { e ->
        Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(zip.getInputStream(e), null)
        }
    }

private fun XmlPullParser.a(name: String): String? = getAttributeValue(null, name)

private fun parseLength(v: String?): Double? {
    if (v == null) return null
    val m = Regex("""(-?[\d.]+)\s*(cm|mm|in|pt|pc|px)?""").find(v) ?: return null
    val n = m.groupValues[1].toDoubleOrNull() ?: return null
    return when (m.groupValues[2]) { // a centímetros
        "mm" -> n / 10; "in" -> n * 2.54; "pt" -> n * 2.54 / 72; "pc" -> n * 2.54 / 6; "px" -> n * 2.54 / 96
        else -> n
    }
}
private fun cmToEmu(cm: Double) = (cm * 360_000).toLong()

private fun parseOdfColor(v: String?): Int? =
    v?.takeIf { it.startsWith("#") && it.length == 7 }?.substring(1)?.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }

/** Lee estilos automáticos (y de styles.xml) → nombre → propiedades de texto/párrafo. */
private fun readOdfStyles(zip: ZipFile): Map<String, OdfTextStyle> {
    val out = HashMap<String, OdfTextStyle>()
    for (file in listOf("styles.xml", "content.xml")) {
        val p = odfParser(zip, file) ?: continue
        var current: String? = null
        var style = OdfTextStyle()
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "style:style" -> { current = p.a("style:name"); style = OdfTextStyle(parent = p.a("style:parent-style-name")) }
                    "style:text-properties" -> if (current != null) style = style.copy(
                        bold = p.a("fo:font-weight")?.let { it == "bold" || (it.toIntOrNull() ?: 400) >= 600 } ?: style.bold,
                        italic = p.a("fo:font-style")?.let { it == "italic" || it == "oblique" } ?: style.italic,
                        underline = p.a("style:text-underline-style")?.let { it != "none" } ?: style.underline,
                        sizePt = p.a("fo:font-size")?.takeIf { it.endsWith("pt") }?.removeSuffix("pt")?.toFloatOrNull() ?: style.sizePt,
                        color = parseOdfColor(p.a("fo:color")) ?: style.color
                    )
                    "style:paragraph-properties" -> if (current != null) style = style.copy(
                        align = when (p.a("fo:text-align")) {
                            "center" -> OfficeAlign.CENTER; "end", "right" -> OfficeAlign.END
                            "justify" -> OfficeAlign.JUSTIFY; null -> style.align; else -> OfficeAlign.START
                        }
                    )
                }
                XmlPullParser.END_TAG -> if (p.name == "style:style") { current?.let { out[it] = style }; current = null }
            }
        }
    }
    return out
}

/** Resuelve herencia de estilos (style:parent-style-name). */
private fun Map<String, OdfTextStyle>.resolve(name: String?): OdfTextStyle {
    var s = OdfTextStyle(); var n = name; var guard = 0
    while (n != null && guard++ < 10) {
        val st = this[n] ?: break
        s = OdfTextStyle(
            s.bold ?: st.bold, s.italic ?: st.italic, s.underline ?: st.underline,
            s.sizePt ?: st.sizePt, s.color ?: st.color, s.align ?: st.align
        )
        n = st.parent
    }
    return s
}

private fun ZipFile.odfBitmap(path: String?, maxPx: Int = 1400): Bitmap? {
    val e = path?.let { getEntry(it.removePrefix("./")) } ?: return null
    return try {
        val bytes = getInputStream(e).use { it.readBytesLimited() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Throwable) { null }
}

/**
 * Lector de párrafos ODF compartido: acumula runs con estilos anidados (text:span),
 * espacios (text:s), tabulaciones y saltos de línea.
 */
private class OdfParagraphBuilder(private val styles: Map<String, OdfTextStyle>) {
    val runs = ArrayList<OfficeRun>()
    private val spanStack = ArrayList<OdfTextStyle>()
    var paragraphStyle = OdfTextStyle()

    fun start(styleName: String?) { runs.clear(); spanStack.clear(); paragraphStyle = styles.resolve(styleName) }
    fun pushSpan(styleName: String?) { spanStack += styles.resolve(styleName) }
    fun popSpan() { if (spanStack.isNotEmpty()) spanStack.removeAt(spanStack.size - 1) }

    fun text(t: String) {
        if (t.isEmpty()) return
        val s = spanStack.lastOrNull() ?: OdfTextStyle()
        val ps = paragraphStyle
        runs += OfficeRun(
            t, bold = s.bold ?: ps.bold ?: false, italic = s.italic ?: ps.italic ?: false,
            underline = s.underline ?: ps.underline ?: false, sizePt = s.sizePt ?: ps.sizePt, color = s.color ?: ps.color
        )
    }

    fun build(heading: Int, bullet: Boolean, level: Int) =
        OfficeParagraph(runs.toList(), heading, bullet, level, paragraphStyle.align ?: OfficeAlign.START)
}

// ── ODT ─────────────────────────────────────────────────────────────────────────────

object OdtReader {
    suspend fun read(file: File): DocxDocument = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val styles = readOdfStyles(zip)
            val p = odfParser(zip, "content.xml") ?: throw AppException(R.string.odf_documents_no_es_un_documento_opendocument)
            val blocks = ArrayList<DocBlock>()
            val para = OdfParagraphBuilder(styles)
            var inBody = false; var inPara = false
            var heading = 0; var listDepth = 0
            var tableDepth = 0
            var rows = ArrayList<List<String>>(); var row = ArrayList<String>()
            val cell = StringBuilder()
            var inNote = 0

            while (p.next() != XmlPullParser.END_DOCUMENT) {
                when (p.eventType) {
                    XmlPullParser.START_TAG -> when (p.name) {
                        "office:text" -> inBody = true
                        "text:note" -> inNote++
                        "table:table" -> { tableDepth++; if (tableDepth == 1) rows = ArrayList() }
                        "table:table-row" -> if (tableDepth == 1) row = ArrayList()
                        "table:table-cell" -> if (tableDepth == 1) cell.setLength(0)
                        "text:list" -> listDepth++
                        "text:p", "text:h" -> if (inBody && inNote == 0) {
                            inPara = true
                            heading = if (p.name == "text:h") (p.a("text:outline-level")?.toIntOrNull() ?: 1) else 0
                            para.start(p.a("text:style-name"))
                        }
                        "text:span" -> para.pushSpan(p.a("text:style-name"))
                        "text:s" -> if (inPara) para.text(" ".repeat(p.a("text:c")?.toIntOrNull() ?: 1))
                        "text:tab" -> if (inPara) para.text("\t")
                        "text:line-break" -> if (inPara) para.text("\n")
                        "draw:image" -> if (inBody && tableDepth == 0) zip.odfBitmap(p.a("xlink:href"))?.let { blocks += DocBlock.Image(it) }
                    }
                    XmlPullParser.TEXT -> if (inPara && inNote == 0) {
                        if (tableDepth > 0) cell.append(p.text) else para.text(p.text ?: "")
                    }
                    XmlPullParser.END_TAG -> when (p.name) {
                        "text:note" -> inNote--
                        "text:span" -> para.popSpan()
                        "text:list" -> listDepth--
                        "text:p", "text:h" -> if (inPara) {
                            inPara = false
                            if (tableDepth > 0) cell.append('\n')
                            else blocks += DocBlock.Paragraph(para.build(heading, listDepth > 0, (listDepth - 1).coerceAtLeast(0)))
                        }
                        "table:table-cell" -> if (tableDepth == 1) row += cell.toString().trim()
                        "table:table-row" -> if (tableDepth == 1) rows += row
                        "table:table" -> { if (tableDepth == 1 && rows.isNotEmpty()) blocks += DocBlock.Table(rows); tableDepth-- }
                    }
                }
            }
            DocxDocument(blocks)
        }
    }
}

// ── ODS ─────────────────────────────────────────────────────────────────────────────

object OdsReader {
    suspend fun read(file: File, maxRows: Int = 5000, maxCols: Int = 200): XlsxDocument = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val p = odfParser(zip, "content.xml") ?: throw AppException(R.string.odf_documents_no_es_una_hoja_opendocument)
            val sheets = ArrayList<SheetData>()
            var cells = HashMap<Int, HashMap<Int, String>>()
            var name = ""; var r = 0; var c = 0; var maxR = 0; var maxC = 0; var truncated = false
            var rowRepeat = 1; var colRepeat = 1
            var cellValue: String? = null
            val text = StringBuilder(); var inP = false; var pCount = 0

            while (p.next() != XmlPullParser.END_DOCUMENT) {
                when (p.eventType) {
                    XmlPullParser.START_TAG -> when (p.name) {
                        "table:table" -> { cells = HashMap(); name = p.a("table:name") ?: "Hoja"; r = 0; maxR = 0; maxC = 0; truncated = false }
                        "table:table-row" -> { c = 0; rowRepeat = (p.a("table:number-rows-repeated")?.toIntOrNull() ?: 1).coerceAtLeast(1) }
                        "table:table-cell", "table:covered-table-cell" -> {
                            colRepeat = (p.a("table:number-columns-repeated")?.toIntOrNull() ?: 1).coerceAtLeast(1)
                            text.setLength(0); pCount = 0
                            cellValue = when (p.a("office:value-type")) {
                                "float", "percentage", "currency" -> p.a("office:value")?.let(::fmtNum)
                                "boolean" -> if (p.a("office:boolean-value") == "true") "VERDADERO" else "FALSO"
                                else -> null
                            }
                        }
                        "text:p" -> { inP = true; if (pCount++ > 0) text.append('\n') }
                        "text:s" -> if (inP) text.append(" ".repeat(p.a("text:c")?.toIntOrNull() ?: 1))
                    }
                    XmlPullParser.TEXT -> if (inP) text.append(p.text)
                    XmlPullParser.END_TAG -> when (p.name) {
                        "text:p" -> inP = false
                        "table:table-cell", "table:covered-table-cell" -> {
                            val v = text.toString().ifEmpty { cellValue ?: "" }
                            if (v.isNotEmpty()) {
                                // Las repeticiones se aplican a celdas con contenido (las vacías solo avanzan)
                                for (rr in 0 until rowRepeat.coerceAtMost(1000)) for (cc in 0 until colRepeat.coerceAtMost(1000)) {
                                    val row = r + rr; val col = c + cc
                                    if (row < maxRows && col < maxCols) {
                                        cells.getOrPut(row) { HashMap() }[col] = v
                                        if (row + 1 > maxR) maxR = row + 1
                                        if (col + 1 > maxC) maxC = col + 1
                                    } else truncated = true
                                }
                            }
                            c += colRepeat
                        }
                        "table:table-row" -> r += rowRepeat
                        "table:table" -> sheets += SheetData(name, cells, maxR, maxC, truncated)
                    }
                }
            }
            XlsxDocument(sheets)
        }
    }

    private fun fmtNum(raw: String): String {
        val d = raw.toDoubleOrNull() ?: return raw
        if (d == Math.floor(d) && kotlin.math.abs(d) < 1e15) return d.toLong().toString()
        return "%.10g".format(java.util.Locale.US, d).trimEnd('0').trimEnd('.')
    }
}

// ── ODP ─────────────────────────────────────────────────────────────────────────────

object OdpReader {
    suspend fun read(file: File): PptxDocument = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val styles = readOdfStyles(zip)
            var wCm = 28.0; var hCm = 15.75
            odfParser(zip, "styles.xml")?.let { sp ->
                while (sp.next() != XmlPullParser.END_DOCUMENT) {
                    if (sp.eventType == XmlPullParser.START_TAG && sp.name == "style:page-layout-properties") {
                        parseLength(sp.a("fo:page-width"))?.let { wCm = it }
                        parseLength(sp.a("fo:page-height"))?.let { hCm = it }
                        break
                    }
                }
            }
            val p = odfParser(zip, "content.xml") ?: throw AppException(R.string.odf_documents_no_es_una_presentacion_opendocument)
            val slides = ArrayList<Slide>()
            var elements = ArrayList<SlideElement>()
            val para = OdfParagraphBuilder(styles)
            var inPage = false; var inNotes = 0
            var x = 0L; var y = 0L; var w = 0L; var h = 0L; var cls: String? = null
            var paragraphs = ArrayList<OfficeParagraph>()
            var image: Bitmap? = null
            var frameDepth = 0; var inPara = false; var listDepth = 0
            val notes = StringBuilder()

            while (p.next() != XmlPullParser.END_DOCUMENT) {
                when (p.eventType) {
                    XmlPullParser.START_TAG -> when (p.name) {
                        "draw:page" -> { inPage = true; elements = ArrayList(); notes.setLength(0) }
                        "presentation:notes" -> inNotes++
                        "draw:frame", "draw:custom-shape", "draw:rect", "draw:ellipse", "draw:text-box" -> if (inPage && inNotes == 0 && p.name != "draw:text-box") {
                            if (frameDepth++ == 0) {
                                x = cmToEmu(parseLength(p.a("svg:x")) ?: 0.0); y = cmToEmu(parseLength(p.a("svg:y")) ?: 0.0)
                                w = cmToEmu(parseLength(p.a("svg:width")) ?: 0.0); h = cmToEmu(parseLength(p.a("svg:height")) ?: 0.0)
                                cls = p.a("presentation:class"); paragraphs = ArrayList(); image = null
                            }
                        }
                        "draw:image" -> if (inPage && inNotes == 0) image = zip.odfBitmap(p.a("xlink:href"))
                        "text:list" -> listDepth++
                        "text:p", "text:h" -> { inPara = true; para.start(p.a("text:style-name")) }
                        "text:span" -> para.pushSpan(p.a("text:style-name"))
                        "text:s" -> if (inPara) para.text(" ".repeat(p.a("text:c")?.toIntOrNull() ?: 1))
                        "text:line-break" -> if (inPara) para.text("\n")
                    }
                    XmlPullParser.TEXT -> if (inPara) {
                        if (inNotes > 0) notes.append(p.text) else para.text(p.text ?: "")
                    }
                    XmlPullParser.END_TAG -> when (p.name) {
                        "presentation:notes" -> inNotes--
                        "text:span" -> para.popSpan()
                        "text:list" -> listDepth--
                        "text:p", "text:h" -> {
                            inPara = false
                            if (inNotes > 0) notes.append('\n')
                            else if (frameDepth > 0) paragraphs += para.build(0, listDepth > 0 || cls == "outline", (listDepth - 1).coerceAtLeast(0))
                        }
                        "draw:frame", "draw:custom-shape", "draw:rect", "draw:ellipse" -> if (inPage && inNotes == 0 && frameDepth > 0) {
                            if (--frameDepth == 0) {
                                val img = image
                                if (img != null && paragraphs.none { it.plainText.isNotBlank() }) {
                                    elements += SlideElement.Picture(x, y, w, h, img)
                                } else if (paragraphs.any { it.plainText.isNotBlank() }) {
                                    val size = when (cls) { "title" -> 36f; "subtitle" -> 24f; else -> 18f }
                                    val ps = if (cls == "title") paragraphs.map { it.copy(runs = it.runs.map { r -> r.copy(bold = true) }) } else paragraphs
                                    elements += SlideElement.TextBox(x, y, w, h, ps, null, size)
                                }
                            }
                        }
                        "draw:page" -> { slides += Slide(elements, null, notes.toString().trim().ifBlank { null }); inPage = false }
                    }
                }
            }
            PptxDocument(cmToEmu(wCm), cmToEmu(hCm), slides)
        }
    }
}
