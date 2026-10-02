package com.github.premtechworks.synqvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.graphicsLayer
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.motion.rememberIsScreenVisible
import com.github.premtechworks.synqvia.ui.motion.snappySpring
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.ui.theme.TextTertiary
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.github.premtechworks.synqvia.service.SyncConnectionState
import com.github.premtechworks.synqvia.ui.theme.AccentAmber
import com.github.premtechworks.synqvia.ui.theme.AccentBlue
import com.github.premtechworks.synqvia.ui.theme.AccentGreen
import com.github.premtechworks.synqvia.ui.theme.AccentRed
import com.github.premtechworks.synqvia.ui.theme.BgBottom
import com.github.premtechworks.synqvia.ui.theme.BgTop
import com.github.premtechworks.synqvia.ui.theme.OnPrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.OutlineDark
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SurfaceDark
import com.github.premtechworks.synqvia.ui.theme.SurfaceHigh
import com.github.premtechworks.synqvia.ui.theme.SurfaceInset
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.SynqviaTypography
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary
import com.github.premtechworks.synqvia.ui.theme.TextTertiary

// ==========================================
// 1) SynqviaScreen
// ==========================================

/**
 * Screen wrapper with vertical navy gradient background, status-bar insets,
 * and bottom padding to clear the 64dp flat bottom navigation bar.
 */
@Composable
fun SynqviaScreen(
    modifier: Modifier = Modifier,
    applyStatusBarInsets: Boolean = true,
    bottomPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .then(if (applyStatusBarInsets) Modifier.statusBarsPadding() else Modifier)
            .padding(bottom = bottomPadding),
        content = content
    )
}

/**
 * Stylized circular "Q" logo mark: cyan ring with 5dp stroke and a rounded tail at bottom-right.
 */
@Composable
fun SynqviaLogoMark(
    modifier: Modifier = Modifier,
    color: Color = PrimaryCyan,
    size: Dp = 36.dp
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 5.dp.toPx()
        val radius = (this.size.minDimension - strokeWidth * 2) / 2
        val center = androidx.compose.ui.geometry.Offset(this.size.width / 2, this.size.height / 2)

        // Outer cyan ring
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Tail at bottom-right
        val tailStartX = center.x + radius * 0.45f
        val tailStartY = center.y + radius * 0.45f
        val tailEndX = center.x + radius + strokeWidth * 0.65f
        val tailEndY = center.y + radius + strokeWidth * 0.65f

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(tailStartX, tailStartY),
            end = androidx.compose.ui.geometry.Offset(tailEndX, tailEndY),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

// ==========================================
// 2) SynqviaCard
// ==========================================

/**
 * Standard card surface: 16dp rounded corners (20dp for hero cards),
 * 1dp outline border, flat SurfaceDark background, NO glow or blur.
 */
@Composable
fun SynqviaCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = SurfaceDark,
    borderColor: Color = OutlineDark,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(padding),
        content = content
    )
}

// ==========================================
// 3) IconTile
// ==========================================

/**
 * Icon container with 12dp radius, tint@14% alpha background, centered icon.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryCyan,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ==========================================
// 4) CircleIconButton
// ==========================================

/**
 * 40dp circular icon button with surfaceHigh background and 1dp outline border.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tint: Color = TextPrimary,
    backgroundColor: Color = SurfaceHigh,
    borderColor: Color = OutlineDark,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .pressable(
                targetScale = 0.9f,
                enabled = true,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// ==========================================
// 5) PrimaryButton
// ==========================================

/**
 * 44dp height (customizable), 12dp radius, filled cyan with dark text, 14sp SemiBold.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 44.dp,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryCyan,
            contentColor = OnPrimaryCyan,
            disabledContainerColor = PrimaryCyan.copy(alpha = 0.35f),
            disabledContentColor = OnPrimaryCyan.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .height(height)
            .pressable(targetScale = 0.96f, enabled = enabled && !loading)
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = OnPrimaryCyan,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = SynqviaTypography.ButtonLabel,
                color = OnPrimaryCyan
            )
        }
    }
}

// ==========================================
// 6) OutlinedCyanButton
// ==========================================

/**
 * 44dp height (customizable), transparent background, 1dp cyan border, cyan text, 12dp radius.
 */
