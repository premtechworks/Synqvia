package com.github.premtechworks.synqvia.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.ui.util.AppVersionHelper
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
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import com.github.premtechworks.synqvia.ui.components.SettingsNavRow
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.components.SynqviaScreen
import androidx.compose.ui.res.vectorResource
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
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
    var isContactSupportOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.settingsTabReselectTrigger.collect {
            isContactSupportOpen = false
        }
    }

    val reduceMotion = LocalReduceMotion.current

    AnimatedContent(
        targetState = isContactSupportOpen,
        transitionSpec = {
            if (reduceMotion) {
                (fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(120)))
                    .apply { targetContentZIndex = 1f }
            } else {
                if (targetState) {
                    val enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                    ) { it } + fadeIn(
                        animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                    )
                    val exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                    ) { -it / 3 } + fadeOut(
                        animationSpec = tween(durationMillis = 140, easing = DecelerateEasing)
                    )
                    (enter togetherWith exit).apply { targetContentZIndex = 1f }
                } else {
                    val enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                    ) { -it / 3 } + fadeIn(
                        animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                    )
                    val exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                    ) { it } + fadeOut(
                        animationSpec = tween(durationMillis = 140, easing = DecelerateEasing)
                    )
                    (enter togetherWith exit).apply { targetContentZIndex = 1f }
                }
            }
        },
        label = "settings_subscreen_transition",
        modifier = modifier
    ) { showContactSupport ->
        if (showContactSupport) {
            BackHandler {
                isContactSupportOpen = false
            }
            ContactSupportScreen(
                onBack = { isContactSupportOpen = false },
                onShowMessage = { viewModel.showUserMessage(it) }
            )
        } else {
            val config by viewModel.config.collectAsState()
            val logs by viewModel.diagnosticLogs.collectAsState()

            val autoSync by viewModel.autoSync.collectAsState()
            val syncTextOnly by viewModel.syncTextOnly.collectAsState()
            val clearOnDisconnect by viewModel.clearOnDisconnect.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
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
                onReduceMotionFollowSystemChange = { viewModel.setReduceMotionFollowSystem(it) },
                onHapticFeedbackChange = { viewModel.setHapticFeedback(it) },
                onOpenContactSupport = { isContactSupportOpen = true },
                onCopyMac = { mac ->
                    clipboardManager.setText(AnnotatedString(mac))
                    viewModel.showUserMessage("Copied")
                },
                onCopyLogs = {
                    val fullLogText = logs.joinToString("\n") { "${it.formattedTime} [${it.level}] ${it.message}" }
                    clipboardManager.setText(AnnotatedString(fullLogText))
                    viewModel.showUserMessage("Diagnostic log copied to clipboard")
                },
                onClearLogs = { viewModel.clearLogs() }
            )
        }
    }
}

