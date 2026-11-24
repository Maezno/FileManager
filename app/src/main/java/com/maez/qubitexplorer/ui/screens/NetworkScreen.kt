package com.maez.qubitexplorer.ui.screens

import com.maez.qubitexplorer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.qubitexplorer.model.DiscoveredHost
import com.maez.qubitexplorer.model.NetworkConnection
import com.maez.qubitexplorer.model.StorageProtocol
import com.maez.qubitexplorer.ui.components.AppProgressBar
import com.maez.qubitexplorer.ui.components.ControlShape
import com.maez.qubitexplorer.ui.components.EmptyState
import com.maez.qubitexplorer.ui.components.GlassAlertDialog
import com.maez.qubitexplorer.ui.components.GlassIconButton
import com.maez.qubitexplorer.ui.components.GlassSurface
import com.maez.qubitexplorer.ui.components.IconBadge
import com.maez.qubitexplorer.ui.components.IconBubble
import com.maez.qubitexplorer.ui.components.PrimaryButton
import com.maez.qubitexplorer.ui.components.SecondaryButton
import com.maez.qubitexplorer.ui.components.SectionCard
import com.maez.qubitexplorer.ui.components.SelectChip
import com.maez.qubitexplorer.ui.components.SettingSwitchRow
import com.maez.qubitexplorer.ui.components.Tag
import com.maez.qubitexplorer.ui.components.appTextFieldColors
import com.maez.qubitexplorer.ui.theme.Aqua
import com.maez.qubitexplorer.ui.theme.BgElevated
import com.maez.qubitexplorer.ui.theme.CardDark
import com.maez.qubitexplorer.ui.theme.Danger
import com.maez.qubitexplorer.ui.theme.QubitExplorerTheme
import com.maez.qubitexplorer.ui.theme.GlassCardShape
import com.maez.qubitexplorer.ui.theme.TextMuted
import com.maez.qubitexplorer.ui.theme.TextPrimary
import com.maez.qubitexplorer.ui.theme.TextSecondary
import com.maez.qubitexplorer.ui.theme.Violet
import com.maez.qubitexplorer.viewmodel.MainViewModel

@Composable
fun NetworkScreen(viewModel: MainViewModel) {
    val savedConnections by viewModel.savedConnections.collectAsState()
    val discoveredHosts by viewModel.discoveredHosts.collectAsState()
    val isDiscovering by viewModel.isDiscoveringLan.collectAsState()
    val lanProgress by viewModel.lanScanProgress.collectAsState()

    NetworkScreenContent(
        savedConnections = savedConnections,
        discoveredHosts = discoveredHosts,
        isDiscovering = isDiscovering,
        lanProgress = lanProgress,
        onScanLan = { viewModel.scanLocalNetworkForServers() },
        onConnect = { viewModel.connectAndBrowseNetwork(it) },
        onSaveConnection = { existingId, name, protocol, host, port, shareName, username, password, anonymous, initialPath, useTls ->
            viewModel.saveNetworkConnection(
                existingId = existingId,
                name = name,
                protocol = protocol,
                host = host,
                port = port,
                shareName = shareName,
                username = username,
                password = password,
                anonymous = anonymous,
                initialPath = initialPath,
                useTls = useTls
            )
        },
        onDeleteConnection = { viewModel.deleteNetworkConnection(it) }
    )
}

