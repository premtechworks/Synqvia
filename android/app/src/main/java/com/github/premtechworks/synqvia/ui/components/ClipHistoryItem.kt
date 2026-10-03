package com.github.premtechworks.synqvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.ui.components.SynqviaLightDarkPreview
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import com.github.premtechworks.synqvia.ui.rememberCardGlassStyle
import com.github.premtechworks.synqvia.ui.rememberGlassCardBorderBrush
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.LocalHazeState
import com.github.premtechworks.synqvia.ui.LocalIsBlurSupported
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.motion.snappySpring
import com.github.premtechworks.synqvia.ui.motion.softSpring
import com.github.premtechworks.synqvia.ui.synqviaGlass
import com.github.premtechworks.synqvia.ui.theme.synqviaCardShadow
import com.github.premtechworks.synqvia.ui.util.formatHistoryItemTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.filled.Delete
import kotlin.math.abs

import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.SynqviaTypography

/**
 * Redesigned History Row:
 * - Padding 14dp horizontal, 12dp vertical
 * - Top line: SourcePill + relative time + right-aligned Pin (20dp, gold when pinned) and three-dot (20dp) with 36dp touch targets
 * - Body: 14sp textPrimary, sans-serif, max 2 lines, ellipsized
 * - Footer: "${clip.text.length} chars" in 12sp textSecondary
 */
