package com.github.premtechworks.synqvia.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.entryStagger
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.components.ScreenScaffold
import com.github.premtechworks.synqvia.ui.components.ScrollableColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.github.premtechworks.synqvia.service.ClipAccessService
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.IconTile
import com.github.premtechworks.synqvia.ui.components.SmallCyanButton
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.components.SynqviaScreen
import com.github.premtechworks.synqvia.ui.theme.AccentAmber
import com.github.premtechworks.synqvia.ui.theme.AccentGreen
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary

data class ImeSetupState(
    val isGranted: Boolean,
    val description: String,
    val actionLabel: String
)

data class SetupCompletionState(
    val isAllRequiredGranted: Boolean,
    val missingRequiredCount: Int,
    val title: String,
    val subtitle: String
)

fun resolveImeSetupState(isDefaultIme: Boolean, isImeEnabled: Boolean): ImeSetupState {
    return when {
        isDefaultIme -> ImeSetupState(
            isGranted = true,
            description = "Synqvia is your default keyboard. Automatic capture is running.",
            actionLabel = ""
        )
        isImeEnabled -> ImeSetupState(
            isGranted = false,
            description = "Synqvia is enabled, but not set as the default keyboard. Automatic capture requires Synqvia to be the default.",
            actionLabel = "Set Default"
        )
        else -> ImeSetupState(
            isGranted = false,
            description = "Enable the Synqvia keyboard in system settings to use the clipboard panel.",
            actionLabel = "Enable"
        )
    }
}

fun resolveSetupCompletionState(
    btGranted: Boolean,
    notificationGranted: Boolean,
    batteryGranted: Boolean
): SetupCompletionState {
    val missingCount = listOf(btGranted, notificationGranted, batteryGranted).count { !it }
    return if (missingCount == 0) {
        SetupCompletionState(
            isAllRequiredGranted = true,
            missingRequiredCount = 0,
            title = "All set! You're ready to sync",
            subtitle = "All required permissions are configured."
        )
    } else {
        SetupCompletionState(
            isAllRequiredGranted = false,
            missingRequiredCount = missingCount,
            title = "$missingCount required step${if (missingCount > 1) "s" else ""} remaining",
            subtitle = "Configure required permissions to enable sync."
        )
    }
}

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onOpenImeSettings: () -> Unit = {}
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
    var isAccessibilityGranted by remember {
        mutableStateOf(checkAccessibilityService(context))
    }

    val isDefaultIme by viewModel.isDefaultIme.collectAsState()
    val imm = remember {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    }
    var isImeEnabled by remember {
        mutableStateOf(
            imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true
        )
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

    val haptics = LocalAppHaptics.current
    // Refresh states when returning from system settings or IME picker
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val wasBt = btPermissionGranted
                val wasNotif = notificationPermissionGranted
                val wasBattery = isBatteryIgnoringOptimizations
                val wasAccess = isAccessibilityGranted
                val wasIme = isImeEnabled
                val wasDefaultIme = isDefaultIme

                val newBt = checkBtPermissions(context)
                val newNotif = checkNotificationPermission(context)
                val newBattery = checkBatteryOptimization(context)
                val newAccess = checkAccessibilityService(context)
                val newIme = imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true
                viewModel.refreshImeStatus()
                val newDefaultIme = viewModel.isDefaultIme.value

                btPermissionGranted = newBt
                notificationPermissionGranted = newNotif
                isBatteryIgnoringOptimizations = newBattery
                isAccessibilityGranted = newAccess
                isImeEnabled = newIme

                val newlyGranted = (!wasBt && newBt) ||
                        (!wasNotif && newNotif) ||
                        (!wasBattery && newBattery) ||
                        (!wasAccess && newAccess) ||
                        (!wasIme && newIme) ||
                        (!wasDefaultIme && newDefaultIme)

                if (newlyGranted) {
                    haptics.confirm()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SetupScreenContent(
        btGranted = btPermissionGranted,
        notificationGranted = notificationPermissionGranted,
        batteryGranted = isBatteryIgnoringOptimizations,
        accessibilityGranted = isAccessibilityGranted,
        isDefaultIme = isDefaultIme,
        isImeEnabled = isImeEnabled,
        scrollState = scrollState,
        onRequestBluetooth = {
            if (btPermissionGranted) {
                try {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            } else {
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
        },
        onRequestNotification = {
            if (notificationPermissionGranted) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationPermissionGranted = true
                }
            }
        },
        onRequestBattery = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    if (!isBatteryIgnoringOptimizations) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                }
            }
        },
        onRequestAccessibility = {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            } catch (_: Exception) {}
        },
        onRequestIme = {
            if (isDefaultIme || isImeEnabled) {
                imm?.showInputMethodPicker()
            } else {
                try {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        },
        onOpenImeSettings = onOpenImeSettings,
        modifier = modifier
    )
}

