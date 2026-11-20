package com.maez.qubitexplorer.services

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveExtractorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun zip(name: String, entries: Map<String, String?>): File {
        val f = File(tmp.root, name)
        ZipOutputStream(f.outputStream()).use { z ->
            entries.forEach { (path, content) ->
                z.putNextEntry(ZipEntry(path))
                content?.let { z.write(it.toByteArray()) }
                z.closeEntry()
            }
        }
        return f
    }

    @Test
    fun isArchive_reconoceExtensionesSinImportarMayusculas() {
        assertTrue(ArchiveExtractor.isArchive("foto.ZIP"))
        assertTrue(ArchiveExtractor.isArchive("datos.tar.gz"))
        assertTrue(ArchiveExtractor.isArchive("comic.cbr"))
        assertFalse(ArchiveExtractor.isArchive("documento.pdf"))
        assertFalse(ArchiveExtractor.isArchive("sin_extension"))
    }

    @Test
    fun normalizeName_unificaSeparadoresYRecorta() {
        assertEquals("a/b/c.txt", ArchiveExtractor.normalizeName("a\\b\\c.txt"))
        assertEquals("carpeta", ArchiveExtractor.normalizeName("./carpeta/"))
        assertEquals("x.txt", ArchiveExtractor.normalizeName("/x.txt"))
    }

    @Test
    fun defaultDestination_quitaExtensionesYEvitaColisiones() {
        val tgz = File(tmp.root, "backup.tar.gz").apply { writeText("") }
        assertEquals("backup", ArchiveExtractor.defaultDestination(tgz).name)

        val z = File(tmp.root, "fotos.zip").apply { writeText("") }
        File(tmp.root, "fotos").mkdirs()
        assertEquals("fotos (2)", ArchiveExtractor.defaultDestination(z).name)
    }

    @Test
    fun list_devuelveEntradasDelZip() = runBlocking {
        val f = zip("a.zip", mapOf("dir/" to null, "dir/hola.txt" to "hola", "raiz.txt" to "x"))
        val listing = ArchiveExtractor.list(f)
        assertEquals(2, listing.fileCount)
        assertTrue(listing.entries.any { it.name.trimEnd('/') == "dir" && it.isDirectory })
    }

    @Test
    fun extract_escribeArchivosYRespetaFiltro() = runBlocking {
        val f = zip("b.zip", mapOf("uno.txt" to "1", "sub/dos.txt" to "22"))
        val dest = File(tmp.root, "out")
        assertEquals(2, ArchiveExtractor.extract(f, dest))
        assertEquals("22", File(dest, "sub/dos.txt").readText())

        val dest2 = File(tmp.root, "out2")
        ArchiveExtractor.extract(f, dest2, include = { it == "uno.txt" })
        assertTrue(File(dest2, "uno.txt").exists())
        assertFalse(File(dest2, "sub/dos.txt").exists())
    }

    @Test
    fun extract_bloqueaZipSlip() = runBlocking {
        val f = zip("malo.zip", mapOf("../../escapado.txt" to "pwn", "ok.txt" to "ok"))
        val dest = File(tmp.root, "seguro/dentro")
        ArchiveExtractor.extract(f, dest)
        assertTrue(File(dest, "ok.txt").exists())
        assertFalse(File(tmp.root, "escapado.txt").exists())
        assertFalse(File(tmp.root, "seguro/escapado.txt").exists())
    }
}
