package com.maez.qubitexplorer.model

import com.maez.qubitexplorer.R

enum class TransferType(@androidx.annotation.StringRes val labelRes: Int) {
    COPY(R.string.transfer_type_copy),
    MOVE(R.string.transfer_type_move),
    COMPRESS(R.string.transfer_type_compress),
    EXTRACT(R.string.transfer_type_extract)
}

enum class TransferStatus(@androidx.annotation.StringRes val labelRes: Int) {
    QUEUED(R.string.transfer_status_queued),
    RUNNING(R.string.transfer_status_running),
    PAUSED(R.string.transfer_status_paused),
    COMPLETED(R.string.transfer_status_completed),
    ERROR(R.string.transfer_status_error),
    CANCELLED(R.string.transfer_status_cancelled)
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
    val sourceConnections: List<NetworkConnection> = emptyList(),
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
