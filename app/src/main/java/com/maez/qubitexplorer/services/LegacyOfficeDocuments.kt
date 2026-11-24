package com.maez.qubitexplorer.services

import com.maez.qubitexplorer.R
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Lectores de formatos binarios de Office 97-2003 (DOC, XLS, PPT).
 * Todos viven dentro de un contenedor OLE2 / Compound File Binary. Se extrae el contenido
 * (texto, celdas, diapositivas) para la vista previa; el formato visual es básico.
 */

// ── Contenedor OLE2 (Compound File Binary) ──────────────────────────────────────────

class Ole2File(private val data: ByteArray) {
    private val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    private val sectorSize: Int
    private val miniSectorSize: Int
    private val miniCutoff: Int
    private val fat: IntArray
    private val miniFat: IntArray
    private val miniStream: ByteArray
    val entries: List<Entry>

    data class Entry(val name: String, val type: Int, val start: Int, val size: Long)

    init {
        if (!(data.size >= 512 && bb.getLong(0) == -0x1ee54e5e1fee3030L)) throw AppException(R.string.legacy_office_documents_no_es_un_archivo_ole2)
        sectorSize = 1 shl (bb.getShort(30).toInt() and 0xFFFF)
        miniSectorSize = 1 shl (bb.getShort(32).toInt() and 0xFFFF)
        val numFat = bb.getInt(44)
        val firstDir = bb.getInt(48)
        miniCutoff = bb.getInt(56)
        val firstMiniFat = bb.getInt(60)
        var difatSector = bb.getInt(68)

        // Sectores de la FAT: 109 en el encabezado + cadena DIFAT
        val fatSectors = ArrayList<Int>()
        for (i in 0 until 109) { val s = bb.getInt(76 + i * 4); if (s >= 0) fatSectors += s }
        var guard = 0
        while (difatSector >= 0 && fatSectors.size < numFat && guard++ < 10000) {
            val off = sectorOffset(difatSector)
            for (i in 0 until sectorSize / 4 - 1) { val s = bb.getInt(off + i * 4); if (s >= 0) fatSectors += s }
            difatSector = bb.getInt(off + sectorSize - 4)
        }
        val perSector = sectorSize / 4
        fat = IntArray(fatSectors.size * perSector)
        fatSectors.forEachIndexed { i, s ->
            val off = sectorOffset(s)
            for (j in 0 until perSector) if (off + j * 4 + 4 <= data.size) fat[i * perSector + j] = bb.getInt(off + j * 4)
        }

        val dirBytes = readChain(firstDir, Long.MAX_VALUE)
        val list = ArrayList<Entry>()
        val dbb = ByteBuffer.wrap(dirBytes).order(ByteOrder.LITTLE_ENDIAN)
        var o = 0
        while (o + 128 <= dirBytes.size) {
            val nameLen = (dbb.getShort(o + 64).toInt() and 0xFFFF).coerceIn(0, 64)
            val name = String(dirBytes, o, (nameLen - 2).coerceAtLeast(0), Charsets.UTF_16LE)
            val type = dirBytes[o + 66].toInt()
            val start = dbb.getInt(o + 116)
            val size = dbb.getInt(o + 120).toLong() and 0xFFFFFFFFL
            if (type != 0) list += Entry(name, type, start, size)
            o += 128
        }
        entries = list
        val root = list.firstOrNull { it.type == 5 }
        miniStream = if (root != null && root.start >= 0) readChain(root.start, root.size) else ByteArray(0)
        val miniFatBytes = if (firstMiniFat >= 0) readChain(firstMiniFat, Long.MAX_VALUE) else ByteArray(0)
        val mbb = ByteBuffer.wrap(miniFatBytes).order(ByteOrder.LITTLE_ENDIAN)
        miniFat = IntArray(miniFatBytes.size / 4) { mbb.getInt(it * 4) }
    }

    private fun sectorOffset(s: Int) = (s + 1) * sectorSize