@Composable
fun NetworkScreenContent(
    savedConnections: List<NetworkConnection>,
    discoveredHosts: List<DiscoveredHost>,
    isDiscovering: Boolean,
    lanProgress: Float,
    onScanLan: () -> Unit,
    onConnect: (NetworkConnection) -> Unit,
    onSaveConnection: (
        existingId: String?,
        name: String,
        protocol: StorageProtocol,
        host: String,
        port: Int,
        shareName: String,
        username: String,
        password: String,
        anonymous: Boolean,
        initialPath: String,
        useTls: Boolean
    ) -> Unit,
    onDeleteConnection: (NetworkConnection) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var formName by remember { mutableStateOf("") }
    var formProtocol by remember { mutableStateOf(StorageProtocol.SMB) }
    var formHost by remember { mutableStateOf("") }
    var formPort by remember { mutableStateOf("445") }
    var formShare by remember { mutableStateOf("") }
    var formUser by remember { mutableStateOf("") }
    var formPass by remember { mutableStateOf("") }
    var formAnon by remember { mutableStateOf(false) }
    var formPath by remember { mutableStateOf("/") }
    var formTls by remember { mutableStateOf(false) }

    fun openNewConnectionDialog(
        presetProtocol: StorageProtocol = StorageProtocol.SMB,
        presetHost: String = "",
        presetHostname: String = "",
        presetPort: Int = if (presetProtocol == StorageProtocol.SMB) 445 else 21
    ) {
        editingId = null
        formName = when {
            presetHostname.isNotBlank() && presetHostname != presetHost -> presetHostname
            presetHost.isNotEmpty() -> "${presetProtocol.name} ($presetHost)"
            else -> ""
        }
        formProtocol = presetProtocol
        formHost = presetHost
        formPort = presetPort.toString()
        formShare = ""
        formUser = ""
        formPass = ""
        formAnon = false
        formPath = "/"
        formTls = false
        showDialog = true
    }

    fun openEditDialog(conn: NetworkConnection) {
        editingId = conn.id
        formName = conn.name
        formProtocol = conn.protocol
        formHost = conn.host
        formPort = conn.port.toString()
        formShare = conn.shareName
        formUser = conn.username
        formPass = conn.password
        formAnon = conn.anonymous
        formPath = conn.initialPath
        formTls = conn.useTls
        showDialog = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Cabecera con estilo GlassCard
        item {
            SectionCard(
                title = stringResource(R.string.network_red_local_smb_y_ftp),
                icon = Icons.Default.Lan,
                subtitle = stringResource(R.string.network_explora_carpetas_compartidas_de_windows),
                iconTint = Violet
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.network_anadir_servidor),
                        icon = Icons.Default.Add,
                        onClick = { openNewConnectionDialog(StorageProtocol.SMB) },
                        modifier = Modifier.weight(1f)
                    )

                    SecondaryButton(
                        text = if (isDiscovering) stringResource(R.string.network_escaneando) else stringResource(R.string.network_escanear_wi_fi),
                        icon = Icons.Default.Radar,
                        onClick = onScanLan,
                        enabled = !isDiscovering,
                        tint = Aqua,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isDiscovering) {
                    AppProgressBar(progress = lanProgress)
                }
            }
        }

        // 2. Equipos descubiertos en la red Wi-Fi
        if (discoveredHosts.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.WifiFind, contentDescription = null, tint = Aqua, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.network_equipos_detectados_en_tu_red, discoveredHosts.size),
                        color = Aqua,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
            items(discoveredHosts, key = { "disc:${it.ip}" }) { host ->
                DiscoveredHostCard(
                    host = host,
                    onConfigureSmb = {
                        openNewConnectionDialog(
                            presetProtocol = StorageProtocol.SMB,
                            presetHost = host.ip,
                            presetHostname = host.hostname,
                            presetPort = 445
                        )
                    },
                    onConfigureFtp = {
                        val ftpPort = if (21 in host.openPorts) 21 else 2121
                        openNewConnectionDialog(
                            presetProtocol = StorageProtocol.FTP,
                            presetHost = host.ip,
                            presetHostname = host.hostname,
                            presetPort = ftpPort
                        )
                    }
                )
            }
        }

        // 3. Lista de conexiones guardadas
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Violet, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(R.string.network_servidores_guardados, savedConnections.size),
                    color = Violet,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        if (savedConnections.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Lan,
                    title = stringResource(R.string.network_sin_servidores_guardados),
                    message = stringResource(R.string.network_pulsa_escanear_wi_fi_para)
                )
            }
        } else {
            items(savedConnections, key = { it.id }) { conn ->
                SavedConnectionCard(
                    conn = conn,
                    onConnect = { onConnect(conn) },
                    onEdit = { openEditDialog(conn) },
                    onDelete = { onDeleteConnection(conn) }
                )
            }
        }
    }

    // Modal para Crear / Editar Conexión SMB o FTP
    if (showDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingId == null) stringResource(R.string.network_configurar_servidor_de_red) else stringResource(R.string.network_editar_servidor),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip(
                            text = stringResource(R.string.network_smb_windows_nas),
                            selected = formProtocol == StorageProtocol.SMB,
                            onClick = {
                                formProtocol = StorageProtocol.SMB
                                if (formPort == "21" || formPort == "2121") formPort = "445"
                            },
                            accent = Violet,
                            modifier = Modifier.weight(1f)
                        )
                        SelectChip(
                            text = stringResource(R.string.network_ftp_ftps),
                            selected = formProtocol == StorageProtocol.FTP,
                            onClick = {
                                formProtocol = StorageProtocol.FTP
                                if (formPort == "445") formPort = "21"
                            },
                            accent = Aqua,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = formName,
                        onValueChange = { formName = it },
                        label = { Text(stringResource(R.string.network_nombre_del_equipo)) },
                        singleLine = true,
                        shape = ControlShape,
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formHost,
                            onValueChange = { formHost = it },
                            label = { Text(stringResource(R.string.network_ip_o_host_ej_192)) },
                            singleLine = true,
                            shape = ControlShape,
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formPort,
                            onValueChange = { formPort = it },
                            label = { Text(stringResource(R.string.network_puerto)) },
                            singleLine = true,
                            shape = ControlShape,
                            colors = appTextFieldColors(),
                            modifier = Modifier.width(86.dp)
                        )
                    }

                    if (formProtocol == StorageProtocol.SMB) {
                        OutlinedTextField(
                            value = formShare,
                            onValueChange = { formShare = it },
                            label = { Text(stringResource(R.string.network_carpeta_compartida_vacio_ver_todas)) },
                            singleLine = true,
                            shape = ControlShape,
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (formProtocol == StorageProtocol.FTP) {
                        SettingSwitchRow(
                            title = stringResource(R.string.network_cifrado_tls_ftps),
                            subtitle = if (formTls) stringResource(R.string.network_conexion_y_datos_cifrados_auth) else stringResource(R.string.network_sin_cifrar_usuario_y_contrasena),
                            checked = formTls,
                            onCheckedChange = { formTls = it }
                        )
                    }

                    SettingSwitchRow(
                        title = stringResource(R.string.network_acceso_invitado_sin_contrasena),
                        subtitle = if (formAnon) stringResource(R.string.network_conecta_sin_usuario) else stringResource(R.string.network_ingresar_usuario_y_contrasena_de),
                        checked = formAnon,
                        onCheckedChange = { formAnon = it }
                    )

                    if (!formAnon) {
                        OutlinedTextField(
                            value = formUser,
                            onValueChange = { formUser = it },
                            label = { Text(stringResource(R.string.network_usuario_ej_tu_usuario_de)) },
                            singleLine = true,
                            shape = ControlShape,
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = formPass,
                            onValueChange = { formPass = it },
                            label = { Text(stringResource(R.string.explorer_contrasena)) },
                            singleLine = true,
                            shape = ControlShape,
                            colors = appTextFieldColors(),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val portNum = formPort.toIntOrNull() ?: if (formProtocol == StorageProtocol.SMB) 445 else 21
                    onSaveConnection(
                        editingId,
                        formName,
                        formProtocol,
                        formHost,
                        portNum,
                        formShare,
                        formUser,
                        formPass,
                        formAnon,
                        formPath,
                        formTls
                    )
                    showDialog = false
                }) {
                    Text(stringResource(R.string.explorer_guardar))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.explorer_cancelar))
                }
            }
        )
    }
}

