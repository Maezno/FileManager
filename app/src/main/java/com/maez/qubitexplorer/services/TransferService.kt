package com.maez.qubitexplorer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.maez.qubitexplorer.MainActivity
import com.maez.qubitexplorer.R
import com.maez.qubitexplorer.model.TransferJob
import com.maez.qubitexplorer.model.TransferStatus
import com.maez.qubitexplorer.model.formatBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TransferService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var notificationUpdaterJob: Job? = null
    private var lastNotificationTime = 0L

    companion object {
        const val CHANNEL_ID = "transfers"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.maez.qubitexplorer.TRANSFER_START"
        const val ACTION_CANCEL = "com.maez.qubitexplorer.TRANSFER_CANCEL"
        const val EXTRA_JOB_ID = "extra_job_id"

        fun start(context: Context) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context, jobId: String) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startMonitoringJobs()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                if (jobId != null) {
                    TransferManager.cancel(jobId)
                }
            }
            ACTION_START -> {
                val activeJob = TransferManager.getActiveJob()
                val notification = buildNotification(activeJob)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startMonitoringJobs() {
        notificationUpdaterJob = serviceScope.launch {
            TransferManager.jobs.collectLatest { jobs ->
                val activeJob = jobs.firstOrNull { it.status == TransferStatus.RUNNING }
                    ?: jobs.firstOrNull { it.status == TransferStatus.QUEUED }

                if (activeJob == null) {
                    // Detener servicio si no hay transferencias activas
                    delay(1000)
                    if (!TransferManager.hasActiveTransfers()) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationTime >= 500) {
                        lastNotificationTime = now
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, buildNotification(activeJob))
                    }
                }
            }
        }
    }

    private fun buildNotification(job: TransferJob?): Notification {
        val title = if (job != null) {
            "${job.type.label}: ${job.currentFileName.ifBlank { "Preparando..." }}"
        } else {
            "Transferencias en segundo plano"
        }

        val text = if (job != null) {
            "${job.currentIndex}/${job.totalFiles} archivos • ${job.speedFormatted}"
        } else {
            "Completado"
        }

        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (job != null) {
            val progressInt = (job.progress * 100).toInt()
            builder.setProgress(100, progressInt, job.totalBytes <= 0 && job.totalFiles <= 0)

            // Acción cancelar
            val cancelIntent = Intent(this, TransferService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_JOB_ID, job.id)
            }
            val cancelPending = PendingIntent.getService(
                this,
                job.id.hashCode(),
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelPending)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transferencias de archivos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de copia, movimiento y transferencias en red"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        handleTimeout()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        handleTimeout()
    }

    private fun handleTimeout() {
        // En Android 15+, dataSync tiene un timeout máximo de ejecución
        TransferManager.getActiveJob()?.let {
            TransferManager.cancel(it.id)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
