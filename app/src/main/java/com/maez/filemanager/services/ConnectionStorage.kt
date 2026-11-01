package com.maez.filemanager.services

import android.content.Context
import android.util.Base64
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.StorageProtocol
import org.json.JSONArray
import org.json.JSONObject

class ConnectionStorage(context: Context) {

    private val prefs = context.getSharedPreferences("filemanager_connections", Context.MODE_PRIVATE)

    companion object {
        const val TAB_MEMORY_TIMEOUT_MS = 20 * 60 * 1000L
        private const val ENC_PREFIX = "enc_v1:"
        private const val ENC_KEY = "FileManagerStudioSecretKey"
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CIFRADO Y OFUSCACIÓN BÁSICA DE CONTRASEÑAS (XOR + BASE64 CON COMPATIBILIDAD)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun encryptPassword(plain: String): String {
        if (plain.isEmpty()) return ""
        return try {
            val bytes = plain.toByteArray(Charsets.UTF_8)
            val keyBytes = ENC_KEY.toByteArray(Charsets.UTF_8)
            val xored = ByteArray(bytes.size)
            for (i in bytes.indices) {
                xored[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            ENC_PREFIX + Base64.encodeToString(xored, Base64.NO_WRAP)
        } catch (_: Exception) {
            plain
        }
    }

    private fun decryptPassword(encrypted: String): String {
        if (encrypted.isEmpty()) return ""
        if (!encrypted.startsWith(ENC_PREFIX)) {
            // Compatibilidad hacia atrás con texto plano guardado anteriormente
            return encrypted
        }
        return try {
            val base64Str = encrypted.substring(ENC_PREFIX.length)
            val xored = Base64.decode(base64Str, Base64.NO_WRAP)
            val keyBytes = ENC_KEY.toByteArray(Charsets.UTF_8)
            val bytes = ByteArray(xored.size)
            for (i in xored.indices) {
                bytes[i] = (xored[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            encrypted
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PERSISTENCIA DE CONEXIONES DE RED
    // ─────────────────────────────────────────────────────────────────────────────

    fun loadConnections(): List<NetworkConnection> {
        val raw = prefs.getString("saved_connections", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<NetworkConnection>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawPassword = obj.optString("password", "")
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
                        lastUsedAt = obj.optLong("lastUsedAt", 0L)
                    )
                )
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
                put("password", encryptPassword(c.password))
                put("anonymous", c.anonymous)
                put("initialPath", c.initialPath)
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

    fun getBlurEnabled(): Boolean =
        prefs.getBoolean("blur_enabled", android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)

    fun setBlurEnabled(value: Boolean) {
        prefs.edit().putBoolean("blur_enabled", value).apply()
    }

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
