package com.maez.qubitexplorer.services

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/** Convierte las URIs que envían otras apps ("abrir ubicación") en rutas locales. */
object ExternalPathResolver {

    fun resolve(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> uri.path
            ContentResolver.SCHEME_CONTENT -> resolveContent(context, uri)
            null -> uri.path
            else -> null
        }?.takeIf { File(it).exists() }
    }

    private fun resolveContent(context: Context, uri: Uri): String? {
        // Nuestro propio proveedor: el id del documento ya es la ruta
        if (uri.authority == "${context.packageName}.documents") {
            return runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
                ?: runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        }

        val docId = runCatching {
            if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.getDocumentId(uri)
            else DocumentsContract.getTreeDocumentId(uri)
        }.getOrNull()

        if (docId != null && uri.authority == "com.android.externalstorage.documents") {
            val (volume, rel) = docId.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val base = if (volume.equals("primary", true)) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                "/storage/$volume"
            }
            return if (rel.isEmpty()) base else "$base/$rel"
        }

        if (docId != null && uri.authority == "com.android.providers.downloads.documents" &&
            docId.startsWith("raw:")
        ) return docId.removePrefix("raw:")

        // MediaStore u otros: intentar la columna _data
        return runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
            // Algunas apps mandan content://<authority>/<ruta absoluta>
            ?: uri.path?.let { p ->
                generateSequence(p) { it.substringAfter('/', "").takeIf { s -> s.isNotEmpty() && s != it } }
                    .map { if (it.startsWith("/")) it else "/$it" }
                    .firstOrNull { File(it).exists() && it.startsWith("/storage") }
            }
    }
}
