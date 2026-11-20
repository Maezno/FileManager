package com.maez.qubitexplorer.services

import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale
import kotlin.coroutines.coroutineContext

data class ArchiveEntryInfo(val name: String, val size: Long, val isDirectory: Boolean)

data class ArchiveListing(
    val format: String,
    val entries: List<ArchiveEntryInfo>,
    val note: String? = null
) {
    val totalSize: Long get() = entries.sumOf { it.size }
    val fileCount: Int get() = entries.count { !it.isDirectory }
}

/**
 * Lista y extrae comprimidos: ZIP/JAR/APK, 7z, RAR (v4; RAR5 no lo soporta junrar),
 * TAR y sus variantes (.tar.gz/.tgz, .tar.bz2, .tar.xz, .tar.lzma, .tar.zst*),
 * archivos sueltos .gz/.bz2/.xz/.lzma/.z, y AR/CPIO/ARJ.
 */
object ArchiveExtractor {

    val SUPPORTED_EXTENSIONS = setOf(
        "zip", "jar", "apk", "aar", "war", "epub", "cbz",
        "7z", "cb7", "rar", "cbr",
        "tar", "tgz", "tbz", "tbz2", "txz", "tlz",
        "gz", "gzip", "bz2", "xz", "lzma", "z", "zst",
        "ar", "deb", "cpio", "arj"
    )

    fun isArchive(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in SUPPORTED_EXTENSIONS
    }

    private enum class Kind { ZIP, SEVEN_Z, RAR, TAR_COMPRESSED, SINGLE_COMPRESSED, STREAM_ARCHIVE }

    private fun kindOf(file: File): Kind {
        val n = file.name.lowercase(Locale.ROOT)
        val ext = n.substringAfterLast('.', "")
        return when {
            ext in setOf("zip", "jar", "apk", "aar", "war", "epub", "cbz") -> Kind.ZIP
            ext in setOf("7z", "cb7") -> Kind.SEVEN_Z
            ext in setOf("rar", "cbr") -> Kind.RAR
            ext in setOf("tgz", "tbz", "tbz2", "txz", "tlz") -> Kind.TAR_COMPRESSED
            n.matches(Regex(""".*\.tar\.(gz|gzip|bz2|xz|lzma|z|zst)$""")) -> Kind.TAR_COMPRESSED
            ext in setOf("gz", "gzip", "bz2", "xz", "lzma", "z", "zst") -> Kind.SINGLE_COMPRESSED
            else -> Kind.STREAM_ARCHIVE // tar, ar, deb, cpio, arj
        }
    }

    private fun formatLabel(file: File): String {
        val n = file.name.lowercase(Locale.ROOT)
        return when {
            n.contains(".tar.") -> "TAR." + n.substringAfterLast('.').uppercase(Locale.ROOT)
            else -> n.substringAfterLast('.', "?").uppercase(Locale.ROOT)
        }
    }

    private fun openCompressor(file: File): InputStream {
        val buffered = BufferedInputStream(FileInputStream(file))
        return CompressorStreamFactory(true).createCompressorInputStream(buffered)
    }

    private fun openStreamArchive(input: InputStream): ArchiveInputStream<*> =
        ArchiveStreamFactory().createArchiveInputStream(BufferedInputStream(input))