@Composable
fun OutlinedCyanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 44.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) PrimaryCyan else PrimaryCyan.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = PrimaryCyan,
            disabledContentColor = PrimaryCyan.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .height(height)
            .pressable(targetScale = 0.96f, enabled = enabled)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) PrimaryCyan else PrimaryCyan.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = SynqviaTypography.ButtonLabel,
            color = if (enabled) PrimaryCyan else PrimaryCyan.copy(alpha = 0.35f)
        )
    }
}

// ==========================================
// 7) SmallCyanButton
// ==========================================

/**
 * 34dp height, 10dp radius, 12sp SemiBold text, used in Setup & compact actions.
 */
@Composable
fun SmallCyanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryCyan,
            contentColor = OnPrimaryCyan,
            disabledContainerColor = PrimaryCyan.copy(alpha = 0.35f),
            disabledContentColor = OnPrimaryCyan.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .height(34.dp)
            .pressable(targetScale = 0.96f, enabled = enabled)
    ) {
        Text(
            text = text,
            style = SynqviaType.ButtonSmall.copy(color = OnPrimaryCyan)
        )
    }
}

// ==========================================
// 8) SourcePill
// ==========================================

/**
 * 24dp height pill indicating direction:
 * "From PC" = green tint container with a small 14dp green circle containing a down-left arrow.
 * "To PC" = blue tint container with a small 14dp blue circle containing an up-right arrow.
 */
@Composable
fun SourcePill(
    isFromPc: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (isFromPc) AccentGreen else AccentBlue
    val text = if (isFromPc) "From PC" else "To PC"
    val icon = if (isFromPc) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SurfaceInset,
                modifier = Modifier.size(9.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = SynqviaType.Overline.copy(color = accent)
        )
    }
}

// ==========================================
// 9) StatusDot
// ==========================================

enum class SynqviaStatusState {
    CONNECTED,
    CONNECTING,
    OFFLINE
}

/**
 * 12dp status dot:
 * - connected = soft pulse ring (scale 1 -> 2.2, alpha 0.5 -> 0, 2s loop, only while the screen is visible)
 * - connecting = amber breathing (alpha 0.4 <-> 1, 1.2s)
 * - offline = static
 * - color change tween 300ms
 */
@Composable
fun StatusDot(
    state: SynqviaStatusState,
    modifier: Modifier = Modifier
) {
    val targetColor = when (state) {
        SynqviaStatusState.CONNECTED -> AccentGreen
        SynqviaStatusState.CONNECTING -> AccentAmber
        SynqviaStatusState.OFFLINE -> AccentRed
    }

    val animatedColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "status_dot_color"
    )

    val reduceMotion = LocalReduceMotion.current
    // Infinite animations only run while the screen is actually on-screen; otherwise they would
    // keep driving frames for a backgrounded app.
    val isVisible = rememberIsScreenVisible()
    val animateContinuously = !reduceMotion && isVisible

    val infiniteTransition = rememberInfiniteTransition(label = "status_dot_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connected_pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connected_pulse_alpha"
    )

    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "connecting_breathing_alpha"
    )

    val statusDescription = when (state) {
        SynqviaStatusState.CONNECTED -> "Status: Connected"
        SynqviaStatusState.CONNECTING -> "Status: Connecting"
        SynqviaStatusState.OFFLINE -> "Status: Offline"
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .semantics { contentDescription = statusDescription },
        contentAlignment = Alignment.Center
    ) {
        if (animateContinuously && state == SynqviaStatusState.CONNECTED) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }

        val dotAlpha = if (animateContinuously && state == SynqviaStatusState.CONNECTING) {
            breathingAlpha
        } else {
            1f
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer { alpha = dotAlpha }
                .clip(CircleShape)
                .background(animatedColor)
        )
    }
}

/**
 * Overload accepting domain SyncConnectionState
 */
@Composable
fun StatusDot(
    connectionState: SyncConnectionState,
    modifier: Modifier = Modifier
) {
    val state = when (connectionState) {
        is SyncConnectionState.Connected,
        is SyncConnectionState.Syncing -> SynqviaStatusState.CONNECTED
        is SyncConnectionState.Connecting -> SynqviaStatusState.CONNECTING
        is SyncConnectionState.Failed,
        is SyncConnectionState.Offline,
        is SyncConnectionState.Stopped -> SynqviaStatusState.OFFLINE
    }
    StatusDot(state = state, modifier = modifier)
}

// ==========================================
// 10) SectionHeader
// ==========================================

