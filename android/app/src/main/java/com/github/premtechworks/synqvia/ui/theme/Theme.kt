package com.github.premtechworks.synqvia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF8CF4FF),
    secondary = BlueAccent,
    onSecondary = Color(0xFF002772),
    secondaryContainer = Color(0xFF003A9F),
    onSecondaryContainer = Color(0xFFDCE2FF),
    tertiary = Color(0xFF64D2FF),
    background = DarkNavyBackground,
    onBackground = TextHighEmphasis,
    surface = DarkNavySurface,
    onSurface = TextHighEmphasis,
    surfaceVariant = Color(0xFF162238),
    onSurfaceVariant = TextMediumEmphasis,
    error = StatusOffline,
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8CF4FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF0054CE),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2FF),
    onSecondaryContainer = Color(0xFF00174B),
    tertiary = Color(0xFF006684),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun SynqviaTheme(
    darkTheme: Boolean = true, // Default to deep liquid glass dark mode for visual impact
    dynamicColor: Boolean = false, // Keep distinctive glass theme branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
