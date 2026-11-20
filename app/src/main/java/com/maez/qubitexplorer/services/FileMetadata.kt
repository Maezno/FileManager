package com.maez.qubitexplorer.services

import android.media.MediaMetadataRetriever
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.maez.qubitexplorer.model.FileCategory
import com.maez.qubitexplorer.model.NetworkConnection
import com.maez.qubitexplorer.model.StorageProtocol
import com.maez.qubitexplorer.model.VfsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Qué hacer con los metadatos al copiar o mover archivos. */
enum class ExifCopyMode(val label: String) {
    KEEP_ALL("Conservar todo"),
    STRIP_GPS("Quitar ubicación GPS"),
    STRIP_ALL("Quitar todos los EXIF")
}

data class MetadataCopyOptions(
    val preserveDates: Boolean = true,
    val exifMode: ExifCopyMode = ExifCopyMode.KEEP_ALL
)

data class MetaEntry(val label: String, val value: String)
data class MetaSection(val title: String, val entries: List<MetaEntry>)
data class GpsLocation(val latitude: Double, val longitude: Double, val altitude: Double?)

data class FileMetadataResult(
    val sections: List<MetaSection>,
    val gps: GpsLocation?
) {
    fun asPlainText(): String = buildString {
        sections.forEach { section ->
            appendLine("[${section.title}]")
            section.entries.forEach { appendLine("${it.label}: ${it.value}") }
            appendLine()
        }
    }.trim()
}

object FileMetadataReader {

    /** Tags EXIF que se borran al aplicar ExifCopyMode.STRIP_GPS. */
    private val GPS_TAGS = listOf(
        ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD, ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF, ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF, ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_VERSION_ID, ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_SATELLITES, ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_MEASURE_MODE, ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF, ExifInterface.TAG_GPS_AREA_INFORMATION
    )

    /** Tags que se borran al aplicar STRIP_ALL (se conserva la orientación para no girar la foto). */
    private val PRIVATE_TAGS = GPS_TAGS + listOf(
        ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT, ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME, ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED, ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_IMAGE_DESCRIPTION, ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_CAMERA_OWNER_NAME, ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SERIAL_NUMBER, ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_MAKER_NOTE
    )

    /** Aplica las opciones de copia sobre un archivo local recién escrito. */
    fun applyCopyOptions(target: File, source: VfsItem, options: MetadataCopyOptions) {
        if (options.exifMode != ExifCopyMode.KEEP_ALL && supportsExifWrite(target.extension)) {
            try {
                val exif = ExifInterface(target.absolutePath)
                val tags = if (options.exifMode == ExifCopyMode.STRIP_GPS) GPS_TAGS else PRIVATE_TAGS
                tags.forEach { exif.setAttribute(it, null) }
                exif.saveAttributes()
            } catch (_: Exception) { }
        }
        // Va al final: saveAttributes() reescribe el archivo y cambiaría la fecha.
        if (options.preserveDates && source.lastModified > 0) {
            target.setLastModified(source.lastModified)
        }
    }

    private fun supportsExifWrite(ext: String) =
        ext.lowercase(Locale.ROOT) in setOf("jpg", "jpeg", "png", "webp", "dng")

    private fun supportsExifRead(ext: String) =
        ext.lowercase(Locale.ROOT) in setOf(
            "jpg", "jpeg", "png", "webp", "heic", "heif", "avif", "dng",
            "cr2", "nef", "nrw", "arw", "rw2", "orf", "pef", "srw", "raf"
        )

