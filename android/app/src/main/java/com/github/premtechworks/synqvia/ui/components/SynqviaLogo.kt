package com.github.premtechworks.synqvia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.ui.theme.LocalIsDarkTheme

/**
 * Brand logo component with transparent background.
 * Strictly follows the in-app theme (LocalIsDarkTheme), independent of the device's
 * global OS theme setting.
 */
@Composable
fun SynqviaLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Synqvia",
    isDarkTheme: Boolean = LocalIsDarkTheme.current
) {
    val logoRes = if (isDarkTheme) {
        R.drawable.synqvia_logo_dark
    } else {
        R.drawable.synqvia_logo_light
    }

    Image(
        painter = painterResource(id = logoRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(3f, matchHeightConstraintsFirst = true)
    )
}

