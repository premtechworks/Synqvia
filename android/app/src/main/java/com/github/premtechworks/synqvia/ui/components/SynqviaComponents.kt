package com.github.premtechworks.synqvia.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.service.SyncConnectionState
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.motion.rememberIsScreenVisible
import com.github.premtechworks.synqvia.ui.motion.snappySpring
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.SynqviaTypography
import com.github.premtechworks.synqvia.ui.theme.synqviaCardShadow
import java.util.Locale

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class SynqviaLightDarkPreview

// ==========================================
// 1) SynqviaScreen
// ==========================================

/**
 * Screen wrapper with vertical gradient background, status-bar insets,
 * and bottom padding to clear the bottom navigation bar.
 */
@Composable
fun SynqviaScreen(
    modifier: Modifier = Modifier,
    applyStatusBarInsets: Boolean = true,
    bottomPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = SynqviaTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.bgTop, colors.bgBottom)))
            .then(if (applyStatusBarInsets) Modifier.statusBarsPadding() else Modifier)
            .padding(bottom = bottomPadding),
        content = content
    )
}


// ==========================================
// 2) SynqviaCard
// ==========================================

/**
 * Standard card surface: 16dp rounded corners (20dp for hero cards),
 * 1dp outline border, token surface background, and soft tinted shadow in light mode.
 */
@Composable
fun SynqviaCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = SynqviaTheme.colors.surface,
    borderColor: Color = SynqviaTheme.colors.outline,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SynqviaTheme.colors
    Column(
        modifier = modifier
            .synqviaCardShadow(
                elevation = elevation,
                shape = shape,
                ambientColor = colors.cardShadowAmbient,
                spotColor = colors.cardShadowSpot
            )
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
 * Icon container with 12dp radius, container background, centered icon.
 * Supports passing a container/content pair, with fallback to tint@14% alpha.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    tint: Color = SynqviaTheme.colors.primary,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 12.dp,
    contentDescription: String? = null
) {
    val effectiveContentColor = contentColor ?: tint
    val effectiveContainerColor = containerColor ?: effectiveContentColor.copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(effectiveContainerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveContentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ==========================================
// SettingsNavRow
// ==========================================

/**
 * Shared settings navigation / collapsible header row.
 * Used by: Quick ways to send, Protocol Diagnostic Log, Contact & Support.
 *
 * Metrics:
 * - IconTile: 36dp, 10dp radius, 18dp icon
 * - Row padding: 16dp horizontal, 14dp vertical
 * - Title: Headline SemiBold
 * - Optional subtitle: Footnote textSecondary
 * - Trailing chevron: 20dp textTertiary
 */
@Composable
fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconContainerColor: Color = SynqviaTheme.colors.blueContainer,
    iconContentColor: Color = SynqviaTheme.colors.blue,
    contentDescription: String? = null,
    trailing: @Composable () -> Unit
) {
    val colors = SynqviaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = icon,
                tint = iconContentColor,
                containerColor = iconContainerColor,
                contentColor = iconContentColor,
                size = 36.dp,
                iconSize = 18.dp,
                cornerRadius = 10.dp,
                contentDescription = contentDescription
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SynqviaType.Headline,
                    color = colors.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = SynqviaType.Footnote,
                        color = colors.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailing()
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
    tint: Color = SynqviaTheme.colors.textPrimary,
    backgroundColor: Color = SynqviaTheme.colors.surfaceHigh,
    borderColor: Color = SynqviaTheme.colors.outline,
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
 * 44dp height (customizable), 12dp radius, filled primary with onPrimary text, 14sp SemiBold.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    loading: Boolean = false
) {
    val colors = SynqviaTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = 0.35f),
            disabledContentColor = colors.onPrimary.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .height(height)
            .pressable(targetScale = 0.96f, enabled = enabled && !loading)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.onPrimary,
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
                color = colors.onPrimary
            )
        }
    }
}

// ==========================================
// 6) OutlinedPrimaryButton
// ==========================================

/**
 * 44dp height (customizable), transparent background, 1dp border, primary text, 12dp radius.
 * In light mode defaults to 1dp #BBD4FA border; in dark defaults to primary border.
 */
