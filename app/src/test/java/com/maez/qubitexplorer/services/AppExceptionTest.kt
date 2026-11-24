package com.maez.qubitexplorer.services

import com.maez.qubitexplorer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppExceptionTest {

    private fun translate(msg: String) = smbError(IllegalStateException(msg), "pc")

    @Test
    fun credencialesRechazadasPidenCredenciales() {
        val e = translate("STATUS_LOGON_FAILURE (0xc000006d)")
        assertEquals(R.string.smb_error_logon, e.resId)
        assertTrue(e.requiresCredentials)
        assertTrue(translate("Access is denied.").requiresCredentials)
        assertTrue(translate("status_account_restriction").requiresCredentials)
    }

    @Test
    fun otrosErroresNoPidenCredenciales() {
        assertFalse(translate("STATUS_BAD_NETWORK_NAME").requiresCredentials)
        val generic = translate("Connection timed out")
        assertEquals(R.string.smb_error_generic, generic.resId)
        assertFalse(generic.requiresCredentials)
        assertFalse(IllegalStateException("acceso").requiresCredentials)
    }

    @Test
    fun unErrorYaTraducidoNoSeEnvuelve() {
        val e = AppException(R.string.smb_error_bad_share, "pc")
        assertSame(e, smbError(e, "pc"))
    }
}
