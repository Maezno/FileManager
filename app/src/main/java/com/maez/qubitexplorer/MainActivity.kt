package com.maez.qubitexplorer

import com.maez.qubitexplorer.ui.theme.Motion
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.maez.qubitexplorer.ui.components.AppBackground
import com.maez.qubitexplorer.ui.components.AppBottomNavigationBar
import com.maez.qubitexplorer.ui.components.GlassAlertDialog
import com.maez.qubitexplorer.ui.components.GlassSurface
import com.maez.qubitexplorer.ui.components.IconBadge
import com.maez.qubitexplorer.ui.components.MediaPreviewDialog
import com.maez.qubitexplorer.ui.screens.ExplorerScreen
import com.maez.qubitexplorer.ui.screens.NetworkScreen
import com.maez.qubitexplorer.ui.screens.SettingsScreen
import com.maez.qubitexplorer.ui.screens.SmartSearchScreen
import com.maez.qubitexplorer.ui.theme.Aqua
import com.maez.qubitexplorer.ui.theme.QubitExplorerTheme
import com.maez.qubitexplorer.ui.theme.TextMuted
import com.maez.qubitexplorer.ui.theme.TextPrimary
import com.maez.qubitexplorer.ui.theme.TextSecondary
import com.maez.qubitexplorer.ui.theme.WarningAmber
import com.maez.qubitexplorer.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasAllFilesAccess by mutableStateOf(false)
    private var showProminentDisclosure by mutableStateOf(false)

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updatePermissionState()
        viewModel.refreshCurrentDirectory()
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionState()
        viewModel.refreshCurrentDirectory()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Si el usuario acepta o rechaza, la transferencia sigue funcionando de fondo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updatePermissionState()

        setContent {
            QubitExplorerTheme {
                val selectedTab by viewModel.selectedTab.collectAsState()
                val previewItem by viewModel.previewItem.collectAsState()
                val previewLocalPath by viewModel.previewLocalPath.collectAsState()
                val isPreviewLoading by viewModel.isPreviewLoading.collectAsState()

                BackHandler(enabled = true) {
                    if (selectedTab != 0) {
                        viewModel.selectTab(0)
                    } else {
                        if (!viewModel.navigateUp()) {
                            finish()
                        }
                    }
                }

                
                    MainScreenContent(
                        selectedTab = selectedTab,
                        hasAllFilesAccess = hasAllFilesAccess,
                        onTabSelected = { viewModel.selectTab(it) },
                        onRequestStoragePermissions = { requestStoragePermissions() }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    val forward = targetState > initialState
                                    Motion.tabEnter(forward).togetherWith(Motion.tabExit(forward))
                                },
                                label = "TabScreenTransition"
                            ) { targetTab ->
                                when (targetTab) {
                                    0 -> ExplorerScreen(
                                        viewModel = viewModel,
                                        hasAllFilesAccess = hasAllFilesAccess,
                                        onRequestStoragePermissions = { requestStoragePermissions() }
                                    )
                                    1 -> SmartSearchScreen(viewModel = viewModel)
                                    2 -> NetworkScreen(viewModel = viewModel)
                                    3 -> SettingsScreen(
                                        viewModel = viewModel,
                                        hasAllFilesAccess = hasAllFilesAccess,
                                        onRequestStoragePermissions = { requestStoragePermissions() }
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = previewItem != null,
                                enter = Motion.dialogEnter,
                                exit = Motion.dialogExit
                            ) {
                                previewItem?.let { item ->
                                    MediaPreviewDialog(
                                        item = item,
                                        localPreviewPath = previewLocalPath,
                                        isLoadingRemote = isPreviewLoading,
                                        onOpenExternal = {
                                            viewModel.closePreview()
                                            viewModel.openInExternalApp(item)
                                        },
                                        onShare = {
                                            viewModel.shareItem(item)
                                        },
                                        onOpenFolder = {
                                            viewModel.closePreview()
                                            viewModel.selectTab(0)
                                            val conn = item.connectionId?.let { id ->
                                                viewModel.savedConnections.value.find { it.id == id }
                                            } ?: viewModel.activeConnection.value
                                            viewModel.loadDirectory(item.parentPath, item.protocol, conn)
                                        },
                                        onDismiss = { viewModel.closePreview() }
                                    )
                                }
                            }
                        }
                    }

                    if (showProminentDisclosure) {
                        GlassAlertDialog(
                            onDismissRequest = { showProminentDisclosure = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconBadge(icon = Icons.Default.Security, tint = Aqua, size = 32)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Acceso a Todos los Archivos",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            text = {
                                Text(
                                    text = "Por qué Qubit Explorer necesita acceso a todos los archivos: Para explorar carpetas del dispositivo, detectar archivos duplicados y transferir documentos o multimedia entre tu memoria interna y tus servidores de red local (SMB/FTP).",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { launchManageStorageIntent() }) {
                                    Text(
                                        "Continuar a Ajustes",
                                        color = Aqua,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showProminentDisclosure = false }) {
                                    Text(
                                        "Ahora no",
                                        color = TextMuted
                                    )
                                }
                            }
                        )
                    }
                
            }
        }
    }

    private fun requestNotificationPermissionIfRequired() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestNotificationPermissionIfRequired()
        val prev = hasAllFilesAccess
        updatePermissionState()
        if (!prev && hasAllFilesAccess) {
            viewModel.refreshCurrentDirectory()
        }
    }

    private fun updatePermissionState() {
        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess) {
            showProminentDisclosure = true
        } else {
            launchManageStorageIntent()
        }
    }

    private fun launchManageStorageIntent() {
        showProminentDisclosure = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    selectedTab: Int,
    hasAllFilesAccess: Boolean,
    onTabSelected: (Int) -> Unit,
    onRequestStoragePermissions: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    AppBackground(modifier = Modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "Qubit Explorer",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Qubit Explorer",
                                        color = TextPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val statusColor = if (hasAllFilesAccess) Aqua else WarningAmber
                            GlassSurface(
                                shape = RoundedCornerShape(50),
                                fillColor = statusColor.copy(alpha = 0.12f),
                                borderColor = statusColor.copy(alpha = 0.4f),
                                modifier = Modifier.clickable(enabled = !hasAllFilesAccess) { onRequestStoragePermissions() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (hasAllFilesAccess) "Acceso total" else "Dar permiso",
                                        color = statusColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                AppBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    QubitExplorerTheme {
        MainScreenContent(
            selectedTab = 0,
            hasAllFilesAccess = true,
            onTabSelected = {},
            onRequestStoragePermissions = {},
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        )
    }
}
