package com.github.premtechworks.synqvia.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.derivedStateOf
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.RollingNumber
import com.github.premtechworks.synqvia.ui.motion.entryStagger
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.motion.snappySpring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import com.github.premtechworks.synqvia.ui.components.ScreenScaffold
import com.github.premtechworks.synqvia.ui.components.ScrollableColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.ui.LogItem
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.IconTile
import com.github.premtechworks.synqvia.ui.components.OneTapPathsInfoCard
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.components.SynqviaScreen
import com.github.premtechworks.synqvia.ui.theme.AccentBlue
import com.github.premtechworks.synqvia.ui.theme.AccentRed
import com.github.premtechworks.synqvia.ui.theme.DividerDark
import com.github.premtechworks.synqvia.ui.theme.OutlineDark
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.StatusConnected
import com.github.premtechworks.synqvia.ui.theme.StatusRetrying
import com.github.premtechworks.synqvia.ui.theme.SurfaceDark
import com.github.premtechworks.synqvia.ui.theme.SurfaceHigh
import com.github.premtechworks.synqvia.ui.theme.SurfaceInset
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary
import com.github.premtechworks.synqvia.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.roundToInt

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import com.github.premtechworks.synqvia.ui.components.SynqviaSwitch
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.RollingNumber
import com.github.premtechworks.synqvia.ui.motion.pressable

val TerminalIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Terminal",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(20f, 4f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(12f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(20f, 18f)
        horizontalLineTo(4f)
        verticalLineTo(8f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(10f)
        close()
        moveTo(7.5f, 13f)
        lineToRelative(-2.5f, 2.5f)
        lineToRelative(-1.4f, -1.4f)
        lineToRelative(1.1f, -1.1f)
        lineToRelative(-1.1f, -1.1f)
        lineToRelative(1.4f, -1.4f)
        lineTo(7.5f, 13f)
        close()
        moveTo(9.5f, 15f)
        horizontalLineToRelative(5f)
        verticalLineToRelative(1.5f)
        horizontalLineToRelative(-5f)
        close()
    }.build()
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    val config by viewModel.config.collectAsState()
    val logs by viewModel.diagnosticLogs.collectAsState()

    val autoSync by viewModel.autoSync.collectAsState()
    val syncTextOnly by viewModel.syncTextOnly.collectAsState()
    val clearOnDisconnect by viewModel.clearOnDisconnect.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val reduceMotionFollowSystem by viewModel.reduceMotionFollowSystem.collectAsState()
    val hapticFeedback by viewModel.hapticFeedback.collectAsState()

    val clipboardManager = LocalClipboardManager.current

    SettingsScreenContent(
        config = config,
        logs = logs,
        autoSync = autoSync,
        syncTextOnly = syncTextOnly,
        clearOnDisconnect = clearOnDisconnect,
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        reduceMotionFollowSystem = reduceMotionFollowSystem,
        hapticFeedback = hapticFeedback,
        scrollState = scrollState,
        onMacChange = { newMac -> viewModel.updateMac(newMac) },
        onChannelChange = { newChannel -> viewModel.updateChannel(newChannel) },
        onDeviceNameChange = { newName -> viewModel.updateDeviceName(newName) },
        onHistoryCapChange = { newCap -> viewModel.updateHistoryCapacity(newCap) },
        onAutoSyncChange = { viewModel.setAutoSync(it) },
        onSyncTextOnlyChange = { viewModel.setSyncTextOnly(it) },
        onClearOnDisconnectChange = { viewModel.setClearOnDisconnect(it) },
        onThemeModeChange = { viewModel.setThemeMode(it) },
        onDynamicColorChange = { viewModel.setDynamicColor(it) },
        onReduceMotionFollowSystemChange = { viewModel.setReduceMotionFollowSystem(it) },
        onHapticFeedbackChange = { viewModel.setHapticFeedback(it) },
        onCopyMac = { mac ->
            clipboardManager.setText(AnnotatedString(mac))
            viewModel.showUserMessage("Copied")
        },
        onCopyLogs = {
            val fullLogText = logs.joinToString("\n") { "${it.formattedTime} [${it.level}] ${it.message}" }
            clipboardManager.setText(AnnotatedString(fullLogText))
            viewModel.showUserMessage("Diagnostic log copied to clipboard")
        },
        onClearLogs = { viewModel.clearLogs() },
        modifier = modifier
    )
}