    suspend fun list(file: File): ArchiveListing = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ArchiveEntryInfo>()
        var note: String? = null
        when (kindOf(file)) {
            Kind.ZIP -> ZipFile.builder().setFile(file).get().use { zip ->
                zip.entries.asSequence().forEach {
                    entries += ArchiveEntryInfo(it.name, it.size.coerceAtLeast(0), it.isDirectory)
                }
            }
            Kind.SEVEN_Z -> SevenZFile.builder().setFile(file).get().use { sz ->
                sz.entries.forEach { entries += ArchiveEntryInfo(it.name, it.size, it.isDirectory) }
            }
            Kind.RAR -> Archive(file).use { rar ->
                if (rar.isEncrypted) note = "El RAR está protegido con contraseña."
                rar.fileHeaders.forEach {
                    entries += ArchiveEntryInfo(it.fileName.replace('\\', '/'), it.fullUnpackSize, it.isDirectory)
                }
            }
            Kind.TAR_COMPRESSED -> openCompressor(file).use { c ->
                openStreamArchive(c).use { ar ->
                    generateSequence { ar.nextEntry }.forEach {
                        entries += ArchiveEntryInfo(it.name, it.size.coerceAtLeast(0), it.isDirectory)
                    }
                }
            }
            Kind.SINGLE_COMPRESSED -> {
                // Un solo archivo comprimido: el tamaño real se conoce al descomprimir
                entries += ArchiveEntryInfo(singleOutputName(file), -1, false)
            }
            Kind.STREAM_ARCHIVE -> FileInputStream(file).use { fis ->
                openStreamArchive(fis).use { ar ->
                    generateSequence { ar.nextEntry }.forEach {
                        entries += ArchiveEntryInfo(it.name, it.size.coerceAtLeast(0), it.isDirectory)
                    }
                }
            }
        }
        ArchiveListing(formatLabel(file), entries, note)
    }

    private fun singleOutputName(file: File): String {
        val n = file.name
        val lower = n.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".gzip") -> n.dropLast(5)
            lower.endsWith(".lzma") -> n.dropLast(5)
            lower.endsWith(".gz") || lower.endsWith(".xz") -> n.dropLast(3)
            lower.endsWith(".bz2") || lower.endsWith(".zst") -> n.dropLast(4)
            lower.endsWith(".z") -> n.dropLast(2)
            else -> "$n.out"
        }
    }

    /** Ruta interna normalizada: barras '/', sin './' inicial ni '/' al principio o al final. */
    fun normalizeName(name: String): String =
        name.replace('\\', '/').removePrefix("./").trim('/')

    /** Carpeta destino sugerida: junto al comprimido, con su nombre sin extensión(es). */
    fun defaultDestination(file: File): File {
        val base = file.name
            .replace(Regex("""(?i)\.tar\.(gz|gzip|bz2|xz|lzma|z|zst)$"""), "")
            .substringBeforeLast('.')
            .ifBlank { file.name + "_extraido" }
        var dest = File(file.parentFile, base)
        var i = 2
        while (dest.exists()) dest = File(file.parentFile, "$base ($i)").also { i++ }
        return dest
    }

    /** Resuelve la ruta de salida evitando "zip slip" (entradas con ../ que escapan del destino). */
    private fun safeTarget(destDir: File, entryName: String): File? {
        val clean = entryName.replace('\\', '/').trimStart('/')
        if (clean.isEmpty()) return null
        val out = File(destDir, clean)
        val destPath = destDir.canonicalPath + File.separator
        return if (out.canonicalPath.startsWith(destPath) || out.canonicalPath == destDir.canonicalPath) out else null
    }

    private suspend fun copyStream(input: InputStream, out: File, onBytes: (Long) -> Unit) {
        out.parentFile?.mkdirs()
        out.outputStream().use { o ->
            val buf = ByteArray(64 * 1024)
            var n: Int
            while (input.read(buf).also { n = it } > 0) {
                coroutineContext.ensureActive()
                o.write(buf, 0, n)
                onBytes(n.toLong())
            }
        }
    }

    /**
     * Extrae todo el contenido en [destDir]. Devuelve la cantidad de archivos escritos.
     * [onProgress] recibe bytes escritos acumulados.
     */
    suspend fun extract(
        file: File,
        destDir: File,
        include: ((String) -> Boolean)? = null,
        onProgress: (Long) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        destDir.mkdirs()
        var written = 0L
        var count = 0
        val tick: (Long) -> Unit = { written += it; onProgress(written) }

        suspend fun write(name: String, isDir: Boolean, lastModified: Long?, stream: () -> InputStream) {
            if (include != null && !include(normalizeName(name))) return
            val target = safeTarget(destDir, name) ?: return
            if (isDir) { target.mkdirs(); return }
            stream().let { copyStream(it, target, tick) }
            lastModified?.takeIf { it > 0 }?.let { target.setLastModified(it) }
            count++
        }

        when (kindOf(file)) {
            Kind.ZIP -> ZipFile.builder().setFile(file).get().use { zip ->
                for (e in zip.entries.asSequence().toList()) {
                    coroutineContext.ensureActive()
                    if (e.isDirectory) { write(e.name, true, null) { java.io.ByteArrayInputStream(ByteArray(0)) }; continue }
                    zip.getInputStream(e).use { ins -> write(e.name, false, e.time) { ins } }
                }
            }
            Kind.SEVEN_Z -> SevenZFile.builder().setFile(file).get().use { sz ->
                var e = sz.nextEntry
                while (e != null) {
                    coroutineContext.ensureActive()
                    val entry = e
                    if (entry.isDirectory) write(entry.name, true, null) { java.io.ByteArrayInputStream(ByteArray(0)) }
                    else write(entry.name, false, if (entry.hasLastModifiedDate) entry.lastModifiedDate.time else null) {
                        sz.getInputStream(entry)
                    }
                    e = sz.nextEntry
                }
            }
            Kind.RAR -> Archive(file).use { rar ->
                if (rar.isEncrypted) throw IllegalStateException("El RAR está protegido con contraseña.")
                for (h in rar.fileHeaders) {
                    coroutineContext.ensureActive()
                    val name = h.fileName.replace('\\', '/')
                    if (h.isDirectory) { write(name, true, null) { java.io.ByteArrayInputStream(ByteArray(0)) }; continue }
                    rar.getInputStream(h).use { ins -> write(name, false, h.mTime?.time) { ins } }
                }
            }
            Kind.TAR_COMPRESSED -> openCompressor(file).use { c ->
                openStreamArchive(c).use { ar -> extractStream(ar, ::write) }
            }
            Kind.SINGLE_COMPRESSED -> openCompressor(file).use { c ->
                write(singleOutputName(file), false, file.lastModified()) { c }
            }
            Kind.STREAM_ARCHIVE -> FileInputStream(file).use { fis ->
                openStreamArchive(fis).use { ar -> extractStream(ar, ::write) }
            }
        }
        count
    }

    private suspend fun extractStream(
        ar: ArchiveInputStream<*>,
        write: suspend (String, Boolean, Long?, () -> InputStream) -> Unit
    ) {
        while (true) {
            coroutineContext.ensureActive()
            val e = ar.nextEntry ?: break
            if (!ar.canReadEntryData(e)) continue
            val modified = runCatching { e.lastModifiedDate?.time }.getOrNull()
            // No cerrar ar: el stream del archivo actual es el propio ArchiveInputStream
            write(e.name, e.isDirectory, modified) { NonClosingInputStream(ar) }
        }
    }

    private class NonClosingInputStream(private val inner: InputStream) : InputStream() {
        override fun read(): Int = inner.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = inner.read(b, off, len)
        override fun close() {}
    }
}
