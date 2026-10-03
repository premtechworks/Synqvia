package com.github.premtechworks.synqvia.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.ui.rememberCardGlassStyle
import com.github.premtechworks.synqvia.ui.rememberGlassCardBorderBrush
import com.github.premtechworks.synqvia.ui.components.SynqviaLightDarkPreview
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.LocalHazeState
import com.github.premtechworks.synqvia.ui.LocalIsBlurSupported
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.motion.softSpring
import com.github.premtechworks.synqvia.ui.softCardShadow
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.SynqviaTypography
import com.github.premtechworks.synqvia.ui.theme.synqviaCardShadow
import com.github.premtechworks.synqvia.ui.util.formatHistoryItemTime
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * In-composition overlay for viewing and interacting with a single clipboard history item.
 *
 * Placed at the very top of the root layout above the bottom bar and sticky headers.
 * Layers:
 * (1) Scrim: black 30% (78% on fallback)
 * (2) Full-screen blur effect: 0 -> 20dp blur with tint #050B18 @ 35%
 * (3) The card: frosted blur 32dp, tint #16233B @ 62%, noise 0.04, 24dp corners,
 *     1dp border with vertical gradient (white 22% -> 6%), soft shadow (0 24dp 48dp black 45%),
 *     20dp padding.
 *
 * Dismiss actions:
 * - Tap scrim
 * - Back press
 * - Drag down more than 96dp (card follows finger, scrim and blur fade proportionally, spring snap-back if not dismissed).
 */
