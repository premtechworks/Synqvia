package com.github.premtechworks.synqvia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType

/**
 * Brand mark (Neon Q).
 * In dark theme: [brand_mark_neon] (neon Q on transparent).
 * In light theme: [brand_mark_tile] (neon Q centered on navy rounded tile for contrast).
 */
@Composable
fun SynqviaMark(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val isDark = SynqviaTheme.isDark
    val resId = if (isDark) R.drawable.brand_mark_neon else R.drawable.brand_mark_tile
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/**
 * Brand wordmark ("synqvia").
 * In dark theme: [brand_wordmark_for_dark] (near-white letters with cyan Q).
 * In light theme: [brand_wordmark_for_light] (navy letters with cyan Q).
 * Tightly-cropped asset with aspect ratio 591/150 (~3.94:1), no stretching.
 */
@Composable
fun SynqviaWordmark(
    height: Dp = 30.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Synqvia"
) {
    val isDark = SynqviaTheme.isDark
    val resId = if (isDark) R.drawable.brand_wordmark_for_dark else R.drawable.brand_wordmark_for_light
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .aspectRatio(591f / 150f, matchHeightConstraintsFirst = true)
    )
}

/**
 * Standard brand lockup:
 * Mark 40dp, 10dp gap, Wordmark 30dp tall, vertically centered.
 */
@Composable
fun SynqviaBrandLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 40.dp,
    wordmarkHeight: Dp = 30.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SynqviaMark(size = markSize, contentDescription = null)
        Spacer(modifier = Modifier.width(10.dp))
        SynqviaWordmark(height = wordmarkHeight, contentDescription = "Synqvia")
    }
}

@SynqviaLightDarkPreview
@Composable
private fun PreviewSynqviaBrandLockup() {
    SynqviaTheme {
        Box(
            modifier = Modifier
                .background(SynqviaTheme.colors.bgTop)
                .padding(16.dp)
        ) {
            SynqviaBrandLockup()
        }
    }
}

@SynqviaLightDarkPreview
@Composable
private fun PreviewSynqviaMark() {
    SynqviaTheme {
        Box(
            modifier = Modifier
                .background(SynqviaTheme.colors.bgTop)
                .padding(16.dp)
        ) {
            SynqviaMark(size = 40.dp)
        }
    }
}

/**
 * Debug Preview: Compares SynqviaWordmark (height = 30dp) directly beside Text("Settings", LargeTitle)
 * to verify x-height match within 1dp and vertical center alignment.
 */
@SynqviaLightDarkPreview
@Composable
fun PreviewWordmarkBesideLargeTitle() {
    SynqviaTheme {
        Box(
            modifier = Modifier
                .background(SynqviaTheme.colors.bgTop)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                SynqviaWordmark(height = 30.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Settings",
                    style = SynqviaType.LargeTitle,
                    color = SynqviaTheme.colors.textPrimary
                )
            }
        }
    }
}

/**
 * Verification Preview: Home Header Row at 360dp width in light and dark
 * confirming mark + wordmark + gear fit with no overlap or clipping.
 */
@Preview(name = "Home Header 360dp Light", widthDp = 360, showBackground = true)
@Preview(name = "Home Header 360dp Dark", widthDp = 360, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewHomeHeader360dp() {
    SynqviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SynqviaTheme.colors.bgTop)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SynqviaBrandLockup()
                Spacer(modifier = Modifier.weight(1f))
                CircleIconButton(
                    icon = Icons.Default.Settings,
                    onClick = {},
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = "Settings"
                )
            }
        }
    }
}

/**
 * Verification Preview: Home Header Row at 411dp width in light and dark
 * confirming mark + wordmark + gear fit with no overlap or clipping.
 */
@Preview(name = "Home Header 411dp Light", widthDp = 411, showBackground = true)
@Preview(name = "Home Header 411dp Dark", widthDp = 411, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewHomeHeader411dp() {
    SynqviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SynqviaTheme.colors.bgTop)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SynqviaBrandLockup()
                Spacer(modifier = Modifier.weight(1f))
                CircleIconButton(
                    icon = Icons.Default.Settings,
                    onClick = {},
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = "Settings"
                )
            }
        }
    }
}