    private fun readChain(start: Int, size: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var s = start; var guard = 0
        while (s >= 0 && s < fat.size && guard++ < 2_000_000 && out.size() < size) {
            val off = sectorOffset(s)
            if (off >= data.size) break
            out.write(data, off, minOf(sectorSize, data.size - off))
            s = fat[s]
        }
        val bytes = out.toByteArray()
        return if (size < bytes.size) bytes.copyOf(size.toInt()) else bytes
    }

    private fun readMiniChain(start: Int, size: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var s = start; var guard = 0
        while (s >= 0 && s < miniFat.size && guard++ < 1_000_000 && out.size() < size) {
            val off = s * miniSectorSize
            if (off >= miniStream.size) break
            out.write(miniStream, off, minOf(miniSectorSize, miniStream.size - off))
            s = miniFat[s]
        }
        return out.toByteArray().copyOf(size.toInt().coerceAtMost(out.size()))
    }

    fun stream(name: String): ByteArray? {
        val e = entries.firstOrNull { it.type == 2 && it.name.equals(name, ignoreCase = true) } ?: return null
        return if (e.size < miniCutoff) readMiniChain(e.start, e.size) else readChain(e.start, e.size)
    }

    companion object {
        fun open(file: File): Ole2File {
            if (!(file.length() < 64L * 1024 * 1024)) throw AppException(R.string.legacy_office_documents_archivo_demasiado_grande_para_la)
            return Ole2File(file.readBytes())
        }
    }
}

private fun ByteArray.u16(o: Int): Int = (this[o].toInt() and 0xFF) or ((this[o + 1].toInt() and 0xFF) shl 8)
private fun ByteArray.u32(o: Int): Long =
    (u16(o).toLong()) or (u16(o + 2).toLong() shl 16)
private fun ByteArray.i32(o: Int): Int = u32(o).toInt()
private fun ByteArray.f64(o: Int): Double = ByteBuffer.wrap(this, o, 8).order(ByteOrder.LITTLE_ENDIAN).double

private val CP1252: Charset = runCatching { Charset.forName("windows-1252") }.getOrDefault(Charsets.ISO_8859_1)

// ── DOC (Word 97-2003) ──────────────────────────────────────────────────────────────

object DocReader {
    suspend fun read(file: File): DocxDocument = withContext(Dispatchers.IO) {
        val ole = Ole2File.open(file)
        val word = ole.stream("WordDocument") ?: throw AppException(R.string.legacy_office_documents_no_es_un_documento_de)
        if (!(word.u16(0) == 0xA5EC)) throw AppException(R.string.legacy_office_documents_formato_de_word_no_soportado)
        val flags = word.u16(0x0A)
        if (flags and 0x0100 != 0) throw AppException(R.string.legacy_office_documents_el_documento_esta_protegido_con, passwordProtected = true)
        val table = ole.stream(if (flags and 0x0200 != 0) "1Table" else "0Table")
            ?: throw AppException(R.string.legacy_office_documents_falta_la_tabla_del_documento)
        // FibRgLw97: ccpText, ccpFtn, ccpHdd, ccpMcr, ccpAtn, ccpEdn, ccpTxbx, ccpHdrTxbx (a partir de 0x4C)
        val ccp = IntArray(8) { word.i32(0x4C + it * 4).coerceAtLeast(0) }
        val fcClx = word.i32(0x01A2)
        val lcbClx = word.i32(0x01A6)
        val all = readPieces(word, table, fcClx, lcbClx)
        fun range(start: Int, len: Int) = if (start >= all.length || len <= 0) "" else all.substring(start, minOf(all.length, start + len))

        val body = range(0, ccp[0])
        // Los cuadros de texto van después de cuerpo, notas, encabezados, macros, comentarios y notas finales
        val txbxStart = ccp[0] + ccp[1] + ccp[2] + ccp[3] + ccp[4] + ccp[5]
        val textBoxes = range(txbxStart, ccp[6])
        val images = ArrayList<android.graphics.Bitmap>()
        listOfNotNull(ole.stream("Data"), table, word).forEach { images += findImages(it, 30 - images.size) }

        val imageQueue = ArrayDeque(images)
        val blocks = ArrayList<DocBlock>()
        blocks += buildBlocks(stripFields(body), imageQueue)
        if (textBoxes.isNotBlank()) blocks += buildBlocks(stripFields(textBoxes), imageQueue)
        // Imágenes sin marca en el texto (por ejemplo, flotantes): se agregan al principio
        while (imageQueue.isNotEmpty()) blocks.add(0, DocBlock.Image(imageQueue.removeLast()))
        DocxDocument(markTitle(dropEmptyRuns(blocks)))
    }

