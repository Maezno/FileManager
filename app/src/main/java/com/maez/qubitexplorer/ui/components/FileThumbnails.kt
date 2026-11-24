package com.maez.qubitexplorer.ui.components

import com.maez.qubitexplorer.R
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.PictureAsPdf
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
import com.maez.qubitexplorer.model.FileCategory
import com.maez.qubitexplorer.model.NetworkConnection
import com.maez.qubitexplorer.model.StorageProtocol
import com.maez.qubitexplorer.model.VfsItem
import com.maez.qubitexplorer.services.VfsManager
import com.maez.qubitexplorer.services.readBytesLimited
import com.maez.qubitexplorer.ui.theme.AccentBlueLight
import com.maez.qubitexplorer.ui.theme.AccentGreenLight
import com.maez.qubitexplorer.ui.theme.QubitExplorerTheme
import com.maez.qubitexplorer.ui.theme.AccentPurple
import com.maez.qubitexplorer.ui.theme.BorderDark
import com.maez.qubitexplorer.ui.theme.ConsoleBg
import com.maez.qubitexplorer.ui.theme.MediaDanger
import com.maez.qubitexplorer.ui.theme.DangerRedLight
import com.maez.qubitexplorer.ui.theme.TextSecondary
import com.maez.qubitexplorer.ui.theme.WarningAmber
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
                    FileCategory.AUDIO -> decodeAudioArt(file, targetPx)
                    else -> when (item.extension) {
                        "pdf" -> decodePdfFirstPage(file, targetPx)
                        in OFFICE_THUMB_EXTS -> decodeZipEmbeddedThumbnail(file, targetPx)
                        else -> null
                    }
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

    /** Formatos ZIP (OOXML / ODF) que suelen guardar una miniatura interna. */
    val OFFICE_THUMB_EXTS = setOf("docx", "xlsx", "pptx", "odt", "ods", "odp")

    private fun decodePdfFirstPage(file: File, targetPx: Int): Bitmap? {
        return try {
            android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null
                    renderer.openPage(0).use { page ->
                        val scale = targetPx.toFloat() / maxOf(page.width, page.height)
                        val w = (page.width * scale).toInt().coerceAtLeast(1)
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeAudioArt(file: File, targetPx: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            val bytes = r.embeddedPicture ?: return null
            decodeSampledBytes(bytes, targetPx)
        } catch (_: Exception) {
            null
        } finally {
            try { r.release() } catch (_: Exception) { }
        }
    }

    private fun decodeZipEmbeddedThumbnail(file: File, targetPx: Int): Bitmap? {
        return try {
            java.util.zip.ZipFile(file).use { zip ->
                val entry = zip.entries().asSequence().firstOrNull {
                    val n = it.name.lowercase()
                    n == "docprops/thumbnail.jpeg" || n == "docprops/thumbnail.jpg" ||
                        n == "docprops/thumbnail.png" || n == "thumbnails/thumbnail.png"
                } ?: return null
                if (entry.size > 4 * 1024 * 1024) return null
                decodeSampledBytes(zip.getInputStream(entry).use { it.readBytesLimited(4L * 1024 * 1024) }, targetPx)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBytes(bytes: ByteArray, targetPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > targetPx * 2 || bounds.outHeight / sample > targetPx * 2) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
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
    showBadge: Boolean = true,
    showBorder: Boolean = true
) {
    val context = LocalContext.current
    val cacheKey = "${item.protocol}:${item.path}:${item.sizeBytes}:${item.lastModified}:280"
    var bitmap by remember(cacheKey) { mutableStateOf(ThumbnailCache.get(cacheKey)) }
    var loadAttempted by remember(cacheKey) { mutableStateOf(bitmap != null) }

    val canHaveVisualThumb = !item.isDirectory && item.sizeBytes > 0L && (
        item.category == FileCategory.IMAGE ||
            item.category == FileCategory.VIDEO ||
            item.category == FileCategory.APK ||
            (item.protocol == StorageProtocol.LOCAL && (
                item.category == FileCategory.AUDIO ||
                    item.extension == "pdf" ||
                    item.extension in ThumbnailCache.OFFICE_THUMB_EXTS
                ))
        )

    LaunchedEffect(cacheKey) {
        if (bitmap == null && canHaveVisualThumb) {
            bitmap = ThumbnailCache.loadThumbnail(context, item, vfsManager, connection, 280)
            loadAttempted = true
        }
    }

    val shape = RoundedCornerShape(cornerRadius)
    val typeStyle = fileTypeStyle(item)
    val categoryColor = typeStyle.color

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (bitmap != null) ConsoleBg else categoryColor.copy(alpha = 0.16f))
            .then(
                if (showBorder) Modifier.border(1.dp, if (bitmap != null) BorderDark else categoryColor.copy(alpha = 0.28f), shape)
                else Modifier
            ),
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
            val icon = typeStyle.icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.fillMaxSize(0.55f).widthIn(max = 48.dp)
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
                    color = if (item.sizeBytes == 0L) MediaDanger else Color.White,
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
    QubitExplorerTheme {
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


/** Ícono y color según el tipo de archivo, para distinguir carpetas, PDF, Office, etc. de un vistazo. */
data class FileTypeStyle(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    @androidx.annotation.StringRes val labelRes: Int
)

fun fileTypeStyle(item: VfsItem): FileTypeStyle {
    val i = Icons.Default
    if (item.isDirectory) return FileTypeStyle(i.Folder, Color(0xFF5B5BD6), R.string.media_preview_carpeta)
    if (item.sizeBytes == 0L && (item.category == FileCategory.VIDEO || item.category == FileCategory.IMAGE)) {
        return FileTypeStyle(i.BrokenImage, DangerRedLight, R.string.file_type_vacio)
    }
    return when (item.extension) {
        "pdf" -> FileTypeStyle(i.PictureAsPdf, Color(0xFFE5392F), R.string.file_type_pdf)
        "doc", "docx", "odt", "rtf" -> FileTypeStyle(i.Description, Color(0xFF2B6CD9), R.string.file_type_texto)
        "xls", "xlsx", "ods", "csv", "tsv" -> FileTypeStyle(i.TableChart, Color(0xFF1E9E5A), R.string.file_type_planilla)
        "ppt", "pptx", "odp", "key" -> FileTypeStyle(i.Slideshow, Color(0xFFE8711C), R.string.file_type_presentacion)
        "txt", "md", "log", "ini", "cfg", "conf" -> FileTypeStyle(i.Article, Color(0xFF6B7A90), R.string.file_type_texto_plano)
        "html", "htm", "xml", "json", "yaml", "yml", "js", "ts", "kt", "java", "py", "c", "cpp", "h",
        "cs", "go", "rs", "sh", "bat", "ps1", "css", "sql", "php", "rb", "swift" ->
            FileTypeStyle(i.Code, Color(0xFF0F9DA8), R.string.file_type_codigo)
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst" -> FileTypeStyle(i.Archive, Color(0xFFC99212), R.string.file_type_comprimido)
        "epub", "mobi", "azw3", "fb2" -> FileTypeStyle(i.MenuBook, Color(0xFF8E5CD6), R.string.file_type_libro)
        "ttf", "otf", "woff", "woff2" -> FileTypeStyle(i.FontDownload, Color(0xFF7A6F5A), R.string.file_type_fuente)
        "iso", "img", "dmg" -> FileTypeStyle(i.Album, Color(0xFF5A6B7A), R.string.file_type_imagen_de_disco)
        "exe", "msi" -> FileTypeStyle(i.Terminal, Color(0xFF52606D), R.string.file_type_ejecutable)
        "db", "sqlite", "sqlite3" -> FileTypeStyle(i.Storage, Color(0xFF3C7DA8), R.string.file_type_base_de_datos)
        else -> when (item.category) {
            FileCategory.IMAGE -> FileTypeStyle(i.Image, Color(0xFF2FA866), R.string.file_type_imagen)
            FileCategory.VIDEO -> FileTypeStyle(i.VideoFile, Color(0xFF9B4DE0), R.string.file_type_video)
            FileCategory.AUDIO -> FileTypeStyle(i.AudioFile, Color(0xFFE0457B), R.string.file_type_audio)
            FileCategory.APK -> FileTypeStyle(i.Android, Color(0xFF3DAA4F), R.string.file_type_apk)
            FileCategory.ARCHIVE -> FileTypeStyle(i.Archive, Color(0xFFC99212), R.string.file_type_comprimido)
            FileCategory.DOCUMENT -> FileTypeStyle(i.Description, Color(0xFF2B6CD9), R.string.file_type_documento)
            else -> FileTypeStyle(Icons.AutoMirrored.Filled.InsertDriveFile, TextSecondary, R.string.file_type_archivo)
        }
    }
}
