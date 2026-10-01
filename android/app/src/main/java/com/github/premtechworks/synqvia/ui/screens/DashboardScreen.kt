package com.github.premtechworks.synqvia.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premtechworks.synqvia.ui.GlassPillBadge
import com.github.premtechworks.synqvia.ui.LiquidGlassCard
import com.github.premtechworks.synqvia.ui.MainTab
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.ClipHistoryItem
import com.github.premtechworks.synqvia.ui.components.HeroSyncCard
import com.github.premtechworks.synqvia.ui.theme.BlueAccent
import com.github.premtechworks.synqvia.ui.theme.ConflictLoserColor
import com.github.premtechworks.synqvia.ui.theme.CyanPrimary
import com.github.premtechworks.synqvia.ui.theme.GlassBorderSubtle
import com.github.premtechworks.synqvia.ui.theme.GlassSurface
import com.github.premtechworks.synqvia.ui.theme.LocalSendColor
import com.github.premtechworks.synqvia.ui.theme.RemoteRecvColor

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val config by viewModel.config.collectAsState()
    val stats by viewModel.syncStats.collectAsState()
    val clips by viewModel.filteredClips.collectAsState()
    val isDefaultIme by viewModel.isDefaultIme.collectAsState()
    val recentClips = clips.take(5)
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Sync Card
        item {
            HeroSyncCard(
                connectionState = connectionState,
                config = config,
                onSyncNow = { viewModel.syncNow() },
                onSendTest = { viewModel.sendTestClip() },
                onReconnect = { viewModel.reconnect() },
                onToggleService = { viewModel.toggleService(it) }
            )
        }

        // IME Privilege Warning when Synqvia is not the default keyboard
        if (!isDefaultIme) {
            item {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ime_privilege_card"),
                    backgroundColor = GlassSurface,
                    borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic capture requires Synqvia to be the default keyboard.",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Android restricts background clipboard reading to the default keyboard. Manual Sync Now, Share, and Quick Settings Tile remain functional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                imm?.showInputMethodPicker()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Text(
                                text = "Switch",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Stats Overview Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total",
                    value = stats.totalCount.toString(),
                    accentColor = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Sent ▶",
                    value = stats.localSentCount.toString(),
                    accentColor = LocalSendColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Recv ◀",
                    value = stats.remoteReceivedCount.toString(),
                    accentColor = RemoteRecvColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Loss ⚠",
                    value = stats.conflictCount.toString(),
                    accentColor = ConflictLoserColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Seamless Setup Callout Card if MAC is not configured
        if (config.pcMac.isBlank()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = GlassSurface,
                    borderColor = CyanPrimary.copy(alpha = 0.5f),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup PC Connection",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter Linux Bluetooth MAC to start sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Button(
                            onClick = { onNavigateTab(MainTab.SETTINGS) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Configure", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Recent Activity Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Clipboard Stream",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = { onNavigateTab(MainTab.HISTORY) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "View All",
                        color = CyanPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Recent Clips or Empty Placeholder
        if (recentClips.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = GlassSurface,
                    borderColor = GlassBorderSubtle
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No clipboard items yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copy text on your phone or PC to sync instantly",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(recentClips, key = { it.id }) { clip ->
                ClipHistoryItem(
                    clip = clip,
                    onCopyAndResend = { viewModel.resendClip(clip) },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, clip.text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share clip"))
                    },
                    onDelete = { viewModel.deleteClip(clip.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier,
        backgroundColor = GlassSurface,
        borderColor = accentColor.copy(alpha = 0.3f),
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
