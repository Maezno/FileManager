package com.maez.qubitexplorer.ui.components

import com.maez.qubitexplorer.R
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.qubitexplorer.model.NetworkConnection
import com.maez.qubitexplorer.model.VfsItem
import com.maez.qubitexplorer.services.FileMetadataReader
import com.maez.qubitexplorer.services.FileMetadataResult
import com.maez.qubitexplorer.services.VfsManager
import com.maez.qubitexplorer.ui.theme.Aqua
import com.maez.qubitexplorer.ui.theme.TextMuted
import com.maez.qubitexplorer.ui.theme.TextPrimary
import com.maez.qubitexplorer.ui.theme.Violet
import java.util.Locale

private fun copyText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, context.getString(R.string.properties_copiado, label), Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PropertiesDialog(
    item: VfsItem,
    vfsManager: VfsManager?,
    connection: NetworkConnection?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var result by remember(item.path) { mutableStateOf<FileMetadataResult?>(null) }

    LaunchedEffect(item.path) {
        result = FileMetadataReader.read(item, vfsManager, connection, context.cacheDir)
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileThumbnailBox(
                    item = item,
                    modifier = Modifier.size(36.dp),
                    vfsManager = vfsManager,
                    connection = connection
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.properties_propiedades), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(item.name, color = TextMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
        },
        text = {
            val r = result
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (r == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Violet)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.properties_leyendo_metadatos), color = TextMuted, fontSize = 13.sp)
                    }
                    return@Column
                }

                r.gps?.let { g ->
                    val coords = "%.6f, %.6f".format(Locale.US, g.latitude, g.longitude)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { copyText(context, "Coordenadas", coords) }) {
                            Icon(Icons.Default.ContentCopy, null, tint = Aqua, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.properties_copiar_coordenadas), color = Aqua, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            val uri = Uri.parse("geo:${g.latitude},${g.longitude}?q=${g.latitude},${g.longitude}")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                .onFailure {
                                    val web = Uri.parse("https://www.openstreetmap.org/?mlat=${g.latitude}&mlon=${g.longitude}#map=16/${g.latitude}/${g.longitude}")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, web)) }
                                }
                        }) {
                            Icon(Icons.Default.Map, null, tint = Aqua, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.properties_ver_en_mapa), color = Aqua, fontSize = 12.sp)
                        }
                    }
                }

                r.sections.forEach { section ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            section.title.uppercase(),
                            color = Violet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.properties_copiar_seccion, section.title),
                            tint = TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .combinedClickable(onClick = {
                                    copyText(
                                        context,
                                        section.title,
                                        section.entries.joinToString("\n") { "${it.label}: ${it.value}" }
                                    )
                                })
                        )
                    }
                    section.entries.forEach { e ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { copyText(context, e.label, e.value) }
                                )
                        ) {
                            Text(e.label, color = TextMuted, fontSize = 11.sp)
                            Text(
                                e.value,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = if (e.label in setOf("MD5", "SHA-256")) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
                Text(stringResource(R.string.properties_mantene_presionado_un_valor_para),
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = result != null,
                onClick = { result?.let { copyText(context, "Metadatos", it.asPlainText()) } }
            ) {
                Text(stringResource(R.string.properties_copiar_todo), color = Aqua, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = Violet, fontWeight = FontWeight.Bold)
            }
        }
    )
}
