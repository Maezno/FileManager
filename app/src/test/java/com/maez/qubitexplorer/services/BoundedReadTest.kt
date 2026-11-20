package com.maez.qubitexplorer.services

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class BoundedReadTest {
    @Test
    fun leeCompletoDentroDelLimite() {
        val data = ByteArray(100_000) { it.toByte() }
        assertEquals(data.toList(), ByteArrayInputStream(data).readBytesLimited(100_000).toList())
    }

    @Test(expected = IOException::class)
    fun cortaAlSuperarElLimite() {
        ByteArrayInputStream(ByteArray(1001)).readBytesLimited(1000)
    }
}
