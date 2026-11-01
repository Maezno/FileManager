package com.maez.filemanager.services

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.maez.filemanager.model.DiscoveredHost
import com.maez.filemanager.model.NetworkConnection
import com.maez.filemanager.model.StorageProtocol
import com.maez.filemanager.model.VfsItem
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.EnumSet
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.min

class VfsManager {

    private val smbConfig = SmbConfig.builder()
        .withTimeout(8, TimeUnit.SECONDS)
        .withSoTimeout(10, TimeUnit.SECONDS)
        .withMultiProtocolNegotiate(true)
        .build()

    /**
     * Separa la ruta virtual SMB en (shareName, subPathInsideShare).
     * - Si la conexión ya tiene `conn.shareName` fijo (ej. "Videos"), cualquier ruta relativa cuelga de ese share.
     * - Si `conn.shareName` está vacío (conexión a la raíz del PC `\\192.168.1.45`), entonces:
     *   - Ruta "/" -> shareName = "" (lista todas las carpetas compartidas del equipo)
     *   - Ruta "/Usuarios/Maez/Videos" -> shareName = "Usuarios", subPath = "Maez\Videos"
     */
    private fun resolveSmbTarget(conn: NetworkConnection, requestedPath: String): Pair<String, String> {
        val fixedShare = conn.shareName.trim('/', '\\')
        val cleanReq = requestedPath.trim('/', '\\').replace('\\', '/')

        if (fixedShare.isNotEmpty()) {
            val sub = if (cleanReq.equals(fixedShare, ignoreCase = true)) {
                ""
            } else if (cleanReq.startsWith("$fixedShare/", ignoreCase = true)) {
                cleanReq.substring(fixedShare.length + 1)
            } else {
                cleanReq
            }
            return fixedShare to sub.replace('/', '\\')
        }

        if (cleanReq.isEmpty()) {
            return "" to ""
        }
        val slashIdx = cleanReq.indexOf('/')
        return if (slashIdx < 0) {
            cleanReq to ""
        } else {
            val share = cleanReq.substring(0, slashIdx)
            val sub = cleanReq.substring(slashIdx + 1).replace('/', '\\')
            share to sub
        }
    }

    private fun buildJcifsContext(conn: NetworkConnection, forceGuest: Boolean = false): CIFSContext {
        val props = Properties().apply {
            setProperty("jcifs.smb.client.enableSMB2", "true")
            setProperty("jcifs.smb.client.useSMB2Negotiation", "true")
            setProperty("jcifs.smb.client.ipcSigningEnforced", "false")
            setProperty("jcifs.smb.client.responseTimeout", "8000")
            setProperty("jcifs.smb.client.soTimeout", "10000")
            setProperty("jcifs.smb.client.connTimeout", "6000")
        }
        val base = BaseContext(PropertyConfiguration(props))
        return when {
            forceGuest -> base.withGuestCrendentials()
            conn.anonymous || conn.username.isBlank() -> base.withAnonymousCredentials()
            else -> {
                val domain = if (conn.username.contains('\\')) conn.username.substringBefore('\\') else ""
                val user = if (conn.username.contains('\\')) conn.username.substringAfter('\\') else conn.username
                base.withCredentials(NtlmPasswordAuthenticator(domain, user, conn.password))
            }
        }
    }

    private fun buildSmbjAuth(conn: NetworkConnection, useGuestFallback: Boolean = false): AuthenticationContext {
        return when {
            useGuestFallback -> AuthenticationContext.guest()
            conn.anonymous || conn.username.isBlank() -> AuthenticationContext.anonymous()
            else -> {
                val domain = if (conn.username.contains('\\')) conn.username.substringBefore('\\') else ""
                val user = if (conn.username.contains('\\')) conn.username.substringAfter('\\') else conn.username
                AuthenticationContext(user, conn.password.toCharArray(), domain)
            }
        }
    }

    private fun translateSmbException(e: Exception, host: String): Exception {
        val msg = e.message ?: e.toString()
        val readable = when {
            msg.contains("STATUS_LOGON_FAILURE", ignoreCase = true) ||
                msg.contains("0xc000006d", ignoreCase = true) ||
                msg.contains("Logon failure", ignoreCase = true) ->
                "Windows ($host) rechazó el usuario o contraseña. Pulsa 'Configurar Credenciales' e ingresa el usuario y contraseña de tu PC."

            msg.contains("STATUS_ACCESS_DENIED", ignoreCase = true) ||
                msg.contains("0xc0000022", ignoreCase = true) ||
                msg.contains("Access is denied", ignoreCase = true) ->
                "Acceso denegado en $host. Tu PC requiere iniciar sesión con usuario y contraseña de Windows (no permite acceso anónimo)."

            msg.contains("STATUS_BAD_NETWORK_NAME", ignoreCase = true) ||
                msg.contains("0xc00000cc", ignoreCase = true) ->
                "No se encontró el nombre de la carpeta compartida en $host. Deja el campo 'Carpeta compartida' vacío para ver todas las carpetas del equipo."

            msg.contains("STATUS_ACCOUNT_RESTRICTION", ignoreCase = true) ||
                msg.contains("0xc000006e", ignoreCase = true) ->
                "Restricción de cuenta en Windows ($host): Windows bloquea cuentas sin contraseña por red. Usa una cuenta con contraseña o edita las credenciales."

            else -> "No se pudo conectar a \\\\$host: $msg"
        }
        return IllegalStateException(readable, e)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. LISTADO DE CARPETAS (LOCAL, SMB, FTP)
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun listDirectory(
        path: String,
        protocol: StorageProtocol,
        connection: NetworkConnection? = null,
        showHidden: Boolean = false
    ): List<VfsItem> = withContext(Dispatchers.IO) {
        when (protocol) {
            StorageProtocol.LOCAL -> listLocalDirectory(path, showHidden)
            StorageProtocol.SMB -> listSmbDirectory(
                connection ?: throw IllegalArgumentException("Conexión SMB requerida"),
                path,
                showHidden
            )
            StorageProtocol.FTP -> listFtpDirectory(
                connection ?: throw IllegalArgumentException("Conexión FTP requerida"),
                path,
                showHidden
            )
        }
    }

    private fun listLocalDirectory(path: String, showHidden: Boolean): List<VfsItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val children = dir.listFiles() ?: return emptyList()

        return children
            .filter { showHidden || !it.name.startsWith(".") }
            .map { file ->
                VfsItem(
                    path = file.absolutePath,
                    name = file.name,
                    parentPath = dir.absolutePath,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    lastModified = file.lastModified(),
                    protocol = StorageProtocol.LOCAL,
                    connectionId = null
                )
            }
    }

