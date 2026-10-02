package com.github.premtechworks.synqvia.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion

val LocalIsDarkTheme = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = OnPrimaryCyan,
    primaryContainer = PrimaryCyan.copy(alpha = 0.14f),
    onPrimaryContainer = PrimaryCyan,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = AccentBlue.copy(alpha = 0.14f),
    onSecondaryContainer = AccentBlue,
    tertiary = AccentGreen,
    onTertiary = OnPrimaryCyan,
    background = BgTop,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = OutlineDark,
    outlineVariant = DividerDark,
    error = AccentRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCyanLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryCyanLight.copy(alpha = 0.14f),
    onPrimaryContainer = TextPrimaryLight,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = AccentBlue.copy(alpha = 0.14f),
    onSecondaryContainer = TextPrimaryLight,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    background = BgTopLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceHighLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = DividerLight,
    error = AccentRed,
    onError = Color.White
)


@Composable
fun SynqviaTheme(
    themeMode: String = "system",
    darkTheme: Boolean = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        baseScheme.copy(
            primary = dynamic.primary,
            onPrimary = dynamic.onPrimary,
            secondary = dynamic.secondary,
            onSecondary = dynamic.onSecondary
        )
    } else {
        baseScheme
    }

    val animatedScheme = colorScheme.animateColors(durationMillis = 300)

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
private fun ColorScheme.animateColors(durationMillis: Int = 300): ColorScheme {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) return this

    val spec = tween<Color>(durationMillis = durationMillis)
    val primaryAnim by animateColorAsState(targetValue = primary, animationSpec = spec, label = "primary")
    val onPrimaryAnim by animateColorAsState(targetValue = onPrimary, animationSpec = spec, label = "onPrimary")
    val primaryContainerAnim by animateColorAsState(targetValue = primaryContainer, animationSpec = spec, label = "primaryContainer")
    val onPrimaryContainerAnim by animateColorAsState(targetValue = onPrimaryContainer, animationSpec = spec, label = "onPrimaryContainer")
    val secondaryAnim by animateColorAsState(targetValue = secondary, animationSpec = spec, label = "secondary")
    val onSecondaryAnim by animateColorAsState(targetValue = onSecondary, animationSpec = spec, label = "onSecondary")
    val backgroundAnim by animateColorAsState(targetValue = background, animationSpec = spec, label = "background")
    val onBackgroundAnim by animateColorAsState(targetValue = onBackground, animationSpec = spec, label = "onBackground")
    val surfaceAnim by animateColorAsState(targetValue = surface, animationSpec = spec, label = "surface")
    val onSurfaceAnim by animateColorAsState(targetValue = onSurface, animationSpec = spec, label = "onSurface")
    val surfaceVariantAnim by animateColorAsState(targetValue = surfaceVariant, animationSpec = spec, label = "surfaceVariant")
    val onSurfaceVariantAnim by animateColorAsState(targetValue = onSurfaceVariant, animationSpec = spec, label = "onSurfaceVariant")
    val outlineAnim by animateColorAsState(targetValue = outline, animationSpec = spec, label = "outline")

    return copy(
        primary = primaryAnim,
        onPrimary = onPrimaryAnim,
        primaryContainer = primaryContainerAnim,
        onPrimaryContainer = onPrimaryContainerAnim,
        secondary = secondaryAnim,
        onSecondary = onSecondaryAnim,
        background = backgroundAnim,
        onBackground = onBackgroundAnim,
        surface = surfaceAnim,
        onSurface = onSurfaceAnim,
        surfaceVariant = surfaceVariantAnim,
        onSurfaceVariant = onSurfaceVariantAnim,
        outline = outlineAnim
    )
}