    private fun readPieces(word: ByteArray, table: ByteArray, fcClx: Int, lcbClx: Int): String {
        var i = fcClx
        val end = (fcClx + lcbClx).coerceAtMost(table.size)
        // Saltar Prc (propiedades) hasta el Pcdt
        while (i < end && table[i].toInt() == 0x01) i += 3 + table.u16(i + 1)
        if (!(i < end && table[i].toInt() == 0x02)) throw AppException(R.string.legacy_office_documents_tabla_de_piezas_no_encontrada)
        val lcb = table.i32(i + 1)
        val plc = i + 5
        val n = (lcb - 4) / 12
        val sb = StringBuilder()
        for (k in 0 until n) {
            val cpStart = table.i32(plc + k * 4)
            val cpEnd = table.i32(plc + (k + 1) * 4)
            val pcd = plc + (n + 1) * 4 + k * 8
            val fcRaw = table.i32(pcd + 2)
            val compressed = fcRaw and 0x40000000 != 0
            val count = cpEnd - cpStart
            if (count <= 0) continue
            if (compressed) {
                val fc = (fcRaw and 0x3FFFFFFF) / 2
                if (fc + count <= word.size) sb.append(String(word, fc, count, CP1252))
            } else {
                val fc = fcRaw
                if (fc + count * 2 <= word.size) sb.append(String(word, fc, count * 2, Charsets.UTF_16LE))
            }
        }
        return sb.toString()
    }

