package com.maez.filemanager.ui.components

import android.graphics.BitmapFactory
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maez.filemanager.model.FileCategory
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import com.maez.filemanager.ui.theme.AccentBlueLight
import com.maez.filemanager.ui.theme.AccentGreenLight
import com.maez.filemanager.ui.theme.BorderDark
import com.maez.filemanager.ui.theme.CardDark
import com.maez.filemanager.ui.theme.ConsoleBg
import com.maez.filemanager.ui.theme.DangerRedLight
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Diálogo de vista previa integrada para Fotos y Videos (estilo MediaRecoveryMobile).
 */
@Composable
fun MediaPreviewDialog(
    item: VfsItem,
    localPreviewPath: String?,
    isLoadingRemote: Boolean,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onOpenFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    var errorMsg by remember(localPreviewPath) { mutableStateOf<String?>(null) }
    var imageBitmap by remember(localPreviewPath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(localPreviewPath, item.category) {
        if (localPreviewPath != null && item.category == FileCategory.IMAGE) {
            imageBitmap = withContext(Dispatchers.IO) {
                try {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                    BitmapFactory.decodeFile(localPreviewPath, opts)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            if (imageBitmap == null) {
                errorMsg = "No se pudo decodificar la imagen."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = CardDark,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.sizeFormatted} • ${item.parentPath}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ConsoleBg),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingRemote -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = AccentGreenLight)
                                Text("Cargando vista previa desde red...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        item.sizeBytes == 0L -> {
                            Text(
                                text = "Archivo vacío (0 B) — Sin contenido para previsualizar",
                                color = DangerRedLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        errorMsg != null || localPreviewPath == null -> {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No se pudo mostrar la vista previa integrada",
                                    color = DangerRedLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = errorMsg ?: "Puedes abrir el archivo con un reproductor externo.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        item.category == FileCategory.IMAGE -> {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap!!,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                CircularProgressIndicator(color = AccentBlueLight)
                            }
                        }
                        item.category == FileCategory.VIDEO || item.category == FileCategory.AUDIO -> {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val controller = MediaController(ctx)
                                        controller.setAnchorView(this)
                                        setMediaController(controller)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = false
                                            start()
                                        }
                                        setOnErrorListener { _, what, extra ->
                                            errorMsg = "El contenedor requiere un reproductor externo (código $what/$extra)."
                                            true
                                        }
                                        setVideoPath(localPreviewPath)
                                        requestFocus()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Carpeta",
                        icon = Icons.Default.FolderOpen,
                        onClick = onOpenFolder,
                        tint = AccentBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Compartir",
                        icon = Icons.Default.Share,
                        onClick = onShare,
                        tint = AccentBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Abrir",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = onOpenExternal,
                        tint = AccentGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaPreviewDialogPreview() {
    FileManagerTheme {
        MediaPreviewDialog(
            item = VfsItem(
                name = "Foto_Playa.jpg",
                path = "/storage/emulated/0/DCIM/Foto_Playa.jpg",
                parentPath = "/storage/emulated/0/DCIM",
                sizeBytes = 4_200_000,
                lastModified = System.currentTimeMillis(),
                isDirectory = false,
                protocol = StorageProtocol.LOCAL
            ),
            localPreviewPath = null,
            isLoadingRemote = false,
            onOpenExternal = {},
            onShare = {},
            onOpenFolder = {},
            onDismiss = {}
        )
    }
}
