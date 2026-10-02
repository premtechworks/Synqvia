package com.github.premtechworks.synqvia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.ui.HeaderGlassStyle
import com.github.premtechworks.synqvia.ui.LocalHazeState
import com.github.premtechworks.synqvia.ui.LocalIsBlurSupported
import com.github.premtechworks.synqvia.ui.motion.StandardEasing
import com.github.premtechworks.synqvia.ui.motion.StaggerHost
import com.github.premtechworks.synqvia.ui.synqviaGlass
import com.github.premtechworks.synqvia.ui.theme.BgBottom
import com.github.premtechworks.synqvia.ui.theme.BgTop
import dev.chrisbanes.haze.hazeSource

/**
 * Height of the bottom navigation bar (56dp + navigationBars bottom inset),
 * measured dynamically once and provided to child screens.
 */
val LocalBottomBarHeight = compositionLocalOf<Dp> { 0.dp }

/**
 * Shared screen layout scaffold establishing exactly one owner of window insets.
 *
 * Edge-to-edge:
 * - Content fills the WHOLE window and draws behind the bottom bar.
 * - Header row sits at: top = statusBars top inset + 12dp, height = 48dp, horizontal padding = 16dp.
 * - Scroll containers receive [contentPadding] with:
 *     start = 16dp, end = 16dp,
 *     top = statusBars top + 12dp + 48dp + 12dp (header offset),
 *     bottom = LocalBottomBarHeight + 24dp.
 * - Titles sit at the identical Y coordinate across all tabs.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    isScrolled: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    content: @Composable (contentPadding: PaddingValues) -> Unit
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTop = statusBarTop + 12.dp
    val headerHeight = 48.dp
    val headerOffset = if (header != null) {
        headerTop + headerHeight + 12.dp
    } else {
        statusBarTop + 12.dp
    }
    val bottomBarHeight = LocalBottomBarHeight.current
    val bottomPadding = bottomBarHeight + 24.dp

    val contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = headerOffset,
        bottom = bottomPadding
    )

    val hazeState = LocalHazeState.current
    val isBlurSupported = LocalIsBlurSupported.current

    val headerAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(
            durationMillis = 150,
            easing = StandardEasing
        ),
        label = "sticky_header_glass_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        // Full window content (draws behind bottom bar and scrolls to full height)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier
                )
        ) {
            // One entrance stagger per visit to this screen; the host disables it once played so
            // recycled LazyColumn items don't replay it on scroll.
            StaggerHost {
                content(contentPadding)
            }
        }

        // Pinned header row with fading glass material and hairline
        if (header != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerTop + headerHeight + 8.dp)
            ) {
                // Glass background material (transparent at scroll 0, fades in after 8dp of scroll)
                if (headerAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = headerAlpha }
                            .synqviaGlass(
                                hazeState = hazeState,
                                style = HeaderGlassStyle,
                                isBlurSupported = isBlurSupported,
                                fallbackColor = Color(0xFF0B1426)
                            )
                    )

                    // 0.5dp bottom hairline (white 10%)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { alpha = headerAlpha }
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                }

                // Header content positioned identically across all tabs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = headerTop)
                        .height(headerHeight)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    header()
                }
            }
        }
    }
}

/**
 * Scrollable column that fills the entire window without clipping or shortening the scroll container.
 * Content padding is applied internally via spacers and horizontal padding so the viewport reaches
 * edge-to-edge.
 */
@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    scrollState: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val startPad = contentPadding.calculateStartPadding(layoutDirection)
    val endPad = contentPadding.calculateEndPadding(layoutDirection)
    val topPad = contentPadding.calculateTopPadding()
    val bottomPad = contentPadding.calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = startPad, end = endPad),
        horizontalAlignment = horizontalAlignment
    ) {
        Spacer(modifier = Modifier.height(topPad))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
        Spacer(modifier = Modifier.height(bottomPad))
    }
}