    /** Campos de Word: 0x13 código 0x14 resultado 0x15 → se conserva solo el resultado. */
    private fun stripFields(t: String): String {
        val sb = StringBuilder()
        var depth = 0; val inCode = ArrayList<Boolean>()
        for (c in t) {
            when (c) {
                '\u0013' -> { depth++; inCode += true }
                '\u0014' -> if (inCode.isNotEmpty()) inCode[inCode.size - 1] = false
                '\u0015' -> { if (depth > 0) { depth--; inCode.removeAt(inCode.size - 1) } }
                else -> if (inCode.none { it }) sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Busca imágenes JPEG/PNG incrustadas en un stream (las fotos de los .doc viven en el stream "Data"). */
    private fun findImages(bytes: ByteArray, max: Int): List<android.graphics.Bitmap> {
        val out = ArrayList<android.graphics.Bitmap>()
        var i = 0
        while (i < bytes.size - 8 && out.size < max) {
            val jpeg = bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD8.toByte() && bytes[i + 2] == 0xFF.toByte()
            val png = bytes[i] == 0x89.toByte() && bytes[i + 1] == 'P'.code.toByte() && bytes[i + 2] == 'N'.code.toByte() && bytes[i + 3] == 'G'.code.toByte()
            if (jpeg || png) {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, i, bytes.size - i, bounds)
                if (bounds.outWidth >= 16 && bounds.outHeight >= 16) {
                    var sample = 1
                    while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2
                    android.graphics.BitmapFactory.decodeByteArray(bytes, i, bytes.size - i,
                        android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })?.let { out += it }
                    i += 64 // evitar volver a encontrar la misma imagen
                    continue
                }
            }
            i++
        }
        return out
    }

    private fun dropEmptyRuns(blocks: List<DocBlock>): List<DocBlock> =
        blocks.filterIndexed { i, b ->
            !(b is DocBlock.Paragraph && b.p.plainText.isBlank() && i > 0 &&
                (blocks[i - 1] as? DocBlock.Paragraph)?.p?.plainText?.isBlank() == true)
        }

    /** Heurística de título: primer párrafo corto. */
    private fun markTitle(blocks: List<DocBlock>): List<DocBlock> {
        val out = blocks.toMutableList()
        val first = out.indexOfFirst { it is DocBlock.Paragraph && it.p.plainText.isNotBlank() }
        if (first >= 0) {
            val p = (out[first] as DocBlock.Paragraph).p
            if (p.plainText.trim().length in 1..80) out[first] = DocBlock.Paragraph(p.copy(headingLevel = 1))
        }
        return out
    }

    private fun buildBlocks(text: String, images: ArrayDeque<android.graphics.Bitmap>): List<DocBlock> {
        val blocks = ArrayList<DocBlock>()
        val cur = StringBuilder()
        var cells = ArrayList<String>()
        var rows = ArrayList<List<String>>()
        var lastCellEnd = false

        fun flushTable() {
            if (cells.isNotEmpty()) { rows += cells; cells = ArrayList() }
            if (rows.isNotEmpty()) { blocks += DocBlock.Table(rows); rows = ArrayList() }
        }
        fun paragraph(s: String) {
            val clean = s.replace('\u000B', '\n').replace('\u000C', '\n').replace(Regex("[\\x00-\\x08\\x0E-\\x1F]"), "")
            blocks += DocBlock.Paragraph(OfficeParagraph(listOf(OfficeRun(clean))))
        }

        for (c in text) {
            when (c) {
                '\u0007' -> {
                    if (cur.isEmpty() && lastCellEnd) { rows += cells; cells = ArrayList() } // fin de fila
                    else { cells += cur.toString().trim(); cur.setLength(0) }
                    lastCellEnd = true
                }
                '\u0001', '\u0008' -> {
                    // Imagen en línea (0x01) u objeto de dibujo (0x08)
                    images.removeFirstOrNull()?.let { img ->
                        if (cur.isNotBlank() && cells.isEmpty()) { paragraph(cur.toString()); cur.setLength(0) }
                        if (cells.isEmpty()) blocks += DocBlock.Image(img) else images.addFirst(img)
                    }
                }
                '\r' -> {
                    lastCellEnd = false
                    if (cells.isNotEmpty()) cur.append('\n') // párrafo dentro de una celda
                    else { flushTable(); paragraph(cur.toString()); cur.setLength(0) }
                }
                else -> { lastCellEnd = false; cur.append(c) }
            }
        }
        flushTable()
        if (cur.isNotBlank()) paragraph(cur.toString())
        return blocks
    }
}

// ── XLS (Excel 97-2003, BIFF8) ──────────────────────────────────────────────────────

object XlsReader {
    private const val BOF = 0x0809; private const val EOF = 0x000A
    private const val BOUNDSHEET = 0x0085; private const val SST = 0x00FC; private const val CONTINUE = 0x003C
    private const val LABELSST = 0x00FD; private const val NUMBER = 0x0203; private const val RK = 0x027E
    private const val MULRK = 0x00BD; private const val LABEL = 0x0204; private const val FORMULA = 0x0006
    private const val STRING = 0x0207; private const val BOOLERR = 0x0205; private const val FILEPASS = 0x002F

    private class Rec(val type: Int, val offset: Int, val data: ByteArray)