@Composable
fun OutlinedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    borderColor: Color = if (SynqviaTheme.isDark) SynqviaTheme.colors.primary else Color(0xFFBBD4FA),
    contentColor: Color = SynqviaTheme.colors.primary
) {
    val strokeColor = if (enabled) borderColor else borderColor.copy(alpha = 0.35f)
    val effectiveContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.35f)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, strokeColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = effectiveContentColor,
            disabledContentColor = contentColor.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .height(height)
            .pressable(targetScale = 0.96f, enabled = enabled)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = effectiveContentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = SynqviaTypography.ButtonLabel,
            color = effectiveContentColor
        )
    }
}

@Composable
fun OutlinedCyanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    borderColor: Color = if (SynqviaTheme.isDark) SynqviaTheme.colors.primary else Color(0xFFBBD4FA),
    contentColor: Color = SynqviaTheme.colors.primary
) {
    OutlinedPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        height = height,
        borderColor = borderColor,
        contentColor = contentColor
    )
}

// ==========================================
// 7) TonalButton
// ==========================================

/**
 * 34dp height, 10dp radius, 12sp SemiBold text, used in Setup & compact actions.
 */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = SynqviaTheme.colors.tonalButtonContainer,
    contentColor: Color = SynqviaTheme.colors.tonalButtonContent
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.35f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .height(34.dp)
            .pressable(targetScale = 0.96f, enabled = enabled)
    ) {
        Text(
            text = text,
            style = SynqviaType.ButtonSmall.copy(color = contentColor)
        )
    }
}

@Composable
fun SmallCyanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = SynqviaTheme.colors.tonalButtonContainer,
    contentColor: Color = SynqviaTheme.colors.tonalButtonContent
) {
    TonalButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor
    )
}

// ==========================================
// 8) SourcePill
// ==========================================

/**
 * 24dp height pill indicating direction:
 * "From PC" = greenContainer bg + greenText fg + green circle glyph.
 * "To PC" = blueContainer bg + blue fg + blue circle glyph.
 */
@Composable
fun SourcePill(
    isFromPc: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors
    val isDark = colors.isDark
    val bg = if (isFromPc) colors.pillFromPcBg else colors.pillToPcBg
    val fg = if (isFromPc) colors.pillFromPcFg else colors.pillToPcFg
    val text = if (isFromPc) "From PC" else "To PC"
    val icon = if (isFromPc) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                1.dp,
                if (isDark) fg.copy(alpha = 0.35f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(fg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) colors.surfaceInset else Color.White,
                modifier = Modifier.size(9.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = SynqviaType.Overline.copy(color = fg)
        )
    }
}

// ==========================================
// 9) StatusDot
// ==========================================

enum class SynqviaStatusState {
    CONNECTED, CONNECTING, OFFLINE
}

/**
 * 24dp status indicator:
 * - connected = green dot with 24dp halo ring in light mode / pulse in dark mode
 * - connecting = amber breathing (alpha 0.4 <-> 1, 1.2s)
 * - offline = static red
 * - color change tween 300ms
 */