@Composable
fun ClipHistoryItem(
    clip: ClipEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onResend: () -> Unit,
    onDelete: () -> Unit,
    searchQuery: String = "",
    precomputedTime: String? = null,
    precomputedHost: String? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    val hazeState = LocalHazeState.current
    val isBlurSupported = LocalIsBlurSupported.current
    val haptics = LocalAppHaptics.current
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()

    val pinScale = remember { Animatable(1f) }
    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark
    val pinColor by animateColorAsState(
        targetValue = if (clip.pinned) colors.gold else colors.textTertiary,
        animationSpec = tween(durationMillis = 200),
        label = "pinColor"
    )

    val offsetX = remember(clip.id) { Animatable(0f) }
    var rowWidthPx by remember(clip.id) { mutableFloatStateOf(0f) }
    var crossedThreshold by remember(clip.id) { mutableStateOf(false) }

    val threshold = if (rowWidthPx > 0f) rowWidthPx * 0.45f else 1f
    val currentOffset = offsetX.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width.toFloat() }
            .pointerInput(clip.id, rowWidthPx) {
                if (rowWidthPx <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { crossedThreshold = false },
                    onDragEnd = {
                        coroutineScope.launch {
                            val current = offsetX.value
                            val t = rowWidthPx * 0.45f
                            if (current < -t) {
                                // Full swipe LEFT past 45% of the width -> delete
                                if (!reduceMotion) {
                                    offsetX.animateTo(-rowWidthPx, tween(120, easing = DecelerateEasing))
                                }
                                onDelete()
                            } else if (current > t) {
                                // Full swipe RIGHT past 45% -> toggle pin and spring back
                                haptics.confirm()
                                onTogglePin()
                                offsetX.animateTo(0f, snappySpring())
                            } else {
                                // Spring snap-back when released below threshold
                                offsetX.animateTo(0f, snappySpring())
                            }
                            crossedThreshold = false
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, snappySpring())
                            crossedThreshold = false
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val current = offsetX.value
                        val t = rowWidthPx * 0.45f
                        val newTarget = current + dragAmount
                        // Rubber-band resistance past 45% threshold
                        val damped = if (newTarget > t) {
                            t + (newTarget - t) * 0.35f
                        } else if (newTarget < -t) {
                            -t + (newTarget + t) * 0.35f
                        } else {
                            newTarget
                        }

                        val isOver = abs(damped) >= t
                        if (isOver && !crossedThreshold) {
                            haptics.tick()
                            crossedThreshold = true
                        } else if (!isOver && crossedThreshold) {
                            crossedThreshold = false
                        }

                        coroutineScope.launch {
                            offsetX.snapTo(damped)
                        }
                    }
                )
            }
    ) {
        // BACKGROUND ACTION ZONE (revealed on swipe)
        if (currentOffset != 0f) {
            val isLeftSwipe = currentOffset < 0
            val zoneColor = if (isDark) {
                if (isLeftSwipe) colors.red.copy(alpha = 0.22f) else colors.gold.copy(alpha = 0.22f)
            } else {
                if (isLeftSwipe) colors.red else colors.gold
            }
            val deleteIconTint = if (isDark) colors.red else Color.White
            val pinIconTint = if (isDark) colors.gold else Color(0xFF0B1B33)
            val iconAlignment = if (isLeftSwipe) Alignment.CenterEnd else Alignment.CenterStart
            val progress = (abs(currentOffset) / threshold).coerceIn(0f, 1.2f)

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(zoneColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = iconAlignment
            ) {
                if (isLeftSwipe) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = deleteIconTint,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = progress
                                scaleY = progress
                            }
                    )
                } else {
                    Icon(
                        imageVector = if (clip.pinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                        contentDescription = if (clip.pinned) "Unpin" else "Pin",
                        tint = pinIconTint,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = progress
                                scaleY = progress
                            }
                    )
                }
            }
        }

        // FOREGROUND CONTENT ROW
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = currentOffset
                }
                .background(colors.surface)
                .pressable(
                    targetScale = 0.97f,
                    showOverlay = true,
                    onClick = {
                        if (abs(offsetX.value) < 10f) {
                            onClick()
                        }
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { contentDescription = "Clip item: ${clip.text.take(30)}" }
        ) {
        // Top line: SourcePill + optional Conflict Pill + relative time + Pin + MoreVert
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourcePill(isFromPc = clip.isRemote)

            if (clip.conflictLoser) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.amberContainer)
                        .border(1.dp, colors.amber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Conflict",
                        style = SynqviaType.Overline.copy(
                            color = colors.amber
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = precomputedTime ?: formatHistoryItemTime(clip.ts),
                style = SynqviaType.CaptionTnum,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Pin button with 48dp touch target (visual icon 20dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .pressable(targetScale = 0.9f) {
                        if (clip.pinned) {
                            haptics.tick()
                        } else {
                            haptics.confirm()
                        }
                        onTogglePin()
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
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (clip.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (clip.pinned) "Unpin clip" else "Pin clip",
                    tint = pinColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = pinScale.value
                            scaleY = pinScale.value
                        }
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Vertical 3-dot menu button with 48dp touch target (visual icon 20dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .pressable(targetScale = 0.9f) {
                        haptics.tick()
                        menuExpanded = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More clip actions",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .then(
                            if (!isDark) Modifier.synqviaCardShadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp)) else Modifier
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .synqviaGlass(
                            hazeState = hazeState,
                            style = rememberCardGlassStyle(),
                            isBlurSupported = isBlurSupported,
                            fallbackColor = colors.surfaceHigh,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, rememberGlassCardBorderBrush(), RoundedCornerShape(16.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isCopied) "Copied" else "Copy",
                                color = if (isCopied) colors.green else colors.textPrimary,
                                style = SynqviaTypography.Body
                            )
                        },
                        leadingIcon = {
                            AnimatedContent(
                                targetState = isCopied,
                                transitionSpec = {
                                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                                },
                                label = "dropdownCopyIcon"
                            ) { copied ->
                                if (copied) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.green,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            haptics.confirm()
                            onCopy()
                            isCopied = true
                            coroutineScope.launch {
                                delay(1200)
                                isCopied = false
                                menuExpanded = false
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = colors.textPrimary, style = SynqviaTypography.Body) },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            haptics.tick()
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Resend to PC", color = colors.textPrimary, style = SynqviaTypography.Body) },
                        leadingIcon = {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            haptics.tick()
                            menuExpanded = false
                            onResend()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (clip.pinned) "Unpin" else "Pin",
                                color = if (clip.pinned) colors.gold else colors.textPrimary,
                                style = SynqviaTypography.Body
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (clip.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = if (clip.pinned) colors.gold else colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            if (clip.pinned) haptics.tick() else haptics.confirm()
                            menuExpanded = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = colors.red, style = SynqviaTypography.Body) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colors.red, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            haptics.reject()
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // If a clip is a URL, show its host in cyan (12sp SemiBold) above the text
        val urlHost = precomputedHost ?: remember(clip.text) { com.github.premtechworks.synqvia.ui.util.UrlUtil.extractHost(clip.text) }
        if (urlHost != null) {
            Text(
                text = urlHost,
                style = SynqviaType.CaptionSemiBold,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Body: 14sp textPrimary, sans-serif, max 2 lines, ellipsized with search match highlight
        if (clip.sensitive) {
            Text(
                text = "•••••••• (Sensitive Content)",
                style = SynqviaTypography.Body,
                color = colors.textSecondary,
                maxLines = 1
            )
        } else if (clip.text.isEmpty()) {
            Text(
                text = "(empty clipboard)",
                style = SynqviaTypography.Body,
                color = colors.textSecondary,
                maxLines = 1
            )
        } else {
            val highlightedText = remember(clip.text, searchQuery) {
                if (searchQuery.isBlank()) {
                    androidx.compose.ui.text.AnnotatedString(clip.text)
                } else {
                    val builder = androidx.compose.ui.text.AnnotatedString.Builder(clip.text)
                    val lowerText = clip.text.lowercase(java.util.Locale.ROOT)
                    val lowerQuery = searchQuery.lowercase(java.util.Locale.ROOT)
                    var startIndex = 0
                    while (startIndex < clip.text.length) {
                        val index = lowerText.indexOf(lowerQuery, startIndex)
                        if (index == -1) break
                        builder.addStyle(
                            androidx.compose.ui.text.SpanStyle(background = colors.primary.copy(alpha = if (isDark) 0.18f else 0.14f)),
                            index,
                            index + lowerQuery.length
                        )
                        startIndex = index + lowerQuery.length
                    }
                    builder.toAnnotatedString()
                }
            }

            Text(
                text = highlightedText,
                style = SynqviaTypography.Body,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Footer: "${clip.text.length} chars" in 12sp textSecondary
        Text(
            text = "${clip.text.length} chars",
            style = SynqviaType.CaptionTnum,
            color = colors.textSecondary
        )
    }
}
}

/**
 * Backwards compatibility overload
 */
@Composable
fun ClipHistoryItem(
    clip: ClipEntity,
    onCopyAndResend: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ClipHistoryItem(
        clip = clip,
        onClick = onClick,
        onTogglePin = onTogglePin,
        onCopy = onCopyAndResend,
        onShare = onShare,
        onResend = onCopyAndResend,
        onDelete = onDelete,
        modifier = modifier
    )
}

// ==========================================
// Previews
// ==========================================

@SynqviaLightDarkPreview
@Composable
fun PreviewClipHistoryItemPinned() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.surface).padding(8.dp)) {
            ClipHistoryItem(
                clip = ClipEntity(
                    id = "p1",
                    text = "build the debug app and install it on connected device.",
                    ts = System.currentTimeMillis() - 22 * 60 * 1000,
                    src = "prem-pc",
                    direction = "remote",
                    pinned = true
                ),
                onClick = {},
                onTogglePin = {},
                onCopy = {},
                onShare = {},
                onResend = {},
                onDelete = {}
            )
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewClipHistoryItemEmpty() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.surface).padding(8.dp)) {
            ClipHistoryItem(
                clip = ClipEntity(
                    id = "p2",
                    text = "",
                    ts = System.currentTimeMillis() - 59 * 60 * 1000,
                    src = "android",
                    direction = "local",
                    pinned = false
                ),
                onClick = {},
                onTogglePin = {},
                onCopy = {},
                onShare = {},
                onResend = {},
                onDelete = {}
            )
        }
    }
}

@SynqviaLightDarkPreview
@Composable
fun PreviewClipHistoryItemSensitive() {
    SynqviaTheme {
        Box(modifier = Modifier.background(SynqviaTheme.colors.surface).padding(8.dp)) {
            ClipHistoryItem(
                clip = ClipEntity(
                    id = "p3",
                    text = "secret_password_123",
                    ts = System.currentTimeMillis() - 1000,
                    src = "android",
                    direction = "local",
                    sensitive = true
                ),
                onClick = {},
                onTogglePin = {},
                onCopy = {},
                onShare = {},
                onResend = {},
                onDelete = {}
            )
        }
    }
}
