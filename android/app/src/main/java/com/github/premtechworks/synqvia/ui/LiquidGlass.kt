package com.github.premtechworks.synqvia.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.ui.theme.SurfaceHigh
import com.github.premtechworks.synqvia.ui.theme.SurfaceInset
import com.github.premtechworks.synqvia.ui.theme.OutlineDark
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SurfaceDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Composition locals for Haze frosted glass hierarchy.
 */
val LocalHazeState: ProvidableCompositionLocal<HazeState?> = compositionLocalOf { null }
val LocalIsBlurSupported: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

/**
 * Checks if frosted blur effects can run within budget.
 * Supported on API 31+ with hardware RenderNode blur, and disabled on low-RAM devices.
 */
fun isBlurSupported(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    if (am != null && am.isLowRamDevice) return false
    return true
}

/**
 * Glass material styles:
 * - Bottom Bar: blur 24dp, tint #0B1426 @ 72%
 * - Sticky Header: blur 24dp, tint #0B1426 @ 72%
 * - Card: blur 32dp, tint #16233B @ 62%, noise 0.04
 */
val BottomBarGlassStyle = HazeStyle(
    blurRadius = 24.dp,
    tint = HazeTint(SurfaceInset.copy(alpha = 0.72f)),
    fallbackTint = HazeTint(SurfaceInset)
)

val HeaderGlassStyle = HazeStyle(
    blurRadius = 24.dp,
    tint = HazeTint(SurfaceInset.copy(alpha = 0.72f)),
    fallbackTint = HazeTint(SurfaceInset)
)

val CardGlassStyle = HazeStyle(
    blurRadius = 32.dp,
    tint = HazeTint(SurfaceHigh.copy(alpha = 0.62f)),
    noiseFactor = 0.04f,
    fallbackTint = HazeTint(SurfaceHigh)
)

@Composable
fun rememberBottomBarGlassStyle(): HazeStyle {
    val colors = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors
    return if (colors.isDark) {
        BottomBarGlassStyle
    } else {
        HazeStyle(
            blurRadius = 24.dp,
            tint = HazeTint(colors.glassTintBar),
            fallbackTint = HazeTint(colors.navBarBg)
        )
    }
}

@Composable
fun rememberHeaderGlassStyle(): HazeStyle {
    val colors = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors
    return if (colors.isDark) {
        HeaderGlassStyle
    } else {
        HazeStyle(
            blurRadius = 16.dp,
            tint = HazeTint(colors.bgTop.copy(alpha = 0.85f)),
            fallbackTint = HazeTint(colors.bgTop)
        )
    }
}

@Composable
fun rememberCardGlassStyle(): HazeStyle {
    val colors = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors
    return if (colors.isDark) {
        CardGlassStyle
    } else {
        HazeStyle(
            blurRadius = 20.dp,
            tint = HazeTint(colors.surface.copy(alpha = 0.94f)),
            fallbackTint = HazeTint(colors.surface)
        )
    }
}

/**
 * Vertical 1dp border gradient for frosted glass cards (white 22% -> 6%).
 */
val GlassCardBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.22f),
        Color.White.copy(alpha = 0.06f)
    )
)

@Composable
fun rememberGlassCardBorderBrush(): Brush {
    val isDark = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.isDark
    return if (isDark) {
        GlassCardBorderBrush
    } else {
        androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.80f))
    }
}

/**
 * Soft drop shadow matching CSS: `0 24dp 48dp rgba(0, 0, 0, 0.45)`
 */
fun Modifier.softCardShadow(
    offsetY: Dp = 24.dp,
    blurRadius: Dp = 48.dp,
    color: Color = Color.Black.copy(alpha = 0.45f),
    cornerRadius: Dp = 24.dp
): Modifier = this.drawBehind {
    if (color.alpha <= 0f) return@drawBehind
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                blurRadius.toPx(),
                0f,
                offsetY.toPx(),
                color.toArgb()
            )
        }
        val r = cornerRadius.toPx()
        canvas.nativeCanvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            r, r,
            paint
        )
    }
}

/**
 * Modifier to apply glass frosted blur if supported, or solid fallback color if not.
 */
fun Modifier.synqviaGlass(
    hazeState: HazeState?,
    style: HazeStyle,
    isBlurSupported: Boolean,
    fallbackColor: Color = SurfaceHigh,
    shape: Shape? = null
): Modifier {
    val clipped = if (shape != null) this.clip(shape) else this
    return if (isBlurSupported && hazeState != null) {
        clipped.hazeEffect(state = hazeState, style = style)
    } else {
        clipped.background(fallbackColor)
    }
}

/**
 * Card container with flat dark-navy surface, 16dp rounded corners, and thin outline border.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors.surface,
    borderColor: Color = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors.outline,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        color = Color.Transparent,
        contentColor = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors.textPrimary
    ) {
        Box(content = content)
    }
}

/**
 * Clean status indicator dot with a soft subtle aura.
 */
@Composable
fun GlowingStatusDot(
    statusColor: Color,
    glowColor: Color = statusColor.copy(alpha = 0.25f),
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Box(
        modifier = modifier.size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 1.5f)
                .clip(CircleShape)
                .background(glowColor)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}

/**
 * Pill badge for chip labels and counters with 14% container tint and 35% border.
 */
@Composable
fun GlassPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors.primary,
    textColor: Color = com.github.premtechworks.synqvia.ui.theme.SynqviaTheme.colors.textPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
