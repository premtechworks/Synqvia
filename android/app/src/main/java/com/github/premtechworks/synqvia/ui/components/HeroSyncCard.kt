package com.github.premtechworks.synqvia.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.service.SyncConnectionState
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.motion.softSpring
import com.github.premtechworks.synqvia.ui.theme.AccentBlue
import com.github.premtechworks.synqvia.ui.theme.AccentGreen
import com.github.premtechworks.synqvia.ui.theme.BgTop
import com.github.premtechworks.synqvia.ui.theme.OutlineDark
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SurfaceDark
import com.github.premtechworks.synqvia.ui.theme.SurfaceInset
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.SynqviaTypography
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary
import com.github.premtechworks.synqvia.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Redesigned Connection Card (Hero Sync Card) matching mockup screen 1:
 * - 20dp radius SynqviaCard, 16dp padding
 * - Row 1: StatusDot + state title & subtitle + chevron-right
 * - Inset panel: Laptop icon + target PC info + CH pill + relative sync time with refresh
 * - Action buttons: PrimaryButton (Sync Now) + OutlinedCyanButton (Send Test)
 */
@Composable
fun HeroSyncCard(
    connectionState: SyncConnectionState,
    config: SyncConfig,
    onSyncNow: () -> Unit,
    onSendTest: () -> Unit,
    onReconnect: () -> Unit,
    onToggleService: (Boolean) -> Unit = {},
    onOpenPair: () -> Unit = {},
    lastSyncTimestamp: Long = 0L,
    modifier: Modifier = Modifier
) {
    val (statusTitle, statusSubtitle) = resolveConnectionStatusText(connectionState, config)
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
    val lastSyncedText = remember(lastSyncTimestamp, tick) {
        formatLastSyncedText(lastSyncTimestamp)
    }

    val haptics = LocalAppHaptics.current
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    var isSyncingLocal by remember { mutableStateOf(false) }
    var isSyncSuccess by remember { mutableStateOf(false) }
    val successMorphAnim = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotate"
    )

    val isActuallySyncing = isSyncingLocal || connectionState is SyncConnectionState.Syncing

    LaunchedEffect(connectionState) {
        if (connectionState is SyncConnectionState.Syncing) {
            isSyncingLocal = true
        } else if (isSyncingLocal && connectionState is SyncConnectionState.Connected) {
            isSyncingLocal = false
            isSyncSuccess = true
            haptics.confirm()
            if (!reduceMotion) {
                successMorphAnim.snapTo(0f)
                successMorphAnim.animateTo(1f, tween(700, easing = DecelerateEasing))
            }
            delay(1500)
            isSyncSuccess = false
        }
    }

    val nudgeOffset = remember { Animatable(0f) }
    var isTestSent by remember { mutableStateOf(false) }
    val targetPcName = if (connectionState is SyncConnectionState.Connected && connectionState.peerName.isNotBlank()) {
        connectionState.peerName
    } else if (config.deviceName.isNotBlank()) {
        config.deviceName
    } else {
        "PC"
    }

    SynqviaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_sync_card")
            .semantics { contentDescription = "Sync connection status: $statusTitle, $statusSubtitle" },
        shape = RoundedCornerShape(20.dp),
        padding = 16.dp
    ) {
        // Row 1: StatusDot (12dp) + Title/Subtitle Crossfade Column + 20dp ChevronRight
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(connectionState = connectionState)
            Spacer(modifier = Modifier.width(12.dp))
            AnimatedContent(
                targetState = statusTitle to statusSubtitle,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "status_text_crossfade",
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onOpenPair)
            ) { (title, subtitle) ->
                Column {
                    Text(
                        text = title,
                        style = SynqviaType.Headline,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = SynqviaType.Caption,
                        color = TextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Pair settings",
                        onClick = onOpenPair
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Pair settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Inset Panel (surfaceInset, 14dp radius, 12dp padding)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceInset)
                .border(1.dp, OutlineDark, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Icons.Default.Laptop,
                    tint = PrimaryCyan,
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (connectionState is SyncConnectionState.Connected && connectionState.peerName.isNotBlank()) {
                            connectionState.peerName
                        } else {
                            "Linux PC"
                        },
                        style = SynqviaType.Headline,
                        color = TextPrimary
                    )
                    Text(
                        text = if (config.pcMac.isNotBlank()) config.pcMac else "F8:34:41:53:BE:24",
                        style = SynqviaType.MonoSmall,
                        color = TextSecondary
                    )
                }
                // CH pill (blue tint, cyan 11sp SemiBold text, 20dp tall)
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentBlue.copy(alpha = 0.14f))
                        .border(1.dp, AccentBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CH ${config.channel}",
                        style = SynqviaType.Overline.copy(
                            color = PrimaryCyan,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Relative sync time row with tappable refresh icon to reconnect
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Reconnect",
                        onClick = onReconnect
                    )
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reconnect",
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = lastSyncedText,
                    style = SynqviaType.CaptionTnum,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row: PrimaryButton (Sync Now) + OutlinedCyanButton (Send Test)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val syncIconRotation = if (isActuallySyncing && !reduceMotion) {
                syncRotation
            } else if (isSyncSuccess && !reduceMotion) {
                successMorphAnim.value * 360f
            } else 0f

            val syncIconScale = if (isSyncSuccess && !reduceMotion) {
                // Morph bounce: 0.8 -> 1.2 -> 1.0
                val p = successMorphAnim.value
                if (p < 0.5f) 0.8f + (p / 0.5f) * 0.4f else 1.2f - ((p - 0.5f) / 0.5f) * 0.2f
            } else 1f

            val syncLabel = if (isSyncSuccess) {
                "Synced"
            } else if (isActuallySyncing) {
                "Syncing…"
            } else {
                "Sync Now"
            }

            val isOffline = connectionState is SyncConnectionState.Offline ||
                    connectionState is SyncConnectionState.Failed ||
                    connectionState is SyncConnectionState.Stopped

            if (isOffline) {
                androidx.compose.material3.Button(
                    onClick = {
                        haptics.tick()
                        onReconnect()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        contentColor = com.github.premtechworks.synqvia.ui.theme.OnPrimaryCyan
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .pressable(targetScale = 0.96f)
                        .testTag("retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Retry",
                        style = SynqviaTypography.ButtonLabel,
                        color = com.github.premtechworks.synqvia.ui.theme.OnPrimaryCyan
                    )
                }
            } else {
                androidx.compose.material3.Button(
                    onClick = {
                        haptics.tick()
                        isSyncingLocal = true
                        onSyncNow()
                        coroutineScope.launch {
                            delay(1200)
                            if (isSyncingLocal) {
                                isSyncingLocal = false
                                isSyncSuccess = true
                                haptics.confirm()
                                if (!reduceMotion) {
                                    successMorphAnim.snapTo(0f)
                                    successMorphAnim.animateTo(1f, tween(700, easing = DecelerateEasing))
                                }
                                delay(1500)
                                isSyncSuccess = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        contentColor = com.github.premtechworks.synqvia.ui.theme.OnPrimaryCyan
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .pressable(targetScale = 0.96f)
                        .testTag("sync_now_button")
                ) {
                    Icon(
                        imageVector = if (isSyncSuccess) Icons.Default.Check else Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationZ = syncIconRotation
                                scaleX = syncIconScale
                                scaleY = syncIconScale
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncLabel,
                        style = SynqviaTypography.ButtonLabel,
                        color = com.github.premtechworks.synqvia.ui.theme.OnPrimaryCyan
                    )
                }
            }

            androidx.compose.material3.OutlinedButton(
                onClick = {
                    haptics.confirm()
                    isTestSent = true
                    onSendTest()
                    coroutineScope.launch {
                        if (!reduceMotion) {
                            nudgeOffset.snapTo(0f)
                            nudgeOffset.animateTo(8f, tween(100, easing = DecelerateEasing))
                            nudgeOffset.animateTo(0f, com.github.premtechworks.synqvia.ui.motion.softSpring())
                        }
                        delay(1500)
                        isTestSent = false
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = PrimaryCyan
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .pressable(targetScale = 0.96f)
                    .testTag("send_test_button")
            ) {
                Icon(
                    imageVector = if (isTestSent) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            translationX = if (!reduceMotion) nudgeOffset.value * density.density else 0f
                        }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTestSent) "Sent to $targetPcName" else "Send Test",
                    style = SynqviaTypography.ButtonLabel,
                    color = PrimaryCyan,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * State text mapping:
 * - Connected
 * - Connecting…
 * - Offline — retrying
 * - Offline — Bluetooth off
 * - Offline — set PC MAC
 * Subtitle is "Paired with <peer name>" when connected, otherwise the reason.
 */
fun resolveConnectionStatusText(
    state: SyncConnectionState,
    config: SyncConfig
): Pair<String, String> {
    return when (state) {
        is SyncConnectionState.Connected -> {
            val peer = if (state.peerName.isNotBlank()) state.peerName else "prem-pc"
            "Connected" to "Paired with $peer"
        }
        is SyncConnectionState.Connecting -> {
            val subtitle = if (state.nextRetrySec > 0) {
                "Retrying in ${state.nextRetrySec}s"
            } else if (state.attempt > 1) {
                "Connecting ${state.attempt}/${state.maxAttempts}…"
            } else {
                "Establishing Bluetooth RFCOMM socket…"
            }
            "Connecting…" to subtitle
        }
        is SyncConnectionState.Syncing -> {
            "Connected" to "Syncing clipboard…"
        }
        is SyncConnectionState.Offline -> {
            val title = when {
                config.pcMac.isBlank() -> "Offline — set PC MAC"
                state.reason.contains("bluetooth", ignoreCase = true) ||
                state.reason.contains("adapter", ignoreCase = true) -> "Offline — Bluetooth off"
                state.reason.contains("retry", ignoreCase = true) -> "Offline — retrying"
                else -> "Offline"
            }
            val subtitle = if (config.pcMac.isBlank()) {
                "Configure target device MAC in Settings"
            } else {
                state.reason.ifBlank { "Service is currently offline" }
            }
            title to subtitle
        }
        is SyncConnectionState.Failed -> {
            val title = when {
                config.pcMac.isBlank() -> "Offline — set PC MAC"
                else -> "Offline — retrying"
            }
            val subtitle = if (config.pcMac.isBlank()) {
                "Configure target device MAC in Settings"
            } else {
                "Connection failed. Tap refresh to retry."
            }
            title to subtitle
        }
        is SyncConnectionState.Stopped -> {
            val title = when {
                config.pcMac.isBlank() -> "Offline — set PC MAC"
                else -> "Offline"
            }
            title to "Sync service is paused. Tap Sync Now to start."
        }
    }
}

/**
 * Formats relative time from the last successful sync/ack ("Synced just now", "Synced 3m ago", "Not synced yet").
 */
fun formatLastSyncedText(timestamp: Long): String {
    if (timestamp <= 0L) return "Not synced yet"
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Synced just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Synced just now"
        minutes < 60 -> "Synced ${minutes}m ago"
        hours < 24 -> "Synced ${hours}h ago"
        else -> "Synced ${days}d ago"
    }
}

// ==========================================
// Previews
// ==========================================

@Preview(name = "Hero Sync Card - Connected", showBackground = true)
@Composable
fun PreviewHeroSyncCardConnected() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            HeroSyncCard(
                connectionState = SyncConnectionState.Connected("prem-pc", "F8:34:41:53:BE:24"),
                config = SyncConfig(
                    pcMac = "F8:34:41:53:BE:24",
                    channel = 1,
                    historyCap = 500,
                    deviceName = "Android (a386a5)",
                    deviceId = "android-a386a5"
                ),
                onSyncNow = {},
                onSendTest = {},
                onReconnect = {},
                lastSyncTimestamp = System.currentTimeMillis() - 30 * 1000
            )
        }
    }
}

@Preview(name = "Hero Sync Card - Connecting", showBackground = true)
@Composable
fun PreviewHeroSyncCardConnecting() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            HeroSyncCard(
                connectionState = SyncConnectionState.Connecting(attempt = 2, maxAttempts = 5, nextRetrySec = 4),
                config = SyncConfig(
                    pcMac = "F8:34:41:53:BE:24",
                    channel = 1,
                    historyCap = 500,
                    deviceName = "Android (a386a5)",
                    deviceId = "android-a386a5"
                ),
                onSyncNow = {},
                onSendTest = {},
                onReconnect = {},
                lastSyncTimestamp = 0L
            )
        }
    }
}

@Preview(name = "Hero Sync Card - Offline", showBackground = true)
@Composable
fun PreviewHeroSyncCardOffline() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            HeroSyncCard(
                connectionState = SyncConnectionState.Offline("Bluetooth adapter is disabled"),
                config = SyncConfig(
                    pcMac = "F8:34:41:53:BE:24",
                    channel = 1,
                    historyCap = 500,
                    deviceName = "Android (a386a5)",
                    deviceId = "android-a386a5"
                ),
                onSyncNow = {},
                onSendTest = {},
                onReconnect = {},
                lastSyncTimestamp = 0L
            )
        }
    }
}