@Composable
fun ClipDetailOverlay(
    clip: ClipEntity,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isBlurSupported = LocalIsBlurSupported.current
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalAppHaptics.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val maxBodyHeight = (configuration.screenHeightDp * 0.4f).dp
    val isFromPc = clip.isRemote

    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    val dragOffsetY = remember { Animatable(0f) }
    val enterAnim = remember { Animatable(0f) }

    LaunchedEffect(clip.id) {
        if (reduceMotion) {
            enterAnim.snapTo(1f)
        } else {
            enterAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
            )
        }
    }

    fun dismissWithAnimation() {
        coroutineScope.launch {
            if (reduceMotion) {
                onDismiss()
            } else {
                enterAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 150, easing = DecelerateEasing)
                )
                onDismiss()
            }
        }
    }

    BackHandler {
        dismissWithAnimation()
    }

    // Dismiss fraction during drag (0 to 1)
    val dragFraction = if (dismissThresholdPx > 0f) {
        (dragOffsetY.value / (dismissThresholdPx * 2f)).coerceIn(0f, 1f)
    } else 0f

    val effectiveProgress = (enterAnim.value * (1f - dragFraction)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val colors = SynqviaTheme.colors
        val isDark = SynqviaTheme.isDark

        // LAYER 1: Scrim (Tap to dismiss)
        val scrimBase = if (isDark) {
            Color.Black.copy(alpha = if (isBlurSupported) 0.30f else 0.78f)
        } else {
            colors.scrim
        }
        val scrimColor = scrimBase.copy(alpha = scrimBase.alpha * effectiveProgress)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWithAnimation() }
                )
        )

        // LAYER 2: Full-screen Blur Effect
        if (isBlurSupported && hazeState != null && effectiveProgress > 0.01f) {
            val blurRadiusDp = (20f * effectiveProgress).dp
            val blurTint = colors.glassTintSheet.copy(alpha = colors.glassTintSheet.alpha * effectiveProgress)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = blurRadiusDp,
                            tint = HazeTint(blurTint)
                        )
                    )
            )
        }

        // LAYER 3: The Card
        val cardShape = RoundedCornerShape(24.dp)
        val cardGlassStyle = rememberCardGlassStyle()
        val glassBorderBrush = rememberGlassCardBorderBrush()
        val cardModifier = if (isBlurSupported && hazeState != null) {
            Modifier
                .then(
                    if (isDark) {
                        Modifier.softCardShadow(
                            offsetY = 24.dp,
                            blurRadius = 48.dp,
                            color = Color.Black.copy(alpha = 0.45f * effectiveProgress),
                            cornerRadius = 24.dp
                        )
                    } else {
                        Modifier.synqviaCardShadow(elevation = 12.dp, shape = cardShape)
                    }
                )
                .clip(cardShape)
                .then(
                    if (isDark) {
                        Modifier.hazeEffect(
                            state = hazeState,
                            style = cardGlassStyle
                        )
                    } else {
                        Modifier.background(colors.surface)
                    }
                )
                .border(
                    1.dp,
                    if (isDark) glassBorderBrush else androidx.compose.ui.graphics.SolidColor(colors.outline),
                    cardShape
                )
        } else {
            Modifier
                .then(
                    if (!isDark) Modifier.synqviaCardShadow(elevation = 12.dp, shape = cardShape) else Modifier
                )
                .clip(cardShape)
                .background(if (isDark) colors.surfaceHigh else colors.surface)
                .border(1.dp, colors.outline, cardShape)
        }

        val cardTranslationY = if (reduceMotion) 0f else {
            dragOffsetY.value + (1f - enterAnim.value) * with(density) { 32.dp.toPx() }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    translationY = cardTranslationY
                    alpha = effectiveProgress
                }
                .pointerInput(clip.id, dismissThresholdPx) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY.value > dismissThresholdPx) {
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = dismissThresholdPx * 3f,
                                        animationSpec = tween(durationMillis = 150)
                                    )
                                    onDismiss()
                                }
                            } else {
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = softSpring()
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffsetY.animateTo(0f, softSpring())
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val newY = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                            coroutineScope.launch {
                                dragOffsetY.snapTo(newY)
                            }
                        }
                    )
                }
                .then(cardModifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume clicks inside card */ }
                )
                .padding(20.dp)
        ) {
            // Header Row: SourcePill, relative time, Close (X) button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourcePill(isFromPc = isFromPc)

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatHistoryItemTime(clip.ts),
                    style = SynqviaType.CaptionTnum,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.weight(1f))

                // Circular close button (36dp)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.outline, CircleShape)
                        .pressable(
                            targetScale = 0.9f,
                            onClick = { dismissWithAnimation() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDark) colors.textSecondary else Color(0xFF2B3A55),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val urlHost = remember(clip.text) { com.github.premtechworks.synqvia.ui.util.UrlUtil.extractHost(clip.text) }

            if (urlHost != null) {
                Text(
                    text = urlHost,
                    style = SynqviaType.CaptionSemiBold,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Body: nested inset panel (surfaceInset, 14dp radius, 14dp padding)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxBodyHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceInset)
                    .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                SelectionContainer {
                    val displayText = if (clip.sensitive) {
                        "•••••••• (Sensitive Content)"
                    } else if (clip.text.isEmpty()) {
                        "(empty clipboard)"
                    } else {
                        clip.text
                    }

                    val textColor = if (clip.text.isEmpty() || clip.sensitive) {
                        colors.textSecondary
                    } else {
                        colors.textPrimary
                    }

                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = displayText,
                            style = SynqviaTypography.Body,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info: character count
            Text(
                text = "${clip.text.length} chars",
                style = SynqviaType.CaptionTnum,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            var isCopied by remember { mutableStateOf(false) }
            val pinScale = remember { Animatable(1f) }

            // Action row
            if (isDark) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailActionItem(
                        icon = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        label = if (isCopied) "Copied" else "Copy",
                        tint = if (isCopied) colors.green else colors.textPrimary,
                        onClick = {
                            haptics.confirm()
                            onCopy(clip.text)
                            isCopied = true
                            coroutineScope.launch {
                                delay(1200)
                                isCopied = false
                                dismissWithAnimation()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DetailActionItem(
                        icon = Icons.Default.Share,
                        label = "Share",
                        tint = colors.textPrimary,
                        onClick = {
                            haptics.tick()
                            onShare(clip.text)
                            dismissWithAnimation()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (urlHost != null) {
                        DetailActionItem(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            label = "Open link",
                            tint = colors.primary,
                            onClick = {
                                haptics.tick()
                                val normalized = com.github.premtechworks.synqvia.ui.util.UrlUtil.normalizeUrl(clip.text)
                                if (normalized != null) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                dismissWithAnimation()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DetailActionItem(
                        icon = if (clip.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        label = if (clip.pinned) "Unpin" else "Pin",
                        tint = if (clip.pinned) colors.gold else colors.textPrimary,
                        iconScale = pinScale.value,
                        onClick = {
                            if (!clip.pinned) haptics.confirm() else haptics.tick()
                            onTogglePin(clip.id, !clip.pinned)
                            if (!reduceMotion) {
                                coroutineScope.launch {
                                    pinScale.snapTo(1f)
                                    if (!clip.pinned) {
                                        pinScale.animateTo(1.3f, tween(100, easing = DecelerateEasing))
                                        pinScale.animateTo(1f, softSpring())
                                    } else {
                                        pinScale.animateTo(0.85f, tween(100, easing = DecelerateEasing))
                                        pinScale.animateTo(1f, softSpring())
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DetailActionItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = colors.red,
                        onClick = {
                            haptics.reject()
                            onDelete(clip.id)
                            dismissWithAnimation()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceInset)
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LightDetailActionItem(
                            icon = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            label = if (isCopied) "Copied" else "Copy",
                            iconTint = if (isCopied) colors.green else Color(0xFF2B3A55),
                            labelColor = colors.textPrimary,
                            onClick = {
                                haptics.confirm()
                                onCopy(clip.text)
                                isCopied = true
                                coroutineScope.launch {
                                    delay(1200)
                                    isCopied = false
                                    dismissWithAnimation()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(colors.divider)
                        )

                        LightDetailActionItem(
                            icon = Icons.Default.Share,
                            label = "Share",
                            iconTint = Color(0xFF2B3A55),
                            labelColor = colors.textPrimary,
                            onClick = {
                                haptics.tick()
                                onShare(clip.text)
                                dismissWithAnimation()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (urlHost != null) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(colors.divider)
                            )

                            LightDetailActionItem(
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                label = "Open link",
                                iconTint = colors.primary,
                                labelColor = colors.textPrimary,
                                onClick = {
                                    haptics.tick()
                                    val normalized = com.github.premtechworks.synqvia.ui.util.UrlUtil.normalizeUrl(clip.text)
                                    if (normalized != null) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                    dismissWithAnimation()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(colors.divider)
                        )

                        LightDetailActionItem(
                            icon = if (clip.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            label = if (clip.pinned) "Unpin" else "Pin",
                            iconTint = if (clip.pinned) colors.gold else Color(0xFF2B3A55),
                            labelColor = colors.textPrimary,
                            iconScale = pinScale.value,
                            onClick = {
                                if (!clip.pinned) haptics.confirm() else haptics.tick()
                                onTogglePin(clip.id, !clip.pinned)
                                if (!reduceMotion) {
                                    coroutineScope.launch {
                                        pinScale.snapTo(1f)
                                        if (!clip.pinned) {
                                            pinScale.animateTo(1.3f, tween(100, easing = DecelerateEasing))
                                            pinScale.animateTo(1f, softSpring())
                                        } else {
                                            pinScale.animateTo(0.85f, tween(100, easing = DecelerateEasing))
                                            pinScale.animateTo(1f, softSpring())
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(colors.divider)
                        )

                        LightDetailActionItem(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            iconTint = colors.red,
                            labelColor = colors.redText,
                            onClick = {
                                haptics.reject()
                                onDelete(clip.id)
                                dismissWithAnimation()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility wrapper for ClipDetailSheet
 */
@Composable
fun ClipDetailSheet(
    clip: ClipEntity,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ClipDetailOverlay(
        clip = clip,
        onDismiss = onDismiss,
        onCopy = onCopy,
        onShare = onShare,
        onTogglePin = onTogglePin,
        onDelete = onDelete,
        modifier = modifier
    )
}

@Composable
private fun DetailActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pressable(
                targetScale = 0.94f,
                showOverlay = true,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SynqviaTheme.colors.surfaceHigh)
                .border(1.dp, SynqviaTheme.colors.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "actionIcon"
            ) { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = SynqviaType.ButtonSmall.copy(color = tint),
            color = tint
        )
    }
}

@Composable
private fun LightDetailActionItem(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pressable(
                targetScale = 0.94f,
                showOverlay = true,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "lightActionIcon"
        ) { targetIcon ->
            Icon(
                imageVector = targetIcon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = SynqviaType.CaptionSemiBold.copy(
                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = labelColor
            ),
            color = labelColor,
            maxLines = 1
        )
    }
}

@SynqviaLightDarkPreview
@Composable
private fun ClipDetailSheetPreview() {
    SynqviaTheme {
        ClipDetailOverlay(
            clip = ClipEntity(
                id = "1",
                text = "build the debug app and install it on connected device.",
                ts = System.currentTimeMillis() - 22 * 60 * 1000,
                src = "linux-prem-pc",
                direction = "remote",
                pinned = true
            ),
            onDismiss = {},
            onCopy = {},
            onShare = {},
            onTogglePin = { _, _ -> },
            onDelete = {}
        )
    }
}