    suspend fun read(
        item: VfsItem,
        vfsManager: VfsManager?,
        connection: NetworkConnection?,
        cacheDir: File
    ): FileMetadataResult = withContext(Dispatchers.IO) {
        val sections = mutableListOf<MetaSection>()
        var gps: GpsLocation? = null
        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        // --- General ---
        val general = mutableListOf(
            MetaEntry("Nombre", item.name),
            MetaEntry("Ruta", item.path),
            MetaEntry("Carpeta contenedora", item.parentPath),
            MetaEntry("Protocolo", item.protocol.name),
            MetaEntry(
                "Tipo",
                if (item.isDirectory) "Carpeta" else "${item.extension.uppercase()} (${item.category.name})"
            )
        )
        if (!item.isDirectory) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.extension)
            general += MetaEntry("Tipo MIME", mime ?: "desconocido")
            general += MetaEntry("Tamaño", "${item.sizeFormatted} (${"%,d".format(item.sizeBytes)} bytes)")
        }
        general += MetaEntry(
            "Modificado",
            if (item.lastModified > 0) dateFmt.format(Date(item.lastModified)) else "Desconocido"
        )
        sections += MetaSection("General", general)

        // --- Sistema de archivos (solo local) ---
        val localFile: File? = if (item.protocol == StorageProtocol.LOCAL) File(item.path) else null
        if (localFile != null && localFile.exists()) {
            val fs = mutableListOf<MetaEntry>()
            fs += MetaEntry("Ruta canónica", runCatching { localFile.canonicalPath }.getOrDefault(item.path))
            fs += MetaEntry(
                "Permisos (app)",
                buildString {
                    append(if (localFile.canRead()) "r" else "-")
                    append(if (localFile.canWrite()) "w" else "-")
                    append(if (localFile.canExecute()) "x" else "-")
                }
            )
            fs += MetaEntry("Oculto", if (localFile.isHidden) "Sí" else "No")
            runCatching {
                val st = android.system.Os.stat(localFile.absolutePath)
                fs += MetaEntry("Modo Unix", "%o".format(st.st_mode and 0x1FF))
                fs += MetaEntry("Inodo", st.st_ino.toString())
                fs += MetaEntry("UID / GID", "${st.st_uid} / ${st.st_gid}")
                fs += MetaEntry("Último acceso", dateFmt.format(Date(st.st_atime * 1000)))
                fs += MetaEntry("Cambio de estado", dateFmt.format(Date(st.st_ctime * 1000)))
            }
            if (item.isDirectory) {
                val children = localFile.listFiles()
                if (children != null) {
                    fs += MetaEntry(
                        "Contenido",
                        "${children.count { it.isDirectory }} carpetas, ${children.count { it.isFile }} archivos"
                    )
                }
            }
            sections += MetaSection("Sistema de archivos", fs)
        }

        if (item.isDirectory) return@withContext FileMetadataResult(sections, null)

        // Archivo local para analizar (descarga temporal si es remoto y razonablemente chico)
        val needsMedia = item.category == FileCategory.IMAGE || item.category == FileCategory.VIDEO ||
            item.category == FileCategory.AUDIO || supportsExifRead(item.extension)
        var tempFile: File? = null
        val analysisFile: File? = when {
            localFile != null -> localFile
            needsMedia && vfsManager != null && item.sizeBytes in 1..(60L * 1024 * 1024) -> {
                val tmp = File(cacheDir, "meta_${System.nanoTime()}.${item.extension}")
                val ok = runCatching { vfsManager.downloadToLocalFile(item, connection, tmp) }.getOrDefault(false)
                if (ok) { tempFile = tmp; tmp } else { tmp.delete(); null }
            }
            else -> null
        }

        try {
            if (analysisFile != null && supportsExifRead(item.extension)) {
                runCatching {
                    val exif = ExifInterface(analysisFile.absolutePath)
                    readExif(exif, sections)
                    val ll = exif.latLong
                    if (ll != null && !(ll[0] == 0.0 && ll[1] == 0.0)) {
                        val alt = exif.getAltitude(Double.NaN).takeIf { !it.isNaN() }
                        gps = GpsLocation(ll[0], ll[1], alt)
                    }
                }
            }

            if (analysisFile != null && (item.category == FileCategory.VIDEO || item.category == FileCategory.AUDIO)) {
                runCatching {
                    val r = MediaMetadataRetriever()
                    try {
                        r.setDataSource(analysisFile.absolutePath)
                        val (entries, loc) = readMedia(r)
                        if (entries.isNotEmpty()) {
                            sections += MetaSection(
                                if (item.category == FileCategory.VIDEO) "Video" else "Audio",
                                entries
                            )
                        }
                        if (gps == null) gps = loc
                    } finally {
                        r.release()
                    }
                }
            }

            gps?.let { g ->
                val entries = mutableListOf(
                    MetaEntry("Latitud", "%.6f".format(Locale.US, g.latitude)),
                    MetaEntry("Longitud", "%.6f".format(Locale.US, g.longitude)),
                    MetaEntry("DMS", "${toDms(g.latitude, true)}  ${toDms(g.longitude, false)}")
                )
                g.altitude?.let { entries += MetaEntry("Altitud", "%.1f m".format(Locale.US, it)) }
                sections.add(minOf(1, sections.size), MetaSection("Ubicación GPS", entries))
            }

            // Hash solo para archivos locales de hasta 200 MB
            if (localFile != null && item.sizeBytes in 1..(200L * 1024 * 1024)) {
                runCatching {
                    val md5 = MessageDigest.getInstance("MD5")
                    val sha = MessageDigest.getInstance("SHA-256")
                    localFile.inputStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var n: Int
                        while (input.read(buf).also { n = it } > 0) {
                            md5.update(buf, 0, n); sha.update(buf, 0, n)
                        }
                    }
                    sections += MetaSection(
                        "Integridad",
                        listOf(
                            MetaEntry("MD5", md5.digest().toHex()),
                            MetaEntry("SHA-256", sha.digest().toHex())
                        )
                    )
                }
            }
        } finally {
            tempFile?.delete()
        }

        FileMetadataResult(sections, gps)
    }

    private fun readExif(exif: ExifInterface, sections: MutableList<MetaSection>) {
        fun MutableList<MetaEntry>.add(label: String, tag: String, transform: (String) -> String = { it }) {
            exif.getAttribute(tag)?.trim()?.takeIf { it.isNotEmpty() }?.let { add(MetaEntry(label, transform(it))) }
        }

        val image = mutableListOf<MetaEntry>()
        val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        if (w > 0 && h > 0) {
            image += MetaEntry("Dimensiones", "$w × $h px (%.1f MP)".format(w * h / 1_000_000.0))
        }
        image.add("Orientación", ExifInterface.TAG_ORIENTATION) { orientationLabel(it.toIntOrNull() ?: 0) }
        image.add("Fecha de captura", ExifInterface.TAG_DATETIME_ORIGINAL)
        image.add("Zona horaria", ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
        image.add("Fecha digitalización", ExifInterface.TAG_DATETIME_DIGITIZED)
        image.add("Resolución X", ExifInterface.TAG_X_RESOLUTION)
        image.add("Resolución Y", ExifInterface.TAG_Y_RESOLUTION)
        image.add("Espacio de color", ExifInterface.TAG_COLOR_SPACE) { if (it == "1") "sRGB" else "Sin calibrar ($it)" }
        image.add("Software", ExifInterface.TAG_SOFTWARE)
        image.add("Descripción", ExifInterface.TAG_IMAGE_DESCRIPTION)
        image.add("Autor", ExifInterface.TAG_ARTIST)
        image.add("Copyright", ExifInterface.TAG_COPYRIGHT)
        if (image.isNotEmpty()) sections += MetaSection("Imagen", image)

        val camera = mutableListOf<MetaEntry>()
        camera.add("Fabricante", ExifInterface.TAG_MAKE)
        camera.add("Modelo", ExifInterface.TAG_MODEL)
        camera.add("Lente", ExifInterface.TAG_LENS_MODEL)
        camera.add("Número de serie", ExifInterface.TAG_BODY_SERIAL_NUMBER)
        camera.add("Apertura", ExifInterface.TAG_F_NUMBER) { "f/$it" }
        exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0).takeIf { it > 0 }?.let {
            camera += MetaEntry("Exposición", if (it < 1) "1/${(1 / it).toInt()} s" else "$it s")
        }
        camera.add("ISO", ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 }?.let {
            camera += MetaEntry("Distancia focal", "%.1f mm".format(it))
        }
        camera.add("Focal equiv. 35mm", ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM) { "$it mm" }
        camera.add("Compensación exp.", ExifInterface.TAG_EXPOSURE_BIAS_VALUE)
        camera.add("Flash", ExifInterface.TAG_FLASH) { if ((it.toIntOrNull() ?: 0) and 1 == 1) "Disparado" else "No disparado" }
        camera.add("Balance de blancos", ExifInterface.TAG_WHITE_BALANCE) { if (it == "0") "Automático" else "Manual" }
        if (camera.isNotEmpty()) sections += MetaSection("Cámara", camera)
    }

    private fun readMedia(r: MediaMetadataRetriever): Pair<List<MetaEntry>, GpsLocation?> {
        val out = mutableListOf<MetaEntry>()
        fun add(label: String, key: Int, transform: (String) -> String = { it }) {
            r.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }?.let { out += MetaEntry(label, transform(it)) }
        }
        add("Duración", MediaMetadataRetriever.METADATA_KEY_DURATION) { formatDuration(it.toLongOrNull() ?: 0) }
        val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        if (w != null && h != null) out += MetaEntry("Resolución", "$w × $h")
        add("Rotación", MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) { "$it°" }
        add("Bitrate", MediaMetadataRetriever.METADATA_KEY_BITRATE) {
            "${(it.toLongOrNull() ?: 0) / 1000} kbps"
        }
        add("Fotogramas", MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
        add("Tipo MIME (contenedor)", MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        add("Fecha", MediaMetadataRetriever.METADATA_KEY_DATE)
        add("Título", MediaMetadataRetriever.METADATA_KEY_TITLE)
        add("Artista", MediaMetadataRetriever.METADATA_KEY_ARTIST)
        add("Álbum", MediaMetadataRetriever.METADATA_KEY_ALBUM)
        add("Artista del álbum", MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
        add("Género", MediaMetadataRetriever.METADATA_KEY_GENRE)
        add("Año", MediaMetadataRetriever.METADATA_KEY_YEAR)
        add("Pista", MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        add("Compositor", MediaMetadataRetriever.METADATA_KEY_COMPOSER)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            add("Frecuencia de muestreo", MediaMetadataRetriever.METADATA_KEY_SAMPLERATE) { "$it Hz" }
            add("Bits por muestra", MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
        }
        val loc = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)?.let { parseIso6709(it) }
        return out to loc
    }

    /** Parsea ubicación ISO-6709 tipo "+34.0522-118.2437/" o "+34.0522-118.2437+089.000/". */
    internal fun parseIso6709(s: String): GpsLocation? {
        val m = Regex("""([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)?""").find(s) ?: return null
        val lat = m.groupValues[1].toDoubleOrNull() ?: return null
        val lon = m.groupValues[2].toDoubleOrNull() ?: return null
        val alt = m.groupValues[3].takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        return GpsLocation(lat, lon, alt)
    }

    private fun toDms(value: Double, isLat: Boolean): String {
        val a = abs(value)
        val d = a.toInt()
        val mFull = (a - d) * 60
        val m = mFull.toInt()
        val s = (mFull - m) * 60
        val ref = if (isLat) (if (value >= 0) "N" else "S") else (if (value >= 0) "E" else "O")
        return "%d°%02d'%05.2f\"%s".format(Locale.US, d, m, s, ref)
    }

    private fun orientationLabel(o: Int) = when (o) {
        1 -> "Normal"
        3 -> "Rotada 180°"
        6 -> "Rotada 90° horario"
        8 -> "Rotada 90° antihorario"
        2 -> "Espejada horizontal"
        4 -> "Espejada vertical"
        else -> "Sin definir"
    }

    private fun formatDuration(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
