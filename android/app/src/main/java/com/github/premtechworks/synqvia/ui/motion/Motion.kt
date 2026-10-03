package com.github.premtechworks.synqvia.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Standard motion duration tokens (in ms).
 */
object MotionDuration {
    const val Fast = 120
    const val Standard = 200
    const val Medium = 300
    const val Slow = 450
}

/**
 * Custom CubicBezier motion curves matching iOS-level taste.
 */
val DecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Springs tuned for premium physical responsiveness.
 * snappy: damping 0.8, stiffness 700 (presses, switches, tight controls)
 * soft: damping 0.75, stiffness 300 (navigation pills, cards, floating sheets)
 * bouncy: damping 0.6, stiffness 400 (icons, checkmarks, badges)
 */
fun <T> snappySpring() = spring<T>(dampingRatio = 0.8f, stiffness = 700f)
fun <T> softSpring() = spring<T>(dampingRatio = 0.75f, stiffness = 300f)
fun <T> bouncySpring() = spring<T>(dampingRatio = 0.6f, stiffness = 400f)

val SnappySpringFloat = spring<Float>(dampingRatio = 0.8f, stiffness = 700f)
val SoftSpringFloat = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)
val BouncySpringFloat = spring<Float>(dampingRatio = 0.6f, stiffness = 400f)

/**
 * CompositionLocal indicating whether animations should be reduced.
 * True when system animator duration scale is 0 or user has enabled reduce motion.
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Checks system ANIMATOR_DURATION_SCALE to determine if animations are globally disabled.
 */
fun isSystemReduceMotionEnabled(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        scale == 0f
    } catch (_: Exception) {
        false
    }
}

/**
 * Modifier.pressable:
 * Scale effect using the snappy spring (scale 0.97 for cards/rows, 0.96 for buttons, 0.9 for icon buttons).
 * Plus optional 8% white overlay on cards and rows; no ripple on cards.
 * When LocalReduceMotion is active: uses scale 1.0 (no scale or translate transforms).
 */
fun Modifier.pressable(
    targetScale: Float = 0.97f,
    showOverlay: Boolean = false,
    overlayColor: Color? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val internalSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by internalSource.collectIsPressedAsState()
    val isDark = SynqviaTheme.isDark
    val effectiveOverlayColor = overlayColor ?: if (isDark) Color(0x14FFFFFF) else Color(0x0F0B1B33)

    val scale by animateFloatAsState(
        targetValue = if (!reduceMotion && enabled && isPressed) targetScale else 1f,
        animationSpec = SnappySpringFloat,
        label = "pressable_scale"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (enabled && isPressed && showOverlay) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "pressable_overlay"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = internalSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier.pointerInput(enabled, internalSource) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val press = androidx.compose.foundation.interaction.PressInteraction.Press(androidx.compose.ui.geometry.Offset.Zero)
                        internalSource.tryEmit(press)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            internalSource.tryEmit(androidx.compose.foundation.interaction.PressInteraction.Release(press))
                        } else {
                            internalSource.tryEmit(androidx.compose.foundation.interaction.PressInteraction.Cancel(press))
                        }
                    }
                }
            }
        )
        .drawWithContent {
            drawContent()
            if (overlayAlpha > 0f) {
                drawRect(color = effectiveOverlayColor.copy(alpha = effectiveOverlayColor.alpha * overlayAlpha))
            }
        }
}

/**
 * Entry-stagger tokens: 40ms between consecutive items, first 8 items only, 300ms decelerate each.
 */
object MotionStagger {
    /** Delay between consecutive items. */
    const val StepDelayMs = 40L

    /** Items beyond this index do not stagger. */
    const val MaxItems = 8

    /** Per-item animation duration. */
    const val ItemDurationMs = 300

    /** Longest possible full stagger: last item's delay + its animation. */
    const val TotalMs = (MaxItems - 1) * StepDelayMs + ItemDurationMs
}

/**
 * True while the screen's entrance stagger is still allowed to play. Provided by [StaggerHost].
 */
val LocalStaggerEnabled = compositionLocalOf { true }

/**
 * Hosts one screen's entrance stagger. Wraps the screen content and disables [LocalStaggerEnabled]
 * once [MotionStagger.TotalMs] has elapsed, so items composed later (or recycled by a LazyColumn on
 * scroll) render immediately instead of replaying the entrance.
 */
@Composable
fun StaggerHost(content: @Composable () -> Unit) {
    var enabled by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(MotionStagger.TotalMs)
        enabled = false
    }
    CompositionLocalProvider(LocalStaggerEnabled provides enabled, content = content)
}

