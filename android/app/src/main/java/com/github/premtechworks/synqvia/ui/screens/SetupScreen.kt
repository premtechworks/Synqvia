package com.github.premtechworks.synqvia.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.github.premtechworks.synqvia.ui.LiquidGlassCard
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.theme.CyanPrimary
import com.github.premtechworks.synqvia.ui.theme.DarkNavyBackground
import com.github.premtechworks.synqvia.ui.theme.GlassBorderSubtle
import com.github.premtechworks.synqvia.ui.theme.GlassSurface
import com.github.premtechworks.synqvia.ui.theme.StatusConnected

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var btPermissionGranted by remember {
        mutableStateOf(checkBtPermissions(context))
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var isBatteryIgnoringOptimizations by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        btPermissionGranted = checkBtPermissions(context)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        notificationPermissionGranted = checkNotificationPermission(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("setup_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "✦ Seamless Setup",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure permissions to allow reliable Bluetooth sync even with Android 10-14 background clipboard limits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // 1. Bluetooth Permissions
        item {
            SetupItemCard(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth Permissions",
                description = "Required to connect to your Linux PC via RFCOMM Classic socket.",
                isComplete = btPermissionGranted,
                actionLabel = if (btPermissionGranted) "Granted" else "Grant Access",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        btPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    } else {
                        btPermissionGranted = true
                    }
                }
            )
        }

        // 2. Notification Permission
        item {
            SetupItemCard(
                icon = Icons.Default.Notifications,
                title = "Persistent Notification",
                description = "Shows real-time connection status and quick 'Sync to PC' action button.",
                isComplete = notificationPermissionGranted,
                actionLabel = if (notificationPermissionGranted) "Granted" else "Enable",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationPermissionGranted = true
                    }
                }
            )
        }

        // 3. Battery Optimization
        item {
            SetupItemCard(
                icon = Icons.Default.BatteryAlert,
                title = "Unrestricted Battery",
                description = "Prevents OEM aggressive battery managers from killing the background sync daemon.",
                isComplete = isBatteryIgnoringOptimizations,
                actionLabel = if (isBatteryIgnoringOptimizations) "Unrestricted" else "Configure",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                }
            )
        }

        // 4. Accessibility Service for Background Selection Grabbing
        item {
            SetupItemCard(
                icon = Icons.Default.AccessibilityNew,
                title = "Accessibility Selection Cache",
                description = "Overcomes Android 10+ background clipboard blocking by grabbing text selections as you copy.",
                isComplete = false, // Cannot definitively query without service inspection, offer direct shortcut
                actionLabel = "Open Settings",
                onAction = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }

        // 5. Clipboard Keyboard (IME)
        item {
            val isDefaultIme by viewModel.isDefaultIme.collectAsState()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            val isImeEnabled = imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true

            SetupItemCard(
                icon = Icons.Default.TouchApp,
                title = "Clipboard Keyboard (IME)",
                description = if (isDefaultIme) {
                    "Synqvia is set as the default keyboard. Automatic background clipboard capture is active."
                } else if (isImeEnabled) {
                    "Synqvia is enabled, but not set as the default keyboard. Automatic capture requires Synqvia to be the default keyboard."
                } else {
                    "Enable and set Synqvia as default keyboard to capture clipboard directly and paste clips from history into any app."
                },
                isComplete = isDefaultIme,
                actionLabel = if (isDefaultIme) "Active" else if (isImeEnabled) "Set Default" else "Enable Keyboard",
                onAction = {
                    if (isImeEnabled) {
                        imm?.showInputMethodPicker()
                    } else {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            )
        }

        // 6. One-Tap Shortcuts Info Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = GlassSurface,
                borderColor = CyanPrimary.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "4 Built-In One-Tap Sync Paths",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Quick Settings Tile: Swipe down status bar and tap 'Sync to PC'\n" +
                               "• Selection Menu: Highlight text anywhere and pick 'Send to PC'\n" +
                               "• Share Target: Use system Share Sheet → 'Send to PC'\n" +
                               "• Notification Action: Tap 'Sync to PC' in ongoing notification",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isComplete: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = GlassSurface,
        borderColor = if (isComplete) StatusConnected.copy(alpha = 0.4f) else GlassBorderSubtle,
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isComplete) StatusConnected.copy(alpha = 0.15f) else Color(0x18FFFFFF))
                    .border(
                        1.dp,
                        if (isComplete) StatusConnected.copy(alpha = 0.4f) else Color(0x28FFFFFF),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isComplete) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isComplete) StatusConnected else CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isComplete) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusConnected
                )
            } else {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color(0xFF00363D)
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private fun checkBtPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PermissionChecker.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PermissionChecker.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } else {
        true
    }
}