@Composable
private fun SavedConnectionCard(
    conn: NetworkConnection,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassSurface(
        shape = GlassCardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconBubble(icon = Icons.Default.Computer, tint = Violet, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = conn.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = conn.displayAddress,
                            color = Aqua,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val userText = if (conn.anonymous || conn.username.isBlank()) {
                            stringResource(R.string.network_acceso_invitado_anonimo)
                        } else {
                            stringResource(R.string.network_usuario, conn.username)
                        }
                        Text(text = userText, color = TextMuted, fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlassIconButton(icon = Icons.Default.Edit, onClick = onEdit, tint = Violet, contentDescription = stringResource(R.string.network_editar))
                    GlassIconButton(icon = Icons.Default.Delete, onClick = onDelete, tint = Danger, contentDescription = stringResource(R.string.explorer_eliminar))
                }
            }

            PrimaryButton(
                text = stringResource(R.string.network_conectar_y_explorar),
                icon = Icons.Default.FolderOpen,
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DiscoveredHostCard(
    host: DiscoveredHost,
    onConfigureSmb: () -> Unit,
    onConfigureFtp: () -> Unit
) {
    GlassSurface(
        shape = GlassCardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                IconBubble(icon = Icons.Default.Computer, tint = Aqua, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (host.hostname == host.ip) stringResource(R.string.network_unnamed_host, host.ip) else host.hostname,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Tag(text = host.ip, color = Violet)
                        if (host.supportsSmb) Tag(text = stringResource(R.string.network_smb), color = Violet)
                        if (host.supportsFtp) Tag(text = stringResource(R.string.network_ftp), color = Aqua)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (host.supportsSmb) {
                    SecondaryButton(
                        text = stringResource(R.string.network_smb),
                        icon = Icons.Default.Add,
                        onClick = onConfigureSmb,
                        tint = Violet
                    )
                }
                if (host.supportsFtp) {
                    SecondaryButton(
                        text = stringResource(R.string.network_ftp),
                        icon = Icons.Default.Add,
                        onClick = onConfigureFtp,
                        tint = Aqua
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun NetworkScreenPreview() {
    QubitExplorerTheme {
        NetworkScreenContent(
            savedConnections = listOf(
                NetworkConnection(
                    id = "1",
                    name = "Servidor NAS Oficina",
                    protocol = StorageProtocol.SMB,
                    host = "192.168.1.100",
                    port = 445,
                    shareName = "Archivos",
                    username = "admin"
                )
            ),
            discoveredHosts = listOf(
                DiscoveredHost(
                    ip = "192.168.1.50",
                    hostname = "PC-DESK-MAEZ",
                    openPorts = listOf(445, 21)
                )
            ),
            isDiscovering = false,
            lanProgress = 0f,
            onScanLan = {},
            onConnect = {},
            onSaveConnection = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onDeleteConnection = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF120B26)
@Composable
fun DiscoveredHostCardPreview() {
    QubitExplorerTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            DiscoveredHostCard(
                host = DiscoveredHost(
                    ip = "192.168.1.50",
                    hostname = "PC-DESK-MAEZ",
                    openPorts = listOf(445, 21)
                ),
                onConfigureSmb = {},
                onConfigureFtp = {}
            )
        }
    }
}

