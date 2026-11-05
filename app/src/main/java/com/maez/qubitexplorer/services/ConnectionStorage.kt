package com.maez.qubitexplorer.services

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import com.maez.qubitexplorer.model.NetworkConnection
import com.maez.qubitexplorer.model.StorageProtocol
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ConnectionStorage(context: Context) {

    private val prefs = context.getSharedPreferences("filemanager_connections", Context.MODE_PRIVATE)

    companion object {
        const val TAB_MEMORY_TIMEOUT_MS = 20 * 60 * 1000L
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "filemanager_pw_key"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        // Prefijos de contraseña
        private const val ENC2_PREFIX = "ENC2:"
        private const val LEGACY_ENC_PREFIX_1 = "enc_v1:"
        private const val LEGACY_ENC_PREFIX_2 = "ENC:"

        // Clave legacy para descifrado XOR histórico
        private const val LEGACY_ENC_KEY = "FileManagerStudioSecretKey"
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE CLAVE EN ANDROID KEYSTORE (AES-256 GCM)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CIFRADO Y DESCIFRADO DE CONTRASEÑAS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Cifra una contraseña en texto plano utilizando AES-256 GCM con una clave almacenada
     * en el Android Keystore y un IV aleatorio de 12 bytes.
     * Retorna "ENC2:" + base64(iv + ciphertext).
     */
    private fun encryptPassword(plain: String): String {
        if (plain.isEmpty()) return ""
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv // 12 bytes por defecto en GCM
            val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            ENC2_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Si la clave se invalida (ej. cambio de bloqueo de pantalla), borrarla y fallar
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            ks.deleteEntry(KEY_ALIAS)
            throw IllegalStateException("Clave de cifrado invalidada. Por favor, ingresa la contraseña nuevamente.")
        } catch (e: Exception) {
            throw IllegalStateException("Error al cifrar la contraseña: ${e.message}")
        }
    }

    /**
     * Descifra una contraseña. Admite tres formatos:
     * 1. Nuevo: "ENC2:" (AES/GCM/NoPadding con Android Keystore).
     * 2. Legado: "enc_v1:" o "ENC:" (XOR reversible histórico).
     * 3. Texto plano directo.
     * Si ocurre un fallo en el Keystore, devuelve cadena vacía sin crashear para que la UI vuelva a solicitarla.
     */
    private fun decryptPassword(encrypted: String): String {
        if (encrypted.isEmpty()) return ""

        // Formato actual: AES-256 GCM con Android Keystore
        if (encrypted.startsWith(ENC2_PREFIX)) {
            return try {
                val combined = Base64.decode(encrypted.substring(ENC2_PREFIX.length), Base64.NO_WRAP)
                if (combined.size < GCM_IV_LENGTH) return ""
                val iv = ByteArray(GCM_IV_LENGTH)
                val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
                System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
                System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val plainBytes = cipher.doFinal(cipherText)
                String(plainBytes, Charsets.UTF_8)
            } catch (e: KeyPermanentlyInvalidatedException) {
                ""
            } catch (_: Exception) {
                ""
            }
        }

        // Formato legado: XOR histórico ("enc_v1:" o "ENC:")
        if (encrypted.startsWith(LEGACY_ENC_PREFIX_1)) {
            return decryptLegacyXor(encrypted.substring(LEGACY_ENC_PREFIX_1.length))
        }
        if (encrypted.startsWith(LEGACY_ENC_PREFIX_2)) {
            return decryptLegacyXor(encrypted.substring(LEGACY_ENC_PREFIX_2.length))
        }

        // Texto plano guardado anteriormente
        return encrypted
    }

    /**
     * Descifrado de respaldo para contraseñas históricas codificadas con XOR + Base64.
     * Aislada y documentada para migración transparente.
     */
    private fun decryptLegacyXor(base64Payload: String): String {
        return try {
            val xored = Base64.decode(base64Payload, Base64.NO_WRAP)
            val keyBytes = LEGACY_ENC_KEY.toByteArray(Charsets.UTF_8)
            val bytes = ByteArray(xored.size)
            for (i in xored.indices) {
                bytes[i] = (xored[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PERSISTENCIA DE CONEXIONES DE RED
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Carga las conexiones guardadas.
     * Si alguna contraseña estaba en formato legado ("ENC:", "enc_v1:" o texto plano),
     * migra y reescribe inmediatamente el archivo guardando con formato "ENC2:".
     */
    fun loadConnections(): List<NetworkConnection> {
        val raw = prefs.getString("saved_connections", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<NetworkConnection>()
            var needsMigration = false

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawPassword = obj.optString("password", "")

                if (rawPassword.isNotEmpty() && !rawPassword.startsWith(ENC2_PREFIX)) {
                    needsMigration = true
                }

                list.add(
                    NetworkConnection(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        protocol = StorageProtocol.valueOf(obj.optString("protocol", StorageProtocol.SMB.name)),
                        host = obj.getString("host"),
                        port = obj.optInt("port", 445),
                        shareName = obj.optString("shareName", ""),
                        username = obj.optString("username", ""),
                        password = decryptPassword(rawPassword),
                        anonymous = obj.optBoolean("anonymous", false),
                        initialPath = obj.optString("initialPath", "/"),
                        useTls = obj.optBoolean("useTls", false),
                        lastUsedAt = obj.optLong("lastUsedAt", 0L)
                    )
                )
            }

            if (needsMigration) {
                saveConnections(list)
            }

            list.sortedByDescending { it.lastUsedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveConnections(connections: List<NetworkConnection>) {
        val arr = JSONArray()
        for (c in connections) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("protocol", c.protocol.name)
                put("host", c.host)
                put("port", c.port)
                put("shareName", c.shareName)
                put("username", c.username)
                try {
                    put("password", encryptPassword(c.password))
                } catch (e: Exception) {
                    put("password", "")
                    // Se podría notificar a la UI aquí si tuviéramos un callback,
                    // por ahora guardamos vacío para no perder el resto de la conexión
                }
                put("anonymous", c.anonymous)
                put("initialPath", c.initialPath)
                put("useTls", c.useTls)
                put("lastUsedAt", c.lastUsedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_connections", arr.toString()).apply()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PERSISTENCIA DE AJUSTES GENERALES DEL USUARIO
    // ─────────────────────────────────────────────────────────────────────────────

    fun getShowHiddenFiles(): Boolean = prefs.getBoolean("pref_show_hidden_files", false)
    fun setShowHiddenFiles(value: Boolean) = prefs.edit().putBoolean("pref_show_hidden_files", value).apply()

    fun getDefaultGridView(): Boolean = prefs.getBoolean("pref_default_grid_view", false)
    fun setDefaultGridView(value: Boolean) = prefs.edit().putBoolean("pref_default_grid_view", value).apply()

    fun getConfirmBeforeDelete(): Boolean = prefs.getBoolean("pref_confirm_before_delete", true)
    fun setConfirmBeforeDelete(value: Boolean) = prefs.edit().putBoolean("pref_confirm_before_delete", value).apply()

    fun getAutoClearRemoteCache(): Boolean = prefs.getBoolean("pref_auto_clear_remote_cache", true)
    fun setAutoClearRemoteCache(value: Boolean) = prefs.edit().putBoolean("pref_auto_clear_remote_cache", value).apply()

    /** Opciones: 0 = Nunca, 5 = 5 min, 20 = 20 min (default), -1 = Siempre */
    fun getTabMemoryTimeoutMinutes(): Int = prefs.getInt("pref_tab_memory_timeout_minutes", 20)
    fun setTabMemoryTimeoutMinutes(value: Int) = prefs.edit().putInt("pref_tab_memory_timeout_minutes", value).apply()

    private fun getTimeoutMs(): Long {
        val mins = getTabMemoryTimeoutMinutes()
        return when (mins) {
            -1 -> Long.MAX_VALUE
            0 -> 0L
            else -> mins * 60 * 1000L
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE MEMORIA DE PESTAÑAS
    // ─────────────────────────────────────────────────────────────────────────────

    fun saveTabLastPath(tabKey: String, path: String) {
        prefs.edit()
            .putString("tab_path_$tabKey", path)
            .putLong("tab_ts_$tabKey", System.currentTimeMillis())
            .apply()
    }

    fun getTabLastPath(tabKey: String, rootPath: String): String {
        val timeoutMs = getTimeoutMs()
        if (timeoutMs == 0L) return rootPath
        val savedPath = prefs.getString("tab_path_$tabKey", null) ?: return rootPath
        val ts = prefs.getLong("tab_ts_$tabKey", 0L)
        if (timeoutMs != Long.MAX_VALUE && System.currentTimeMillis() - ts > timeoutMs) {
            clearTabLastPath(tabKey)
            return rootPath
        }
        return savedPath
    }

    fun clearTabLastPath(tabKey: String) {
        prefs.edit()
            .remove("tab_path_$tabKey")
            .remove("tab_ts_$tabKey")
            .apply()
    }

    fun loadAllValidTabPaths(): Map<String, String> {
        val timeoutMs = getTimeoutMs()
        if (timeoutMs == 0L) return emptyMap()
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, String>()
        val all = prefs.all
        for ((key, value) in all) {
            if (key.startsWith("tab_path_") && value is String) {
                val tabKey = key.removePrefix("tab_path_")
                val ts = prefs.getLong("tab_ts_$tabKey", 0L)
                if (timeoutMs == Long.MAX_VALUE || now - ts <= timeoutMs) {
                    result[tabKey] = value
                } else {
                    clearTabLastPath(tabKey)
                }
            }
        }
        return result
    }
}
