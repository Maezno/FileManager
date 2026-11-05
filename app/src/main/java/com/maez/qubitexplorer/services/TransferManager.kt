package com.maez.qubitexplorer.services

import android.content.Context
import com.maez.qubitexplorer.model.ConflictRequest
import com.maez.qubitexplorer.model.ConflictResolution
import com.maez.qubitexplorer.model.StorageProtocol
import com.maez.qubitexplorer.model.TransferJob
import com.maez.qubitexplorer.model.TransferStatus
import com.maez.qubitexplorer.model.TransferType
import com.maez.qubitexplorer.model.VfsItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object TransferManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val vfsManager = VfsManager()

    private val _jobs = MutableStateFlow<List<TransferJob>>(emptyList())
    val jobs: StateFlow<List<TransferJob>> = _jobs.asStateFlow()

    private val _activeConflict = MutableStateFlow<ConflictRequest?>(null)
    val activeConflict: StateFlow<ConflictRequest?> = _activeConflict.asStateFlow()

    private var executionJob: Job? = null
    private val cancelledJobIds = ConcurrentHashMap.newKeySet<String>()

    // Resolvemos conflictos colgados
    private var activeConflictDeferred: CompletableDeferred<Pair<ConflictResolution, Boolean>>? = null

    var onTransferFinished: ((TransferJob, Boolean) -> Unit)? = null

    fun enqueue(job: TransferJob) {
        _jobs.update { current ->
            val list = current.toMutableList()
            list.add(job)
            list
        }
        checkAndStartWorker()
    }

    fun cancel(jobId: String) {
        cancelledJobIds.add(jobId)
        
        // Si hay un conflicto esperando a este job, cancelarlo
        val currentConflictJob = getActiveJob()
        if (currentConflictJob?.id == jobId) {
            activeConflictDeferred?.complete(ConflictResolution.SKIP to false)
        }
        
        _jobs.update { current ->
            current.map { job ->
                if (job.id == jobId && (job.status == TransferStatus.RUNNING || job.status == TransferStatus.QUEUED)) {
                    job.copy(status = TransferStatus.CANCELLED)
                } else {
                    job
                }
            }
        }
    }

    fun clearFinished() {
        _jobs.update { current ->
            current.filter {
                it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED
            }
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

        val connMap = mutableMapOf<String, com.maez.qubitexplorer.model.NetworkConnection>()
        job.sourceConnections.forEach { connMap[it.id] = it }
        job.targetConnection?.let { connMap[it.id] = it }

        var allSuccess = true
        var isCancelled = false

        // Las carpetas no traen tamaño propio: se suma su contenido para que el progreso sea real
        if (job.items.any { it.isDirectory }) {
            try {
                val total = job.items.sumOf { item ->
                    if (!item.isDirectory) item.sizeBytes
                    else vfsManager.collectFilesRecursive(
                        rootPath = item.path,
                        protocol = item.protocol,
                        connection = item.connectionId?.let { connMap[it] }
                    ).sumOf { it.sizeBytes }
                }
                updateJob(job.id) { it.copy(totalBytes = total) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Sin total conocido el progreso se calcula por elementos
            }
        }

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
                    activeConflictDeferred = deferred
                    _activeConflict.value = ConflictRequest(
                        fileName = item.name,
                        targetParentPath = job.targetParentPath,
                        onResolve = { res, applyToAll ->
                            deferred.complete(res to applyToAll)
                            _activeConflict.value = null
                        }
                    )
                    val (res, applyToAll) = deferred.await()
                    activeConflictDeferred = null
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
                        
                        updateJob(job.id) {
                            it.copy(
                                bytesTransferred = totalBytesTransferred,
                                speedBytesPerSec = currentSpeed
                            )
                        }
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
                bytesTransferred = totalBytesTransferred,
                speedBytesPerSec = 0L,
                errorMessage = if (finalStatus == TransferStatus.ERROR) "Uno o más archivos no se pudieron transferir" else null
            )
        }

        val updatedJob = _jobs.value.firstOrNull { it.id == job.id } ?: job
        onTransferFinished?.invoke(updatedJob, allSuccess)
    }

    private val directoryCache = mutableMapOf<String, List<VfsItem>>()

    private suspend fun checkDestinationExists(
        targetParentPath: String,
        name: String,
        protocol: StorageProtocol,
        conn: com.maez.qubitexplorer.model.NetworkConnection?
    ): Boolean {
        if (protocol == StorageProtocol.LOCAL) {
            return File(targetParentPath, name).exists()
        }
        return try {
            val cacheKey = "${protocol.name}_${conn?.id}_$targetParentPath"
            val list = directoryCache.getOrPut(cacheKey) {
                vfsManager.listDirectory(targetParentPath, protocol, conn, showHidden = true)
            }
            list.any { it.name.equals(name, ignoreCase = true) }
        } catch (_: Exception) {
            // Fall cerrado: si falla la red asume que existe para no sobrescribir sin preguntar
            true
        }
    }

    private suspend fun generateAlternativeName(
        targetParentPath: String,
        originalName: String,
        protocol: StorageProtocol,
        conn: com.maez.qubitexplorer.model.NetworkConnection?
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
        updateJob(jobId) { 
            // Respetar CANCELLED como estado terminal
            if (it.status == TransferStatus.CANCELLED) it else it.copy(status = status)
        }
    }

    private fun updateJob(jobId: String, transform: (TransferJob) -> TransferJob) {
        _jobs.update { current ->
            current.map { job ->
                if (job.id == jobId) transform(job) else job
            }
        }
    }
}
