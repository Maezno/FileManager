package com.maez.qubitexplorer.services

import android.content.Context
import androidx.annotation.StringRes
import com.maez.qubitexplorer.R

/**
 * Error con mensaje para la persona: lleva el id del texto y sus argumentos, así los servicios
 * no necesitan Context y la UI decide por [needsCredentials] en vez de buscar palabras en el texto.
 */
class AppException(
    @param:StringRes val resId: Int,
    vararg val args: Any,
    val needsCredentials: Boolean = false,
    val passwordProtected: Boolean = false,
    cause: Throwable? = null
) : IllegalStateException(cause?.message ?: "AppException($resId)", cause)

/** Mensaje para mostrar: traducido si es un [AppException]; si no, el de la excepción (puede faltar). */
fun Throwable.userMessage(context: Context): String? =
    if (this is AppException) context.getString(resId, *args) else localizedMessage

val Throwable.requiresCredentials: Boolean
    get() = this is AppException && needsCredentials

/** Convierte un error SMB técnico en uno con mensaje entendible. */
fun smbError(e: Exception, host: String): AppException {
    if (e is AppException) return e
    val msg = e.message ?: e.toString()
    fun has(vararg codes: String) = codes.any { msg.contains(it, ignoreCase = true) }
    return when {
        has("STATUS_LOGON_FAILURE", "0xc000006d", "Logon failure") ->
            AppException(R.string.smb_error_logon, host, needsCredentials = true, cause = e)
        has("STATUS_ACCESS_DENIED", "0xc0000022", "Access is denied") ->
            AppException(R.string.smb_error_access_denied, host, needsCredentials = true, cause = e)
        has("STATUS_BAD_NETWORK_NAME", "0xc00000cc") ->
            AppException(R.string.smb_error_bad_share, host, cause = e)
        has("STATUS_ACCOUNT_RESTRICTION", "0xc000006e") ->
            AppException(R.string.smb_error_account_restriction, host, needsCredentials = true, cause = e)
        else -> AppException(
            R.string.smb_error_generic, host, msg,
            needsCredentials = e is jcifs.smb.SmbAuthException,
            cause = e
        )
    }
}
