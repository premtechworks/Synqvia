package com.github.premtechworks.synqvia.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.IconTile
import com.github.premtechworks.synqvia.ui.components.PrimaryButton
import com.github.premtechworks.synqvia.ui.components.SynqviaBrandLockup
import com.github.premtechworks.synqvia.ui.components.SynqviaScreen
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.motion.snappySpring
import com.github.premtechworks.synqvia.ui.motion.softSpring
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import kotlin.math.absoluteValue

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onNavigateToPair: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingContent(
        onGetStarted = {
            viewModel.setOnboardingDone(true)
            onNavigateToPair()
        },
        modifier = modifier
    )
}

@Composable
fun OnboardingContent(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageCount = 5
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    SynqviaScreen(
        applyStatusBarInsets = true,
        bottomPadding = 0.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("onboarding_pager")
            ) { page ->
                val reduceMotion = LocalReduceMotion.current
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (reduceMotion) 1f else (1f - pageOffset.absoluteValue * 0.7f).coerceIn(0f, 1f)
                            if (!reduceMotion) {
                                translationX = pageOffset * 32.dp.toPx()
                            }
                        }
                ) {
                    when (page) {
                        0 -> OnboardingHeroPage(pageOffset = pageOffset)
                        1 -> OnboardingTemplatePage(
                            icon = Icons.Default.Bluetooth,
                            title = "Instant RFCOMM Bluetooth",
                            description = "Direct peer-to-peer sync with zero cloud servers. Fast, local, private."
                        )
                        2 -> OnboardingTemplatePage(
                            icon = Icons.Default.Bolt,
                            title = "Seamless 4-Way Capture",
                            description = "Works from selection menu, tile, background service, or clipboard keyboard."
                        )
                        3 -> OnboardingTemplatePage(
                            icon = Icons.Default.Security,
                            title = "End-to-End Local Security",
                            description = "Your clipboard never leaves your local Bluetooth range. 100% telemetry-free."
                        )
                        else -> OnboardingTemplatePage(
                            icon = Icons.Default.Check,
                            title = "Ready to Pair",
                            description = "Turn on Bluetooth on your Linux PC running the Synqvia daemon to connect."
                        )
                    }
                }
            }

            // Pinned Bottom Area: Dot Indicators + 52dp Get Started Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val colors = SynqviaTheme.colors
                // 5 Dot Indicators: active is 20dp pill morphing with spring
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 20.dp else 6.dp,
                            animationSpec = softSpring(),
                            label = "dot_width_$index"
                        )
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primary else if (colors.isDark) colors.outline else Color(0xFFC5D2E6))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val btnInteractionSource = remember { MutableInteractionSource() }
                val isBtnPressed by btnInteractionSource.collectIsPressedAsState()
                val reduceMotion = LocalReduceMotion.current
                val arrowOffset by animateDpAsState(
                    targetValue = if (!reduceMotion && isBtnPressed) 4.dp else 0.dp,
                    animationSpec = snappySpring(),
                    label = "arrow_offset"
                )

                Button(
                    onClick = onGetStarted,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    interactionSource = btnInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .pressable(targetScale = 0.96f, interactionSource = btnInteractionSource)
                        .testTag("onboarding_get_started_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            style = SynqviaType.Button.copy(color = colors.onPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "→",
                            style = SynqviaType.Button.copy(color = colors.onPrimary),
                            modifier = Modifier.offset(x = arrowOffset)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mockup Page 1: Hero onboarding page with animated Bluetooth connection diagram and feature chips.
 */
@Composable
private fun OnboardingHeroPage(pageOffset: Float = 0f) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "pulse_transition")
    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    val glowTransition = rememberInfiniteTransition(label = "glow_transition")
    val glowPulse by glowTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val caretTransition = rememberInfiniteTransition(label = "caret_blink")
    val caretAlpha by caretTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val colors = SynqviaTheme.colors

        // Header block at about 12% from the top
        Spacer(modifier = Modifier.weight(0.12f))

        // Synqvia Logo + Name + Tagline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SynqviaBrandLockup(
                markSize = 36.dp,
                wordmarkHeight = 22.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Clipboard sync between Android and Linux.",
                style = SynqviaType.Callout,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }

        // Illustration and chips block vertically centered in the remaining space
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Illustration: Phone -> Pulsing Dotted Line -> Bluetooth -> Pulsing Dotted Line -> Laptop (~160dp tall)
            Row(
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (!reduceMotion) {
                            // Parallax: the illustration moves at 0.5x
                            translationX = pageOffset * 0.5f * 100.dp.toPx()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Phone outline icon (44x72dp, 2dp stroke, green Android head inside)
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            2.dp,
                            if (colors.isDark) colors.primary.copy(alpha = 0.8f) else Color(0xFF0B1B33),
                            RoundedCornerShape(8.dp)
                        )
                        .background(colors.surfaceInset),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = Color(0xFF3DDC84),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Dotted line with 6 dots and fade pulse
                PulsingDottedLine(pulsePhase = pulsePhase)

                Spacer(modifier = Modifier.width(6.dp))

                // Bluetooth circle: 56dp, soft glow pulse (2.4s)
                // Light mode: solid primary fill with white glyph, soft primary @ 30% glow and a 2dp white ring
                // Dark mode: cyan @ 14% fill, 2dp cyan border, cyan glyph, cyan glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (colors.isDark) colors.primary.copy(alpha = if (reduceMotion) 0.12f else 0.15f * glowPulse)
                                else colors.primary.copy(alpha = if (reduceMotion) 0.25f else 0.30f * glowPulse)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) colors.primary.copy(alpha = 0.14f) else colors.primary)
                            .border(2.dp, if (colors.isDark) colors.primary else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (colors.isDark) colors.primary else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Dotted line with 6 dots and fade pulse
                PulsingDottedLine(pulsePhase = (pulsePhase + 3f) % 6f)

                Spacer(modifier = Modifier.width(6.dp))

                // Laptop (screen + base) with blinking caret
                val laptopOutline = if (colors.isDark) colors.textSecondary else Color(0xFF0B1B33).copy(alpha = 0.80f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Laptop Screen: 80x52dp, outline stroke, inset bg, prompt + blinking caret
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 52.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .border(2.dp, laptopOutline, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(colors.surfaceInset),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$ ",
                                style = SynqviaType.MonoSmall.copy(
                                    color = laptopOutline
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 12.dp)
                                    .background(colors.primary.copy(alpha = if (reduceMotion) 1f else caretAlpha))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(1.dp))

                    // Laptop Base: 94x6dp platform in outline stroke with center notch
                    Box(
                        modifier = Modifier
                            .size(width = 94.dp, height = 6.dp)
                            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp, topStart = 1.dp, topEnd = 1.dp))
                            .background(colors.surfaceInset)
                            .border(1.5.dp, laptopOutline, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp, topStart = 1.dp, topEnd = 1.dp)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 2.dp)
                                .background(if (colors.isDark) colors.textTertiary.copy(alpha = 0.5f) else Color(0xFF0B1B33).copy(alpha = 0.20f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Feature Chips with colored icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureChip(
                    text = "Offline & Private",
                    icon = Icons.Default.Lock,
                    iconTint = if (colors.isDark) colors.primary else colors.green
                )
                FeatureChip(
                    text = "Text + History",
                    icon = Icons.Default.Description,
                    iconTint = if (colors.isDark) colors.primary else colors.blue
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FeatureChip(
                text = "No Internet Needed",
                icon = Icons.Default.WifiOff,
                iconTint = if (colors.isDark) colors.primary else colors.blue
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Pinned bottom space buffer
        Spacer(modifier = Modifier.height(112.dp))
    }
}

@Composable
private fun PulsingDottedLine(pulsePhase: Float) {
    val colors = SynqviaTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val dist = (pulsePhase - index).let { if (it < 0) it + 6f else it }
            val dotColor = if (colors.isDark) {
                val alpha = (1f - (dist / 6f)).coerceIn(0.25f, 1f)
                colors.primary.copy(alpha = alpha)
            } else {
                val highlightAlpha = (1f - (dist / 2f)).coerceIn(0f, 1f)
                lerp(Color(0xFF9FB4D6), colors.primary, highlightAlpha)
            }
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun FeatureChip(
    text: String,
    icon: ImageVector,
    iconTint: Color = SynqviaTheme.colors.primary
) {
    val colors = SynqviaTheme.colors
    Box(
        modifier = Modifier
            .then(
                if (!colors.isDark) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = colors.cardShadowSpot,
                        ambientColor = colors.cardShadowAmbient
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (colors.isDark) colors.surfaceHigh else colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = SynqviaType.Chip.copy(color = if (colors.isDark) colors.textSecondary else colors.textPrimary),
                color = if (colors.isDark) colors.textSecondary else colors.textPrimary
            )
        }
    }
}

@Composable
private fun OnboardingTemplatePage(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = SynqviaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        IconTile(
            icon = icon,
            tint = colors.primary,
            size = 64.dp,
            iconSize = 32.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = SynqviaType.Title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            style = SynqviaType.Callout,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(112.dp))
    }
}

@Preview(name = "Onboarding - Hero Page", showBackground = true)
@Composable
fun PreviewOnboardingHero() {
    SynqviaTheme {
        OnboardingContent(onGetStarted = {})
    }
}
