package com.maez.filemanager.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.exifinterface.media.ExifInterface
import com.maez.filemanager.model.FileCategory
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import com.maez.filemanager.services.VfsManager
import com.maez.filemanager.ui.theme.AccentBlueLight
import com.maez.filemanager.ui.theme.AccentGreenLight
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.AccentPurple
import com.maez.filemanager.ui.theme.BorderDark
import com.maez.filemanager.ui.theme.ConsoleBg
import com.maez.filemanager.ui.theme.DangerRedLight
import com.maez.filemanager.ui.theme.TextSecondary
import com.maez.filemanager.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbnailCache {
    private val cache = object : LruCache<String, ImageBitmap>(120) {}

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    suspend fun loadThumbnail(
        context: Context,
        item: VfsItem,
        vfsManager: VfsManager? = null,
        connection: NetworkConnection? = null,
        targetPx: Int = 280
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        if (item.isDirectory || item.sizeBytes <= 0L) return@withContext null
        val cacheKey = "${item.protocol}:${item.path}:${item.sizeBytes}:${item.lastModified}:$targetPx"
        get(cacheKey)?.let { return@withContext it }

        val bmp: Bitmap? = try {
            if (item.protocol == StorageProtocol.LOCAL) {
                val file = File(item.path)
                if (!file.exists() || file.length() <= 0L) return@withContext null
                when (item.category) {
                    FileCategory.IMAGE -> decodeLocalImageThumbnail(file, targetPx)
                    FileCategory.VIDEO -> decodeLocalVideoThumbnail(file, targetPx)
                    FileCategory.APK -> decodeLocalApkIcon(context, file)
                    else -> null
                }
            } else if (vfsManager != null && item.category == FileCategory.IMAGE && item.sizeBytes <= 12 * 1024 * 1024L) {
                // Para imágenes en red SMB/FTP (hasta 12 MB), cachear en carpeta temporal y generar miniatura
                val thumbCacheDir = File(context.cacheDir, "remote_thumbs").apply { mkdirs() }
                val safeName = item.path.hashCode().toUInt().toString(16) + "_" + item.name
                val tempFile = File(thumbCacheDir, safeName)
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    vfsManager.downloadToLocalFile(item, connection, tempFile)
                }
                if (tempFile.exists() && tempFile.length() > 0L) {
                    decodeLocalImageThumbnail(tempFile, targetPx)
                } else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        bmp?.asImageBitmap()?.also { put(cacheKey, it) }
    }

    private fun decodeLocalImageThumbnail(file: File, targetPx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return ThumbnailUtils.createImageThumbnail(file, Size(targetPx, targetPx), null)
            } catch (_: Exception) {
            }
        }
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > targetPx * 2 || boundsOpts.outHeight / sampleSize > targetPx * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null

        // Respetar rotación EXIF
        return try {
            val exif = ExifInterface(file.absolutePath)
            val rot = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rot != 0f) {
                val matrix = Matrix().apply { postRotate(rot) }
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            } else raw
        } catch (_: Exception) {
            raw
        }
    }

    private fun decodeLocalVideoThumbnail(file: File, targetPx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val thumb = ThumbnailUtils.createVideoThumbnail(file, Size(targetPx, targetPx), null)
                if (thumb != null) return thumb
            } catch (_: Exception) {
            }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetPx, targetPx)
                    ?: retriever.getFrameAtTime(0L)
            } else {
                retriever.getFrameAtTime(0L)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun decodeLocalApkIcon(context: Context, file: File): Bitmap? {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
            val appInfo = info.applicationInfo ?: return null
            appInfo.sourceDir = file.absolutePath
            appInfo.publicSourceDir = file.absolutePath
            appInfo.loadIcon(pm)?.toBitmap(128, 128)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Vista de miniatura cuadrada/rectangular para modo Cuadrícula y modo Lista.
 * Carga de forma asíncrona la previsualización real de fotos, videos y APKs.
 */
@Composable
fun FileThumbnailBox(
    item: VfsItem,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    vfsManager: VfsManager? = null,
    connection: NetworkConnection? = null,
    showBadge: Boolean = true
) {
    val context = LocalContext.current
    val cacheKey = "${item.protocol}:${item.path}:${item.sizeBytes}:${item.lastModified}:280"
    var bitmap by remember(cacheKey) { mutableStateOf(ThumbnailCache.get(cacheKey)) }
    var loadAttempted by remember(cacheKey) { mutableStateOf(bitmap != null) }

    val canHaveVisualThumb = !item.isDirectory && item.sizeBytes > 0L && (
        item.category == FileCategory.IMAGE ||
            item.category == FileCategory.VIDEO ||
            item.category == FileCategory.APK
        )

    LaunchedEffect(cacheKey) {
        if (bitmap == null && canHaveVisualThumb) {
            bitmap = ThumbnailCache.loadThumbnail(context, item, vfsManager, connection, 280)
            loadAttempted = true
        }
    }

    val shape = RoundedCornerShape(cornerRadius)
    val categoryColor = when {
        item.isDirectory -> AccentBlueLight
        item.sizeBytes == 0L -> DangerRedLight
        item.category == FileCategory.VIDEO -> AccentPurple
        item.category == FileCategory.IMAGE -> AccentGreenLight
        item.category == FileCategory.AUDIO -> WarningAmber
        item.category == FileCategory.DOCUMENT -> AccentBlueLight
        item.category == FileCategory.ARCHIVE -> WarningAmber
        item.category == FileCategory.APK -> AccentGreenLight
        else -> TextSecondary
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (bitmap != null) ConsoleBg else categoryColor.copy(alpha = 0.12f))
            .border(1.dp, if (bitmap != null) BorderDark else categoryColor.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Si es un video, mostrar botón Play superpuesto elegante
            if (item.category == FileCategory.VIDEO) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            val icon = when {
                item.isDirectory -> Icons.Default.Folder
                item.sizeBytes == 0L && (item.category == FileCategory.VIDEO || item.category == FileCategory.IMAGE) -> Icons.Default.BrokenImage
                loadAttempted && item.category == FileCategory.VIDEO -> Icons.Default.VideoFile
                item.category == FileCategory.VIDEO -> Icons.Default.VideoFile
                item.category == FileCategory.IMAGE -> Icons.Default.Image
                item.category == FileCategory.AUDIO -> Icons.Default.AudioFile
                item.category == FileCategory.DOCUMENT -> Icons.Default.Description
                item.category == FileCategory.ARCHIVE -> Icons.Default.Archive
                item.category == FileCategory.APK -> Icons.Default.Android
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(32.dp)
            )
        }

        if (showBadge && !item.isDirectory && item.extension.isNotBlank() && bitmap == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (item.sizeBytes == 0L) "0B" else item.extension.uppercase().take(4),
                    color = if (item.sizeBytes == 0L) DangerRedLight else Color.White,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FileThumbnailBoxPreview() {
    FileManagerTheme {
        FileThumbnailBox(
            item = VfsItem(
                name = "Video_Ejemplo.mp4",
                path = "/storage/emulated/0/Video_Ejemplo.mp4",
                parentPath = "/storage/emulated/0",
                sizeBytes = 12_500_000,
                lastModified = System.currentTimeMillis(),
                isDirectory = false,
                protocol = StorageProtocol.LOCAL
            ),
            modifier = Modifier.size(64.dp)
        )
    }
}
