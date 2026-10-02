package com.github.premtechworks.synqvia.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Search
import com.github.premtechworks.synqvia.ui.components.ScreenScaffold
import com.github.premtechworks.synqvia.ui.components.ScrollableColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.graphicsLayer
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.StandardEasing
import com.github.premtechworks.synqvia.ui.theme.AccentGreen
import com.github.premtechworks.synqvia.ui.theme.OnPrimaryCyan
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.service.ClipSyncService
import com.github.premtechworks.synqvia.service.SyncConnectionState
import com.github.premtechworks.synqvia.ui.MainTab
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.CircleIconButton
import com.github.premtechworks.synqvia.ui.components.EditMacDialog
import com.github.premtechworks.synqvia.ui.components.IconTile
import com.github.premtechworks.synqvia.ui.components.OutlinedCyanButton
import com.github.premtechworks.synqvia.ui.components.PrimaryButton
import com.github.premtechworks.synqvia.ui.components.RadarSignalIcon
import com.github.premtechworks.synqvia.ui.components.SmallCyanButton
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.components.SynqviaScreen
import com.github.premtechworks.synqvia.ui.components.SynqviaTopBar
import com.github.premtechworks.synqvia.ui.theme.OutlineDark
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SurfaceDark
import com.github.premtechworks.synqvia.ui.theme.SurfaceInset
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary
import com.github.premtechworks.synqvia.ui.theme.TextTertiary
import com.github.premtechworks.synqvia.ui.CardGlassStyle
import com.github.premtechworks.synqvia.ui.GlassCardBorderBrush
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.LocalHazeState
import com.github.premtechworks.synqvia.ui.LocalIsBlurSupported
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.synqviaGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PairedDeviceItem(
    val name: String,
    val mac: String
)

