package com.github.premtechworks.synqvia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.snappySpring
import com.github.premtechworks.synqvia.ui.theme.AccentBlue
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary

/**
 * "Quick ways to send" (collapsible, collapsed by default):
 * 4 rows, each with a 28dp icon tile (Quick Settings Tile, Selection Menu, Share Sheet, Notification Action),
 * a 14sp title and a 12sp secondary line.
 */
@Composable
fun OneTapPathsInfoCard(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = DecelerateEasing),
        label = "quick_ways_chevron"
    )

    SynqviaCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(snappySpring()),
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    haptics.tick()
                    isExpanded = !isExpanded
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = Icons.Default.TouchApp,
                    tint = PrimaryCyan,
                    size = 36.dp,
                    iconSize = 18.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Quick ways to send",
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickWayRow(
                    icon = Icons.Default.Tune,
                    title = "Quick Settings Tile",
                    subtitle = "Swipe down status bar and tap 'Sync to PC'"
                )
                QuickWayRow(
                    icon = Icons.Default.ContentCut,
                    title = "Selection Menu",
                    subtitle = "Highlight text anywhere and pick 'Send to PC'"
                )
                QuickWayRow(
                    icon = Icons.Default.Share,
                    title = "Share Sheet",
                    subtitle = "Use system Share Sheet → 'Send to PC'"
                )
                QuickWayRow(
                    icon = Icons.Default.Notifications,
                    title = "Notification Action",
                    subtitle = "Tap 'Sync to PC' in ongoing notification"
                )
            }
        }
    }
}

@Composable
private fun QuickWayRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AccentBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SynqviaType.Body,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = SynqviaType.Caption,
                color = TextSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OneTapPathsInfoCardPreview() {
    SynqviaTheme {
        OneTapPathsInfoCard(modifier = Modifier.padding(16.dp))
    }
}