/**
 * Entry stagger on first display of a screen:
 * Cards and rows fade + translateY 12dp -> 0, 40ms stagger for the first 8 items, 300ms decelerate.
 * Once per visit, not on scroll.
 *
 * The stagger is gated by [LocalStaggerEnabled], which [StaggerHost] flips to false once the
 * full stagger has played. Without that gate a recycled LazyColumn item scrolled back into view
 * would restart its `Animatable` from 0 and replay the whole entrance.
 */
fun Modifier.entryStagger(
    index: Int,
    trigger: Boolean = true
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current
    val staggerEnabled = LocalStaggerEnabled.current

    // Already played, out of range, or explicitly suppressed: render settled, no animation.
    if (index >= MotionStagger.MaxItems || !trigger || !staggerEnabled) {
        return@composed this
    }

    val anim = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            delay(index * MotionStagger.StepDelayMs)
            if (reduceMotion) {
                anim.animateTo(1f, tween(MotionDuration.Fast))
            } else {
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = MotionStagger.ItemDurationMs,
                        easing = DecelerateEasing
                    )
                )
            }
        }
    }

    // With reduce motion we still want the plain 120ms fade, so alpha follows the animation
    // while only the translate is suppressed.
    this.graphicsLayer {
        alpha = anim.value
        if (!reduceMotion) {
            translationY = with(density) { 12.dp.toPx() } * (1f - anim.value)
        }
    }
}

/**
 * True while the hosting lifecycle is at least STARTED (i.e. the screen is actually on-screen).
 * Infinite animations should be gated on this so they stop driving frames when the app is
 * backgrounded.
 */
@Composable
fun rememberIsScreenVisible(): Boolean {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val isVisible by rememberUpdatedState(
        lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    )
    var visible by remember { mutableStateOf(isVisible) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            visible = lifecycleOwner.lifecycle.currentState.isAtLeast(
                androidx.lifecycle.Lifecycle.State.STARTED
            )
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        visible = lifecycleOwner.lifecycle.currentState.isAtLeast(
            androidx.lifecycle.Lifecycle.State.STARTED
        )
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return visible
}

/**
 * Rolling Number display:
 * Stat tiles and chip counts roll to the new value (AnimatedContent, per-digit vertical slide, 400ms, tabular figures).
 */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val textStyle = style.copy(
        color = color,
        fontFeatureSettings = "tnum"
    )

    if (reduceMotion) {
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
            label = "rolling_number_reduced",
            modifier = modifier
        ) { target ->
            Text(text = target.toString(), style = textStyle)
        }
    } else {
        Row(modifier = modifier) {
            val digits = value.toString().toList()
            digits.forEachIndexed { index, char ->
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val slideDirection = if (targetState > initialState) 1 else -1
                        val enter = slideInVertically(
                            animationSpec = tween(400, easing = DecelerateEasing)
                        ) { it * slideDirection } + fadeIn(tween(400))
                        val exit = slideOutVertically(
                            animationSpec = tween(400, easing = DecelerateEasing)
                        ) { -it * slideDirection } + fadeOut(tween(400))
                        enter togetherWith exit
                    },
                    label = "digit_$index"
                ) { digit ->
                    Text(text = digit.toString(), style = textStyle)
                }
            }
        }
    }
}

/**
 * Shimmering placeholder cards: surface with a moving highlight, 1.2s loop.
 * Dark: 8% white sweep; Light: white @ 60% sweep on #EAF0FA.
 * Disabled when LocalReduceMotion is enabled.
 */
fun Modifier.shimmerHighlight(
    baseColor: Color? = null,
    sweepColor: Color? = null
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val isDark = SynqviaTheme.isDark
    val effectiveBaseColor = baseColor ?: if (isDark) Color.Transparent else Color(0xFFEAF0FA)
    val effectiveSweepColor = sweepColor ?: if (isDark) Color(0x14FFFFFF) else Color(0x99FFFFFF)

    if (reduceMotion) {
        return@composed if (effectiveBaseColor != Color.Transparent) {
            this.background(effectiveBaseColor)
        } else this
    }

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    drawWithContent {
        if (effectiveBaseColor != Color.Transparent) {
            drawRect(color = effectiveBaseColor)
        }
        drawContent()
        val width = size.width
        val height = size.height
        if (width > 0f) {
            val startX = width * translateAnim
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    effectiveSweepColor,
                    Color.Transparent
                ),
                start = Offset(startX - width * 0.5f, 0f),
                end = Offset(startX + width * 0.5f, height)
            )
            drawRect(brush = brush)
        }
    }
}