/**
 * 16sp SemiBold title with an optional cyan 18dp leading icon, and an optional right-aligned cyan 13sp action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = SynqviaTypography.SectionTitle,
                color = TextPrimary
            )
        }
        if (actionText != null) {
            Text(
                text = actionText,
                style = SynqviaType.ButtonSmall.copy(color = PrimaryCyan),
                modifier = if (onAction != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onAction
                    )
                } else Modifier
            )
        }
    }
}

// ==========================================
// 11) SynqviaTopBar
// ==========================================

/**
 * 56dp tall top bar with optional 40dp circle back button, 20sp SemiBold title, and optional trailing content.
 */
@Composable
fun SynqviaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    centerTitle: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Navigate back"
            )
            if (!centerTitle) {
                Spacer(modifier = Modifier.width(12.dp))
            }
        } else if (centerTitle) {
            Spacer(modifier = Modifier.size(40.dp))
        }

        Text(
            text = title,
            style = if (centerTitle) SynqviaType.Headline else SynqviaType.Title,
            color = TextPrimary,
            textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            trailing()
        } else if (centerTitle) {
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

/**
 * Radar / Signal Wave Icon (three curved arcs)
 * Animates three arcs fading in sequence (1.2s loop) while connecting, static otherwise.
 */
@Composable
fun RadarSignalIcon(
    isConnecting: Boolean,
    modifier: Modifier = Modifier,
    color: Color = PrimaryCyan
) {
    val reduceMotion = LocalReduceMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_phase"
    )

    val radarDesc = if (isConnecting) "Connecting radar signal" else "Radar signal idle"

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(24.dp)
            .semantics { contentDescription = radarDesc }
    ) {
        val strokeWidth = 2.dp.toPx()
        val centerX = size.width * 0.15f
        val centerY = size.height * 0.5f

        val radii = listOf(6.dp.toPx(), 11.dp.toPx(), 16.dp.toPx())
        val arcStroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        radii.forEachIndexed { index, r ->
            val arcAlpha = if (isConnecting && !reduceMotion) {
                val dist = (phase - index).let { if (it < 0) it + 3f else it }
                (1f - (dist / 3f)).coerceIn(0.25f, 1.0f)
            } else {
                1.0f
            }

            drawArc(
                color = color.copy(alpha = arcAlpha),
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(centerX - r, centerY - r),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = arcStroke
            )
        }
    }
}

/**
 * Edit Bluetooth MAC dialog with uppercase formatting and regex validation.
 */
@Composable
fun EditMacDialog(
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
                        style = SynqviaTypography.Caption,
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

// ==========================================
// 12) Stubs for Rows (Finished in later prompts)
// ==========================================

/**
 * Custom compact switch matching mockup:
 * 44x26dp, with the thumb spring-moving, stretching 4dp while pressed,
 * and a 200ms track color tween.
 */
@Composable
fun SynqviaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = LocalAppHaptics.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val trackColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) PrimaryCyan else SurfaceHigh,
        animationSpec = tween(durationMillis = 200),
        label = "switch_track_color"
    )

    val thumbWidth by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 20.dp,
        animationSpec = snappySpring(),
        label = "switch_thumb_width"
    )

    val targetThumbX = if (checked) (44.dp - 3.dp - thumbWidth) else 3.dp
    val thumbX by animateDpAsState(
        targetValue = targetThumbX,
        animationSpec = snappySpring(),
        label = "switch_thumb_x"
    )

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (enabled) trackColor else trackColor.copy(alpha = 0.4f))
            .border(
                1.dp,
                if (checked) PrimaryCyan else OutlineDark,
                RoundedCornerShape(13.dp)
            )
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = { newValue ->
                            haptics.tick()
                            onCheckedChange(newValue)
                        }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(width = thumbWidth, height = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.5f))
        )
    }
}

/**
 * Row stub for settings toggle items
 */
@Composable
fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconTile(icon = icon, tint = PrimaryCyan, size = 36.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SynqviaTypography.CardTitleSmall, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = SynqviaTypography.Caption, color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        SynqviaSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Row stub for key-value display (e.g., Linux PC MAC, history capacity)
 */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = SynqviaTypography.Body, color = TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = if (isMonospace) SynqviaType.Mono else SynqviaType.Headline,
                color = TextPrimary
            )
            if (onCopy != null) {
                Spacer(modifier = Modifier.width(8.dp))
                CircleIconButton(
                    icon = Icons.Default.ContentCopy,
                    onClick = onCopy,
                    size = 32.dp,
                    iconSize = 16.dp,
                    contentDescription = "Copy $label"
                )
            }
        }
    }
}

