package com.maez.qubitexplorer.ui.components

import com.maez.qubitexplorer.R
import androidx.compose.ui.res.stringResource
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Abre el PDF una vez y renderiza páginas bajo demanda (PdfRenderer admite una página abierta a la vez). */
private class PdfDocumentHolder(file: File) {
    private val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val lock = Mutex()
    val pageCount: Int = renderer.pageCount
    val aspectRatios: List<Float> = (0 until pageCount).map { i ->
        renderer.openPage(i).use { it.width.toFloat() / it.height.coerceAtLeast(1) }
    }

    suspend fun render(index: Int, widthPx: Int): ImageBitmap = lock.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(index).use { page ->
                val w = widthPx.coerceIn(200, 2400)
                val h = (w * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp.asImageBitmap()
            }
        }
    }

    /** Texto de la página (PdfRenderer lo expone desde Android 15 o con la extensión de SDK 13). */
    @android.annotation.SuppressLint("NewApi")
    suspend fun text(index: Int): String? = lock.withLock {
        if (!pdfTextSupported()) return@withLock null
        withContext(Dispatchers.IO) {
            runCatching {
                renderer.openPage(index).use { page -> page.textContents.joinToString("\n") { it.text } }
            }.getOrNull()
        }
    }

    fun close() {
        runCatching { renderer.close() }
        runCatching { pfd.close() }
    }
}

fun pdfTextSupported(): Boolean =
    android.os.Build.VERSION.SDK_INT >= 35 ||
        (android.os.Build.VERSION.SDK_INT >= 31 &&
            android.os.ext.SdkExtensions.getExtensionVersion(android.os.Build.VERSION_CODES.S) >= 13)

@Composable
fun PdfViewerScreen(
    fileName: String,
    localPath: String?,
    isLoading: Boolean,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler { onClose() }
        var doc by remember { mutableStateOf<PdfDocumentHolder?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(localPath) {
            if (localPath == null) return@LaunchedEffect
            try {
                doc = withContext(Dispatchers.IO) { PdfDocumentHolder(File(localPath)) }
            } catch (e: SecurityException) {
                error = context.getString(R.string.pdf_viewer_el_pdf_esta_protegido_con)
            } catch (e: Exception) {
                error = context.getString(R.string.pdf_viewer_no_se_pudo_abrir_el, e.javaClass.simpleName)
            }
        }
        DisposableEffect(doc) {
            val opened = doc // capturar la instancia; leer el estado en onDispose cerraría la nueva
            onDispose { opened?.close() }
        }

        val listState = rememberLazyListState()
        val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
        var scale by remember { mutableFloatStateOf(1f) }
        var textMode by remember { mutableStateOf(false) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF2A2C33))
                .statusBarsPadding()
                .padding(bottom = activityNavBarBottomPadding())
                .imePadding()
        ) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF1C1D22)).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.close), tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(fileName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    doc?.let { Text(stringResource(R.string.pdf_viewer_pagina_de, currentPage, it.pageCount), color = Color(0xFFB0B4C0), fontSize = 11.sp) }
                }
                IconButton(onClick = { textMode = !textMode }) {
                    Icon(
                        Icons.Default.TextFields, stringResource(R.string.pdf_viewer_seleccionar_texto),
                        tint = if (textMode) Color(0xFF82AAFF) else Color.White
                    )
                }
                if (!textMode) IconButton(onClick = { scale = (scale / 1.25f).coerceAtLeast(1f); if (scale == 1f) offset = Offset.Zero }) {
                    Icon(Icons.Default.ZoomOut, stringResource(R.string.pdf_viewer_alejar), tint = Color.White)
                }
                if (!textMode) IconButton(onClick = { scale = (scale * 1.25f).coerceAtMost(5f) }) {
                    Icon(Icons.Default.ZoomIn, stringResource(R.string.pdf_viewer_acercar), tint = Color.White)
                }
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, stringResource(R.string.explorer_compartir), tint = Color.White) }
                IconButton(onClick = onOpenExternal) { Icon(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.office_viewer_abrir_con_otra_app), tint = Color.White) }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val d = doc
                when {
                    error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error!!, color = Color.White, modifier = Modifier.padding(24.dp))
                        TextButton(onClick = onOpenExternal) { Text(stringResource(R.string.office_viewer_abrir_con_otra_app)) }
                    }
                    isLoading || d == null -> CircularProgressIndicator(color = Color.White)
                    textMode -> PdfTextView(d)
                    else -> BoxWithConstraints(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.2f
                                })
                            }
                            .pointerInput(Unit) {
                                // Solo gestos de dos dedos: un dedo sigue desplazando la lista
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        if (event.changes.count { it.pressed } >= 2) {
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            scale = (scale * zoom).coerceIn(1f, 5f)
                                            offset = if (scale == 1f) Offset.Zero else {
                                                val maxX = size.width * (scale - 1) / 2
                                                Offset((offset.x + pan.x).coerceIn(-maxX, maxX), 0f)
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                    ) {
                        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale; scaleY = scale
                                    translationX = offset.x
                                }
                        ) {
                            items(d.pageCount) { index ->
                                PdfPage(d, index, widthPx)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPage(doc: PdfDocumentHolder, index: Int, widthPx: Int) {
    var bitmap by remember(index) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(index, widthPx) {
        // Se renderiza al doble para que el zoom se vea nítido
        bitmap = runCatching { doc.render(index, widthPx * 2) }.getOrNull()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(doc.aspectRatios[index])
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(it, contentDescription = stringResource(R.string.pdf_viewer_pagina, index + 1), contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxSize())
        } ?: CircularProgressIndicator(color = Color(0xFF9AA0AE))
    }
}


/** Vista de texto del PDF: seleccionable y copiable, página por página. */
@Composable
private fun PdfTextView(doc: PdfDocumentHolder) {
    if (!pdfTextSupported()) {
        Text(stringResource(R.string.pdf_viewer_seleccionar_texto_de_un_pdf),
            color = Color.White, modifier = Modifier.padding(24.dp)
        )
        return
    }
    androidx.compose.foundation.text.selection.SelectionContainer(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(doc.pageCount) { index ->
                var text by remember(index) { mutableStateOf<String?>(null) }
                LaunchedEffect(index) { text = doc.text(index) ?: "" }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.pdf_viewer_pagina_2, index + 1), color = Color(0xFF8A8FA3), fontSize = 11.sp)
                    when {
                        text == null -> CircularProgressIndicator(Modifier.padding(8.dp), color = Color(0xFF9AA0AE))
                        text!!.isBlank() -> Text(stringResource(R.string.pdf_viewer_sin_texto_la_pagina_puede), color = Color(0xFF8A8FA3), fontSize = 13.sp)
                        else -> Text(text!!, color = Color(0xFF1F2330), fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
