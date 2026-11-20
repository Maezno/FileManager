package com.maez.qubitexplorer.services

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Límite por defecto para partes internas de documentos/comprimidos leídas en memoria. */
const val MAX_EMBEDDED_PART_BYTES = 32L * 1024 * 1024

/**
 * Lee todo el flujo pero corta si supera [maxBytes]. A diferencia de confiar en el tamaño
 * declarado de una entrada ZIP (que puede mentir), esto protege de "zip bombs".
 */
fun InputStream.readBytesLimited(maxBytes: Long = MAX_EMBEDDED_PART_BYTES): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val n = read(buf)
        if (n < 0) break
        total += n
        if (total > maxBytes) throw IOException("Contenido demasiado grande (> $maxBytes bytes)")
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
