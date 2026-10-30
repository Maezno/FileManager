package com.maez.filemanager.services

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import com.maez.filemanager.model.DeviceSummary
import com.maez.filemanager.model.DuplicateFileEntry
import com.maez.filemanager.model.FileCategory
import com.maez.filemanager.model.MediaSearchItem
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlin.math.min

class SmartSearchEngine(
    private val context: Context,
    private val vfsManager: VfsManager
) {

    // Caché en memoria y disco para evitar re-analizar archivos sin cambios
    private val metadataCache = ConcurrentHashMap<String, MediaSearchItem>()

    private val majorBrandMap = mapOf(
        "msnv" to "Sony",
        "xavc" to "Sony",
        "caep" to "Canon",
        "cdes" to "Canon",
        "kddi" to "KDDI (AU)",
        "niko" to "Nikon"
    )

    private val editorRegex = Regex(
        "adobe|premiere|after effects|media encoder|davinci|resolve|final cut|handbrake|ffmpeg|lavf|lavc|vegas|capcut|inshot|kdenlive|shotcut|camtasia|filmora|kinemaster|clipchamp|movavi|openshot|wondershare|canva|\\bobs\\b",
        RegexOption.IGNORE_CASE
    )

    private val vivoExtRegex = Regex("vivoMediaExtInfo[^{]*(\\{.*?\\})")
    private val vivoModelRegex = Regex("\"com\\.android\\.camera\\.takenmodel\"\\s*:\\s*\"([^\"]+)\"")

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. BUSCADOR Y ANALIZADOR DE METADATOS, ORIENTACIÓN Y DISPOSITIVOS
    // (Port de VideoLibraryService.cs de MediaRecovery ampliado a todo tipo de archivos y Red)
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun analyzeFiles(
        files: List<VfsItem>,
        connectionMap: Map<String, NetworkConnection>,
        filterMonth: Int? = null, // 1..12
        filterYear: Int? = null,
        categoryFilter: FileCategory = FileCategory.ALL,
        minSizeBytes: Long = 0L,
        analyzeMediaHeaders: Boolean = true,
        onProgress: (Int, Int, String) -> Unit
    ): List<MediaSearchItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaSearchItem>()
        val cal = Calendar.getInstance()

        // Pre-filtrado rápido por categoría, fecha y tamaño mínimo
        val candidates = files.filter { item ->
            if (item.isDirectory) return@filter false
            if (item.sizeBytes < minSizeBytes) return@filter false
            if (categoryFilter != FileCategory.ALL && item.category != categoryFilter) return@filter false

            if (filterMonth != null || filterYear != null) {
                if (item.lastModified <= 0L) return@filter false
                cal.timeInMillis = item.lastModified
                val m = cal.get(Calendar.MONTH) + 1
                val y = cal.get(Calendar.YEAR)
                if (filterMonth != null && m != filterMonth) return@filter false
                if (filterYear != null && y != filterYear) return@filter false
            }
            true
        }

        val total = candidates.size
        candidates.forEachIndexed { idx, item ->
            coroutineContext.ensureActive()
            onProgress(idx + 1, total, item.name)

            val cacheKey = "${item.protocol}:${item.connectionId ?: ""}:${item.path}:${item.sizeBytes}:${item.lastModified}"
            val cached = metadataCache[cacheKey]
            if (cached != null) {
                results.add(cached)
                return@forEachIndexed
            }

            val analyzed = if (analyzeMediaHeaders && (item.category == FileCategory.VIDEO || item.category == FileCategory.IMAGE)) {
                val conn = item.connectionId?.let { connectionMap[it] }
                when (item.category) {
                    FileCategory.IMAGE -> analyzeImageItem(item, conn)
                    FileCategory.VIDEO -> analyzeVideoItem(item, conn)
                    else -> MediaSearchItem(item = item, deviceModel = item.category.label, orientation = "N/A")
                }
            } else {
                MediaSearchItem(
                    item = item,
                    deviceModel = item.category.label,
                    orientation = "N/A"
                )
            }

            metadataCache[cacheKey] = analyzed
            results.add(analyzed)
        }

        results.sortedByDescending { it.item.lastModified }
    }

    private suspend fun analyzeImageItem(item: VfsItem, conn: NetworkConnection?): MediaSearchItem {
        return try {
            val exif = if (item.protocol == StorageProtocol.LOCAL) {
                ExifInterface(item.path)
            } else {
                // Leer solo los primeros 64 KB por SMB/FTP para extraer EXIF sin descargar toda la foto
                val headerBytes = vfsManager.readBytesAt(item, 0L, 64 * 1024, conn)
                if (headerBytes.isEmpty()) return MediaSearchItem(item = item, deviceModel = "Imagen sin EXIF")
                ExifInterface(ByteArrayInputStream(headerBytes))
            }

            var width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            var height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            val orientationTag = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val rotation = when (orientationTag) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
                else -> 0
            }

            if (rotation == 90 || rotation == 270) {
                val tmp = width
                width = height
                height = tmp
            }

            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim().orEmpty()

            val deviceModel = when {
                model.isNotEmpty() && make.isNotEmpty() -> {
                    val combined = if (model.contains(make, ignoreCase = true)) model else "$make $model"
                    cleanModelString(combined)
                }
                model.isNotEmpty() -> cleanModelString(model)
                make.isNotEmpty() -> cleanModelString(make)
                software.isNotEmpty() && editorRegex.containsMatchIn(software) -> "[Editado] ${cleanModelString(software)}"
                item.name.contains("WA0", ignoreCase = true) || item.path.contains("WhatsApp", ignoreCase = true) -> "WhatsApp (Imagen)"
                else -> "Imagen / Sin cámara EXIF"
            }

            val orientation = when {
                width <= 0 || height <= 0 -> "Horizontal"
                height > width -> "Vertical"
                else -> "Horizontal"
            }

            val res = if (width > 0 && height > 0) "${width}×${height}" else ""
            MediaSearchItem(
                item = item,
                deviceModel = deviceModel,
                orientation = orientation,
                resolution = res,
                rotation = rotation
            )
        } catch (_: Exception) {
            MediaSearchItem(item = item, deviceModel = "Imagen", orientation = "Horizontal")
        }
    }

    private suspend fun analyzeVideoItem(item: VfsItem, conn: NetworkConnection?): MediaSearchItem {
        var width = 0
        var height = 0
        var rotation = 0
        var durationSec = 0.0

        // 1. Si es Local, MediaMetadataRetriever da resolución, rotación y duración instantáneamente
        if (item.protocol == StorageProtocol.LOCAL) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(item.path)
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                durationSec = durMs / 1000.0
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

        // 2. Leer cabecera (primeros 128 KB) y cola (últimos 128 KB) para detectar dispositivo, cámara y átomo tkhd
        val chunkSize = 128 * 1024
        val headBytes = vfsManager.readBytesAt(item, 0L, chunkSize, conn)
        val tailOffset = (item.sizeBytes - chunkSize).coerceAtLeast(0L)
        val tailBytes = if (tailOffset > 0L) vfsManager.readBytesAt(item, tailOffset, chunkSize, conn) else ByteArray(0)

        // Si es por red (SMB/FTP) o retriever no obtuvo dimensiones, parsear átomo 'tkhd' en head/tail
        if (width <= 0 || height <= 0) {
            val dims = parseMp4TkhdDimensions(headBytes) ?: parseMp4TkhdDimensions(tailBytes)
            if (dims != null) {
                width = dims.first
                height = dims.second
                rotation = dims.third
            }
        }

        if (rotation == 90 || rotation == 270) {
            val tmp = width
            width = height
            height = tmp
        }

        val orientation = if (height > width) "Vertical" else "Horizontal"
        val res = if (width > 0 && height > 0) "${width}×${height}" else ""

        val deviceModel = detectVideoDeviceFromBytes(item, headBytes, tailBytes)

        return MediaSearchItem(
            item = item,
            deviceModel = deviceModel,
            orientation = orientation,
            resolution = res,
            rotation = rotation,
            durationSeconds = durationSec
        )
    }

    /**
     * Port directo de DetectDeviceModel + TryExtractVivoModel de VideoLibraryService.cs
     * sobre los bloques de cabecera y cola del contenedor MP4/MOV/MKV.
     */
    private fun detectVideoDeviceFromBytes(item: VfsItem, head: ByteArray, tail: ByteArray): String {
        val headText = String(head, Charsets.ISO_8859_1)
        val tailText = String(tail, Charsets.ISO_8859_1)
        val combined = "$headText\n$tailText"

        // 1. Vivo (vivoMediaExtInfo JSON en los últimos 128 KB)
        val vivoMatch = vivoExtRegex.find(tailText) ?: vivoExtRegex.find(headText)
        if (vivoMatch != null) {
            val modelMatch = vivoModelRegex.find(vivoMatch.groupValues[1])
            if (modelMatch != null) {
                return cleanModelString(modelMatch.groupValues[1])
            }
        }

        // 2. Apple QuickTime (iPhone / iPad)
        val appleModel = extractPrintableAfterMarker(combined, "com.apple.quicktime.model")
        val appleMake = extractPrintableAfterMarker(combined, "com.apple.quicktime.make")
        if (appleModel.isNotEmpty() || appleMake.isNotEmpty()) {
            if (appleModel.contains("iPhone", ignoreCase = true) || appleModel.contains("iPad", ignoreCase = true)) {
                return cleanModelString(appleModel)
            }
            if (appleMake.contains("Apple", ignoreCase = true)) {
                return if (appleModel.isNotEmpty()) cleanModelString("Apple $appleModel") else "Apple iPhone / iPad"
            }
        }

        // 3. Android estándar (com.android.model / com.android.manufacturer)
        val androidModel = extractPrintableAfterMarker(combined, "com.android.model")
        val androidMake = extractPrintableAfterMarker(combined, "com.android.manufacturer")
        if (androidModel.isNotEmpty()) {
            val full = if (androidMake.isNotEmpty() && !androidModel.contains(androidMake, ignoreCase = true)) {
                "$androidMake $androidModel"
            } else {
                androidModel
            }
            return cleanModelString(full)
        }

        // 4. Samsung por utc_offset
        if (combined.contains("com.samsung.android.utc_offset")) {
            val ver = extractPrintableAfterMarker(combined, "com.android.version")
            return if (ver.isNotEmpty() && ver.length <= 6) "Samsung (Android $ver)" else "Samsung"
        }

        // 5. Marcas de cámaras por ftyp major_brand (Sony XAVC/MSNV, Canon, Nikon)
        if (head.size >= 12 && head[4] == 'f'.code.toByte() && head[5] == 't'.code.toByte() && head[6] == 'y'.code.toByte() && head[7] == 'p'.code.toByte()) {
            val brand = String(head, 8, 4, Charsets.US_ASCII).trim().lowercase(Locale.ROOT)
            majorBrandMap[brand]?.let { return it }
        }
        for ((brandKey, brandName) in majorBrandMap) {
            if (headText.take(64).contains(brandKey, ignoreCase = true)) {
                return brandName
            }
        }

        // 6. DJI
        val lowerName = item.name.lowercase(Locale.ROOT)
        if (lowerName.startsWith("dji_")) {
            return "DJI (Osmo / Pocket / Drone)"
        }

        // 7. WhatsApp
        if (lowerName.contains("wa00") || item.path.contains("whatsapp", ignoreCase = true)) {
            return "WhatsApp (Comprimido)"
        }

        // 8. Software de edición (Premiere, DaVinci, CapCut, OBS, Lavf/FFmpeg)
        val editorMatch = editorRegex.find(combined)
        if (editorMatch != null) {
            val found = editorMatch.value.uppercase(Locale.ROOT)
            val label = when {
                found.contains("PREMIERE") || found.contains("ADOBE") -> "Adobe Premiere"
                found.contains("DAVINCI") || found.contains("RESOLVE") -> "DaVinci Resolve"
                found.contains("CAPCUT") -> "CapCut"
                found.contains("INSHOT") -> "InShot"
                found.contains("OBS") -> "OBS Studio"
                found.contains("HANDBRAKE") -> "HandBrake"
                found.contains("LAVF") || found.contains("FFMPEG") -> "FFmpeg / Lavf"
                else -> editorMatch.value
            }
            return "[Editado] $label"
        }

        // 9. Android genérico
        val androidVer = extractPrintableAfterMarker(combined, "com.android.version")
        if (androidVer.isNotEmpty() && androidVer.first().isDigit()) {
            return "Android genérico (Android ${androidVer.take(4)})"
        }

        return "Desconocido"
    }

    private fun extractPrintableAfterMarker(content: String, marker: String): String {
        val idx = content.indexOf(marker, ignoreCase = true)
        if (idx < 0) return ""
        val windowStart = idx + marker.length
        val windowEnd = min(content.length, windowStart + 96)
        val slice = content.substring(windowStart, windowEnd)

        // Buscar secuencia de caracteres ASCII legibles (ej. tras cabecera 'data' en átomos udta/meta)
        val cleaned = slice
            .replace("data", " ")
            .map { ch -> if (ch.code in 32..122) ch else '\u0000' }
            .joinToString("")
            .split('\u0000')
            .map { it.trim() }
            .firstOrNull { it.length >= 2 && it.any { c -> c.isLetterOrDigit() } }
            ?: ""

        return cleaned
    }

    /**
     * Extrae ancho, alto y rotación directamente del átomo 'tkhd' (Track Header) de un MP4/MOV
     */
    private fun parseMp4TkhdDimensions(bytes: ByteArray): Triple<Int, Int, Int>? {
        if (bytes.size < 92) return null
        val target = byteArrayOf('t'.code.toByte(), 'k'.code.toByte(), 'h'.code.toByte(), 'd'.code.toByte())
        var i = 0
        val max = bytes.size - 88
        while (i <= max) {
            if (bytes[i] == target[0] && bytes[i + 1] == target[1] && bytes[i + 2] == target[2] && bytes[i + 3] == target[3]) {
                val version = bytes[i + 4].toInt() and 0xFF
                val matrixOffset = if (version == 1) i + 4 + 48 else i + 4 + 36
                val widthOffset = matrixOffset + 36
                if (widthOffset + 8 <= bytes.size) {
                    val buf = ByteBuffer.wrap(bytes)
                    val a = buf.getInt(matrixOffset)
                    val b = buf.getInt(matrixOffset + 4)
                    val c = buf.getInt(matrixOffset + 12)
                    val d = buf.getInt(matrixOffset + 16)
                    val wFixed = buf.getInt(widthOffset)
                    val hFixed = buf.getInt(widthOffset + 4)
                    val width = (wFixed ushr 16) and 0xFFFF
                    val height = (hFixed ushr 16) and 0xFFFF
                    if (width in 64..16384 && height in 64..16384) {
                        val rot = when {
                            b > 0 && c < 0 -> 90
                            b < 0 && c > 0 -> 270
                            a < 0 && d < 0 -> 180
                            else -> 0
                        }
                        return Triple(width, height, rot)
                    }
                }
            }
            i++
        }
        return null
    }

    private fun cleanModelString(text: String): String {
        val cleaned = text
            .replace("Google Android", "")
            .replace("google/", "")
            .replace("samsung/", "Samsung ")
            .replace("SAMSUNG", "Samsung")
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return if (cleaned.length > 60) cleaned.take(60) else cleaned
    }

    fun buildDeviceSummaries(items: List<MediaSearchItem>): List<DeviceSummary> {
        return items
            .groupBy { it.deviceModel }
            .map { (model, list) ->
                val v = list.count { it.orientation.equals("Vertical", ignoreCase = true) }
                val h = list.count { it.orientation.equals("Horizontal", ignoreCase = true) }
                val o = list.size - v - h
                DeviceSummary(
                    deviceModel = model,
                    verticalCount = v,
                    horizontalCount = h,
                    otherCount = o
                )
            }
            .sortedByDescending { it.totalCount }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. BUSCADOR DE DUPLICADOS UNIVERSAL (1 Carpeta o Comparación de 2 Carpetas A vs B)
    // Funciona con cualquier tipo de archivo y cruza Local <-> SMB <-> FTP
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun scanDuplicates(
        folderAFiles: List<VfsItem>,
        folderBFiles: List<VfsItem>? = null,
        connectionMap: Map<String, NetworkConnection>,
        onProgress: (Int, Int, String) -> Unit
    ): List<DuplicateFileEntry> = withContext(Dispatchers.IO) {
        val duplicates = mutableListOf<DuplicateFileEntry>()
        val twoFoldersMode = !folderBFiles.isNullOrEmpty()

        data class Candidate(val item: VfsItem, val isFolderB: Boolean)

        val allCandidates = mutableListOf<Candidate>()
        folderAFiles.filter { !it.isDirectory && it.sizeBytes > 0L }.forEach {
            allCandidates.add(Candidate(it, false))
        }
        if (twoFoldersMode) {
            folderBFiles.filter { !it.isDirectory && it.sizeBytes > 0L }.forEach {
                allCandidates.add(Candidate(it, true))
            }
        }

        // Fase 1: Agrupar por tamaño exacto en bytes
        val sizeGroups = allCandidates
            .groupBy { it.item.sizeBytes }
            .values
            .filter { group ->
                if (group.size <= 1) false
                else if (twoFoldersMode) group.any { !it.isFolderB } && group.any { it.isFolderB }
                else true
            }

        val totalToCheck = sizeGroups.sumOf { it.size }
        var processed = 0
        var groupId = 1

        for (group in sizeGroups) {
            coroutineContext.ensureActive()

            // Fase 2: Hash SHA-256 de triple muestra (64 KB inicio, 64 KB centro, 64 KB final)
            val hashGroups = linkedMapOf<String, MutableList<Candidate>>()

            for (cand in group) {
                coroutineContext.ensureActive()
                processed++
                onProgress(processed, totalToCheck, cand.item.name)

                val conn = cand.item.connectionId?.let { connectionMap[it] }
                val signature = computeTripleSampleSha256(cand.item, conn)
                if (signature.isNotEmpty()) {
                    hashGroups.getOrPut(signature) { mutableListOf() }.add(cand)
                }
            }

            for ((_, matches) in hashGroups) {
                if (matches.size <= 1) continue
                if (twoFoldersMode && (!matches.any { !it.isFolderB } || !matches.any { it.isFolderB })) {
                    continue
                }

                val sorted = matches.sortedWith(
                    compareBy<Candidate> { it.isFolderB }.thenBy { it.item.lastModified }
                )
                sorted.forEachIndexed { idx, match ->
                    duplicates.add(
                        DuplicateFileEntry(
                            groupId = groupId,
                            item = match.item,
                            isPrimary = (idx == 0),
                            sourceLabel = if (match.isFolderB) "Carpeta B" else "Carpeta A"
                        )
                    )
                }
                groupId++
            }
        }

        duplicates
    }

    private suspend fun computeTripleSampleSha256(item: VfsItem, conn: NetworkConnection?): String {
        return try {
            val size = item.sizeBytes
            val sampleSize = min(64 * 1024L, size).toInt()
            val digest = MessageDigest.getInstance("SHA-256")

            // 1. Muestra inicial
            val startBytes = vfsManager.readBytesAt(item, 0L, sampleSize, conn)
            if (startBytes.isEmpty()) return ""
            digest.update(startBytes)

            // 2. Muestra central
            if (size > sampleSize * 3L) {
                val midBytes = vfsManager.readBytesAt(item, size / 2L, sampleSize, conn)
                digest.update(midBytes)
            }

            // 3. Muestra final
            if (size > sampleSize * 2L) {
                val endBytes = vfsManager.readBytesAt(item, (size - sampleSize).coerceAtLeast(0L), sampleSize, conn)
                digest.update(endBytes)
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. EXPORTACIÓN A CSV (Compatible con Excel regional, delimitador ';')
    // ─────────────────────────────────────────────────────────────────────────────

    fun exportResultsToCsv(
        targetFile: File,
        items: List<MediaSearchItem>,
        summaries: List<DeviceSummary>,
        exportDetails: Boolean
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        targetFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            if (exportDetails) {
                writer.appendLine("Archivo;Carpeta;Protocolo;Categoría;Dispositivo;Orientación;Resolución;Duración (s);Tamaño (MB);Fecha")
                for (it in items) {
                    val mb = String.format(Locale.US, "%.2f", it.item.sizeBytes / (1024.0 * 1024.0))
                    val dur = String.format(Locale.US, "%.1f", it.durationSeconds)
                    val dateStr = if (it.item.lastModified > 0L) sdf.format(Date(it.item.lastModified)) else ""
                    writer.appendLine(
                        "${escapeCsv(it.item.name)};${escapeCsv(it.item.parentPath)};${it.item.protocol.name};${it.item.category.label};${escapeCsv(it.deviceModel)};${it.orientation};${it.resolution};$dur;$mb;$dateStr"
                    )
                }
            } else {
                writer.appendLine("Dispositivo / Categoría;Verticales;Horizontales;Otros;Total;% Vertical")
                for (s in summaries) {
                    writer.appendLine(
                        "${escapeCsv(s.deviceModel)};${s.verticalCount};${s.horizontalCount};${s.otherCount};${s.totalCount};${s.pctVertical}"
                    )
                }
            }
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.isEmpty()) return ""
        return if (value.contains(';') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