    private fun listSmbDirectory(
        conn: NetworkConnection,
        requestedPath: String,
        showHidden: Boolean
    ): List<VfsItem> {
        val (shareName, subPath) = resolveSmbTarget(conn, requestedPath)

        // CASO A: Si estamos en la raíz del servidor sin share específico -> Listar todas las carpetas compartidas (Shares) del PC
        if (shareName.isEmpty()) {
            return listSmbServerShares(conn, showHidden)
        }

        // CASO B: Dentro de una carpeta compartida específica -> Usar smbj con fallback a jcifs-ng
        return try {
            listSmbShareWithSmbj(conn, shareName, subPath, showHidden, useGuestFallback = false)
        } catch (firstErr: Exception) {
            // Si el usuario estaba en modo anónimo y falló, probar Guest o jcifs-ng antes de rendirse
            if (conn.anonymous || conn.username.isBlank()) {
                try {
                    return listSmbShareWithSmbj(conn, shareName, subPath, showHidden, useGuestFallback = true)
                } catch (_: Exception) {
                }
            }
            try {
                listSmbShareWithJcifs(conn, shareName, subPath, showHidden)
            } catch (_: Exception) {
                throw translateSmbException(firstErr, conn.host)
            }
        }
    }

    private fun listSmbServerShares(conn: NetworkConnection, showHidden: Boolean): List<VfsItem> {
        val url = "smb://${conn.host}:${conn.port}/"
        val tryContexts = buildList {
            add(buildJcifsContext(conn, forceGuest = false))
            if (conn.anonymous || conn.username.isBlank()) {
                add(buildJcifsContext(conn, forceGuest = true))
            }
        }

        var lastException: Exception? = null
        for (ctx in tryContexts) {
            try {
                SmbFile(url, ctx).use { root ->
                    val shares = root.listFiles() ?: emptyArray()
                    return shares
                        .filterNotNull()
                        .map { it.name.trim('/', '\\') }
                        .filter { it.isNotEmpty() }
                        .filter { showHidden || (!it.endsWith("$") && !it.equals("IPC$", ignoreCase = true)) }
                        .map { share ->
                            VfsItem(
                                path = "/$share",
                                name = share,
                                parentPath = "/",
                                isDirectory = true,
                                sizeBytes = 0L,
                                lastModified = 0L,
                                protocol = StorageProtocol.SMB,
                                connectionId = conn.id
                            )
                        }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw translateSmbException(
            lastException ?: IllegalStateException("No se pudieron listar los recursos compartidos de ${conn.host}"),
            conn.host
        )
    }

    private fun listSmbShareWithSmbj(
        conn: NetworkConnection,
        shareName: String,
        subPath: String,
        showHidden: Boolean,
        useGuestFallback: Boolean
    ): List<VfsItem> {
        val fixedShareMode = conn.shareName.trim('/', '\\').isNotEmpty()
        val normalizedParent = when {
            fixedShareMode && subPath.isEmpty() -> "/"
            fixedShareMode -> "/${subPath.replace('\\', '/')}"
            subPath.isEmpty() -> "/$shareName"
            else -> "/$shareName/${subPath.replace('\\', '/')}"
        }

        SMBClient(smbConfig).use { smb ->
            smb.connect(conn.host, conn.port).use { c ->
                c.authenticate(buildSmbjAuth(conn, useGuestFallback)).use { s ->
                    val share = s.connectShare(shareName) as? DiskShare
                        ?: throw IllegalStateException("El recurso '$shareName' no es una carpeta compartida de disco")
                    share.use { disk ->
                        val entries = disk.list(subPath)
                        return entries
                            .filter { it.fileName != "." && it.fileName != ".." }
                            .filter { showHidden || !it.fileName.startsWith(".") }
                            .map { info ->
                                val isDir = EnumWithValue.EnumUtils.isSet(
                                    info.fileAttributes,
                                    com.hierynomus.msfscc.FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                                )
                                val itemPath = if (normalizedParent == "/") {
                                    "/${info.fileName}"
                                } else {
                                    "$normalizedParent/${info.fileName}"
                                }
                                VfsItem(
                                    path = itemPath,
                                    name = info.fileName,
                                    parentPath = normalizedParent,
                                    isDirectory = isDir,
                                    sizeBytes = if (isDir) 0L else info.endOfFile,
                                    lastModified = info.changeTime.toEpochMillis(),
                                    protocol = StorageProtocol.SMB,
                                    connectionId = conn.id
                                )
                            }
                    }
                }
            }
        }
    }

    private fun listSmbShareWithJcifs(
        conn: NetworkConnection,
        shareName: String,
        subPath: String,
        showHidden: Boolean
    ): List<VfsItem> {
        val fixedShareMode = conn.shareName.trim('/', '\\').isNotEmpty()
        val normalizedParent = when {
            fixedShareMode && subPath.isEmpty() -> "/"
            fixedShareMode -> "/${subPath.replace('\\', '/')}"
            subPath.isEmpty() -> "/$shareName"
            else -> "/$shareName/${subPath.replace('\\', '/')}"
        }
        val relUrl = if (subPath.isEmpty()) "$shareName/" else "$shareName/${subPath.replace('\\', '/').trim('/')}/"
        val fullUrl = "smb://${conn.host}:${conn.port}/$relUrl"

        val ctx = buildJcifsContext(conn)
        SmbFile(fullUrl, ctx).use { dir ->
            val children = dir.listFiles() ?: emptyArray()
            return children
                .filterNotNull()
                .filter { showHidden || !it.name.trim('/').startsWith(".") }
                .map { f ->
                    val cleanName = f.name.trim('/', '\\')
                    val isDir = f.isDirectory
                    val itemPath = if (normalizedParent == "/") "/$cleanName" else "$normalizedParent/$cleanName"
                    VfsItem(
                        path = itemPath,
                        name = cleanName,
                        parentPath = normalizedParent,
                        isDirectory = isDir,
                        sizeBytes = if (isDir) 0L else f.length(),
                        lastModified = f.lastModified,
                        protocol = StorageProtocol.SMB,
                        connectionId = conn.id
                    )
                }
        }
    }

    private fun listFtpDirectory(
        conn: NetworkConnection,
        remotePath: String,
        showHidden: Boolean
    ): List<VfsItem> {
        val normalizedPath = if (remotePath.isBlank()) "/" else "/" + remotePath.trim('/')
        return withFtpClient(conn) { ftp ->
            val files = ftp.listFiles(normalizedPath) ?: emptyArray()
            files
                .filterNotNull()
                .filter { it.name != "." && it.name != ".." }
                .filter { showHidden || !it.name.startsWith(".") }
                .map { f ->
                    val itemPath = if (normalizedPath == "/") "/${f.name}" else "$normalizedPath/${f.name}"
                    VfsItem(
                        path = itemPath,
                        name = f.name,
                        parentPath = normalizedPath,
                        isDirectory = f.isDirectory,
                        sizeBytes = if (f.isDirectory) 0L else f.size,
                        lastModified = f.timestamp?.timeInMillis ?: 0L,
                        protocol = StorageProtocol.FTP,
                        connectionId = conn.id
                    )
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. RECOLECCIÓN RECURSIVA (LOCAL, SMB Y FTP) PARA BÚSQUEDAS Y DUPLICADOS
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun collectFilesRecursive(
        rootPath: String,
        protocol: StorageProtocol,
        connection: NetworkConnection? = null,
        recursive: Boolean = true,
        includeDirectories: Boolean = false,
        onFoundProgress: ((Int, String) -> Unit)? = null
    ): List<VfsItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<VfsItem>()
        when (protocol) {
            StorageProtocol.LOCAL -> {
                collectLocalRecursive(File(rootPath), recursive, includeDirectories, results, onFoundProgress)
            }
            StorageProtocol.SMB -> {
                val conn = connection ?: return@withContext emptyList()
                collectSmbRecursive(conn, rootPath, recursive, includeDirectories, results, onFoundProgress)
            }
            StorageProtocol.FTP -> {
                val conn = connection ?: return@withContext emptyList()
                collectFtpRecursive(conn, rootPath, recursive, includeDirectories, results, onFoundProgress)
            }
        }
        results
    }

    private suspend fun collectLocalRecursive(
        dir: File,
        recursive: Boolean,
        includeDirectories: Boolean,
        out: MutableList<VfsItem>,
        onProgress: ((Int, String) -> Unit)?
    ) {
        coroutineContext.ensureActive()
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.name.startsWith(".")) continue
            val item = VfsItem(
                path = child.absolutePath,
                name = child.name,
                parentPath = dir.absolutePath,
                isDirectory = child.isDirectory,
                sizeBytes = if (child.isDirectory) 0L else child.length(),
                lastModified = child.lastModified(),
                protocol = StorageProtocol.LOCAL
            )
            if (child.isDirectory) {
                if (includeDirectories) out.add(item)
                if (recursive && !child.name.equals("Android", ignoreCase = true)) {
                    collectLocalRecursive(child, true, includeDirectories, out, onProgress)
                }
            } else {
                out.add(item)
                if (out.size % 25 == 0) {
                    onProgress?.invoke(out.size, child.name)
                }
            }
        }
    }

    private suspend fun collectSmbRecursive(
        conn: NetworkConnection,
        startPath: String,
        recursive: Boolean,
        includeDirectories: Boolean,
        out: MutableList<VfsItem>,
        onProgress: ((Int, String) -> Unit)?
    ) {
        suspend fun walkVirtualPath(vPath: String) {
            coroutineContext.ensureActive()
            val items = try {
                listSmbDirectory(conn, vPath, showHidden = false)
            } catch (_: Exception) {
                return
            }
            for (item in items) {
                if (item.isDirectory) {
                    if (includeDirectories) out.add(item)
                    if (recursive) walkVirtualPath(item.path)
                } else {
                    out.add(item)
                    if (out.size % 10 == 0) {
                        onProgress?.invoke(out.size, item.name)
                    }
                }
            }
        }
        walkVirtualPath(startPath)
    }

    private suspend fun collectFtpRecursive(
        conn: NetworkConnection,
        startPath: String,
        recursive: Boolean,
        includeDirectories: Boolean,
        out: MutableList<VfsItem>,
        onProgress: ((Int, String) -> Unit)?
    ) {
        withFtpClient(conn) { ftp ->
            fun walkFtp(path: String) {
                val norm = if (path.isBlank()) "/" else "/" + path.trim('/')
                val entries = try {
                    ftp.listFiles(norm) ?: emptyArray()
                } catch (_: Exception) {
                    return
                }
                for (f in entries) {
                    if (f == null || f.name == "." || f.name == ".." || f.name.startsWith(".")) continue
                    val fullPath = if (norm == "/") "/${f.name}" else "$norm/${f.name}"
                    val item = VfsItem(
                        path = fullPath,
                        name = f.name,
                        parentPath = norm,
                        isDirectory = f.isDirectory,
                        sizeBytes = if (f.isDirectory) 0L else f.size,
                        lastModified = f.timestamp?.timeInMillis ?: 0L,
                        protocol = StorageProtocol.FTP,
                        connectionId = conn.id
                    )
                    if (f.isDirectory) {
                        if (includeDirectories) out.add(item)
                        if (recursive) walkFtp(fullPath)
                    } else {
                        out.add(item)
                        onProgress?.invoke(out.size, f.name)
                    }
                }
            }
            walkFtp(startPath)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. LECTURA PARCIAL POR OFFSET (SEEK) EN LOCAL, SMB Y FTP
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun readBytesAt(
        item: VfsItem,
        offset: Long,
        length: Int,
        connection: NetworkConnection? = null
    ): ByteArray = withContext(Dispatchers.IO) {
        if (length <= 0 || offset < 0L) return@withContext ByteArray(0)
        when (item.protocol) {
            StorageProtocol.LOCAL -> {
                val f = File(item.path)
                if (!f.exists() || offset >= f.length()) return@withContext ByteArray(0)
                RandomAccessFile(f, "r").use { raf ->
                    raf.seek(offset)
                    val toRead = min(length.toLong(), f.length() - offset).toInt()
                    val buf = ByteArray(toRead)
                    val read = raf.read(buf)
                    if (read <= 0) ByteArray(0) else if (read == toRead) buf else buf.copyOf(read)
                }
            }
            StorageProtocol.SMB -> {
                val conn = connection ?: return@withContext ByteArray(0)
                val (shareName, subPath) = resolveSmbTarget(conn, item.path)
                if (shareName.isEmpty() || subPath.isEmpty()) return@withContext ByteArray(0)
                try {
                    SMBClient(smbConfig).use { smb ->
                        smb.connect(conn.host, conn.port).use { c ->
                            c.authenticate(buildSmbjAuth(conn)).use { s ->
                                val share = s.connectShare(shareName) as? DiskShare
                                    ?: return@withContext ByteArray(0)
                                share.use { disk ->
                                    val smbFile = disk.openFile(
                                        subPath,
                                        EnumSet.of(AccessMask.GENERIC_READ),
                                        null,
                                        SMB2ShareAccess.ALL,
                                        SMB2CreateDisposition.FILE_OPEN,
                                        null
                                    )
                                    smbFile.use { rf ->
                                        val buf = ByteArray(length)
                                        val read = rf.read(buf, offset, 0, length)
                                        if (read <= 0) ByteArray(0) else if (read == length) buf else buf.copyOf(read)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    ByteArray(0)
                }
            }
            StorageProtocol.FTP -> {
                val conn = connection ?: return@withContext ByteArray(0)
                try {
                    withFtpClient(conn) { ftp ->
                        ftp.restartOffset = offset
                        val stream = ftp.retrieveFileStream(item.path) ?: return@withFtpClient ByteArray(0)
                        val out = ByteArrayOutputStream()
                        stream.use { ins ->
                            val tmp = ByteArray(8192)
                            var remaining = length
                            while (remaining > 0) {
                                val r = ins.read(tmp, 0, min(tmp.size, remaining))
                                if (r <= 0) break
                                out.write(tmp, 0, r)
                                remaining -= r
                            }
                        }
                        try {
                            ftp.completePendingCommand()
                        } catch (_: Exception) {
                        }
                        out.toByteArray()
                    }
                } catch (_: Exception) {
                    ByteArray(0)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. OPERACIONES DE ARCHIVOS (CREAR CARPETA, RENOMBRAR, ELIMINAR, COPIAR/MOVER)
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun createFolder(
        parentPath: String,
        folderName: String,
        protocol: StorageProtocol,
        connection: NetworkConnection? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (protocol) {
                StorageProtocol.LOCAL -> File(parentPath, folderName).mkdirs()
                StorageProtocol.SMB -> {
                    val conn = connection ?: return@withContext false
                    val (shareName, subPath) = resolveSmbTarget(conn, parentPath)
                    if (shareName.isEmpty()) return@withContext false
                    val target = if (subPath.isEmpty()) folderName else "$subPath\\$folderName"
                    withSmbDiskShare(conn, shareName) { disk ->
                        disk.mkdir(target)
                        true
                    }
                }
                StorageProtocol.FTP -> {
                    val conn = connection ?: return@withContext false
                    val target = if (parentPath == "/") "/$folderName" else "${parentPath.trimEnd('/')}/$folderName"
                    withFtpClient(conn) { ftp ->
                        ftp.makeDirectory(target)
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun renameItem(
        item: VfsItem,
        newName: String,
        connection: NetworkConnection? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (item.protocol) {
                StorageProtocol.LOCAL -> {
                    val src = File(item.path)
                    val dst = File(src.parentFile, newName)
                    src.renameTo(dst)
                }
                StorageProtocol.SMB -> {
                    val conn = connection ?: return@withContext false
                    val (shareName, subPath) = resolveSmbTarget(conn, item.path)
                    if (shareName.isEmpty() || subPath.isEmpty()) return@withContext false
                    val parentSub = subPath.substringBeforeLast('\\', "")
                    val dstPath = if (parentSub.isEmpty()) newName else "$parentSub\\$newName"
                    withSmbDiskShare(conn, shareName) { disk ->
                        val entry = disk.open(
                            subPath,
                            EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OPEN,
                            null
                        )
                        entry.use {
                            it.rename(dstPath, false)
                        }
                        true
                    }
                }
                StorageProtocol.FTP -> {
                    val conn = connection ?: return@withContext false
                    val dstPath = if (item.parentPath == "/") "/$newName" else "${item.parentPath.trimEnd('/')}/$newName"
                    withFtpClient(conn) { ftp ->
                        ftp.rename(item.path, dstPath)
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteItem(
        item: VfsItem,
        connection: NetworkConnection? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (item.protocol) {
                StorageProtocol.LOCAL -> {
                    val f = File(item.path)
                    if (f.isDirectory) f.deleteRecursively() else f.delete()
                }
                StorageProtocol.SMB -> {
                    val conn = connection ?: return@withContext false
                    val (shareName, subPath) = resolveSmbTarget(conn, item.path)
                    if (shareName.isEmpty() || subPath.isEmpty()) return@withContext false
                    withSmbDiskShare(conn, shareName) { disk ->
                        if (item.isDirectory) {
                            disk.rmdir(subPath, true)
                        } else {
                            disk.rm(subPath)
                        }
                        true
                    }
                }
                StorageProtocol.FTP -> {
                    val conn = connection ?: return@withContext false
                    withFtpClient(conn) { ftp ->
                        if (item.isDirectory) {
                            ftp.removeDirectory(item.path)
                        } else {
                            ftp.deleteFile(item.path)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun transferFile(
        source: VfsItem,
        sourceConn: NetworkConnection?,
        targetParentPath: String,
        targetProtocol: StorageProtocol,
        targetConn: NetworkConnection?,
        deleteSourceAfter: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        if (source.isDirectory) {
            try {
                // 1. Crear la carpeta de destino
                val created = createFolder(targetParentPath, source.name, targetProtocol, targetConn)
                val newDirPath = if (targetParentPath == "/") "/${source.name}" else "${targetParentPath.trimEnd('/')}/${source.name}"
                // 2. Listar los hijos de la carpeta origen
                val children = listDirectory(source.path, source.protocol, sourceConn, showHidden = true)
                // 3. Transferir recursivamente cada hijo
                var allSuccess = true
                for (child in children) {
                    val childSuccess = transferFile(
                        source = child,
                        sourceConn = sourceConn,
                        targetParentPath = newDirPath,
                        targetProtocol = targetProtocol,
                        targetConn = targetConn,
                        deleteSourceAfter = deleteSourceAfter
                    )
                    if (!childSuccess) allSuccess = false
                }
                if (deleteSourceAfter && allSuccess) {
                    deleteItem(source, sourceConn)
                }
                return@withContext allSuccess
            } catch (_: Exception) {
                return@withContext false
            }
        }
        try {
            if (source.protocol == StorageProtocol.LOCAL && targetProtocol == StorageProtocol.LOCAL) {
                val srcFile = File(source.path)
                val dstFile = File(targetParentPath, source.name)
                srcFile.inputStream().use { input ->
                    dstFile.outputStream().use { output ->
                        input.copyTo(output, 64 * 1024)
                    }
                }
                if (deleteSourceAfter) srcFile.delete()
                return@withContext true
            }

            val tempFile = File.createTempFile("vfs_transfer_", "_${source.name}")
            try {
                val localSourceFile = if (source.protocol == StorageProtocol.LOCAL) {
                    File(source.path)
                } else {
                    downloadToLocalFile(source, sourceConn, tempFile)
                    tempFile
                }

                when (targetProtocol) {
                    StorageProtocol.LOCAL -> {
                        val dst = File(targetParentPath, source.name)
                        localSourceFile.copyTo(dst, overwrite = true)
                    }
                    StorageProtocol.SMB -> {
                        val conn = targetConn ?: return@withContext false
                        val (shareName, subPath) = resolveSmbTarget(conn, targetParentPath)
                        if (shareName.isEmpty()) return@withContext false
                        val remoteTarget = if (subPath.isEmpty()) source.name else "$subPath\\${source.name}"
                        withSmbDiskShare(conn, shareName) { disk ->
                            val smbFile = disk.openFile(
                                remoteTarget,
                                EnumSet.of(AccessMask.GENERIC_WRITE),
                                null,
                                SMB2ShareAccess.ALL,
                                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                                null
                            )
                            smbFile.use { rf ->
                                rf.outputStream.use { out ->
                                    FileInputStream(localSourceFile).use { ins ->
                                        ins.copyTo(out, 64 * 1024)
                                    }
                                }
                            }
                            true
                        }
                    }
                    StorageProtocol.FTP -> {
                        val conn = targetConn ?: return@withContext false
                        val remoteTarget = if (targetParentPath == "/") "/${source.name}" else "${targetParentPath.trimEnd('/')}/${source.name}"
                        withFtpClient(conn) { ftp ->
                            FileInputStream(localSourceFile).use { ins ->
                                ftp.storeFile(remoteTarget, ins)
                            }
                        }
                    }
                }

                if (deleteSourceAfter) {
                    deleteItem(source, sourceConn)
                }
                true
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun downloadToLocalFile(
        source: VfsItem,
        sourceConn: NetworkConnection?,
        destFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (source.protocol) {
                StorageProtocol.LOCAL -> {
                    File(source.path).copyTo(destFile, overwrite = true)
                    true
                }
                StorageProtocol.SMB -> {
                    val conn = sourceConn ?: return@withContext false
                    val (shareName, subPath) = resolveSmbTarget(conn, source.path)
                    if (shareName.isEmpty() || subPath.isEmpty()) return@withContext false
                    withSmbDiskShare(conn, shareName) { disk ->
                        val smbFile = disk.openFile(
                            subPath,
                            EnumSet.of(AccessMask.GENERIC_READ),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OPEN,
                            null
                        )
                        smbFile.use { rf ->
                            rf.inputStream.use { ins ->
                                FileOutputStream(destFile).use { out ->
                                    ins.copyTo(out, 64 * 1024)
                                }
                            }
                        }
                        true
                    }
                }
                StorageProtocol.FTP -> {
                    val conn = sourceConn ?: return@withContext false
                    withFtpClient(conn) { ftp ->
                        FileOutputStream(destFile).use { out ->
                            ftp.retrieveFile(source.path, out)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. ESCÁNER DE RED LOCAL Y RESOLUCIÓN DE NOMBRES DE EQUIPOS (SMB NTLMSSP + NETBIOS + DNS)
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun discoverLanServers(onProgress: (Int, Int) -> Unit): List<DiscoveredHost> = withContext(Dispatchers.IO) {
        val localIp = findLocalIpv4() ?: return@withContext emptyList()
        val prefix = localIp.substringBeforeLast('.')
        val found = java.util.concurrent.ConcurrentLinkedQueue<DiscoveredHost>()
        val total = 254
        val counter = java.util.concurrent.atomic.AtomicInteger(0)

        coroutineScope {
            val jobs = (1..254).map { hostNum ->
                async(Dispatchers.IO) {
                    val ip = "$prefix.$hostNum"
                    val openPorts = mutableListOf<Int>()
                    for (port in listOf(445, 21, 2121)) {
                        try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(ip, port), 240)
                                openPorts.add(port)
                            }
                        } catch (_: Exception) {
                        }
                    }
                    if (openPorts.isNotEmpty()) {
                        val hostName = resolveDeviceName(ip, openPorts)
                        found.add(DiscoveredHost(ip = ip, hostname = hostName, openPorts = openPorts))
                    }
                    val done = counter.incrementAndGet()
                    if (done % 8 == 0 || done == total) {
                        onProgress(done, total)
                    }
                }
            }
            jobs.awaitAll()
        }

        found.sortedBy {
            it.ip.substringAfterLast('.').toIntOrNull() ?: 999
        }
    }

    /**
     * Obtiene el nombre real del equipo (PC Windows, Samba, NAS, servidor FTP):
     * 1. Si el puerto 445 está abierto: Negociación SMB2 + NTLMSSP Type 1 -> extrae el Computer Name de NTLMSSP_CHALLENGE (funciona incluso con Firewall de Windows activo).
     * 2. Consulta NetBIOS Node Status (NBSTAT) por UDP 137.
     * 3. Consulta jcifs-ng NameServiceClient.
     * 4. Reverse DNS estándar del router.
     */
    private fun resolveDeviceName(ip: String, openPorts: List<Int>): String {
        // 1. Extraer nombre del equipo directamente del handshake SMB2 NTLMSSP en TCP 445
        if (445 in openPorts) {
            querySmb2NtlmComputerName(ip)?.takeIf { it.isNotBlank() && it != ip }?.let { return it }
        }

        // 2. Consulta directa NetBIOS Node Status (UDP 137)
        queryNetBiosUdp137Name(ip)?.takeIf { it.isNotBlank() && it != ip }?.let { return it }

        // 3. jcifs-ng NetBIOS lookup
        try {
            val ctx = BaseContext(PropertyConfiguration(Properties()))
            val nbtArr = ctx.nameServiceClient.getNbtAllByAddress(ip)
            val valid = nbtArr?.firstOrNull {
                val n = it.hostName?.trim().orEmpty()
                n.isNotEmpty() && n != ip && !n.startsWith("0.0.0.0") && !n.contains("__MSBROWSE__")
            }
            if (valid != null) {
                return valid.hostName.trim()
            }
        } catch (_: Exception) {
        }

        // 4. Reverse DNS del router local
        try {
            val parts = ip.split('.').map { it.toInt().toByte() }.toByteArray()
            val addr = InetAddress.getByAddress(parts)
            val host = addr.canonicalHostName?.trim().orEmpty()
            if (host.isNotEmpty() && host != ip) {
                return host.substringBefore('.')
            }
        } catch (_: Exception) {
        }

        return "Equipo ($ip)"
    }

    /**
     * Envía un paquete SMB2 NEGOTIATE seguido de SMB2 SESSION_SETUP con NTLMSSP_NEGOTIATE
     * por el puerto TCP 445 y extrae MsvAvNbComputerName / MsvAvDnsComputerName del desafío NTLMSSP_CHALLENGE.
     */
    private fun querySmb2NtlmComputerName(ip: String): String? {
        return try {
            Socket().use { socket ->
                socket.soTimeout = 1200
                socket.connect(InetSocketAddress(ip, 445), 800)
                val out = socket.getOutputStream()
                val ins = DataInputStream(socket.getInputStream())

                // Paso 1: SMB2 NEGOTIATE Request (dialectos 0x0202, 0x0210, 0x0300, 0x0302, 0x0311)
                val negotiateBody = ByteBuffer.allocate(64 + 36 + 10).order(ByteOrder.LITTLE_ENDIAN)
                // SMB2 Header (64 bytes)
                negotiateBody.put(byteArrayOf(0xFE.toByte(), 'S'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte()))
                negotiateBody.putShort(64) // StructureSize
                negotiateBody.putShort(0)  // CreditCharge
                negotiateBody.putInt(0)    // Status
                negotiateBody.putShort(0)  // Command = NEGOTIATE (0)
                negotiateBody.putShort(1)  // Credits
                negotiateBody.putInt(0)    // Flags
                negotiateBody.putInt(0)    // NextCommand
                negotiateBody.putLong(0L)  // MessageId = 0
                negotiateBody.putInt(0)    // Reserved
                negotiateBody.putInt(0)    // TreeId
                negotiateBody.putLong(0L)  // SessionId
                negotiateBody.put(ByteArray(16)) // Signature

                // SMB2 NEGOTIATE Payload (36 bytes + 5 dialectos)
                negotiateBody.putShort(36) // StructureSize
                negotiateBody.putShort(5)  // DialectCount = 5
                negotiateBody.putShort(1)  // SecurityMode = Signing Enabled
                negotiateBody.putShort(0)  // Reserved
                negotiateBody.putInt(0x7F) // Capabilities
                negotiateBody.put(ByteArray(16) { (it + 1).toByte() }) // ClientGuid
                negotiateBody.putLong(0L)  // ClientStartTime / NegotiateContext
                negotiateBody.putShort(0x0202)
                negotiateBody.putShort(0x0210)
                negotiateBody.putShort(0x0300)
                negotiateBody.putShort(0x0302)
                negotiateBody.putShort(0x0311)

                val negBytes = negotiateBody.array()
                val negFrame = ByteBuffer.allocate(4 + negBytes.size).order(ByteOrder.BIG_ENDIAN)
                negFrame.putInt(negBytes.size)
                negFrame.put(negBytes)
                out.write(negFrame.array())
                out.flush()

                // Leer respuesta SMB2 NEGOTIATE
                val respLen1 = ins.readInt() and 0x00FFFFFF
                if (respLen1 <= 0 || respLen1 > 65536) return null
                val resp1 = ByteArray(respLen1)
                ins.readFully(resp1)

                // Paso 2: SMB2 SESSION_SETUP Request con NTLMSSP_NEGOTIATE (Type 1) dentro de SPNEGO o directo
                val ntlmType1 = byteArrayOf(
                    'N'.code.toByte(), 'T'.code.toByte(), 'L'.code.toByte(), 'M'.code.toByte(),
                    'S'.code.toByte(), 'S'.code.toByte(), 'P'.code.toByte(), 0x00,
                    0x01, 0x00, 0x00, 0x00, // Type 1 (NEGOTIATE)
                    0x07, 0x82.toByte(), 0x08, 0xE2.toByte(), // Flags (UNICODE | REQUEST_TARGET | NTLM | EXTENDED_SESSIONSECURITY | TARGET_INFO)
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Domain
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  // Workstation
                )

                val sessBody = ByteBuffer.allocate(64 + 24 + ntlmType1.size).order(ByteOrder.LITTLE_ENDIAN)
                // SMB2 Header
                sessBody.put(byteArrayOf(0xFE.toByte(), 'S'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte()))
                sessBody.putShort(64)
                sessBody.putShort(1)
                sessBody.putInt(0)
                sessBody.putShort(1) // Command = SESSION_SETUP (1)
                sessBody.putShort(1)
                sessBody.putInt(0)
                sessBody.putInt(0)
                sessBody.putLong(1L) // MessageId = 1
                sessBody.putInt(0)
                sessBody.putInt(0)
                sessBody.putLong(0L)
                sessBody.put(ByteArray(16))

                // SMB2 SESSION_SETUP Payload (24 bytes + security buffer)
                sessBody.putShort(25)
                sessBody.put(0) // Flags
                sessBody.put(1) // SecurityMode
                sessBody.putInt(0) // Capabilities
                sessBody.putInt(0) // Channel
                sessBody.putShort((64 + 24).toShort()) // SecurityBufferOffset
                sessBody.putShort(ntlmType1.size.toShort()) // SecurityBufferLength
                sessBody.putLong(0L) // PreviousSessionId
                sessBody.put(ntlmType1)

                val sessBytes = sessBody.array()
                val sessFrame = ByteBuffer.allocate(4 + sessBytes.size).order(ByteOrder.BIG_ENDIAN)
                sessFrame.putInt(sessBytes.size)
                sessFrame.put(sessBytes)
                out.write(sessFrame.array())
                out.flush()

                // Leer respuesta SMB2 SESSION_SETUP (contiene NTLMSSP_CHALLENGE Type 2)
                val respLen2 = ins.readInt() and 0x00FFFFFF
                if (respLen2 <= 0 || respLen2 > 65536) return null
                val resp2 = ByteArray(respLen2)
                ins.readFully(resp2)

                parseNtlmType2ComputerName(resp2)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Busca la firma "NTLMSSP\0" con MessageType = 2 (Challenge) y extrae el nombre NetBIOS o DNS del equipo.
     */
    private fun parseNtlmType2ComputerName(packet: ByteArray): String? {
        val sig = byteArrayOf(
            'N'.code.toByte(), 'T'.code.toByte(), 'L'.code.toByte(), 'M'.code.toByte(),
            'S'.code.toByte(), 'S'.code.toByte(), 'P'.code.toByte(), 0x00,
            0x02, 0x00, 0x00, 0x00
        )
        var base = -1
        for (i in 0..(packet.size - sig.size - 40)) {
            var match = true
            for (j in sig.indices) {
                if (packet[i + j] != sig[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                base = i
                break
            }
        }
        if (base < 0) return null

        val buf = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        val targetNameLen = buf.getShort(base + 12).toInt() and 0xFFFF
        val targetNameOffset = buf.getInt(base + 16)

        // Revisar TargetInfo (offset 40 en NTLMSSP Type 2: TargetInfoLen [2B], TargetInfoMaxLen [2B], TargetInfoOffset [4B])
        if (base + 48 <= packet.size) {
            val targetInfoLen = buf.getShort(base + 40).toInt() and 0xFFFF
            val targetInfoOffset = buf.getInt(base + 44)
            val absInfoStart = base + targetInfoOffset
            if (targetInfoLen > 4 && absInfoStart > base && absInfoStart + targetInfoLen <= packet.size) {
                var pos = absInfoStart
                val end = absInfoStart + targetInfoLen
                var nbName: String? = null
                var dnsName: String? = null
                while (pos + 4 <= end) {
                    val avId = buf.getShort(pos).toInt() and 0xFFFF
                    val avLen = buf.getShort(pos + 2).toInt() and 0xFFFF
                    pos += 4
                    if (avId == 0 || pos + avLen > end) break
                    val str = String(packet, pos, avLen, Charsets.UTF_16LE).trim()
                    if (avId == 1 && str.isNotEmpty()) nbName = str // MsvAvNbComputerName
                    if (avId == 3 && str.isNotEmpty()) dnsName = str // MsvAvDnsComputerName
                    pos += avLen
                }
                if (!nbName.isNullOrBlank()) return nbName
                if (!dnsName.isNullOrBlank()) return dnsName.substringBefore('.')
            }
        }

        // Fallback a TargetName directo
        val absTarget = base + targetNameOffset
        if (targetNameLen > 0 && absTarget > base && absTarget + targetNameLen <= packet.size) {
            val tName = String(packet, absTarget, targetNameLen, Charsets.UTF_16LE).trim()
            if (tName.isNotEmpty()) return tName
        }

        return null
    }

    /**
     * Consulta directa NetBIOS Node Status (NBSTAT) por UDP 137.
     */
    private fun queryNetBiosUdp137Name(ip: String): String? {
        return try {
            val request = byteArrayOf(
                0x82.toByte(), 0x28.toByte(), 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x20, 0x43, 0x4B, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x00, 0x00, 0x21, 0x00, 0x01
            )
            DatagramSocket().use { socket ->
                socket.soTimeout = 450
                val addr = InetAddress.getByName(ip)
                socket.send(DatagramPacket(request, request.size, addr, 137))
                val respBuf = ByteArray(512)
                val respPkt = DatagramPacket(respBuf, respBuf.size)
                socket.receive(respPkt)
                if (respPkt.length > 57) {
                    val numNames = respBuf[56].toInt() and 0xFF
                    var offset = 57
                    for (i in 0 until numNames) {
                        if (offset + 18 > respPkt.length) break
                        val name = String(respBuf, offset, 15, Charsets.US_ASCII).trim()
                        val type = respBuf[offset + 15].toInt() and 0xFF
                        val flags = respBuf[offset + 16].toInt() and 0xFF
                        val isGroup = (flags and 0x80) != 0
                        // Tipo 0x20 (File Server) o 0x00 (Workstation) único
                        if (!isGroup && (type == 0x20 || type == 0x00) && name.isNotEmpty() && !name.startsWith("__")) {
                            return name
                        }
                        offset += 18
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findLocalIpv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPERS INTERNOS SMB / FTP
    // ─────────────────────────────────────────────────────────────────────────────

    private fun <T> withSmbDiskShare(conn: NetworkConnection, shareName: String, block: (DiskShare) -> T): T {
        val client = SMBClient(smbConfig)
        return client.use { smb ->
            smb.connect(conn.host, conn.port).use { c ->
                c.authenticate(buildSmbjAuth(conn)).use { s ->
                    val share = s.connectShare(shareName.trim('/', '\\')) as? DiskShare
                        ?: throw IllegalStateException("Recurso compartido '$shareName' no disponible")
                    share.use { disk ->
                        block(disk)
                    }
                }
            }
        }
    }

    private fun <T> withFtpClient(conn: NetworkConnection, block: (FTPClient) -> T): T {
        val ftp = FTPClient()
        ftp.connectTimeout = 8000
        ftp.defaultTimeout = 10000
        ftp.controlEncoding = "UTF-8"
        try {
            ftp.connect(conn.host, conn.port)
            val user = if (conn.anonymous || conn.username.isBlank()) "anonymous" else conn.username
            val pass = if (conn.anonymous || conn.username.isBlank()) "anonymous@" else conn.password
            if (!ftp.login(user, pass)) {
                throw IllegalStateException("Autenticación FTP fallida para '$user'")
            }
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            return block(ftp)
        } finally {
            try {
                if (ftp.isConnected) {
                    ftp.logout()
                    ftp.disconnect()
                }
            } catch (_: Exception) {
            }
        }
    }
}
