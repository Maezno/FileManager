package com.maez.filemanager.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class StorageProtocol(val label: String) {
    LOCAL("Local"),
    SMB("SMB (Windows / NAS)"),
    FTP("FTP / FTPS")
}

enum class FileCategory(val label: String) {
    ALL("Todos"),
    FOLDER("Carpetas"),
    IMAGE("Fotos"),
    VIDEO("Videos"),
    AUDIO("Audio"),
    DOCUMENT("Documentos"),
    ARCHIVE("Comprimidos"),
    APK("Apps APK"),
    OTHER("Otros");

    companion object {
        private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp", "raw", "dng")
        private val VIDEO_EXT = setOf("mp4", "mov", "mkv", "avi", "3gp", "mts", "m4v", "webm", "m2ts", "wmv")
        private val AUDIO_EXT = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma")
        private val DOC_EXT = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "md", "json", "xml", "html")
        private val ARCHIVE_EXT = setOf("zip", "rar", "7z", "tar", "gz", "bz2")

        fun fromExtension(ext: String, isDirectory: Boolean): FileCategory {
            if (isDirectory) return FOLDER
            return when (ext.lowercase(Locale.ROOT)) {
                in IMAGE_EXT -> IMAGE
                in VIDEO_EXT -> VIDEO
                in AUDIO_EXT -> AUDIO
                in DOC_EXT -> DOCUMENT
                in ARCHIVE_EXT -> ARCHIVE
                "apk", "xapk", "apks" -> APK
                else -> OTHER
            }
        }
    }
}

enum class MediaOrientation(val label: String) {
    ALL("Todas"),
    VERTICAL("Vertical"),
    HORIZONTAL("Horizontal")
}

enum class SortField(val label: String) {
    NAME("Nombre"),
    DATE("Fecha"),
    SIZE("Tamaño"),
    TYPE("Tipo")
}

data class VfsItem(
    val path: String,
    val name: String,
    val parentPath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val protocol: StorageProtocol = StorageProtocol.LOCAL,
    val connectionId: String? = null
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    val category: FileCategory
        get() = FileCategory.fromExtension(extension, isDirectory)

    val sizeFormatted: String
        get() = if (isDirectory) "Carpeta" else formatBytes(sizeBytes)

    val dateFormatted: String
        get() = if (lastModified <= 0L) "--" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastModified))
}

data class NetworkConnection(
    val id: String,
    val name: String,
    val protocol: StorageProtocol,
    val host: String,
    val port: Int = if (protocol == StorageProtocol.SMB) 445 else 21,
    val shareName: String = "", // Para SMB (ej. "Users", "Public", "Media")
    val username: String = "",
    val password: String = "",
    val anonymous: Boolean = false,
    val initialPath: String = "/",
    val lastUsedAt: Long = 0L
) {
    val displayAddress: String
        get() = when (protocol) {
            StorageProtocol.SMB -> "smb://$host/${shareName.trim('/')}"
            StorageProtocol.FTP -> "ftp://$host:$port${if (initialPath.startsWith("/")) initialPath else "/$initialPath"}"
            StorageProtocol.LOCAL -> initialPath
        }
}

data class DiscoveredHost(
    val ip: String,
    val hostname: String,
    val openPorts: List<Int>
) {
    val supportsSmb: Boolean get() = 445 in openPorts
    val supportsFtp: Boolean get() = 21 in openPorts || 2121 in openPorts
}

data class MediaSearchItem(
    val item: VfsItem,
    val deviceModel: String = "Desconocido",
    val orientation: String = "Horizontal", // "Vertical" | "Horizontal" | "Cuadrado" | "N/A"
    val resolution: String = "",
    val rotation: Int = 0,
    val durationSeconds: Double = 0.0
) {
    val durationFormatted: String
        get() {
            if (durationSeconds <= 0.0) return ""
            val totalSec = durationSeconds.toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            return if (m > 0) "${m}m ${String.format(Locale.US, "%02ds", s)}" else "${s}s"
        }
}

data class DeviceSummary(
    val deviceModel: String,
    val verticalCount: Int,
    val horizontalCount: Int,
    val otherCount: Int = 0
) {
    val totalCount: Int get() = verticalCount + horizontalCount + otherCount
    val pctVertical: String
        get() = if (totalCount > 0) "${Math.round(verticalCount * 100.0 / totalCount)}%" else "0%"
}

data class DuplicateFileEntry(
    val groupId: Int,
    val item: VfsItem,
    val isPrimary: Boolean,
    val sourceLabel: String // "Carpeta A" o "Carpeta B"
)

data class ClipboardState(
    val items: List<VfsItem> = emptyList(),
    val isMove: Boolean = false
)

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