@Composable
fun PairScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    PairScreenContent(
        config = config,
        connectionState = connectionState,
        onBack = onBack,
        onPairAndConnect = { mac, name ->
            viewModel.updateMac(mac)
            if (name.isNotBlank()) {
                viewModel.updateDeviceName(name)
            }
            viewModel.setOnboardingDone(true)
            viewModel.reconnect()
            viewModel.setTab(MainTab.SYNC)
            onNavigateToMain()
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreenContent(
    config: SyncConfig,
    connectionState: SyncConnectionState,
    onBack: () -> Unit,
    onPairAndConnect: (mac: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
    previewPairedDevices: List<PairedDeviceItem>? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasBtPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBtPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasBtPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun getPairedDevices(): List<PairedDeviceItem> {
        if (previewPairedDevices != null) return previewPairedDevices

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter?.isEnabled == true) {
                adapter.bondedDevices?.map { device ->
                    PairedDeviceItem(
                        name = try { device.name ?: "Unknown Device" } catch (_: SecurityException) { "Unknown Device" },
                        mac = device.address
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    val pairedDevices = remember(hasBtPermission) { getPairedDevices() }

    var selectedDevice by remember(config, pairedDevices) {
        val initialMac = config.pcMac
        val foundDevice = pairedDevices.firstOrNull { it.mac.equals(initialMac, ignoreCase = true) }
        mutableStateOf(
            when {
                foundDevice != null -> foundDevice
                initialMac.isNotBlank() -> PairedDeviceItem(
                    name = config.deviceName.ifBlank { "Linux PC" },
                    mac = initialMac
                )
                pairedDevices.isNotEmpty() -> pairedDevices.first()
                else -> null
            }
        )
    }

    val isBluetoothOn = remember(hasBtPermission) {
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            adapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }
    }

    val isDeviceBonded = remember(selectedDevice, pairedDevices) {
        val mac = selectedDevice?.mac
        if (mac.isNullOrBlank()) false
        else pairedDevices.any { it.mac.equals(mac, ignoreCase = true) }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showManualMacDialog by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var screenAlpha by remember { mutableStateOf(1f) }

    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalAppHaptics.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(connectionState) {
        if (isConnecting && connectionState is SyncConnectionState.Connected) {
            isConnecting = false
            isSuccess = true
            haptics.confirm()
            delay(400L)
            if (!reduceMotion) {
                animate(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = tween(250)
                ) { value, _ -> screenAlpha = value }
            }
            selectedDevice?.let { onPairAndConnect(it.mac, it.name) }
        }
    }

    ScreenScaffold(
        modifier = modifier.graphicsLayer { alpha = screenAlpha },
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = "Navigate back"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Pair with your Linux PC",
                    style = SynqviaType.Headline,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                CircleIconButton(
                    icon = Icons.Default.HelpOutline,
                    onClick = { showHelpDialog = true },
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = "Help - How to pair"
                )
            }
        }
    ) { contentPadding ->
        ScrollableColumn(
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .testTag("pair_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Make sure Synqvia is running on your Linux PC and Bluetooth is enabled.",
                style = SynqviaType.Footnote,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // DEVICE CARD OR EMPTY STATE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val currentTarget = selectedDevice
                if (currentTarget != null && currentTarget.mac.isNotBlank()) {
                    val borderFlashColor by animateColorAsState(
                        targetValue = if (isSuccess) AccentGreen else OutlineDark,
                        animationSpec = tween(400),
                        label = "pair_border_flash"
                    )

                    val btnWidth by animateDpAsState(
                        targetValue = if (!reduceMotion && isConnecting) 56.dp else 340.dp,
                        animationSpec = tween(300, easing = StandardEasing),
                        label = "pair_btn_width"
                    )
                    val btnCorner by animateDpAsState(
                        targetValue = if (!reduceMotion && isConnecting) 28.dp else 12.dp,
                        animationSpec = tween(300, easing = StandardEasing),
                        label = "pair_btn_corner"
                    )

                    // Target Device Card (SynqviaCard, 20dp radius, 16dp padding)
                    SynqviaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderFlashColor, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 56dp rounded-12 tile with cyan laptop icon
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceInset)
                                    .border(1.dp, OutlineDark, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Laptop,
                                    contentDescription = null,
                                    tint = PrimaryCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Column: device name and MAC monospace
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTarget.name,
                                    style = SynqviaType.Headline,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentTarget.mac,
                                    style = SynqviaType.Mono.copy(letterSpacing = 0.sp),
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            RadarSignalIcon(
                                isConnecting = isConnecting,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    if (!isConnecting && !isSuccess) {
                                        isConnecting = true
                                        coroutineScope.launch {
                                            delay(1200L)
                                            isConnecting = false
                                            isSuccess = true
                                            haptics.confirm()
                                            delay(400L)
                                            if (!reduceMotion) {
                                                animate(
                                                    initialValue = 1f,
                                                    targetValue = 0f,
                                                    animationSpec = tween(250)
                                                ) { value, _ -> screenAlpha = value }
                                            }
                                            onPairAndConnect(currentTarget.mac, currentTarget.name)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(btnCorner),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryCyan,
                                    contentColor = OnPrimaryCyan
                                ),
                                modifier = Modifier
                                    .width(if (isConnecting) btnWidth else 340.dp)
                                    .fillMaxWidth(if (isConnecting) 0f else 1f)
                                    .height(48.dp)
                                    .pressable(targetScale = 0.96f)
                                    .testTag("pair_connect_btn")
                            ) {
                                Crossfade(
                                    targetState = when {
                                        isSuccess -> "success"
                                        isConnecting -> "connecting"
                                        else -> "idle"
                                    },
                                    animationSpec = tween(150),
                                    label = "pair_btn_crossfade"
                                ) { state ->
                                    when (state) {
                                        "connecting" -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = OnPrimaryCyan,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                        "success" -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = OnPrimaryCyan,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Connected",
                                                    style = SynqviaType.Button.copy(color = OnPrimaryCyan)
                                                )
                                            }
                                        }
                                        else -> {
                                            Text(
                                                text = "Pair & Connect",
                                                style = SynqviaType.Button.copy(color = OnPrimaryCyan)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty State: No paired devices found
                    SynqviaCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        padding = 20.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconTile(
                                icon = Icons.Default.Bluetooth,
                                tint = PrimaryCyan,
                                size = 48.dp,
                                iconSize = 24.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No paired devices found",
                                style = SynqviaType.Headline,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Make sure your PC is paired in Android Bluetooth settings first.",
                                style = SynqviaType.Footnote,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SmallCyanButton(
                                text = "Open Bluetooth Settings",
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Below device card: OutlinedCyanButton "Select Different Device" (height 48dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedCyanButton(
                    text = "Select Different Device",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBtPermission) {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                        showBottomSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_different_device_btn"),
                    height = 48.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "Before you connect" card with 3 numbered live status rows
            BeforeYouConnectCard(
                isBluetoothOn = isBluetoothOn,
                isDaemonRunning = connectionState is SyncConnectionState.Connected || isConnecting || isSuccess,
                isPaired = isDeviceBonded
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

        // Bottom Sheet: Paired Devices Selection
        val hazeState = LocalHazeState.current
        val isBlurSupported = LocalIsBlurSupported.current

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = bottomSheetState,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .synqviaGlass(
                            hazeState = hazeState,
                            style = CardGlassStyle,
                            isBlurSupported = isBlurSupported,
                            fallbackColor = SurfaceDark,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .border(1.dp, GlassCardBorderBrush, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 24.dp, bottom = 32.dp)
                    ) {
                        Text(
                            text = "Paired Bluetooth Devices",
                            style = SynqviaType.Headline,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select your Linux machine running Synqvia.",
                            style = SynqviaType.Caption,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (pairedDevices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Bluetooth devices found paired with this phone.",
                                    style = SynqviaType.Body,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pairedDevices) { device ->
                                    val isSelected = selectedDevice?.mac.equals(device.mac, ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) SurfaceInset else SurfaceDark)
                                            .border(
                                                1.dp,
                                                if (isSelected) PrimaryCyan.copy(alpha = 0.5f) else OutlineDark,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .pressable(
                                                targetScale = 0.98f,
                                                showOverlay = true,
                                                onClick = {
                                                    haptics.tick()
                                                    selectedDevice = device
                                                    showBottomSheet = false
                                                }
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Laptop,
                                            contentDescription = null,
                                            tint = if (isSelected) PrimaryCyan else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = device.name,
                                                style = SynqviaType.Body,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = device.mac,
                                                style = SynqviaType.MonoSmall.copy(letterSpacing = 0.sp),
                                                color = TextSecondary
                                            )
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedDevice = device
                                                showBottomSheet = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = PrimaryCyan,
                                                unselectedColor = TextTertiary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedCyanButton(
                            text = "Enter MAC manually",
                            onClick = {
                                showBottomSheet = false
                                showManualMacDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp
                        )
                    }
                }
            }
        }

        // Manual MAC Dialog
        if (showManualMacDialog) {
            EditMacDialog(
                initialMac = selectedDevice?.mac ?: config.pcMac,
                onDismiss = { showManualMacDialog = false },
                onConfirm = { customMac ->
                    selectedDevice = PairedDeviceItem(
                        name = "Custom PC",
                        mac = customMac
                    )
                    showManualMacDialog = false
                }
            )
        }

        // Help "?" Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = SurfaceDark,
                title = {
                    Text(
                        text = "Pairing with Linux",
                        style = SynqviaType.Headline,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "To enable RFCOMM syncing on your Linux PC:",
                            style = SynqviaType.Body,
                            color = TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceInset)
                                .border(1.dp, OutlineDark, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "$ synqvia-daemon\n# Or verify with bluetoothctl:\n$ bluetoothctl show",
                                style = SynqviaType.MonoSmall,
                                color = PrimaryCyan
                            )
                        }
                        Text(
                            text = "Ensure your phone is paired in your Linux Bluetooth settings first.",
                            style = SynqviaType.Caption,
                            color = TextTertiary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("Got it", color = PrimaryCyan)
                    }
                }
            )
        }
}

/**
 * "Before you connect" card with 3 numbered live status rows:
 * (1) Bluetooth on; (2) Synqvia running on PC (mention synqvia --show); (3) PC paired with phone (bonded check).
 * Rows tick green as satisfied.
 */
@Composable
private fun BeforeYouConnectCard(
    isBluetoothOn: Boolean,
    isDaemonRunning: Boolean,
    isPaired: Boolean,
    modifier: Modifier = Modifier
) {
    SynqviaCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        padding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Before you connect",
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            BeforeConnectStepRow(
                stepNumber = 1,
                title = "Bluetooth on",
                subtitle = if (isBluetoothOn) "Bluetooth is enabled on this device" else "Enable Bluetooth in Android settings",
                isSatisfied = isBluetoothOn
            )

            Spacer(modifier = Modifier.height(12.dp))

            BeforeConnectStepRow(
                stepNumber = 2,
                title = "Synqvia running on PC",
                subtitle = if (isDaemonRunning) "Daemon active and responding" else "Run synqvia --show to verify daemon",
                isSatisfied = isDaemonRunning
            )

            Spacer(modifier = Modifier.height(12.dp))

            BeforeConnectStepRow(
                stepNumber = 3,
                title = "PC paired with phone",
                subtitle = if (isPaired) "Device is bonded in Bluetooth settings" else "Pair device in Android Bluetooth settings first",
                isSatisfied = isPaired
            )
        }
    }
}

@Composable
private fun BeforeConnectStepRow(
    stepNumber: Int,
    title: String,
    subtitle: String,
    isSatisfied: Boolean
) {
    val circleBg by animateColorAsState(
        targetValue = if (isSatisfied) AccentGreen.copy(alpha = 0.15f) else SurfaceInset,
        animationSpec = tween(300),
        label = "pair_step_bg_$stepNumber"
    )
    val circleBorder by animateColorAsState(
        targetValue = if (isSatisfied) AccentGreen else OutlineDark,
        animationSpec = tween(300),
        label = "pair_step_border_$stepNumber"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(circleBg)
                .border(1.dp, circleBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isSatisfied,
                animationSpec = tween(200),
                label = "pair_step_icon_$stepNumber"
            ) { satisfied ->
                if (satisfied) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Step $stepNumber completed",
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        style = SynqviaType.FootnoteSemiBold,
                        color = TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SynqviaType.SubheadlineSemiBold,
                color = if (isSatisfied) TextPrimary else TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = SynqviaType.Caption,
                color = if (isSatisfied) AccentGreen.copy(alpha = 0.85f) else TextTertiary
            )
        }
    }
}

@Preview(name = "Pair Screen - Device Found", showBackground = true)
@Composable
fun PreviewPairScreenDeviceFound() {
    SynqviaTheme {
        PairScreenContent(
            config = SyncConfig(
                pcMac = "F8:34:41:53:BE:24",
                channel = 1,
                historyCap = 500,
                deviceName = "prem-pc",
                deviceId = "android-device"
            ),
            connectionState = SyncConnectionState.Stopped,
            onBack = {},
            onPairAndConnect = { _, _ -> },
            previewPairedDevices = listOf(
                PairedDeviceItem("prem-pc", "F8:34:41:53:BE:24"),
                PairedDeviceItem("work-thinkpad", "00:1A:7D:DA:71:13")
            )
        )
    }
}

@Preview(name = "Pair Screen - Empty State", showBackground = true)
@Composable
fun PreviewPairScreenEmpty() {
    SynqviaTheme {
        PairScreenContent(
            config = SyncConfig(
                pcMac = "",
                channel = 1,
                historyCap = 500,
                deviceName = "",
                deviceId = "android-device"
            ),
            connectionState = SyncConnectionState.Stopped,
            onBack = {},
            onPairAndConnect = { _, _ -> },
            previewPairedDevices = emptyList()
        )
    }
}
