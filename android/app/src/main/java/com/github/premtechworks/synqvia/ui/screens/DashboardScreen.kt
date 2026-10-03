package com.github.premtechworks.synqvia.ui.screens

import android.content.Context
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.data.SyncStats
import com.github.premtechworks.synqvia.service.SyncConnectionState
import com.github.premtechworks.synqvia.ui.MainTab
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.CircleIconButton
import com.github.premtechworks.synqvia.ui.components.HeroSyncCard
import com.github.premtechworks.synqvia.ui.components.IconTile
import com.github.premtechworks.synqvia.ui.components.ScreenScaffold
import com.github.premtechworks.synqvia.ui.components.ScrollableColumn
import com.github.premtechworks.synqvia.ui.components.SectionHeader
import com.github.premtechworks.synqvia.ui.components.SourcePill
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.components.SynqviaBrandLockup
import com.github.premtechworks.synqvia.ui.components.formatLastSyncedText
import com.github.premtechworks.synqvia.ui.theme.AccentAmber
import com.github.premtechworks.synqvia.ui.theme.AccentBlue
import com.github.premtechworks.synqvia.ui.theme.AccentGreen
import com.github.premtechworks.synqvia.ui.theme.AccentRed
import com.github.premtechworks.synqvia.ui.theme.BgTop
import com.github.premtechworks.synqvia.ui.theme.OutlineDark
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SurfaceDark
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.SynqviaTypography
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary
import com.github.premtechworks.synqvia.ui.theme.TextTertiary
import com.github.premtechworks.synqvia.ui.theme.synqviaCardShadow

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.github.premtechworks.synqvia.ui.theme.SurfaceHigh
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.RollingNumber
import com.github.premtechworks.synqvia.ui.motion.entryStagger
import com.github.premtechworks.synqvia.ui.motion.pressable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onOpenPair: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val config by viewModel.config.collectAsState()
    val stats by viewModel.syncStats.collectAsState()
    val recentClips by viewModel.recentClips.collectAsState()
    val isKeyboardWarningVisible by viewModel.isKeyboardWarningVisible.collectAsState()
    val context = LocalContext.current

    DashboardContent(
        connectionState = connectionState,
        config = config,
        stats = stats,
        recentClips = recentClips,
        isKeyboardWarningVisible = isKeyboardWarningVisible,
        scrollState = scrollState,
        onSyncNow = { viewModel.syncNow() },
        onSendTest = { viewModel.sendTestClip() },
        onReconnect = { viewModel.reconnect() },
        onNavigateTab = onNavigateTab,
        onSelectFilter = { filter -> viewModel.setFilter(filter) },
        onOpenPair = onOpenPair,
        onSnoozeWarning = { viewModel.snoozeKeyboardWarning() },
        onSwitchIme = {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        },
        onClipClick = { clip -> viewModel.selectClipForDetail(clip) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    connectionState: SyncConnectionState,
    config: SyncConfig,
    stats: SyncStats,
    latestClip: ClipEntity? = null,
    recentClips: List<ClipEntity> = latestClip?.let { listOf(it) } ?: emptyList(),
    isKeyboardWarningVisible: Boolean,
    onSyncNow: () -> Unit,
    onSendTest: () -> Unit,
    onReconnect: () -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    onSelectFilter: (com.github.premtechworks.synqvia.ui.ClipFilter) -> Unit = {},
    onSnoozeWarning: () -> Unit = {},
    onSwitchIme: () -> Unit,
    onClipClick: (ClipEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onOpenPair: () -> Unit = {}
) {
    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark
    val isScrolled by remember { derivedStateOf { scrollState.value > 8 } }

    ScreenScaffold(
        modifier = modifier,
        isScrolled = isScrolled,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SynqviaBrandLockup()
                Spacer(modifier = Modifier.weight(1f))
                CircleIconButton(
                    icon = Icons.Default.Settings,
                    onClick = { onNavigateTab(MainTab.SETTINGS) },
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = "Settings"
                )
            }
        }
    ) { contentPadding ->
        val pullRefreshState = rememberPullToRefreshState()
        var isRefreshing by remember { mutableStateOf(false) }
        val haptics = LocalAppHaptics.current
        val coroutineScope = rememberCoroutineScope()

        val isVisible = com.github.premtechworks.synqvia.ui.motion.rememberIsScreenVisible()
        var tick by remember { mutableStateOf(0L) }
        LaunchedEffect(isVisible) {
            if (isVisible) {
                while (true) {
                    delay(30_000L)
                    tick = System.currentTimeMillis()
                }
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                onSyncNow()
                coroutineScope.launch {
                    delay(1200L)
                    isRefreshing = false
                    haptics.confirm()
                }
            },
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentPadding.calculateTopPadding()),
                    containerColor = colors.surfaceHigh,
                    color = colors.primary
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            ScrollableColumn(
                modifier = Modifier.testTag("dashboard_screen"),
                contentPadding = contentPadding,
            scrollState = scrollState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // A) CONNECTION CARD
            // ==========================================
            HeroSyncCard(
                connectionState = connectionState,
                config = config,
                onSyncNow = onSyncNow,
                onSendTest = onSendTest,
                onReconnect = onReconnect,
                onOpenPair = onOpenPair,
                lastSyncTimestamp = recentClips.firstOrNull()?.ts ?: latestClip?.ts ?: 0L,
                modifier = Modifier.entryStagger(index = 0)
            )

        // ==========================================
        // C) KEYBOARD WARNING CARD (Conditional)
        // ==========================================
        if (isKeyboardWarningVisible) {
            val kbCardBg = if (isDark) AccentAmber.copy(alpha = 0.10f) else colors.amberContainer
            val kbCardBorder = if (isDark) AccentAmber.copy(alpha = 0.40f) else colors.amber.copy(alpha = 0.40f)
            val kbShieldTint = if (isDark) AccentAmber else colors.amber
            val kbBtnContainer = if (isDark) AccentAmber else colors.amber
            val kbBtnContent = if (isDark) Color(0xFF04111F) else Color(0xFF3B2400)

            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ime_privilege_card"),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = kbCardBg,
                borderColor = kbCardBorder,
                padding = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: IconTile 36dp (amber shield) + text: title + body
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        IconTile(
                            icon = Icons.Default.Security,
                            tint = kbShieldTint,
                            size = 36.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Set Synqvia as your default keyboard",
                                style = SynqviaType.Headline,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Needed for automatic capture. Sync Now, Share and the Quick Tile still work.",
                                style = SynqviaType.Caption,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Row 2 (right-aligned): text button "Later" + amber filled "Set as default" 34dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = onSnoozeWarning,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Later",
                                style = SynqviaType.ButtonSmall.copy(color = colors.textSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onSwitchIme,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = kbBtnContainer,
                                contentColor = kbBtnContent
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Set as default",
                                style = SynqviaType.ButtonSmall.copy(
                                    color = kbBtnContent
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // D) STATS ROW (4 equal tiles, 8dp gaps, tappable)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entryStagger(index = 2),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalNumColor = if (isDark) PrimaryCyan else colors.blue
            val totalContainer = if (isDark) SurfaceDark else colors.blueContainer
            val totalBorder = if (isDark) OutlineDark else colors.blue.copy(alpha = 0.12f)

            val sentNumColor = if (isDark) AccentBlue else colors.purple
            val sentContainer = if (isDark) SurfaceDark else colors.purpleContainer
            val sentBorder = if (isDark) OutlineDark else colors.purple.copy(alpha = 0.12f)

            val recNumColor = if (isDark) AccentGreen else colors.green
            val recContainer = if (isDark) SurfaceDark else colors.greenContainer
            val recBorder = if (isDark) OutlineDark else colors.green.copy(alpha = 0.12f)

            val lossNumColor = if (isDark) AccentRed else colors.red
            val lossContainer = if (isDark) SurfaceDark else colors.redContainer
            val lossBorder = if (isDark) OutlineDark else colors.red.copy(alpha = 0.12f)

            StatTile(
                value = stats.totalCount,
                label = "Total",
                numberColor = totalNumColor,
                containerColor = totalContainer,
                borderColor = totalBorder,
                onClick = {
                    onSelectFilter(com.github.premtechworks.synqvia.ui.ClipFilter.ALL)
                    onNavigateTab(MainTab.HISTORY)
                },
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = stats.localSentCount,
                label = "Sent",
                numberColor = sentNumColor,
                containerColor = sentContainer,
                borderColor = sentBorder,
                onClick = {
                    onSelectFilter(com.github.premtechworks.synqvia.ui.ClipFilter.SENT)
                    onNavigateTab(MainTab.HISTORY)
                },
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = stats.remoteReceivedCount,
                label = "Received",
                numberColor = recNumColor,
                containerColor = recContainer,
                borderColor = recBorder,
                onClick = {
                    onSelectFilter(com.github.premtechworks.synqvia.ui.ClipFilter.RECEIVED)
                    onNavigateTab(MainTab.HISTORY)
                },
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = stats.conflictCount,
                label = "Loss",
                numberColor = lossNumColor,
                containerColor = lossContainer,
                borderColor = lossBorder,
                onClick = {
                    onSelectFilter(com.github.premtechworks.synqvia.ui.ClipFilter.CONFLICTS)
                    onNavigateTab(MainTab.HISTORY)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // ==========================================
        // E) RECENT ACTIVITY
        // ==========================================
        SectionHeader(
            icon = Icons.Default.Bolt,
            title = "Recent Activity",
            actionText = "View All →",
            onAction = { onNavigateTab(MainTab.HISTORY) }
        )

        if (recentClips.isNotEmpty()) {
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 3),
                shape = RoundedCornerShape(16.dp),
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    recentClips.forEachIndexed { index, clip ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.divider)
                            )
                        }
                        RecentActivityRow(
                            clip = clip,
                            tick = tick,
                            onClick = { onClipClick(clip) }
                        )
                    }
                }
            }
        } else {
            SynqviaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 3),
                shape = RoundedCornerShape(16.dp),
                padding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Copy something on your PC and it will appear here",
                        style = SynqviaType.Footnote,
                        color = colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.TextButton(
                        onClick = onSendTest
                    ) {
                        Text(
                            text = "Send Test",
                            style = SynqviaType.ButtonSmall.copy(color = colors.primary)
                        )
                    }
                }
            }
        }
    }
}
}
}

