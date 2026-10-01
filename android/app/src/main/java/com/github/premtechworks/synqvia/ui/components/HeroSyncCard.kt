package com.github.premtechworks.synqvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.service.SyncConnectionState
import com.github.premtechworks.synqvia.ui.GlassPillBadge
import com.github.premtechworks.synqvia.ui.GlowingStatusDot
import com.github.premtechworks.synqvia.ui.LiquidGlassCard
import com.github.premtechworks.synqvia.ui.theme.BlueAccent
import com.github.premtechworks.synqvia.ui.theme.CyanGlow
import com.github.premtechworks.synqvia.ui.theme.CyanPrimary
import com.github.premtechworks.synqvia.ui.theme.GlassBorder
import com.github.premtechworks.synqvia.ui.theme.GlassSurfaceElevated
import com.github.premtechworks.synqvia.ui.theme.StatusConnected
import com.github.premtechworks.synqvia.ui.theme.StatusConnectedGlow
import com.github.premtechworks.synqvia.ui.theme.StatusOffline
import com.github.premtechworks.synqvia.ui.theme.StatusOfflineGlow
import com.github.premtechworks.synqvia.ui.theme.StatusRetrying
import com.github.premtechworks.synqvia.ui.theme.StatusRetryingGlow

@Composable
fun HeroSyncCard(
    connectionState: SyncConnectionState,
    config: SyncConfig,
    onSyncNow: () -> Unit,
    onSendTest: () -> Unit,
    onReconnect: () -> Unit,
    onToggleService: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEnabled = connectionState !is SyncConnectionState.Stopped

    val (statusTitle, statusSubtitle, statusColor, glowColor) = when (connectionState) {
        is SyncConnectionState.Connected -> Quadruple(
            "Connected",
            "Paired with ${connectionState.peerName}",
            StatusConnected,
            StatusConnectedGlow
        )
        is SyncConnectionState.Connecting -> Quadruple(
            "Connecting...",
            if (connectionState.nextRetrySec > 0)
                "Retrying ${connectionState.attempt}/${connectionState.maxAttempts} • in ${connectionState.nextRetrySec}s"
            else
                "Connecting ${connectionState.attempt}/${connectionState.maxAttempts}…",
            StatusRetrying,
            StatusRetryingGlow
        )
        is SyncConnectionState.Offline -> Quadruple(
            "Offline",
            connectionState.reason,
            StatusOffline,
            StatusOfflineGlow
        )
        is SyncConnectionState.Failed -> Quadruple(
            "Connection Failed",
            "Connection Failed - Try Again",
            StatusOffline,
            StatusOfflineGlow
        )
        is SyncConnectionState.Stopped -> Quadruple(
            "Stopped",
            "Sync service is disabled",
            Color(0xFF64748B),
            Color(0x3364748B)
        )
        is SyncConnectionState.Syncing -> Quadruple(
            "Syncing",
            "Streaming clipboard frames",
            CyanPrimary,
            CyanGlow
        )
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_sync_card")
            .semantics { contentDescription = "Sync connection status: $statusTitle, $statusSubtitle" },
        backgroundColor = GlassSurfaceElevated,
        borderColor = if (connectionState is SyncConnectionState.Connected) GlassBorder else GlassBorder.copy(alpha = 0.4f),
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Status Dot + State Title + Reconnect Button & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    GlowingStatusDot(
                        statusColor = statusColor,
                        glowColor = glowColor,
                        size = 14.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEnabled && connectionState !is SyncConnectionState.Failed) {
                        IconButton(
                            onClick = onReconnect,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x18FFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .testTag("reconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reconnect Bluetooth RFCOMM socket",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleService,
                        modifier = Modifier.testTag("service_toggle_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.35f),
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0x22FFFFFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Adapter & Target Info Glass Plate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x14000000))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Target PC MAC",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (config.pcMac.isNotBlank()) config.pcMac else "Not configured",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (config.pcMac.isNotBlank()) Color.White else StatusRetrying
                            )
                        }
                    }

                    GlassPillBadge(
                        text = "CH ${config.channel}",
                        accentColor = BlueAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Actions Area: Depends on state (Failed, Stopped, or Active)
            when (connectionState) {
                is SyncConnectionState.Failed -> {
                    Button(
                        onClick = onReconnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("try_again_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connection Failed - Try Again",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is SyncConnectionState.Stopped -> {
                    Button(
                        onClick = { onToggleService(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_service_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF00363D)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Sync Service",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    // Action Buttons Row: "Sync Now" + "Send Test"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onSyncNow,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sync_now_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color(0xFF00363D)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sync Now",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onSendTest,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("send_test_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CyanPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Test",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