/**
 * Row stub for actionable settings / permission rows with chevron
 */
@Composable
fun ChevronRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = TextSecondary,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconTile(icon = icon, tint = PrimaryCyan, size = 36.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SynqviaTypography.CardTitleSmall, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = SynqviaTypography.Caption, color = TextSecondary)
            }
        }
        if (value != null) {
            Text(text = value, style = SynqviaTypography.Caption.copy(color = valueColor))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ==========================================
// Previews for every component
// ==========================================

@Preview(name = "Synqvia Screen Preview", showBackground = true)
@Composable
fun PreviewSynqviaScreen() {
    SynqviaTheme {
        SynqviaScreen {
            Text(
                text = "Screen Content inside SynqviaScreen",
                style = SynqviaTypography.ScreenTitle,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Synqvia Card Preview", showBackground = true)
@Composable
fun PreviewSynqviaCard() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            SynqviaCard {
                Text(text = "Synqvia Card Title", style = SynqviaTypography.CardTitle)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Flat card with 16dp radius and 1dp outline border.", style = SynqviaTypography.Body)
            }
        }
    }
}

@Preview(name = "Icon Tile Preview", showBackground = true)
@Composable
fun PreviewIconTile() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            IconTile(icon = Icons.Default.Bluetooth, tint = AccentGreen)
        }
    }
}

@Preview(name = "Circle Icon Button Preview", showBackground = true)
@Composable
fun PreviewCircleIconButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            CircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = {})
        }
    }
}

@Preview(name = "Primary Button Preview", showBackground = true)
@Composable
fun PreviewPrimaryButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            PrimaryButton(text = "Sync Now", icon = Icons.Default.Sync, onClick = {})
        }
    }
}

@Preview(name = "Outlined Cyan Button Preview", showBackground = true)
@Composable
fun PreviewOutlinedCyanButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            OutlinedCyanButton(text = "Send Test", onClick = {})
        }
    }
}

@Preview(name = "Small Cyan Button Preview", showBackground = true)
@Composable
fun PreviewSmallCyanButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            SmallCyanButton(text = "Open", onClick = {})
        }
    }
}

@Preview(name = "Source Pills Preview", showBackground = true)
@Composable
fun PreviewSourcePill() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourcePill(isFromPc = true)
                SourcePill(isFromPc = false)
            }
        }
    }
}

@Preview(name = "Status Dots Preview", showBackground = true)
@Composable
fun PreviewStatusDot() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusDot(state = SynqviaStatusState.CONNECTED)
                StatusDot(state = SynqviaStatusState.CONNECTING)
                StatusDot(state = SynqviaStatusState.OFFLINE)
            }
        }
    }
}

@Preview(name = "Section Header Preview", showBackground = true)
@Composable
fun PreviewSectionHeader() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            SectionHeader(
                icon = Icons.Default.Sync,
                title = "Recent Activity",
                actionText = "View All →",
                onAction = {}
            )
        }
    }
}

@Preview(name = "Top Bar Preview", showBackground = true)
@Composable
fun PreviewSynqviaTopBar() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop)) {
            SynqviaTopBar(
                title = "Clipboard History",
                onBack = {}
            )
        }
    }
}

@Preview(name = "Setting Rows Preview", showBackground = true)
@Composable
fun PreviewRows() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            SynqviaCard {
                SettingToggleRow(
                    title = "Auto Sync",
                    subtitle = "Sync clipboard changes automatically",
                    checked = true,
                    onCheckedChange = {}
                )
                KeyValueRow(
                    label = "Linux PC Bluetooth MAC",
                    value = "F8:34:41:53:BE:24",
                    isMonospace = true,
                    onCopy = {}
                )
                ChevronRow(
                    title = "Bluetooth Permissions",
                    subtitle = "Required to connect to Linux PC",
                    value = "Active",
                    valueColor = AccentGreen,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Synqvia Logo Mark Preview", showBackground = true)
@Composable
fun PreviewSynqviaLogoMark() {
    SynqviaTheme {
        Box(modifier = Modifier.background(BgTop).padding(16.dp)) {
            SynqviaLogoMark()
        }
    }
}