    suspend fun read(file: File, maxRows: Int = 5000, maxCols: Int = 200): XlsxDocument = withContext(Dispatchers.IO) {
        val ole = Ole2File.open(file)
        val wb = ole.stream("Workbook") ?: ole.stream("Book")
            ?: throw AppException(R.string.legacy_office_documents_no_es_un_libro_de)
        val recs = ArrayList<Rec>()
        var p = 0
        while (p + 4 <= wb.size) {
            val type = wb.u16(p); val len = wb.u16(p + 2)
            if (p + 4 + len > wb.size) break
            recs += Rec(type, p, wb.copyOfRange(p + 4, p + 4 + len))
            p += 4 + len
        }
        if (recs.any { it.type == FILEPASS }) throw AppException(R.string.legacy_office_documents_el_libro_esta_protegido_con, passwordProtected = true)

        val sheetNames = LinkedHashMap<Int, String>()
        var sst: List<String> = emptyList()
        var i = 0
        while (i < recs.size) {
            val r = recs[i]
            when (r.type) {
                BOUNDSHEET -> {
                    val pos = r.data.i32(0)
                    val sheetType = r.data[5].toInt()
                    val cch = r.data[6].toInt() and 0xFF
                    val high = r.data[7].toInt() and 1
                    val name = if (high == 1) String(r.data, 8, cch * 2, Charsets.UTF_16LE) else String(r.data, 8, cch, Charsets.ISO_8859_1)
                    if (sheetType == 0) sheetNames[pos] = name
                }
                SST -> {
                    val chunks = arrayListOf(r.data)
                    var j = i + 1
                    while (j < recs.size && recs[j].type == CONTINUE) { chunks += recs[j].data; j++ }
                    sst = readSst(chunks)
                }
            }
            if (r.type == EOF) break
            i++
        }

        val sheets = ArrayList<SheetData>()
        var cur: HashMap<Int, HashMap<Int, String>>? = null
        var name = ""; var maxR = 0; var maxC = 0; var truncated = false
        var pendingFormula: Pair<Int, Int>? = null

        fun put(r: Int, c: Int, v: String) {
            val map = cur ?: return
            if (v.isEmpty()) return
            if (r >= maxRows || c >= maxCols) { truncated = true; return }
            map.getOrPut(r) { HashMap() }[c] = v
            if (r + 1 > maxR) maxR = r + 1
            if (c + 1 > maxC) maxC = c + 1
        }

        for (r in recs) {
            val d = r.data
            when (r.type) {
                BOF -> {
                    val sheetName = sheetNames[r.offset]
                    if (sheetName != null) { cur = HashMap(); name = sheetName; maxR = 0; maxC = 0; truncated = false }
                }
                EOF -> cur?.let { sheets += SheetData(name, it, maxR, maxC, truncated); cur = null }
                LABELSST -> put(d.u16(0), d.u16(2), sst.getOrElse(d.i32(6)) { "" })
                NUMBER -> put(d.u16(0), d.u16(2), fmt(d.f64(6)))
                RK -> put(d.u16(0), d.u16(2), fmt(rk(d.i32(6))))
                MULRK -> {
                    val row = d.u16(0); val first = d.u16(2)
                    val n = (d.size - 6) / 6
                    for (k in 0 until n) put(row, first + k, fmt(rk(d.i32(4 + k * 6 + 2))))
                }
                LABEL -> put(d.u16(0), d.u16(2), xlString(d, 6))
                BOOLERR -> put(d.u16(0), d.u16(2), if (d[7].toInt() == 0) (if (d[6].toInt() != 0) "VERDADERO" else "FALSO") else "#ERROR")
                FORMULA -> {
                    val row = d.u16(0); val col = d.u16(2)
                    if (d.u16(12) == 0xFFFF) {
                        when (d[6].toInt()) {
                            0 -> pendingFormula = row to col // el texto viene en el registro STRING siguiente
                            1 -> put(row, col, if (d[8].toInt() != 0) "VERDADERO" else "FALSO")
                            2 -> put(row, col, "#ERROR")
                        }
                    } else put(row, col, fmt(d.f64(6)))
                }
                STRING -> pendingFormula?.let { (row, col) -> put(row, col, xlString(d, 0)); pendingFormula = null }
            }
        }
        XlsxDocument(sheets)
    }

    private fun rk(v: Int): Double {
        val d = if (v and 0x02 != 0) (v shr 2).toDouble()
        else java.lang.Double.longBitsToDouble((v.toLong() and 0xFFFFFFFCL) shl 32)
        return if (v and 0x01 != 0) d / 100.0 else d
    }

