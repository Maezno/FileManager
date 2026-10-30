package com.maez.filemanager

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.maez.filemanager.ui.components.AppBottomNavigationBar
import com.maez.filemanager.ui.components.MediaPreviewDialog
import com.maez.filemanager.ui.screens.ExplorerScreen
import com.maez.filemanager.ui.screens.NetworkScreen
import com.maez.filemanager.ui.screens.SmartSearchScreen
import com.maez.filemanager.ui.theme.AccentGreenLight
import com.maez.filemanager.ui.theme.BgDark
import com.maez.filemanager.ui.theme.BorderDark
import com.maez.filemanager.ui.theme.CardDark
import com.maez.filemanager.ui.theme.FileManagerTheme
import com.maez.filemanager.ui.theme.TextPrimary
import com.maez.filemanager.ui.theme.WarningAmber
import com.maez.filemanager.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasAllFilesAccess by mutableStateOf(false)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updatePermissionState()

        setContent {
            FileManagerTheme {
                val selectedTab by viewModel.selectedTab.collectAsState()
                val previewItem by viewModel.previewItem.collectAsState()
                val previewLocalPath by viewModel.previewLocalPath.collectAsState()
                val isPreviewLoading by viewModel.isPreviewLoading.collectAsState()

                BackHandler(enabled = selectedTab == 0) {
                    if (!viewModel.navigateUp()) {
                        finish()
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
                        when (selectedTab) {
                            0 -> ExplorerScreen(viewModel = viewModel)
                            1 -> SmartSearchScreen(viewModel = viewModel)
                            2 -> NetworkScreen(viewModel = viewModel)
                        }

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
        }
    }

    override fun onResume() {
        super.onResume()
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
    Scaffold(
        containerColor = BgDark,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CardDark),
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
                                    contentDescription = "FileManager Studio",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "FileManager",
                                        color = TextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "STUDIO MOBILE",
                                        color = AccentGreenLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )
                                }
                            }

                            val statusColor = if (hasAllFilesAccess) AccentGreenLight else WarningAmber
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(statusColor.copy(alpha = 0.12f))
                                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                                    .clickable(enabled = !hasAllFilesAccess) { onRequestStoragePermissions() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
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
                                    text = if (hasAllFilesAccess) "Acceso Total" else "Dar Permiso",
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(color = BorderDark, thickness = 1.dp)
            }
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FileManagerTheme {
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