@Composable
fun SetupScreenContent(
    btGranted: Boolean,
    notificationGranted: Boolean,
    batteryGranted: Boolean,
    accessibilityGranted: Boolean,
    isDefaultIme: Boolean,
    isImeEnabled: Boolean,
    onRequestBluetooth: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestIme: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onOpenImeSettings: () -> Unit = {}
) {
    val imeState = resolveImeSetupState(isDefaultIme, isImeEnabled)
    val completionState = resolveSetupCompletionState(btGranted, notificationGranted, batteryGranted)
    val haptics = LocalAppHaptics.current

    val isScrolled by remember { derivedStateOf { scrollState.value > 8 } }

    androidx.compose.runtime.LaunchedEffect(completionState.isAllRequiredGranted) {
        if (completionState.isAllRequiredGranted) {
            haptics.confirm()
        }
    }

    ScreenScaffold(
        modifier = modifier,
        isScrolled = isScrolled,
        header = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Seamless Setup",
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
                .testTag("setup_screen")
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Configure permissions to allow reliable Bluetooth sync even with Android 10-14 background clipboard limits.",
                style = SynqviaType.Footnote,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section label: REQUIRED
            Text(
                text = "REQUIRED",
                style = SynqviaType.Overline,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            // B) Step Rows (10dp gaps)
            // 1. Bluetooth Permissions
            SetupStepCard(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth Permissions",
                description = "Required to connect to your Linux PC via RFCOMM Classic socket.",
                isGranted = btGranted,
                actionLabel = "Grant",
                onAction = onRequestBluetooth
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Persistent Notification
            SetupStepCard(
                icon = Icons.Default.Notifications,
                title = "Persistent Notification",
                description = "Shows real-time connection status and quick 'Sync to PC' action.",
                isGranted = notificationGranted,
                actionLabel = "Grant",
                onAction = onRequestNotification
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Unrestricted Battery
            SetupStepCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Unrestricted Battery",
                description = "Prevents OEM aggressive battery managers from killing the background sync daemon.",
                isGranted = batteryGranted,
                actionLabel = "Grant",
                onAction = onRequestBattery
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section label: OPTIONAL
            Text(
                text = "OPTIONAL",
                style = SynqviaType.Overline,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            // 4. Accessibility Selection Cache
            SetupStepCard(
                icon = Icons.Default.AccessibilityNew,
                title = "Accessibility Selection Cache",
                description = "Overcomes Android 10+ background clipboard blocking by grabbing text selections as you copy.",
                isGranted = accessibilityGranted,
                actionLabel = "Open",
                onAction = onRequestAccessibility
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Clipboard Keyboard (IME)
            SetupStepCard(
                icon = Icons.Default.Keyboard,
                title = "Clipboard Keyboard (IME)",
                description = imeState.description,
                isGranted = imeState.isGranted,
                actionLabel = imeState.actionLabel,
                onAction = onRequestIme,
                onCardClick = onOpenImeSettings
            )

            // C) Completion Banner (12dp below the last card)
            Spacer(modifier = Modifier.height(12.dp))

            SetupCompletionBanner(completionState = completionState)
        }
    }
}

@Composable
private fun SetupStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null
) {
    val iconTint by animateColorAsState(
        targetValue = if (isGranted) AccentGreen else PrimaryCyan,
        animationSpec = tween(300),
        label = "setup_icon_tint"
    )
    val haptics = LocalAppHaptics.current

    SynqviaCard(
        modifier = modifier
            .fillMaxWidth()
            .pressable(
                targetScale = 0.98f,
                showOverlay = true,
                onClick = {
                    haptics.tick()
                    (onCardClick ?: onAction).invoke()
                }
            ),
        padding = 14.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = icon,
                tint = iconTint,
                size = 44.dp,
                iconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = SynqviaType.Caption,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Crossfade(
                targetState = isGranted,
                animationSpec = tween(200),
                label = "setup_grant_active_crossfade"
            ) { granted ->
                if (granted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active",
                            style = SynqviaType.CaptionSemiBold,
                            color = AccentGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AccentGreen.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    SmallCyanButton(
                        text = actionLabel,
                        onClick = onAction
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupCompletionBanner(
    completionState: SetupCompletionState,
    modifier: Modifier = Modifier
) {
    val isSuccess = completionState.isAllRequiredGranted
    val reduceMotion = LocalReduceMotion.current

    AnimatedContent(
        targetState = isSuccess,
        transitionSpec = {
            if (targetState) {
                // Last required step completes: green banner slides up + fades in
                (slideInVertically(
                    animationSpec = tween(300, easing = DecelerateEasing)
                ) { it / 2 } + fadeIn(tween(300))).togetherWith(
                    fadeOut(tween(150))
                )
            } else {
                fadeIn(tween(200)).togetherWith(fadeOut(tween(200)))
            }
        },
        label = "completion_banner_transition",
        modifier = modifier
    ) { success ->
        val accent = if (success) AccentGreen else AccentAmber
        val checkProgress = remember { Animatable(if (success && !reduceMotion) 0f else 1f) }

        LaunchedEffect(success) {
            if (success) {
                if (!reduceMotion) {
                    checkProgress.snapTo(0f)
                    checkProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 500, easing = DecelerateEasing)
                    )
                } else {
                    checkProgress.snapTo(1f)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                if (success) {
                    // Check mark drawn as a stroke animation over 500ms
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.24f, size.height * 0.52f)
                            lineTo(size.width * 0.44f, size.height * 0.72f)
                            lineTo(size.width * 0.76f, size.height * 0.30f)
                        }
                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(path, false)
                        val totalLength = pathMeasure.length
                        val dst = Path()
                        pathMeasure.getSegment(0f, totalLength * checkProgress.value, dst, true)

                        drawPath(
                            path = dst,
                            color = Color.White,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = completionState.title,
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = completionState.subtitle,
                    style = SynqviaType.Caption,
                    color = TextSecondary
                )
            }
        }
    }
}

internal fun checkBtPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PermissionChecker.PERMISSION_GRANTED
    } else {
        true
    }
}

internal fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PermissionChecker.PERMISSION_GRANTED
    } else {
        true
    }
}

internal fun checkBatteryOptimization(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } else {
        true
    }
}

internal fun checkAccessibilityService(context: Context): Boolean {
    return try {
        val expectedComponentName = ComponentName(context, ClipAccessService::class.java).flattenToString()
        val expectedShortName = ComponentName(context, ClipAccessService::class.java).flattenToShortString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        enabledServices.split(':').any {
            it.equals(expectedComponentName, ignoreCase = true) ||
                    it.equals(expectedShortName, ignoreCase = true) ||
                    ComponentName.unflattenFromString(it)?.packageName == context.packageName
        }
    } catch (_: Exception) {
        false
    }
}

// ==========================================
// Previews
// ==========================================

@Preview(name = "Setup - All Granted", showBackground = true)
@Composable
private fun SetupScreenAllGrantedPreview() {
    SynqviaTheme {
        SetupScreenContent(
            btGranted = true,
            notificationGranted = true,
            batteryGranted = true,
            accessibilityGranted = true,
            isDefaultIme = true,
            isImeEnabled = true,
            onRequestBluetooth = {},
            onRequestNotification = {},
            onRequestBattery = {},
            onRequestAccessibility = {},
            onRequestIme = {}
        )
    }
}

@Preview(name = "Setup - Mockup State (Only Required Granted)", showBackground = true)
@Composable
private fun SetupScreenMockupStatePreview() {
    SynqviaTheme {
        SetupScreenContent(
            btGranted = true,
            notificationGranted = true,
            batteryGranted = true,
            accessibilityGranted = false,
            isDefaultIme = false,
            isImeEnabled = true,
            onRequestBluetooth = {},
            onRequestNotification = {},
            onRequestBattery = {},
            onRequestAccessibility = {},
            onRequestIme = {}
        )
    }
}

@Preview(name = "Setup - Nothing Granted", showBackground = true)
@Composable
private fun SetupScreenNothingGrantedPreview() {
    SynqviaTheme {
        SetupScreenContent(
            btGranted = false,
            notificationGranted = false,
            batteryGranted = false,
            accessibilityGranted = false,
            isDefaultIme = false,
            isImeEnabled = false,
            onRequestBluetooth = {},
            onRequestNotification = {},
            onRequestBattery = {},
            onRequestAccessibility = {},
            onRequestIme = {}
        )
    }
}