@Composable
fun StatusDot(
    state: SynqviaStatusState,
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors
    val isDark = colors.isDark
    val targetColor = when (state) {
        SynqviaStatusState.CONNECTED -> colors.green
        SynqviaStatusState.CONNECTING -> colors.amber
        SynqviaStatusState.OFFLINE -> colors.red
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "status_dot_color"
    )

    val reduceMotion = LocalReduceMotion.current
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
        if (!isDark && state == SynqviaStatusState.CONNECTED) {
            // Light theme halo ring = green @ 20%, 24dp
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.green.copy(alpha = 0.20f))
            )
        } else if (isDark && animateContinuously && state == SynqviaStatusState.CONNECTED) {
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
 * 16sp SemiBold title with an optional leading icon, and an optional right-aligned action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = SynqviaTheme.colors
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
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = SynqviaTypography.SectionTitle,
                color = colors.textPrimary
            )
        }
        if (actionText != null) {
            Text(
                text = actionText,
                style = SynqviaType.ButtonSmall.copy(color = colors.primary),
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
    val colors = SynqviaTheme.colors
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
            color = colors.textPrimary,
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
    color: Color = SynqviaTheme.colors.primary
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
                        unfocusedTextColor = colors.textPrimary
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
                        color = colors.red,
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

// ==========================================
// 12) Switch and Settings Rows
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
    val colors = SynqviaTheme.colors
    val haptics = LocalAppHaptics.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetTrackColor = if (checked) colors.switchOnTrack else colors.switchOffTrack
    val targetBorderColor = if (checked) colors.switchOnTrack else colors.switchOffBorder
    val targetThumbColor = if (checked) colors.switchOnThumb else colors.switchOffThumb

    val trackColor by animateColorAsState(
        targetValue = targetTrackColor,
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
                targetBorderColor,
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
                .background(if (enabled) targetThumbColor else targetThumbColor.copy(alpha = 0.5f))
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
    icon: ImageVector? = null,
    iconContainer: Color? = null,
    iconContent: Color? = null
) {
    val colors = SynqviaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconTile(
                icon = icon,
                containerColor = iconContainer,
                contentColor = iconContent,
                tint = colors.primary,
                size = 36.dp,
                iconSize = 18.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SynqviaTypography.CardTitleSmall, color = colors.textPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = SynqviaTypography.Caption, color = colors.textSecondary)
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
    val colors = SynqviaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = SynqviaTypography.Body, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = if (isMonospace) SynqviaType.Mono else SynqviaType.Headline,
                color = colors.textPrimary
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
    valueColor: Color = SynqviaTheme.colors.textSecondary,
    icon: ImageVector? = null,
    iconContainer: Color? = null,
    iconContent: Color? = null
) {
    val colors = SynqviaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconTile(
                icon = icon,
                containerColor = iconContainer,
                contentColor = iconContent,
                tint = colors.primary,
                size = 36.dp,
                iconSize = 18.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SynqviaTypography.CardTitleSmall, color = colors.textPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = SynqviaTypography.Caption, color = colors.textSecondary)
            }
        }
        if (value != null) {
            Text(text = value, style = SynqviaTypography.Caption.copy(color = valueColor))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ==========================================
// Previews for every component (Light & Dark)
// ==========================================

@SynqviaLightDarkPreview
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

@SynqviaLightDarkPreview
@Composable
fun PreviewSynqviaCard() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            SynqviaCard {
                Text(text = "Synqvia Card Title", style = SynqviaTypography.CardTitle, color = SynqviaTheme.colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Card with 16dp radius and 1dp outline border.", style = SynqviaTypography.Body, color = SynqviaTheme.colors.textSecondary)
            }
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewIconTile() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            IconTile(icon = Icons.Default.Bluetooth, tint = SynqviaTheme.colors.green)
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewCircleIconButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            CircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = {})
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewPrimaryButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            PrimaryButton(text = "Sync Now", icon = Icons.Default.Sync, onClick = {})
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewOutlinedCyanButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            OutlinedPrimaryButton(text = "Send Test", onClick = {})
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewSmallCyanButton() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            TonalButton(text = "Open", onClick = {})
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewSourcePill() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourcePill(isFromPc = true)
                SourcePill(isFromPc = false)
            }
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewStatusDot() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusDot(state = SynqviaStatusState.CONNECTED)
                StatusDot(state = SynqviaStatusState.CONNECTING)
                StatusDot(state = SynqviaStatusState.OFFLINE)
            }
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewSectionHeader() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop).padding(16.dp)) {
            SectionHeader(
                icon = Icons.Default.Sync,
                title = "Recent Activity",
                actionText = "View All →",
                onAction = {}
            )
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewSynqviaTopBar() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.bgTop)) {
            SynqviaTopBar(
                title = "History",
                onBack = {}
            )
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewRows() {
    val colors = SynqviaTheme.colors
    SynqviaTheme {
        Box(modifier = Modifier.background(colors.bgTop).padding(16.dp)) {
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
                    valueColor = colors.green,
                    onClick = {}
                )
            }
        }
    }
}


