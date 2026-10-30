package com.maez.filemanager.services

import android.content.Context
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.StorageProtocol
import org.json.JSONArray
import org.json.JSONObject

class ConnectionStorage(context: Context) {

    private val prefs = context.getSharedPreferences("filemanager_connections", Context.MODE_PRIVATE)

    companion object {
        const val TAB_MEMORY_TIMEOUT_MS = 20 * 60 * 1000L // 20 minutos
    }

    fun loadConnections(): List<NetworkConnection> {
        val raw = prefs.getString("saved_connections", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<NetworkConnection>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NetworkConnection(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        protocol = StorageProtocol.valueOf(obj.optString("protocol", StorageProtocol.SMB.name)),
                        host = obj.getString("host"),
                        port = obj.optInt("port", 445),
                        shareName = obj.optString("shareName", ""),
                        username = obj.optString("username", ""),
                        password = obj.optString("password", ""),
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
                put("password", c.password)
                put("anonymous", c.anonymous)
                put("initialPath", c.initialPath)
                put("lastUsedAt", c.lastUsedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_connections", arr.toString()).apply()
    }

    fun saveTabLastPath(tabKey: String, path: String) {
        prefs.edit()
            .putString("tab_path_$tabKey", path)
            .putLong("tab_ts_$tabKey", System.currentTimeMillis())
            .apply()
    }

    fun getTabLastPath(tabKey: String, rootPath: String): String {
        val savedPath = prefs.getString("tab_path_$tabKey", null) ?: return rootPath
        val ts = prefs.getLong("tab_ts_$tabKey", 0L)
        if (System.currentTimeMillis() - ts > TAB_MEMORY_TIMEOUT_MS) {
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
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, String>()
        val all = prefs.all
        for ((key, value) in all) {
            if (key.startsWith("tab_path_") && value is String) {
                val tabKey = key.removePrefix("tab_path_")
                val ts = prefs.getLong("tab_ts_$tabKey", 0L)
                if (now - ts <= TAB_MEMORY_TIMEOUT_MS) {
                    result[tabKey] = value
                } else {
                    clearTabLastPath(tabKey)
                }
            }
        }
        return result
    }
}