@Composable
fun SettingsScreenContent(
    config: SyncConfig,
    logs: List<LogItem>,
    autoSync: Boolean,
    syncTextOnly: Boolean,
    clearOnDisconnect: Boolean,
    themeMode: String,
    dynamicColor: Boolean,
    reduceMotionFollowSystem: Boolean = true,
    hapticFeedback: Boolean = true,
    onMacChange: (String) -> Boolean,
    onChannelChange: (Int) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onHistoryCapChange: (Int) -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onSyncTextOnlyChange: (Boolean) -> Unit,
    onClearOnDisconnectChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onReduceMotionFollowSystemChange: (Boolean) -> Unit = {},
    onHapticFeedbackChange: (Boolean) -> Unit = {},
    onCopyMac: (String) -> Unit,
    onCopyLogs: () -> Unit = {},
    onClearLogs: () -> Unit = {},
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    var showMacDialog by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current

    val isScrolled by remember { derivedStateOf { scrollState.value > 8 } }

    ScreenScaffold(
        modifier = modifier,
        isScrolled = isScrolled,
        header = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = SynqviaType.LargeTitle,
                    color = TextPrimary
                )
            }
        }
    ) { contentPadding ->
        ScrollableColumn(
            contentPadding = contentPadding,
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // B) CARD 1: "Connection & Device"
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 0),
                padding = 16.dp
            ) {
                Text(
                    text = "Connection & Device",
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. MAC row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { showMacDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linux PC Bluetooth MAC",
                            style = SynqviaType.Caption,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = config.pcMac.ifBlank { "AA:BB:CC:DD:EE:FF" },
                            style = SynqviaType.Mono,
                            color = if (config.pcMac.isNotBlank()) TextPrimary else TextTertiary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onCopyMac(config.pcMac) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy MAC",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DividerItem()

                // 2. RFCOMM Channel row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RFCOMM Channel",
                        style = SynqviaType.Body,
                        color = TextPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RollingNumber(
                            value = config.channel,
                            style = SynqviaType.HeadlineTnum,
                            color = PrimaryCyan
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        // Circular "-" button (28dp) with press-pop
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SurfaceHigh)
                                .border(1.dp, OutlineDark, CircleShape)
                                .pressable(
                                    targetScale = 0.90f,
                                    enabled = config.channel > 1,
                                    onClick = {
                                        if (config.channel > 1) {
                                            haptics.tick()
                                            onChannelChange(config.channel - 1)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                style = SynqviaType.Headline,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Circular "+" button (28dp) with press-pop
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SurfaceHigh)
                                .border(1.dp, OutlineDark, CircleShape)
                                .pressable(
                                    targetScale = 0.90f,
                                    enabled = config.channel < 30,
                                    onClick = {
                                        if (config.channel < 30) {
                                            haptics.tick()
                                            onChannelChange(config.channel + 1)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                style = SynqviaType.Headline,
                                color = TextPrimary
                            )
                        }
                    }
                }

                DividerItem()

                // 3. Broadcast Device Name row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { showDeviceNameDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Broadcast Device Name",
                        style = SynqviaType.Body,
                        color = TextPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = config.deviceName,
                            style = SynqviaType.Footnote,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DividerItem()

                // 4. History Capacity row + Dotted Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History Capacity",
                        style = SynqviaType.Body,
                        color = TextPrimary
                    )
                    if (config.historyCap == 0) {
                        Text(
                            text = "Unlimited",
                            style = SynqviaType.CaptionTnum,
                            color = TextSecondary
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RollingNumber(
                                value = config.historyCap,
                                style = SynqviaType.CaptionTnum,
                                color = TextSecondary
                            )
                            Text(
                                text = " items",
                                style = SynqviaType.CaptionTnum,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                DottedCapacitySlider(
                    value = config.historyCap,
                    onValueChange = onHistoryCapChange
                )
            }

            // C) CARD 2: "Data & Sync"
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 1),
                padding = 16.dp
            ) {
                Text(
                    text = "Data & Sync",
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 1: Auto Sync
                SettingsToggleRow(
                    title = "Auto Sync",
                    subtitle = "Sync clipboard changes automatically",
                    leadingIcon = Icons.Default.Sync,
                    checked = autoSync,
                    onCheckedChange = onAutoSyncChange
                )

                DividerItem()

                // Toggle 2: Sync Text Only
                SettingsToggleRow(
                    title = "Sync Text Only",
                    subtitle = "Images and files are not synced (v1)",
                    leadingIcon = Icons.Default.TextFields,
                    checked = syncTextOnly,
                    onCheckedChange = onSyncTextOnlyChange
                )

                DividerItem()

                // Toggle 3: Clear on Device Disconnect
                SettingsToggleRow(
                    title = "Clear on Device Disconnect",
                    subtitle = "Clear remote clipboard when disconnected",
                    leadingIcon = Icons.Default.DeleteSweep,
                    checked = clearOnDisconnect,
                    onCheckedChange = onClearOnDisconnectChange
                )
            }

            // D) CARD 3: "Appearance"
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 2),
                padding = 16.dp
            ) {
                Text(
                    text = "Appearance",
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Theme selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { showThemeDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentBlue.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Theme",
                        style = SynqviaType.Body,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (themeMode.lowercase(Locale.ROOT)) {
                                "light" -> "Light"
                                "dark" -> "Dark"
                                else -> "System"
                            },
                            style = SynqviaType.Footnote,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DividerItem()

                // Use Dynamic Color
                val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                SettingsToggleRow(
                    title = "Use Dynamic Color",
                    subtitle = "Match your Android theme",
                    leadingIcon = Icons.Default.ColorLens,
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    enabled = isDynamicColorSupported
                )

                DividerItem()

                // Reduce Motion: follow system
                SettingsToggleRow(
                    title = "Reduce Motion",
                    subtitle = "Follow system accessibility motion preference",
                    checked = reduceMotionFollowSystem,
                    onCheckedChange = onReduceMotionFollowSystemChange
                )

                DividerItem()

                // Haptic Feedback
                SettingsToggleRow(
                    title = "Haptic Feedback",
                    subtitle = "Vibration ticks on taps, toggles, and sync actions",
                    checked = hapticFeedback,
                    onCheckedChange = onHapticFeedbackChange
                )
            }

            // H) BELOW CARD 3: OneTapPathsInfoCard placed right above the log card
            OneTapPathsInfoCard(
                modifier = Modifier.entryStagger(index = 3)
            )

            // E) Collapsible Protocol Diagnostic Log
            val logHaptics = LocalAppHaptics.current
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(snappySpring())
                    .entryStagger(index = 4),
                padding = 12.dp
            ) {
                val chevronRotation by animateFloatAsState(
                    targetValue = if (showLogs) 180f else 0f,
                    animationSpec = tween(durationMillis = 250, easing = DecelerateEasing),
                    label = "log_chevron"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            logHaptics.tick()
                            showLogs = !showLogs
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TerminalIcon,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Protocol Diagnostic Log",
                            style = SynqviaType.Headline,
                            color = TextPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showLogs) "Hide logs" else "Show logs",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }

                AnimatedVisibility(visible = showLogs) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceInset)
                            .border(1.dp, OutlineDark, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "No diagnostic events logged yet.",
                                    style = SynqviaType.MonoSmall,
                                    color = TextTertiary
                                )
                            } else {
                                logs.takeLast(50).forEach { log ->
                                    val badgeColor = when (log.level) {
                                        "SUCCESS" -> StatusConnected
                                        "FRAME" -> PrimaryCyan
                                        "WARN" -> StatusRetrying
                                        "ERROR" -> AccentRed
                                        else -> TextSecondary
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = log.formattedTime,
                                            style = SynqviaType.MonoSmall,
                                            color = TextTertiary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "[${log.level}]",
                                            style = SynqviaType.MonoSmallMedium,
                                            color = badgeColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = log.message,
                                            style = SynqviaType.MonoSmall,
                                            color = TextSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // "Copy log" and "Clear" text buttons
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Copy log",
                                style = SynqviaType.ButtonSmall.copy(color = PrimaryCyan),
                                color = PrimaryCyan,
                                modifier = Modifier
                                    .clickable(role = Role.Button) {
                                        logHaptics.confirm()
                                        onCopyLogs()
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear",
                                style = SynqviaType.ButtonSmall.copy(color = AccentRed),
                                color = AccentRed,
                                modifier = Modifier
                                    .clickable(role = Role.Button) {
                                        logHaptics.reject()
                                        onClearLogs()
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: "Synqvia 1.0.0 - GPL-3.0" centered, Caption style, textTertiary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Synqvia 1.0.0 - GPL-3.0",
                    style = SynqviaType.Caption,
                    color = TextTertiary
                )
            }
        }
    }

    // Dialogs
    if (showMacDialog) {
        EditMacDialog(
            initialMac = config.pcMac,
            onDismiss = { showMacDialog = false },
            onConfirm = { newMac ->
                val success = onMacChange(newMac)
                if (success) showMacDialog = false
            }
        )
    }

    if (showDeviceNameDialog) {
        EditDeviceNameDialog(
            initialName = config.deviceName,
            deviceId = config.deviceId,
            onDismiss = { showDeviceNameDialog = false },
            onConfirm = { newName ->
                onDeviceNameChange(newName)
                showDeviceNameDialog = false
            }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelectTheme = { selected ->
                onThemeModeChange(selected)
                showThemeDialog = false
            }
        )
    }
}

@Composable
private fun DividerItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(1.dp)
            .background(DividerDark)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentBlue.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = SynqviaType.Body,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = SynqviaType.Caption,
                color = TextSecondary
            )
        }

        SynqviaSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Custom Dotted Slider for History Capacity (0..2000, step 50):
 * - Track is a row of small dots (3dp, spacing ~8dp).
 * - Active dots cyan, inactive dots white @ 20%.
 * - Taller tick every 5th dot.
 * - Thumb: 4dp x 18dp cyan rounded bar, scales 1.25 while dragging, tick haptic on every step.
 */