/**
 * Centered stat tile matching mockup: 14dp radius, ~72dp tall, 24sp Bold number, 12sp textSecondary label.
 */
@Composable
private fun StatTile(
    value: Int,
    label: String,
    numberColor: Color,
    containerColor: Color? = null,
    borderColor: Color? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalAppHaptics.current
    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark
    val bg = containerColor ?: if (isDark) SurfaceDark else colors.surface
    val border = borderColor ?: if (isDark) OutlineDark else colors.outline

    Box(
        modifier = modifier
            .height(72.dp)
            .then(if (!isDark) Modifier.synqviaCardShadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp)) else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .pressable(
                targetScale = 0.97f,
                showOverlay = true,
                onClick = {
                    haptics.tick()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RollingNumber(
                value = value,
                style = SynqviaTypography.StatNumber,
                color = numberColor
            )
            Text(
                text = label,
                style = SynqviaTypography.Caption,
                color = colors.textSecondary
            )
        }
    }
}

/**
 * Clickable row for recent clipboard activities in the Dashboard card.
 */
@Composable
private fun RecentActivityRow(
    clip: ClipEntity,
    tick: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors
    val relativeTime = remember(clip.ts, tick) {
        formatClipTime(clip.ts)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top line: SourcePill + relative time (right-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SourcePill(isFromPc = clip.isRemote)
            Text(
                text = relativeTime,
                style = SynqviaTypography.Caption,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Clip text: 14sp sans-serif, max 2 lines ellipsized
        if (clip.text.isBlank()) {
            Text(
                text = "(empty clipboard)",
                style = SynqviaTypography.Body,
                color = colors.textTertiary
            )
        } else {
            Text(
                text = clip.text,
                style = SynqviaTypography.Body,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Footer: character count
        Text(
            text = "${clip.text.length} chars",
            style = SynqviaTypography.Caption,
            color = colors.textSecondary
        )
    }
}

/**
 * Formats relative time for clipboard item timestamps ("22m ago", "1h ago", "just now").
 */
private fun formatClipTime(timestamp: Long): String {
    if (timestamp <= 0L) return "just now"
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

// ==========================================
// Previews for Connected, Offline, and NotDefaultIme states
// ==========================================

@Preview(name = "Dashboard - Connected", showBackground = true)
@Composable
fun PreviewDashboardConnected() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop)) {
            DashboardContent(
                connectionState = SyncConnectionState.Connected(
                    peerName = "prem-pc",
                    mac = "F8:34:41:53:BE:24"
                ),
                config = SyncConfig(
                    pcMac = "F8:34:41:53:BE:24",
                    channel = 1,
                    historyCap = 500,
                    deviceName = "Android (a386a5)",
                    deviceId = "android-a386a5"
                ),
                stats = SyncStats(totalCount = 302, localSentCount = 0, remoteReceivedCount = 302, conflictCount = 0),
                latestClip = ClipEntity(
                    id = "1",
                    text = "build the debug app and install it on connected device.",
                    ts = System.currentTimeMillis() - 22 * 60 * 1000,
                    src = "prem-pc",
                    direction = "remote"
                ),
                isKeyboardWarningVisible = false,
                onSyncNow = {},
                onSendTest = {},
                onReconnect = {},
                onNavigateTab = {},
                onSwitchIme = {}
            )
        }
    }
}

@Preview(name = "Dashboard - Offline", showBackground = true)
@Composable
fun PreviewDashboardOffline() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop)) {
            DashboardContent(
                connectionState = SyncConnectionState.Offline("Bluetooth adapter is disabled"),
                config = SyncConfig(
                    pcMac = "F8:34:41:53:BE:24",
                    channel = 1,
                    historyCap = 500,
                    deviceName = "Android (a386a5)",
                    deviceId = "android-a386a5"
                ),
                stats = SyncStats(totalCount = 12, localSentCount = 5, remoteReceivedCount = 7, conflictCount = 0),
                latestClip = ClipEntity(
                    id = "2",
                    text = "sudo systemctl status bluetooth",
                    ts = System.currentTimeMillis() - 2 * 60 * 60 * 1000,
                    src = "android",
                    direction = "local"
                ),
                isKeyboardWarningVisible = false,
                onSyncNow = {},
                onSendTest = {},
                onReconnect = {},
                onNavigateTab = {},
                onSwitchIme = {}
            )
        }
    }
}

@Preview(name = "Dashboard - Not Default IME", showBackground = true)
@Composable
fun PreviewDashboardNotDefaultIme() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop)) {
            DashboardContent(
                connectionState = SyncConnectionState.Connected(
                    peerName = "prem-pc",
                    mac = "F8:34:41:53:BE:24"
                ),
                config = SyncConfig(
                    pcMac = "F8:34:41:53:BE:24",
                    channel = 1,
                    historyCap = 500,
                    deviceName = "Android (a386a5)",
                    deviceId = "android-a386a5"
                ),
                stats = SyncStats(totalCount = 302, localSentCount = 0, remoteReceivedCount = 302, conflictCount = 0),
                latestClip = ClipEntity(
                    id = "3",
                    text = "Automatic clipboard capture test",
                    ts = System.currentTimeMillis() - 5 * 60 * 1000,
                    src = "prem-pc",
                    direction = "remote"
                ),
                isKeyboardWarningVisible = true,
                onSyncNow = {},
                onSendTest = {},
                onReconnect = {},
                onNavigateTab = {},
                onSwitchIme = {}
            )
        }
    }
}