@Composable
fun SettingsScreenContent(
    config: SyncConfig,
    logs: List<LogItem>,
    autoSync: Boolean,
    syncTextOnly: Boolean,
    clearOnDisconnect: Boolean,
    themeMode: String,
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
    onReduceMotionFollowSystemChange: (Boolean) -> Unit = {},
    onHapticFeedbackChange: (Boolean) -> Unit = {},
    onOpenContactSupport: () -> Unit = {},
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

    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark

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
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = SynqviaType.LargeTitle,
                    color = colors.textPrimary
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
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. MAC row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color.Transparent else colors.surfaceInset)
                        .clickable(role = Role.Button) { showMacDialog = true }
                        .padding(if (isDark) androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp) else androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linux PC Bluetooth MAC",
                            style = SynqviaType.Caption,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = config.pcMac.ifBlank { "AA:BB:CC:DD:EE:FF" },
                            style = SynqviaType.Mono,
                            color = if (config.pcMac.isNotBlank()) colors.textPrimary else colors.textTertiary
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
                            tint = if (isDark) colors.textSecondary else Color(0xFF2B3A55),
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
                        color = colors.textPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RollingNumber(
                            value = config.channel,
                            style = SynqviaType.HeadlineTnum,
                            color = if (isDark) colors.primary else colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        // Circular "-" button (28dp) with press-pop
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) colors.surfaceHigh else Color.White)
                                .border(1.dp, if (isDark) colors.outline else Color(0xFFC9D6EA), CircleShape)
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
                                color = if (isDark) colors.textPrimary else colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Circular "+" button (28dp) with press-pop
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) colors.surfaceHigh else Color.White)
                                .border(1.dp, if (isDark) colors.outline else Color(0xFFC9D6EA), CircleShape)
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
                                color = if (isDark) colors.textPrimary else colors.primary
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
                        color = colors.textPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = config.deviceName,
                            style = SynqviaType.Footnote,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
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
                        color = colors.textPrimary
                    )
                    Box(
                        modifier = if (isDark) Modifier else Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceInset)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (config.historyCap == 0) {
                            Text(
                                text = "Unlimited",
                                style = SynqviaType.CaptionTnum,
                                color = colors.textSecondary
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RollingNumber(
                                    value = config.historyCap,
                                    style = SynqviaType.CaptionTnum,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = " items",
                                    style = SynqviaType.CaptionTnum,
                                    color = colors.textSecondary
                                )
                            }
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
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 1: Auto Sync
                SettingsToggleRow(
                    title = "Auto Sync",
                    subtitle = "Sync clipboard changes automatically",
                    leadingIcon = Icons.Default.Sync,
                    iconContainerColor = colors.tileAutoSyncContainer,
                    iconContentColor = colors.tileAutoSyncContent,
                    checked = autoSync,
                    onCheckedChange = onAutoSyncChange
                )

                DividerItem()

                // Toggle 2: Sync Text Only
                SettingsToggleRow(
                    title = "Sync Text Only",
                    subtitle = "Images and files are not synced (v1)",
                    leadingIcon = Icons.Default.TextFields,
                    iconContainerColor = colors.tileTextOnlyContainer,
                    iconContentColor = colors.tileTextOnlyContent,
                    checked = syncTextOnly,
                    onCheckedChange = onSyncTextOnlyChange
                )

                DividerItem()

                // Toggle 3: Clear on Device Disconnect
                SettingsToggleRow(
                    title = "Clear on Device Disconnect",
                    subtitle = "Clear remote clipboard when disconnected",
                    leadingIcon = Icons.Default.DeleteSweep,
                    iconContainerColor = colors.tileClearContainer,
                    iconContentColor = colors.tileClearContent,
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
                    color = colors.textPrimary
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
                            .background(colors.tileThemeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = colors.tileThemeContent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Theme",
                        style = SynqviaType.Body,
                        color = colors.textPrimary,
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
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DividerItem()

                // Reduce Motion
                SettingsToggleRow(
                    title = "Reduce Motion",
                    subtitle = "Off follows your system setting. On always minimizes animations.",
                    leadingIcon = Icons.Default.Animation,
                    iconContainerColor = colors.tileThemeContainer,
                    iconContentColor = colors.tileThemeContent,
                    checked = reduceMotionFollowSystem,
                    onCheckedChange = onReduceMotionFollowSystemChange
                )

                DividerItem()

                // Haptic Feedback
                SettingsToggleRow(
                    title = "Haptic Feedback",
                    subtitle = "Vibration ticks on taps, toggles, and sync actions",
                    leadingIcon = Icons.Default.Vibration,
                    iconContainerColor = colors.tileThemeContainer,
                    iconContentColor = colors.tileThemeContent,
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
            val terminalIcon = ImageVector.vectorResource(R.drawable.ic_terminal_rounded)
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(snappySpring())
                    .entryStagger(index = 4),
                padding = 0.dp
            ) {
                val chevronRotation by animateFloatAsState(
                    targetValue = if (showLogs) 180f else 0f,
                    animationSpec = tween(durationMillis = 250, easing = DecelerateEasing),
                    label = "log_chevron"
                )
                SettingsNavRow(
                    icon = terminalIcon,
                    title = "Protocol Diagnostic Log",
                    iconContainerColor = colors.blueContainer,
                    iconContentColor = colors.blue,
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showLogs) "Hide logs" else "Show logs",
                            tint = colors.textTertiary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    },
                    modifier = Modifier.clickable(role = Role.Button) {
                        logHaptics.tick()
                        showLogs = !showLogs
                    }
                )

                AnimatedVisibility(visible = showLogs) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceInset)
                            .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
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
                                    color = colors.textTertiary
                                )
                            } else {
                                logs.takeLast(50).forEach { log ->
                                    val badgeColor = if (colors.isDark) {
                                        when (log.level) {
                                            "SUCCESS" -> Color(0xFF22C55E)
                                            "FRAME", "INFO" -> Color(0xFF00E5FF)
                                            "WARN" -> Color(0xFFF59E0B)
                                            "ERROR" -> Color(0xFFEF4444)
                                            else -> colors.textSecondary
                                        }
                                    } else {
                                        when (log.level) {
                                            "SUCCESS" -> Color(0xFF1A9B5A)
                                            "FRAME", "INFO" -> Color(0xFF1F6FEB)
                                            "WARN" -> Color(0xFFD97706)
                                            "ERROR" -> Color(0xFFC93238)
                                            else -> colors.textSecondary
                                        }
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
                                            color = colors.textTertiary
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
                                            color = if (colors.isDark) colors.textSecondary else Color(0xFF0B1B33).copy(alpha = 0.85f),
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
                                style = SynqviaType.ButtonSmall.copy(color = colors.primary),
                                color = colors.primary,
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
                                style = SynqviaType.ButtonSmall.copy(color = colors.redText),
                                color = colors.redText,
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

            // Contact & Support entry card (above footer)
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_contact_support_card")
                    .entryStagger(index = 5)
                    .pressable(
                        targetScale = 0.98f,
                        showOverlay = true,
                        onClick = {
                            haptics.tick()
                            onOpenContactSupport()
                        }
                    ),
                padding = 0.dp
            ) {
                SettingsNavRow(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = stringResource(R.string.contact_support_title),
                    subtitle = stringResource(R.string.contact_support_subtitle),
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: "Synqvia <version> · GPL-3.0" centered, Caption style, textTertiary
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppVersionHelper.getSettingsFooterText(context),
                    style = SynqviaType.Caption,
                    color = colors.textTertiary
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
            .background(SynqviaTheme.colors.divider)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: ImageVector? = null,
    iconContainerColor: Color? = null,
    iconContentColor: Color? = null,
    enabled: Boolean = true
) {
    val colors = SynqviaTheme.colors
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
                    .background(iconContainerColor ?: (if (colors.isDark) Color(0xFF3B82F6).copy(alpha = 0.14f) else colors.tileAutoSyncContainer)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconContentColor ?: (if (colors.isDark) Color(0xFF00E5FF) else colors.primary),
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
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = SynqviaType.Caption,
                color = colors.textSecondary
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
        val colors = SynqviaTheme.colors
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
                val dotColor = if (isActive) colors.sliderActive else if (i % 5 == 0) colors.sliderTick else colors.sliderInactive

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
                color = colors.sliderActive,
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
    val colors = SynqviaTheme.colors
    var text by remember { mutableStateOf(initialMac) }
    val isValid = remember(text) { text.isBlank() || SyncPreferences.isValidMac(text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Edit PC Bluetooth MAC", color = colors.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.uppercase(Locale.ROOT) },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF", color = colors.textTertiary) },
                    singleLine = true,
                    isError = !isValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
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
                        color = colors.redText,
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
                Text("Save", color = if (isValid) colors.primary else colors.textTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
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
    val colors = SynqviaTheme.colors
    var text by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Broadcast Device Name", color = colors.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(48) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Device ID: $deviceId",
                    style = SynqviaType.MonoSmall,
                    color = colors.textTertiary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
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
    val colors = SynqviaTheme.colors
    val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Choose Theme", color = colors.textPrimary) },
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
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.textSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = SynqviaType.Body, color = colors.textPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
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
            onMacChange = { true },
            onChannelChange = {},
            onDeviceNameChange = {},
            onHistoryCapChange = {},
            onAutoSyncChange = {},
            onSyncTextOnlyChange = {},
            onClearOnDisconnectChange = {},
            onThemeModeChange = {},
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
            onMacChange = { true },
            onChannelChange = {},
            onDeviceNameChange = {},
            onHistoryCapChange = {},
            onAutoSyncChange = {},
            onSyncTextOnlyChange = {},
            onClearOnDisconnectChange = {},
            onThemeModeChange = {},
            onCopyMac = {},
            onClearLogs = {}
        )
    }
}
