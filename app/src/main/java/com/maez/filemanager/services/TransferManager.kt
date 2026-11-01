package com.maez.filemanager.services

import android.content.Context
import com.maez.filemanager.model.ConflictRequest
import com.maez.filemanager.model.ConflictResolution
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.TransferJob
import com.maez.filemanager.model.TransferStatus
import com.maez.filemanager.model.TransferType
import com.maez.filemanager.model.VfsItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

object TransferManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val vfsManager = VfsManager()

    private val _jobs = MutableStateFlow<List<TransferJob>>(emptyList())
    val jobs: StateFlow<List<TransferJob>> = _jobs.asStateFlow()

    private val _activeConflict = MutableStateFlow<ConflictRequest?>(null)
    val activeConflict: StateFlow<ConflictRequest?> = _activeConflict.asStateFlow()

    private var executionJob: Job? = null
    private val cancelledJobIds = mutableSetOf<String>()

    var onTransferFinished: ((TransferJob, Boolean) -> Unit)? = null

    fun enqueue(job: TransferJob) {
        val current = _jobs.value.toMutableList()
        current.add(job)
        _jobs.value = current
        checkAndStartWorker()
    }

    fun cancel(jobId: String) {
        cancelledJobIds.add(jobId)
        val current = _jobs.value.map { job ->
            if (job.id == jobId && (job.status == TransferStatus.RUNNING || job.status == TransferStatus.QUEUED)) {
                job.copy(status = TransferStatus.CANCELLED)
            } else {
                job
            }
        }
        _jobs.value = current
    }

    fun clearFinished() {
        _jobs.value = _jobs.value.filter {
            it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED
        }
    }

    fun hasActiveTransfers(): Boolean {
        return _jobs.value.any { it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED }
    }

    fun getActiveJob(): TransferJob? {
        return _jobs.value.firstOrNull { it.status == TransferStatus.RUNNING }
            ?: _jobs.value.firstOrNull { it.status == TransferStatus.QUEUED }
    }

    private fun checkAndStartWorker() {
        if (executionJob?.isActive == true) return

        executionJob = scope.launch {
            while (isActive) {
                val nextJob = _jobs.value.firstOrNull { it.status == TransferStatus.QUEUED }
                if (nextJob == null) break

                processJob(nextJob)
            }
        }
    }

    private suspend fun processJob(job: TransferJob) {
        if (cancelledJobIds.contains(job.id)) {
            updateJobStatus(job.id, TransferStatus.CANCELLED)
            return
        }

        updateJobStatus(job.id, TransferStatus.RUNNING)

        var totalBytesTransferred = 0L
        var lastTime = System.currentTimeMillis()
        var bytesSinceLastTime = 0L
        var currentSpeed = 0L

        var globalResolution: ConflictResolution? = null

        val connMap = mutableMapOf<String, com.maez.filemanager.model.NetworkConnection>()
        job.targetConnection?.let { connMap[it.id] = it }

        var allSuccess = true
        var isCancelled = false

        for ((index, item) in job.items.withIndex()) {
            if (cancelledJobIds.contains(job.id)) {
                isCancelled = true
                break
            }

            var targetFileName = item.name

            // Chequeo de colisión de nombre si el destino ya tiene el archivo
            val exists = checkDestinationExists(job.targetParentPath, item.name, job.targetProtocol, job.targetConnection)
            if (exists) {
                val resolution = globalResolution ?: run {
                    val deferred = CompletableDeferred<Pair<ConflictResolution, Boolean>>()
                    _activeConflict.value = ConflictRequest(
                        fileName = item.name,
                        targetParentPath = job.targetParentPath,
                        onResolve = { res, applyToAll ->
                            deferred.complete(res to applyToAll)
                            _activeConflict.value = null
                        }
                    )
                    val (res, applyToAll) = deferred.await()
                    if (applyToAll) globalResolution = res
                    res
                }

                when (resolution) {
                    ConflictResolution.SKIP -> {
                        updateJob(job.id) {
                            it.copy(
                                currentIndex = index + 1,
                                currentFileName = item.name
                            )
                        }
                        continue
                    }
                    ConflictResolution.KEEP_BOTH -> {
                        targetFileName = generateAlternativeName(job.targetParentPath, item.name, job.targetProtocol, job.targetConnection)
                    }
                    ConflictResolution.OVERWRITE, ConflictResolution.ASK -> {
                        // Proceder normal sobrescribiendo
                    }
                }
            }

            updateJob(job.id) {
                it.copy(
                    currentIndex = index + 1,
                    currentFileName = targetFileName
                )
            }

            val srcConn = item.connectionId?.let { connMap[it] }

            val success = vfsManager.transferFile(
                source = item,
                sourceConn = srcConn,
                targetParentPath = job.targetParentPath,
                targetProtocol = job.targetProtocol,
                targetConn = job.targetConnection,
                deleteSourceAfter = (job.type == TransferType.MOVE),
                onProgress = { bytesInc ->
                    totalBytesTransferred += bytesInc
                    bytesSinceLastTime += bytesInc
                    val now = System.currentTimeMillis()
                    val diff = now - lastTime
                    if (diff >= 500) {
                        currentSpeed = (bytesSinceLastTime * 1000L) / diff
                        lastTime = now
                        bytesSinceLastTime = 0L
                    }

                    updateJob(job.id) {
                        it.copy(
                            bytesTransferred = totalBytesTransferred,
                            speedBytesPerSec = currentSpeed
                        )
                    }
                },
                isCancelled = { cancelledJobIds.contains(job.id) },
                targetFileName = targetFileName
            )

            if (!success) {
                if (cancelledJobIds.contains(job.id)) {
                    isCancelled = true
                    break
                }
                allSuccess = false
            }
        }

        val finalStatus = when {
            isCancelled -> TransferStatus.CANCELLED
            allSuccess -> TransferStatus.COMPLETED
            else -> TransferStatus.ERROR
        }

        updateJob(job.id) {
            it.copy(
                status = finalStatus,
                speedBytesPerSec = 0L,
                errorMessage = if (finalStatus == TransferStatus.ERROR) "Uno o más archivos no se pudieron transferir" else null
            )
        }

        val updatedJob = _jobs.value.firstOrNull { it.id == job.id } ?: job
        onTransferFinished?.invoke(updatedJob, allSuccess)
    }

    private suspend fun checkDestinationExists(
        targetParentPath: String,
        name: String,
        protocol: StorageProtocol,
        conn: com.maez.filemanager.model.NetworkConnection?
    ): Boolean {
        return try {
            val list = vfsManager.listDirectory(targetParentPath, protocol, conn, showHidden = true)
            list.any { it.name.equals(name, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun generateAlternativeName(
        targetParentPath: String,
        originalName: String,
        protocol: StorageProtocol,
        conn: com.maez.filemanager.model.NetworkConnection?
    ): String {
        val ext = originalName.substringAfterLast('.', "")
        val baseName = if (ext.isNotEmpty()) originalName.substringBeforeLast('.') else originalName
        var counter = 1
        var candidate: String
        do {
            candidate = if (ext.isNotEmpty()) "$baseName ($counter).$ext" else "$baseName ($counter)"
            counter++
        } while (checkDestinationExists(targetParentPath, candidate, protocol, conn))
        return candidate
    }

    private fun updateJobStatus(jobId: String, status: TransferStatus) {
        updateJob(jobId) { it.copy(status = status) }
    }

    private fun updateJob(jobId: String, transform: (TransferJob) -> TransferJob) {
        val current = _jobs.value.map { job ->
            if (job.id == jobId) transform(job) else job
        }
        _jobs.value = current
    }
}
