package com.github.premtechworks.synqvia.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.ui.GlassPillBadge
import com.github.premtechworks.synqvia.ui.LiquidGlassCard
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.theme.CyanPrimary
import com.github.premtechworks.synqvia.ui.theme.GlassBorderSubtle
import com.github.premtechworks.synqvia.ui.theme.GlassSurface
import com.github.premtechworks.synqvia.ui.theme.StatusConnected
import com.github.premtechworks.synqvia.ui.theme.StatusOffline
import com.github.premtechworks.synqvia.ui.theme.StatusRetrying
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val logs by viewModel.diagnosticLogs.collectAsState()

    var pcMacInput by remember(config.pcMac) { mutableStateOf(config.pcMac) }
    var channelInput by remember(config.channel) { mutableIntStateOf(config.channel) }
    var historyCapInput by remember(config.historyCap) { mutableIntStateOf(config.historyCap) }
    var deviceNameInput by remember(config.deviceName) { mutableStateOf(config.deviceName) }
    var showLogs by remember { mutableStateOf(true) }

    val isMacValid = remember(pcMacInput) {
        pcMacInput.isBlank() || SyncPreferences.isValidMac(pcMacInput)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Configuration Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = GlassSurface,
                borderColor = CyanPrimary.copy(alpha = 0.35f),
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connection & Limits",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PC MAC Input
                    Text(
                        text = "Linux PC Bluetooth MAC",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pcMacInput,
                        onValueChange = { pcMacInput = it.uppercase(Locale.ROOT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pc_mac_input"),
                        placeholder = { Text("AA:BB:CC:DD:EE:FF", color = Color(0xFF64748B)) },
                        singleLine = true,
                        isError = !isMacValid,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x18FFFFFF),
                            unfocusedContainerColor = Color(0x10FFFFFF),
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii
                        )
                    )
                    Text(
                        text = if (!isMacValid) "Invalid MAC address format (must be XX:XX:XX:XX:XX:XX)"
                               else "Find PC MAC via: bluetoothctl show | grep Controller",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!isMacValid) StatusOffline else Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Channel Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RFCOMM Fallback Channel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Channel $channelInput (Default 1)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { if (channelInput > 1) channelInput-- },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x24FFFFFF)),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) { Text("-", color = Color.White) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channelInput.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { if (channelInput < 30) channelInput++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x24FFFFFF)),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) { Text("+", color = Color.White) }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // History Cap
                    Text(
                        text = "History Capacity Cap: ${if (historyCapInput == 0) "Unlimited (0)" else "$historyCapInput items"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Slider(
                        value = historyCapInput.toFloat(),
                        onValueChange = { historyCapInput = (it / 50).toInt() * 50 },
                        valueRange = 0f..2000f,
                        steps = 39,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = Color(0x26FFFFFF)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Device Name Input
                    Text(
                        text = "Broadcast Device Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = deviceNameInput,
                        onValueChange = { deviceNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x18FFFFFF),
                            unfocusedContainerColor = Color(0x10FFFFFF),
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Device ID: ${config.deviceId}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Button
                    Button(
                        onClick = {
                            viewModel.saveSettings(
                                pcMac = pcMacInput,
                                channel = channelInput,
                                historyCap = historyCapInput,
                                deviceName = deviceNameInput
                            )
                        },
                        enabled = isMacValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF00363D)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save & Reconnect",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Diagnostic Protocol Logs Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = GlassSurface,
                borderColor = GlassBorderSubtle
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Protocol Diagnostic Log",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        IconButton(onClick = { showLogs = !showLogs }) {
                            Icon(
                                imageVector = if (showLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showLogs) "Hide logs" else "Show logs",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showLogs) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF040810))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            logs.takeLast(10).forEach { log ->
                                val badgeColor = when (log.level) {
                                    "SUCCESS" -> StatusConnected
                                    "FRAME" -> CyanPrimary
                                    "WARN" -> StatusRetrying
                                    else -> Color(0xFF94A3B8)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = log.formattedTime,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[${log.level}]",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = badgeColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.message,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