    private fun fmt(d: Double): String =
        if (d == Math.floor(d) && kotlin.math.abs(d) < 1e15) d.toLong().toString()
        else "%.10g".format(java.util.Locale.US, d).trimEnd('0').trimEnd('.')

    private fun xlString(d: ByteArray, o: Int): String {
        val cch = d.u16(o); val flags = d[o + 2].toInt()
        var p = o + 3
        if (flags and 0x08 != 0) p += 2
        if (flags and 0x04 != 0) p += 4
        return if (flags and 0x01 != 0) String(d, p, (cch * 2).coerceAtMost(d.size - p), Charsets.UTF_16LE)
        else String(d, p, cch.coerceAtMost(d.size - p), Charsets.ISO_8859_1)
    }

    /** SST con registros CONTINUE: un texto puede partirse entre registros y cambiar de codificación. */
    private fun readSst(chunks: List<ByteArray>): List<String> {
        var ci = 0; var pos = 8
        fun ensure() { while (ci < chunks.size && pos >= chunks[ci].size) { ci++; pos = 0 } }
        fun u8(): Int { ensure(); return if (ci < chunks.size) chunks[ci][pos++].toInt() and 0xFF else 0 }
        fun u16(): Int = u8() or (u8() shl 8)
        fun u32(): Int = u16() or (u16() shl 16)
        fun skip(n: Int) { var left = n; while (left > 0 && ci < chunks.size) { ensure(); if (ci >= chunks.size) break; val take = minOf(left, chunks[ci].size - pos); pos += take; left -= take } }

        val unique = if (chunks.isNotEmpty() && chunks[0].size >= 8) chunks[0].i32(4) else 0
        val out = ArrayList<String>(unique.coerceIn(0, 200_000))
        repeat(unique.coerceIn(0, 2_000_000)) {
            ensure(); if (ci >= chunks.size) return out
            val cch = u16(); val flags = u8()
            var high = flags and 0x01 != 0
            val runs = if (flags and 0x08 != 0) u16() else 0
            val ext = if (flags and 0x04 != 0) u32() else 0
            val sb = StringBuilder(cch)
            var left = cch
            while (left > 0 && ci < chunks.size) {
                if (pos >= chunks[ci].size) {
                    ci++; pos = 0
                    if (ci >= chunks.size) break
                    high = (chunks[ci][pos++].toInt() and 0x01) != 0 // byte de opciones al continuar
                }
                val chunk = chunks[ci]
                if (high) {
                    val n = minOf(left, (chunk.size - pos) / 2)
                    sb.append(String(chunk, pos, n * 2, Charsets.UTF_16LE)); pos += n * 2; left -= n
                    if (n == 0) { pos = chunk.size }
                } else {
                    val n = minOf(left, chunk.size - pos)
                    sb.append(String(chunk, pos, n, Charsets.ISO_8859_1)); pos += n; left -= n
                }
            }
            skip(runs * 4 + ext)
            out += sb.toString()
        }
        return out
    }
}

// ── PPT (PowerPoint 97-2003) ────────────────────────────────────────────────────────

object PptReader {
    private const val SLIDE_LIST_WITH_TEXT = 0x0FF0
    private const val SLIDE_PERSIST_ATOM = 0x03F3
    private const val TEXT_HEADER_ATOM = 0x0F9F
    private const val TEXT_CHARS_ATOM = 0x0FA0
    private const val TEXT_BYTES_ATOM = 0x0FA8
    private const val DOCUMENT_ATOM = 0x03E9
    private const val SLIDE_CONTAINER = 0x03EE

    private class TextItem(val type: Int, val text: String)