@Composable
fun DottedCapacitySlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(value / 2000f) }
    val haptics = LocalAppHaptics.current
    val reduceMotion = LocalReduceMotion.current

    val thumbScale by animateFloatAsState(
        targetValue = if (!reduceMotion && isDragging) 1.25f else 1.0f,
        animationSpec = snappySpring(),
        label = "slider_thumb_scale"
    )

    LaunchedEffect(value) {
        if (!isDragging) {
            dragProgress = (value / 2000f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val paddingPx = 8.dp.toPx()
                    val trackWidth = (size.width - 2 * paddingPx).coerceAtLeast(1f)
                    val fraction = ((offset.x - paddingPx) / trackWidth).coerceIn(0f, 1f)
                    val stepIndex = (fraction * 40f).roundToInt()
                    val newValue = stepIndex * 50
                    dragProgress = fraction
                    if (newValue != value) {
                        haptics.tick()
                        onValueChange(newValue)
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val paddingPx = 8.dp.toPx()
                        val trackWidth = (size.width - 2 * paddingPx).coerceAtLeast(1f)
                        val fraction = ((offset.x - paddingPx) / trackWidth).coerceIn(0f, 1f)
                        val stepIndex = (fraction * 40f).roundToInt()
                        val newValue = stepIndex * 50
                        dragProgress = fraction
                        if (newValue != value) {
                            haptics.tick()
                            onValueChange(newValue)
                        }
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val paddingPx = 8.dp.toPx()
                        val trackWidth = (size.width - 2 * paddingPx).coerceAtLeast(1f)
                        val fraction = ((change.position.x - paddingPx) / trackWidth).coerceIn(0f, 1f)
                        val stepIndex = (fraction * 40f).roundToInt()
                        val newValue = stepIndex * 50
                        dragProgress = fraction
                        if (newValue != value) {
                            haptics.tick()
                            onValueChange(newValue)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingPx = 8.dp.toPx()
            val trackWidth = size.width - 2 * paddingPx
            val centerY = size.height / 2f
            val dotRadius = 1.5.dp.toPx()
            val currentProgress = (value / 2000f).coerceIn(0f, 1f)
            val thumbX = paddingPx + currentProgress * trackWidth

            // Draw dots (41 dots for 0..2000 in steps of 50)
            val totalDots = 40
            for (i in 0..totalDots) {
                val dotX = paddingPx + (i.toFloat() / totalDots) * trackWidth
                val isActive = dotX <= thumbX + 1f
                val dotColor = if (isActive) PrimaryCyan else Color.White.copy(alpha = 0.20f)

                if (i % 5 == 0) {
                    // Taller tick every 5th dot
                    val tickHalfHeight = 4.dp.toPx()
                    val tickWidth = 2.dp.toPx()
                    drawRoundRect(
                        color = dotColor,
                        topLeft = Offset(dotX - tickWidth / 2f, centerY - tickHalfHeight),
                        size = Size(tickWidth, tickHalfHeight * 2),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                } else {
                    // Regular 3dp dot
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(dotX, centerY)
                    )
                }
            }

            // Draw thumb: scales 1.25 while dragging
            val baseThumbWidth = 4.dp.toPx()
            val baseThumbHeight = 18.dp.toPx()
            val scaledWidth = baseThumbWidth * thumbScale
            val scaledHeight = baseThumbHeight * thumbScale
            drawRoundRect(
                color = PrimaryCyan,
                topLeft = Offset(thumbX - scaledWidth / 2f, centerY - scaledHeight / 2f),
                size = Size(scaledWidth, scaledHeight),
                cornerRadius = CornerRadius(2.dp.toPx() * thumbScale)
            )
        }
    }
}

@Composable
private fun EditMacDialog(
    initialMac: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialMac) }
    val isValid = remember(text) { text.isBlank() || SyncPreferences.isValidMac(text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit PC Bluetooth MAC", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.uppercase(Locale.ROOT) },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF", color = TextTertiary) },
                    singleLine = true,
                    isError = !isValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = OutlineDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    )
                )
                if (!isValid) {
                    Text(
                        text = "Invalid MAC format (must be XX:XX:XX:XX:XX:XX)",
                        style = SynqviaType.Caption,
                        color = AccentRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = isValid
            ) {
                Text("Save", color = if (isValid) PrimaryCyan else TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EditDeviceNameDialog(
    initialName: String,
    deviceId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Broadcast Device Name", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(48) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = OutlineDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Device ID: $deviceId",
                    style = SynqviaType.MonoSmall,
                    color = TextTertiary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save", color = PrimaryCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ThemePickerDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelectTheme: (String) -> Unit
) {
    val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme", color = TextPrimary) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (key == currentTheme),
                                onClick = { onSelectTheme(key) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (key == currentTheme),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryCyan)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = SynqviaType.Body, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

// ==========================================
// Previews
// ==========================================

@Preview(name = "Settings - Dark Theme", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    SynqviaTheme(darkTheme = true) {
        SettingsScreenContent(
            config = SyncConfig(
                pcMac = "F8:34:41:53:BE:24",
                channel = 1,
                historyCap = 500,
                deviceName = "Android (a386a5)",
                deviceId = "android-a386a5"
            ),
            logs = listOf(
                LogItem(level = "SUCCESS", message = "Connected to Linux PC"),
                LogItem(level = "FRAME", message = "Sync ACK frame received")
            ),
            autoSync = true,
            syncTextOnly = true,
            clearOnDisconnect = false,
            themeMode = "dark",
            dynamicColor = false,
            onMacChange = { true },
            onChannelChange = {},
            onDeviceNameChange = {},
            onHistoryCapChange = {},
            onAutoSyncChange = {},
            onSyncTextOnlyChange = {},
            onClearOnDisconnectChange = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onCopyMac = {},
            onClearLogs = {}
        )
    }
}

@Preview(name = "Settings - Light Theme", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    SynqviaTheme(themeMode = "light", darkTheme = false) {
        SettingsScreenContent(
            config = SyncConfig(
                pcMac = "F8:34:41:53:BE:24",
                channel = 1,
                historyCap = 500,
                deviceName = "Android (a386a5)",
                deviceId = "android-a386a5"
            ),
            logs = emptyList(),
            autoSync = true,
            syncTextOnly = true,
            clearOnDisconnect = false,
            themeMode = "light",
            dynamicColor = false,
            onMacChange = { true },
            onChannelChange = {},
            onDeviceNameChange = {},
            onHistoryCapChange = {},
            onAutoSyncChange = {},
            onSyncTextOnlyChange = {},
            onClearOnDisconnectChange = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onCopyMac = {},
            onClearLogs = {}
        )
    }
}
