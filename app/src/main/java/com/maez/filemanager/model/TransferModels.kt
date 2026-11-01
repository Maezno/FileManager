package com.maez.filemanager.model

enum class TransferType(val label: String) {
    COPY("Copiar"),
    MOVE("Mover"),
    COMPRESS("Comprimir"),
    EXTRACT("Extraer")
}

enum class TransferStatus(val label: String) {
    QUEUED("En cola"),
    RUNNING("Transfiriendo"),
    PAUSED("Pausado"),
    COMPLETED("Completado"),
    ERROR("Error"),
    CANCELLED("Cancelado")
}

enum class ConflictResolution {
    ASK,
    OVERWRITE,
    SKIP,
    KEEP_BOTH
}

data class ConflictRequest(
    val fileName: String,
    val targetParentPath: String,
    val onResolve: (resolution: ConflictResolution, applyToAll: Boolean) -> Unit
)

data class TransferJob(
    val id: String,
    val type: TransferType,
    val items: List<VfsItem>,
    val targetParentPath: String,
    val targetProtocol: StorageProtocol,
    val targetConnection: NetworkConnection?,
    val currentFileName: String = "",
    val currentIndex: Int = 0,
    val totalFiles: Int = items.size,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = items.sumOf { it.sizeBytes },
    val speedBytesPerSec: Long = 0L,
    val status: TransferStatus = TransferStatus.QUEUED,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = when {
            totalBytes > 0L -> (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            totalFiles > 0 -> (currentIndex.toFloat() / totalFiles.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }

    val speedFormatted: String
        get() = if (speedBytesPerSec > 0L) "${formatBytes(speedBytesPerSec)}/s" else "--"
}