    suspend fun read(file: File): PptxDocument = withContext(Dispatchers.IO) {
        val ole = Ole2File.open(file)
        val s = ole.stream("PowerPoint Document") ?: throw AppException(R.string.legacy_office_documents_no_es_una_presentacion_de)
        if (ole.entries.any { it.name.equals("EncryptedSummary", true) }) throw AppException(R.string.legacy_office_documents_la_presentacion_esta_protegida_con, passwordProtected = true)

        // Tamaño de diapositiva (unidades maestras: 576 por pulgada → EMU = x * 1587.5)
        var cx = 9_144_000L; var cy = 6_858_000L
        val slides = ArrayList<MutableList<TextItem>>()
        val freeText = ArrayList<MutableList<TextItem>>() // texto guardado dentro de cada diapositiva

        fun walk(start: Int, end: Int, inSlideList: Boolean, slideContainerIdx: Int) {
            var p = start
            var lastType = 1
            while (p + 8 <= end) {
                val verInst = s.u16(p); val type = s.u16(p + 2); val len = s.i32(p + 4)
                val body = p + 8
                if (len < 0 || body + len > end) break
                val isContainer = (verInst and 0x0F) == 0x0F
                when {
                    type == DOCUMENT_ATOM && len >= 8 -> {
                        cx = (s.i32(body) * 1587.5).toLong().takeIf { it > 0 } ?: cx
                        cy = (s.i32(body + 4) * 1587.5).toLong().takeIf { it > 0 } ?: cy
                    }
                    type == SLIDE_LIST_WITH_TEXT && (verInst shr 4) == 0 -> walk(body, body + len, true, -1)
                    type == SLIDE_CONTAINER -> {
                        freeText += mutableListOf<TextItem>()
                        walk(body, body + len, false, freeText.size - 1)
                    }
                    inSlideList && type == SLIDE_PERSIST_ATOM -> slides += mutableListOf<TextItem>()
                    type == TEXT_HEADER_ATOM -> lastType = s.i32(body)
                    type == TEXT_CHARS_ATOM || type == TEXT_BYTES_ATOM -> {
                        val text = if (type == TEXT_CHARS_ATOM) String(s, body, len, Charsets.UTF_16LE) else String(s, body, len, CP1252)
                        if (inSlideList) slides.lastOrNull()?.add(TextItem(lastType, text))
                        else if (slideContainerIdx >= 0) freeText[slideContainerIdx] += TextItem(lastType, text)
                    }
                    isContainer -> walk(body, body + len, inSlideList, slideContainerIdx)
                }
                p = body + len
            }
        }
        walk(0, s.size, false, -1)

        val count = maxOf(slides.size, freeText.size)
        val result = (0 until count).map { idx ->
            // Texto de la lista de diapositivas + texto dentro de la diapositiva (sin duplicar)
            val listed = slides.getOrNull(idx).orEmpty()
            val inside = freeText.getOrNull(idx).orEmpty().filter { f -> listed.none { it.text == f.text } }
            val items = listed + inside
            val elements = ArrayList<SlideElement>()
            val title = items.filter { it.type == 0 || it.type == 6 }.joinToString("\n") { it.text }
            val body = items.filter { it.type != 0 && it.type != 6 }.map { it.text }
            if (title.isNotBlank()) {
                elements += SlideElement.TextBox(
                    cx / 20, cy / 20, cx * 9 / 10, cy / 5,
                    title.split('\r', '\u000B').map { OfficeParagraph(listOf(OfficeRun(it, bold = true)), align = OfficeAlign.CENTER) },
                    null, 32f
                )
            }
            val bodyParas = body.flatMap { t -> t.split('\r').filter { it.isNotBlank() } }
                .map { OfficeParagraph(listOf(OfficeRun(it.replace('\u000B', '\n'))), bullet = true) }
            if (bodyParas.isNotEmpty()) {
                elements += SlideElement.TextBox(cx / 20, if (title.isBlank()) cy / 10 else cy * 3 / 10, cx * 9 / 10, cy * 3 / 5, bodyParas, null, 20f)
            }
            Slide(elements, null, null)
        }
        PptxDocument(cx, cy, result)
    }
}
