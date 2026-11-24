package com.maez.qubitexplorer.services

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.maez.qubitexplorer.R
import java.io.File
import java.io.FileNotFoundException

/**
 * Publica el almacenamiento interno en el selector de archivos del sistema, para que
 * Qubit Explorer aparezca en "Guardar como" / "Abrir" de otras apps.
 * El id de cada documento es su ruta absoluta.
 */
class QubitDocumentsProvider : DocumentsProvider() {

    private val rootDir: File get() = Environment.getExternalStorageDirectory()

    private val defaultRootProjection = arrayOf(
        Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES
    )
    private val defaultDocProjection = arrayOf(
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE
    )

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: defaultRootProjection)
        val root = rootDir
        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, "qubit_primary")
            add(
                Root.COLUMN_FLAGS,
                Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_IS_CHILD or Root.FLAG_LOCAL_ONLY
            )
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(Root.COLUMN_TITLE, context?.getString(R.string.app_name) ?: "Qubit Explorer")
            add(Root.COLUMN_SUMMARY, context?.getString(R.string.storage_internal))
            add(Root.COLUMN_DOCUMENT_ID, root.absolutePath)
            add(Root.COLUMN_AVAILABLE_BYTES, root.usableSpace)
        }
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: defaultDocProjection)
        includeFile(result, fileFor(documentId))
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: defaultDocProjection)
        fileFor(parentDocumentId).listFiles()
            ?.filterNot { it.name.startsWith(".") }
            ?.forEach { includeFile(result, it) }
        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = fileFor(documentId)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val parent = fileFor(parentDocumentId)
        val target = uniqueFile(parent, displayName, mimeType)
        val ok = if (mimeType == Document.MIME_TYPE_DIR) target.mkdir() else target.createNewFile()
        if (!ok) throw FileNotFoundException("No se pudo crear ${target.absolutePath}")
        return target.absolutePath
    }

    override fun deleteDocument(documentId: String) {
        val file = fileFor(documentId)
        if (!file.deleteRecursively()) throw FileNotFoundException("No se pudo borrar $documentId")
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val file = fileFor(documentId)
        val target = inRoot(File(file.parentFile, safeName(displayName)))
        if (!file.renameTo(target)) throw FileNotFoundException("No se pudo renombrar $documentId")
        return target.absolutePath
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        val parent = runCatching { inRoot(File(parentDocumentId)) }.getOrNull() ?: return false
        val child = runCatching { inRoot(File(documentId)) }.getOrNull() ?: return false
        return child.path.startsWith(parent.path.trimEnd('/') + "/")
    }

    override fun getDocumentType(documentId: String): String = mimeOf(fileFor(documentId))

    // ─────────────────────────────────────────────────────────────────────────

    private fun fileFor(documentId: String): File {
        val file = inRoot(File(documentId))
        if (!file.exists()) throw FileNotFoundException("No existe: $documentId")
        return file
    }

    /**
     * Resuelve la ruta canónica y exige que quede dentro del almacenamiento expuesto.
     * Evita que ids con "../" (o symlinks) lleguen a los datos privados de la app.
     */
    private fun inRoot(file: File): File {
        val canonical = file.canonicalFile
        val root = rootDir.canonicalFile
        if (canonical != root && !canonical.path.startsWith(root.path.trimEnd('/') + "/")) {
            throw FileNotFoundException("Fuera del almacenamiento permitido")
        }
        return canonical
    }

    /** Un nombre nunca puede contener separadores ni ser "." / "..". */
    private fun safeName(displayName: String): String {
        val n = displayName.filterNot { it == '\u0000' }.replace('/', '_').replace('\\', '_').trim()
        if (n.isEmpty() || n == "." || n == "..") throw FileNotFoundException("Nombre no válido")
        return n
    }

    private fun uniqueFile(parent: File, displayName: String, mimeType: String): File {
        var name = safeName(displayName)
        if (mimeType != Document.MIME_TYPE_DIR && !name.contains('.')) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { name = "$name.$it" }
        }
        var candidate = inRoot(File(parent, name))
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty() || it == name) "" else ".$it" }
        var i = 1
        while (candidate.exists()) candidate = File(parent, "$base ($i)$ext").also { i++ }
        return candidate
    }

    private fun mimeOf(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun includeFile(cursor: MatrixCursor, file: File) {
        var flags = 0
        if (file.isDirectory) {
            if (file.canWrite()) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (file.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        if (file.parentFile?.canWrite() == true) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME
        }
        val mime = mimeOf(file)

        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, file.absolutePath)
            add(Document.COLUMN_DISPLAY_NAME, file.name.ifEmpty { file.absolutePath })
            add(Document.COLUMN_SIZE, if (file.isFile) file.length() else null)
            add(Document.COLUMN_MIME_TYPE, mime)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
        }
    }
}
