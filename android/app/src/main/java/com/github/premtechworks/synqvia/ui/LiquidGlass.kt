package com.github.premtechworks.synqvia.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.ui.theme.CyanGlow
import com.github.premtechworks.synqvia.ui.theme.CyanPrimary
import com.github.premtechworks.synqvia.ui.theme.GlassBorder
import com.github.premtechworks.synqvia.ui.theme.GlassBorderSubtle
import com.github.premtechworks.synqvia.ui.theme.GlassSurface
import com.github.premtechworks.synqvia.ui.theme.GlassSurfaceElevated
import com.github.premtechworks.synqvia.ui.theme.StatusConnected
import com.github.premtechworks.synqvia.ui.theme.StatusConnectedGlow
import com.github.premtechworks.synqvia.ui.theme.StatusOffline
import com.github.premtechworks.synqvia.ui.theme.StatusOfflineGlow
import com.github.premtechworks.synqvia.ui.theme.StatusRetrying
import com.github.premtechworks.synqvia.ui.theme.StatusRetryingGlow

/**
 * Liquid Glass Card container with frosted sheen, soft border glow, and depth shadow
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = CyanGlow,
                spotColor = Color.Black
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = (backgroundColor.alpha * 0.7f).coerceAtLeast(0.05f))
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        GlassBorderSubtle,
                        borderColor.copy(alpha = 0.15f)
                    )
                ),
                shape = shape
            ),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(content = content)
    }
}

/**
 * Pulsing animated liquid glow ring for status indicators
 */
@Composable
fun GlowingStatusDot(
    statusColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing wave
        Box(
            modifier = Modifier
                .size(size * pulseScale)
                .clip(CircleShape)
                .background(glowColor.copy(alpha = pulseAlpha))
        )
        // Static aura
        Box(
            modifier = Modifier
                .size(size * 1.35f)
                .clip(CircleShape)
                .background(glowColor)
        )
        // Core glowing dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(statusColor)
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )
    }
}

/**
 * Glass pill badge for chip labels and counters
 */
@Composable
fun GlassPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = CyanPrimary,
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
