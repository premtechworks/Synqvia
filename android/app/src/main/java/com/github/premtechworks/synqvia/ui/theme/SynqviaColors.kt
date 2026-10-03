package com.github.premtechworks.synqvia.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion

/**
 * Complete semantic design tokens for Synqvia.
 * Every custom UI element reads from [SynqviaColors] via [SynqviaTheme.colors].
 */
@Immutable
data class SynqviaColors(
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceInset: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val dateHeaderText: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryPressed: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val tonalButtonContainer: Color,
    val tonalButtonContent: Color,
    val connectionCardBg: Color,
    val connectionCardBorder: Color,
    val chipUnselectedBg: Color,
    val chipUnselectedText: Color,
    val blue: Color,
    val blueContainer: Color,
    val blueText: Color,
    val purple: Color,
    val purpleContainer: Color,
    val green: Color,
    val greenContainer: Color,
    val greenText: Color,
    val greenBannerBorder: Color,
    val red: Color,
    val redContainer: Color,
    val redText: Color,
    val orange: Color,
    val orangeContainer: Color,
    val amber: Color,
    val amberContainer: Color,
    val gold: Color,
    val pillFromPcBg: Color,
    val pillFromPcFg: Color,
    val pillToPcBg: Color,
    val pillToPcFg: Color,
    val switchOnTrack: Color,
    val switchOnThumb: Color,
    val switchOffTrack: Color,
    val switchOffThumb: Color,
    val switchOffBorder: Color,
    val sliderActive: Color,
    val sliderInactive: Color,
    val sliderTick: Color,
    val glassTintBar: Color,
    val glassTintSheet: Color,
    val scrim: Color,
    val cardShadowAmbient: Color,
    val cardShadowSpot: Color,
    val navBarBg: Color,
    val navPillBg: Color,
    val keyboardBg: Color,
    val keyboardKey: Color,
    val keyboardKeyPressed: Color,
    val keyboardKeyFunction: Color,
    val keyboardKeyText: Color,
    val keyboardKeyHint: Color,
    val keyboardEnter: Color,
    val keyboardEnterIcon: Color,
    val keyboardToolbarBg: Color,
    val keyboardToolbarIconInactive: Color,
    val keyboardToolbarActiveBg: Color,
    val keyboardToolbarActiveIcon: Color,
    val tileBluetoothContainer: Color,
    val tileBluetoothContent: Color,
    val tileNotificationContainer: Color,
    val tileNotificationContent: Color,
    val tileBatteryContainer: Color,
    val tileBatteryContent: Color,
    val tileAccessibilityContainer: Color,
    val tileAccessibilityContent: Color,
    val tileKeyboardContainer: Color,
    val tileKeyboardContent: Color,
    val tileAutoSyncContainer: Color,
    val tileAutoSyncContent: Color,
    val tileTextOnlyContainer: Color,
    val tileTextOnlyContent: Color,
    val tileClearContainer: Color,
    val tileClearContent: Color,
    val tileThemeContainer: Color,
    val tileThemeContent: Color,
    val isDark: Boolean
)

val DarkSynqviaColors = SynqviaColors(
    bgTop = Color(0xFF050B18),
    bgBottom = Color(0xFF0A1428),
    surface = Color(0xFF101B2F),
    surfaceInset = Color(0xFF0B1426),
    surfaceHigh = Color(0xFF15233B),
    outline = Color(0xFF1F2D47),
    divider = Color(0x0FFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8FA0BC),
    textTertiary = Color(0xFF7A8CAB),
    dateHeaderText = Color(0xFF8FA0BC),
    primary = Color(0xFF12D4FF),
    onPrimary = Color(0xFF04111F),
    primaryPressed = Color(0xFF0EA5E9),
    primaryContainer = Color(0x2412D4FF),
    onPrimaryContainer = Color(0xFF12D4FF),
    tonalButtonContainer = Color(0xFF12D4FF),
    tonalButtonContent = Color(0xFF04111F),
    connectionCardBg = Color(0xFF101B2F),
    connectionCardBorder = Color(0xFF1F2D47),
    chipUnselectedBg = Color(0xFF15233B),
    chipUnselectedText = Color(0xFF8FA0BC),
    blue = Color(0xFF3D8BFF),
    blueContainer = Color(0x243D8BFF),
    blueText = Color(0xFF3D8BFF),
    purple = Color(0xFFA855F7),
    purpleContainer = Color(0x24A855F7),
    green = Color(0xFF2BD97E),
    greenContainer = Color(0x242BD97E),
    greenText = Color(0xFF2BD97E),
    greenBannerBorder = Color(0x592BD97E),
    red = Color(0xFFFF4D5E),
    redContainer = Color(0x24FF4D5E),
    redText = Color(0xFFFF4D5E),
    orange = Color(0xFFFF7A45),
    orangeContainer = Color(0x24FF7A45),
    amber = Color(0xFFF59E0B),
    amberContainer = Color(0x24F59E0B),
    gold = Color(0xFFFFC21A),
    pillFromPcBg = Color(0x242BD97E),
    pillFromPcFg = Color(0xFF2BD97E),
    pillToPcBg = Color(0x243D8BFF),
    pillToPcFg = Color(0xFF3D8BFF),
    switchOnTrack = Color(0xFF12D4FF),
    switchOnThumb = Color(0xFFFFFFFF),
    switchOffTrack = Color(0xFF15233B),
    switchOffThumb = Color(0xFFFFFFFF),
    switchOffBorder = Color(0xFF1F2D47),
    sliderActive = Color(0xFF12D4FF),
    sliderInactive = Color(0x33FFFFFF),
    sliderTick = Color(0x33FFFFFF),
    glassTintBar = Color(0xFF0B1426).copy(alpha = 0.72f),
    glassTintSheet = Color(0xFF050B18).copy(alpha = 0.62f),
    scrim = Color(0x73000000),
    cardShadowAmbient = Color.Transparent,
    cardShadowSpot = Color.Transparent,
    navBarBg = Color(0xFF0B1426),
    navPillBg = Color(0x2412D4FF),
    keyboardBg = Color(0xFF0A1224),
    keyboardKey = Color(0xFF16223A),
    keyboardKeyPressed = Color(0xFF1F3050),
    keyboardKeyFunction = Color(0xFF111B2E),
    keyboardKeyText = Color(0xFFFFFFFF),
    keyboardKeyHint = Color(0xFF7C8BA1),
    keyboardEnter = Color(0xFF12D4FF),
    keyboardEnterIcon = Color(0xFF04111F),
    keyboardToolbarBg = Color(0xFF0B1426),
    keyboardToolbarIconInactive = Color(0xFF9FB0CC),
    keyboardToolbarActiveBg = Color(0x2E12D4FF),
    keyboardToolbarActiveIcon = Color(0xFF12D4FF),
    tileBluetoothContainer = Color(0x242BD97E),
    tileBluetoothContent = Color(0xFF2BD97E),
    tileNotificationContainer = Color(0x242BD97E),
    tileNotificationContent = Color(0xFF2BD97E),
    tileBatteryContainer = Color(0x242BD97E),
    tileBatteryContent = Color(0xFF2BD97E),
    tileAccessibilityContainer = Color(0x242BD97E),
    tileAccessibilityContent = Color(0xFF2BD97E),
    tileKeyboardContainer = Color(0x242BD97E),
    tileKeyboardContent = Color(0xFF2BD97E),
    tileAutoSyncContainer = Color(0x243D8BFF),
    tileAutoSyncContent = Color(0xFF12D4FF),
    tileTextOnlyContainer = Color(0x243D8BFF),
    tileTextOnlyContent = Color(0xFF12D4FF),
    tileClearContainer = Color(0x24FF4D5E),
    tileClearContent = Color(0xFFFF4D5E),
    tileThemeContainer = Color(0xFF15233B),
    tileThemeContent = Color(0xFF8FA0BC),
    isDark = true
)

val LightSynqviaColors = SynqviaColors(
    bgTop = Color(0xFFF7FAFE),
    bgBottom = Color(0xFFEDF3FC),
    surface = Color(0xFFFFFFFF),
    surfaceInset = Color(0xFFF4F7FC),
    surfaceHigh = Color(0xFFEEF3FB),
    outline = Color(0xFFE1E9F5),
    divider = Color(0xFF0B1B33).copy(alpha = 0.07f),
    textPrimary = Color(0xFF0B1B33),
    textSecondary = Color(0xFF55657F),
    textTertiary = Color(0xFF62738F),
    dateHeaderText = Color(0xFF0B1B33),
    primary = Color(0xFF1F6FEB),
    onPrimary = Color(0xFFFFFFFF),
    primaryPressed = Color(0xFF1A5FCE),
    primaryContainer = Color(0xFFE3EEFF),
    onPrimaryContainer = Color(0xFF1556C7),
    tonalButtonContainer = Color(0xFFD9F0FB),
    tonalButtonContent = Color(0xFF1565D8),
    connectionCardBg = Color(0xFFEEF4FD),
    connectionCardBorder = Color(0xFFDCE7F8),
    chipUnselectedBg = Color(0xFFEAF0FA),
    chipUnselectedText = Color(0xFF3C4D68),
    blue = Color(0xFF1F6FEB),
    blueContainer = Color(0xFFE4EEFF),
    blueText = Color(0xFF1F6FEB),
    purple = Color(0xFF6D4AE0),
    purpleContainer = Color(0xFFEDE8FD),
    green = Color(0xFF1A9B5A),
    greenContainer = Color(0xFFE3F6EC),
    greenText = Color(0xFF12804A),
    greenBannerBorder = Color(0xFFBFE9CF),
    red = Color(0xFFE5484D),
    redContainer = Color(0xFFFDEBEC),
    redText = Color(0xFFC93238),
    orange = Color(0xFFE5593D),
    orangeContainer = Color(0xFFFDECE6),
    amber = Color(0xFFD97706),
    amberContainer = Color(0xFFFEF0DC),
    gold = Color(0xFFF5B000),
    pillFromPcBg = Color(0xFFE3F6EC),
    pillFromPcFg = Color(0xFF12804A),
    pillToPcBg = Color(0xFFE4EEFF),
    pillToPcFg = Color(0xFF1F6FEB),
    switchOnTrack = Color(0xFF1F6FEB),
    switchOnThumb = Color(0xFFFFFFFF),
    switchOffTrack = Color(0xFFE4EAF3),
    switchOffThumb = Color(0xFF8E9BB3),
    switchOffBorder = Color(0xFFCBD5E4),
    sliderActive = Color(0xFF1F6FEB),
    sliderInactive = Color(0xFFC5D2E6),
    sliderTick = Color(0xFFB5C4DC),
    glassTintBar = Color(0xFFFFFFFF).copy(alpha = 0.72f),
    glassTintSheet = Color(0xFFEAF1FC).copy(alpha = 0.30f),
    scrim = Color(0xFF0B1B33).copy(alpha = 0.20f),
    cardShadowAmbient = Color(0xFF1F4B99).copy(alpha = 0.06f),
    cardShadowSpot = Color(0xFF1F4B99).copy(alpha = 0.10f),
    navBarBg = Color(0xFFFFFFFF),
    navPillBg = Color(0xFFE3EEFF),
    keyboardBg = Color(0xFFE8EDF5),
    keyboardKey = Color(0xFFFFFFFF),
    keyboardKeyPressed = Color(0xFFDCE6F5),
    keyboardKeyFunction = Color(0xFFD5DEEB),
    keyboardKeyText = Color(0xFF0B1B33),
    keyboardKeyHint = Color(0xFF7A8AA3),
    keyboardEnter = Color(0xFF1F6FEB),
    keyboardEnterIcon = Color(0xFFFFFFFF),
    keyboardToolbarBg = Color(0xFFFFFFFF),
    keyboardToolbarIconInactive = Color(0xFF55657F),
    keyboardToolbarActiveBg = Color(0xFFE3EEFF),
    keyboardToolbarActiveIcon = Color(0xFF1F6FEB),
    tileBluetoothContainer = Color(0xFFE3F6EC),
    tileBluetoothContent = Color(0xFF1A9B5A),
    tileNotificationContainer = Color(0xFFFDECE6),
    tileNotificationContent = Color(0xFFE5593D),
    tileBatteryContainer = Color(0xFFE3F6EC),
    tileBatteryContent = Color(0xFF1A9B5A),
    tileAccessibilityContainer = Color(0xFFEDE8FD),
    tileAccessibilityContent = Color(0xFF6D4AE0),
    tileKeyboardContainer = Color(0xFFE4EEFF),
    tileKeyboardContent = Color(0xFF1F6FEB),
    tileAutoSyncContainer = Color(0xFFE4EEFF),
    tileAutoSyncContent = Color(0xFF1F6FEB),
    tileTextOnlyContainer = Color(0xFFE4EEFF),
    tileTextOnlyContent = Color(0xFF1F6FEB),
    tileClearContainer = Color(0xFFFDEBEC),
    tileClearContent = Color(0xFFE5484D),
    tileThemeContainer = Color(0xFFEEF1F6),
    tileThemeContent = Color(0xFF2B3A55),
    isDark = false
)

val LocalSynqviaColors = staticCompositionLocalOf { DarkSynqviaColors }

object SynqviaTheme {
    val colors: SynqviaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSynqviaColors.current

    val typography: SynqviaTypography
        get() = SynqviaTypography

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalSynqviaColors.current.isDark
}

fun SynqviaColors.toColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = blue,
            onSecondary = onPrimary,
            secondaryContainer = blueContainer,
            onSecondaryContainer = blueText,
            tertiary = green,
            onTertiary = onPrimary,
            tertiaryContainer = greenContainer,
            onTertiaryContainer = greenText,
            background = bgTop,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = textSecondary,
            outline = outline,
            outlineVariant = divider,
            error = red,
            onError = Color.White,
            errorContainer = redContainer,
            onErrorContainer = redText
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = blue,
            onSecondary = onPrimary,
            secondaryContainer = blueContainer,
            onSecondaryContainer = blueText,
            tertiary = green,
            onTertiary = onPrimary,
            tertiaryContainer = greenContainer,
            onTertiaryContainer = greenText,
            background = bgTop,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = textSecondary,
            outline = outline,
            outlineVariant = divider,
            error = red,
            onError = Color.White,
            errorContainer = redContainer,
            onErrorContainer = redText
        )
    }
}

@Composable
fun SynqviaColors.animateColors(durationMillis: Int = 300): SynqviaColors {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) return this

    val spec = tween<Color>(durationMillis = durationMillis)
    val bgTopAnim by animateColorAsState(bgTop, spec, label = "bgTop")
    val bgBottomAnim by animateColorAsState(bgBottom, spec, label = "bgBottom")
    val surfaceAnim by animateColorAsState(surface, spec, label = "surface")
    val surfaceInsetAnim by animateColorAsState(surfaceInset, spec, label = "surfaceInset")
    val surfaceHighAnim by animateColorAsState(surfaceHigh, spec, label = "surfaceHigh")
    val outlineAnim by animateColorAsState(outline, spec, label = "outline")
    val dividerAnim by animateColorAsState(divider, spec, label = "divider")
    val textPrimaryAnim by animateColorAsState(textPrimary, spec, label = "textPrimary")
    val textSecondaryAnim by animateColorAsState(textSecondary, spec, label = "textSecondary")
    val textTertiaryAnim by animateColorAsState(textTertiary, spec, label = "textTertiary")
    val dateHeaderTextAnim by animateColorAsState(dateHeaderText, spec, label = "dateHeaderText")
    val primaryAnim by animateColorAsState(primary, spec, label = "primary")
    val onPrimaryAnim by animateColorAsState(onPrimary, spec, label = "onPrimary")
    val primaryPressedAnim by animateColorAsState(primaryPressed, spec, label = "primaryPressed")
    val primaryContainerAnim by animateColorAsState(primaryContainer, spec, label = "primaryContainer")
    val onPrimaryContainerAnim by animateColorAsState(onPrimaryContainer, spec, label = "onPrimaryContainer")
    val tonalButtonContainerAnim by animateColorAsState(tonalButtonContainer, spec, label = "tonalButtonContainer")
    val tonalButtonContentAnim by animateColorAsState(tonalButtonContent, spec, label = "tonalButtonContent")
    val connectionCardBgAnim by animateColorAsState(connectionCardBg, spec, label = "connectionCardBg")
    val connectionCardBorderAnim by animateColorAsState(connectionCardBorder, spec, label = "connectionCardBorder")
    val chipUnselectedBgAnim by animateColorAsState(chipUnselectedBg, spec, label = "chipUnselectedBg")
    val chipUnselectedTextAnim by animateColorAsState(chipUnselectedText, spec, label = "chipUnselectedText")
    val blueAnim by animateColorAsState(blue, spec, label = "blue")
    val blueContainerAnim by animateColorAsState(blueContainer, spec, label = "blueContainer")
    val blueTextAnim by animateColorAsState(blueText, spec, label = "blueText")
    val purpleAnim by animateColorAsState(purple, spec, label = "purple")
    val purpleContainerAnim by animateColorAsState(purpleContainer, spec, label = "purpleContainer")
    val greenAnim by animateColorAsState(green, spec, label = "green")
    val greenContainerAnim by animateColorAsState(greenContainer, spec, label = "greenContainer")
    val greenTextAnim by animateColorAsState(greenText, spec, label = "greenText")
    val greenBannerBorderAnim by animateColorAsState(greenBannerBorder, spec, label = "greenBannerBorder")
    val redAnim by animateColorAsState(red, spec, label = "red")
    val redContainerAnim by animateColorAsState(redContainer, spec, label = "redContainer")
    val redTextAnim by animateColorAsState(redText, spec, label = "redText")
    val orangeAnim by animateColorAsState(orange, spec, label = "orange")
    val orangeContainerAnim by animateColorAsState(orangeContainer, spec, label = "orangeContainer")
    val amberAnim by animateColorAsState(amber, spec, label = "amber")
    val amberContainerAnim by animateColorAsState(amberContainer, spec, label = "amberContainer")
    val goldAnim by animateColorAsState(gold, spec, label = "gold")
    val pillFromPcBgAnim by animateColorAsState(pillFromPcBg, spec, label = "pillFromPcBg")
    val pillFromPcFgAnim by animateColorAsState(pillFromPcFg, spec, label = "pillFromPcFg")
    val pillToPcBgAnim by animateColorAsState(pillToPcBg, spec, label = "pillToPcBg")
    val pillToPcFgAnim by animateColorAsState(pillToPcFg, spec, label = "pillToPcFg")
    val switchOnTrackAnim by animateColorAsState(switchOnTrack, spec, label = "switchOnTrack")
    val switchOnThumbAnim by animateColorAsState(switchOnThumb, spec, label = "switchOnThumb")
    val switchOffTrackAnim by animateColorAsState(switchOffTrack, spec, label = "switchOffTrack")
    val switchOffThumbAnim by animateColorAsState(switchOffThumb, spec, label = "switchOffThumb")
    val switchOffBorderAnim by animateColorAsState(switchOffBorder, spec, label = "switchOffBorder")
    val sliderActiveAnim by animateColorAsState(sliderActive, spec, label = "sliderActive")
    val sliderInactiveAnim by animateColorAsState(sliderInactive, spec, label = "sliderInactive")
    val sliderTickAnim by animateColorAsState(sliderTick, spec, label = "sliderTick")
    val glassTintBarAnim by animateColorAsState(glassTintBar, spec, label = "glassTintBar")
    val glassTintSheetAnim by animateColorAsState(glassTintSheet, spec, label = "glassTintSheet")
    val scrimAnim by animateColorAsState(scrim, spec, label = "scrim")
    val cardShadowAmbientAnim by animateColorAsState(cardShadowAmbient, spec, label = "cardShadowAmbient")
    val cardShadowSpotAnim by animateColorAsState(cardShadowSpot, spec, label = "cardShadowSpot")
    val navBarBgAnim by animateColorAsState(navBarBg, spec, label = "navBarBg")
    val navPillBgAnim by animateColorAsState(navPillBg, spec, label = "navPillBg")
    val keyboardBgAnim by animateColorAsState(keyboardBg, spec, label = "keyboardBg")
    val keyboardKeyAnim by animateColorAsState(keyboardKey, spec, label = "keyboardKey")
    val keyboardKeyPressedAnim by animateColorAsState(keyboardKeyPressed, spec, label = "keyboardKeyPressed")
    val keyboardKeyFunctionAnim by animateColorAsState(keyboardKeyFunction, spec, label = "keyboardKeyFunction")
    val keyboardKeyTextAnim by animateColorAsState(keyboardKeyText, spec, label = "keyboardKeyText")
    val keyboardKeyHintAnim by animateColorAsState(keyboardKeyHint, spec, label = "keyboardKeyHint")
    val keyboardEnterAnim by animateColorAsState(keyboardEnter, spec, label = "keyboardEnter")
    val keyboardEnterIconAnim by animateColorAsState(keyboardEnterIcon, spec, label = "keyboardEnterIcon")
    val keyboardToolbarBgAnim by animateColorAsState(keyboardToolbarBg, spec, label = "keyboardToolbarBg")
    val keyboardToolbarIconInactiveAnim by animateColorAsState(keyboardToolbarIconInactive, spec, label = "keyboardToolbarIconInactive")
    val keyboardToolbarActiveBgAnim by animateColorAsState(keyboardToolbarActiveBg, spec, label = "keyboardToolbarActiveBg")
    val keyboardToolbarActiveIconAnim by animateColorAsState(keyboardToolbarActiveIcon, spec, label = "keyboardToolbarActiveIcon")
    val tileBluetoothContainerAnim by animateColorAsState(tileBluetoothContainer, spec, label = "tileBluetoothContainer")
    val tileBluetoothContentAnim by animateColorAsState(tileBluetoothContent, spec, label = "tileBluetoothContent")
    val tileNotificationContainerAnim by animateColorAsState(tileNotificationContainer, spec, label = "tileNotificationContainer")
    val tileNotificationContentAnim by animateColorAsState(tileNotificationContent, spec, label = "tileNotificationContent")
    val tileBatteryContainerAnim by animateColorAsState(tileBatteryContainer, spec, label = "tileBatteryContainer")
    val tileBatteryContentAnim by animateColorAsState(tileBatteryContent, spec, label = "tileBatteryContent")
    val tileAccessibilityContainerAnim by animateColorAsState(tileAccessibilityContainer, spec, label = "tileAccessibilityContainer")
    val tileAccessibilityContentAnim by animateColorAsState(tileAccessibilityContent, spec, label = "tileAccessibilityContent")
    val tileKeyboardContainerAnim by animateColorAsState(tileKeyboardContainer, spec, label = "tileKeyboardContainer")
    val tileKeyboardContentAnim by animateColorAsState(tileKeyboardContent, spec, label = "tileKeyboardContent")
    val tileAutoSyncContainerAnim by animateColorAsState(tileAutoSyncContainer, spec, label = "tileAutoSyncContainer")
    val tileAutoSyncContentAnim by animateColorAsState(tileAutoSyncContent, spec, label = "tileAutoSyncContent")
    val tileTextOnlyContainerAnim by animateColorAsState(tileTextOnlyContainer, spec, label = "tileTextOnlyContainer")
    val tileTextOnlyContentAnim by animateColorAsState(tileTextOnlyContent, spec, label = "tileTextOnlyContent")
    val tileClearContainerAnim by animateColorAsState(tileClearContainer, spec, label = "tileClearContainer")
    val tileClearContentAnim by animateColorAsState(tileClearContent, spec, label = "tileClearContent")
    val tileThemeContainerAnim by animateColorAsState(tileThemeContainer, spec, label = "tileThemeContainer")
    val tileThemeContentAnim by animateColorAsState(tileThemeContent, spec, label = "tileThemeContent")

    return copy(
        bgTop = bgTopAnim,
        bgBottom = bgBottomAnim,
        surface = surfaceAnim,
        surfaceInset = surfaceInsetAnim,
        surfaceHigh = surfaceHighAnim,
        outline = outlineAnim,
        divider = dividerAnim,
        textPrimary = textPrimaryAnim,
        textSecondary = textSecondaryAnim,
        textTertiary = textTertiaryAnim,
        dateHeaderText = dateHeaderTextAnim,
        primary = primaryAnim,
        onPrimary = onPrimaryAnim,
        primaryPressed = primaryPressedAnim,
        primaryContainer = primaryContainerAnim,
        onPrimaryContainer = onPrimaryContainerAnim,
        tonalButtonContainer = tonalButtonContainerAnim,
        tonalButtonContent = tonalButtonContentAnim,
        connectionCardBg = connectionCardBgAnim,
        connectionCardBorder = connectionCardBorderAnim,
        chipUnselectedBg = chipUnselectedBgAnim,
        chipUnselectedText = chipUnselectedTextAnim,
        blue = blueAnim,
        blueContainer = blueContainerAnim,
        blueText = blueTextAnim,
        purple = purpleAnim,
        purpleContainer = purpleContainerAnim,
        green = greenAnim,
        greenContainer = greenContainerAnim,
        greenText = greenTextAnim,
        greenBannerBorder = greenBannerBorderAnim,
        red = redAnim,
        redContainer = redContainerAnim,
        redText = redTextAnim,
        orange = orangeAnim,
        orangeContainer = orangeContainerAnim,
        amber = amberAnim,
        amberContainer = amberContainerAnim,
        gold = goldAnim,
        pillFromPcBg = pillFromPcBgAnim,
        pillFromPcFg = pillFromPcFgAnim,
        pillToPcBg = pillToPcBgAnim,
        pillToPcFg = pillToPcFgAnim,
        switchOnTrack = switchOnTrackAnim,
        switchOnThumb = switchOnThumbAnim,
        switchOffTrack = switchOffTrackAnim,
        switchOffThumb = switchOffThumbAnim,
        switchOffBorder = switchOffBorderAnim,
        sliderActive = sliderActiveAnim,
        sliderInactive = sliderInactiveAnim,
        sliderTick = sliderTickAnim,
        glassTintBar = glassTintBarAnim,
        glassTintSheet = glassTintSheetAnim,
        scrim = scrimAnim,
        cardShadowAmbient = cardShadowAmbientAnim,
        cardShadowSpot = cardShadowSpotAnim,
        navBarBg = navBarBgAnim,
        navPillBg = navPillBgAnim,
        keyboardBg = keyboardBgAnim,
        keyboardKey = keyboardKeyAnim,
        keyboardKeyPressed = keyboardKeyPressedAnim,
        keyboardKeyFunction = keyboardKeyFunctionAnim,
        keyboardKeyText = keyboardKeyTextAnim,
        keyboardKeyHint = keyboardKeyHintAnim,
        keyboardEnter = keyboardEnterAnim,
        keyboardEnterIcon = keyboardEnterIconAnim,
        keyboardToolbarBg = keyboardToolbarBgAnim,
        keyboardToolbarIconInactive = keyboardToolbarIconInactiveAnim,
        keyboardToolbarActiveBg = keyboardToolbarActiveBgAnim,
        keyboardToolbarActiveIcon = keyboardToolbarActiveIconAnim,
        tileBluetoothContainer = tileBluetoothContainerAnim,
        tileBluetoothContent = tileBluetoothContentAnim,
        tileNotificationContainer = tileNotificationContainerAnim,
        tileNotificationContent = tileNotificationContentAnim,
        tileBatteryContainer = tileBatteryContainerAnim,
        tileBatteryContent = tileBatteryContentAnim,
        tileAccessibilityContainer = tileAccessibilityContainerAnim,
        tileAccessibilityContent = tileAccessibilityContentAnim,
        tileKeyboardContainer = tileKeyboardContainerAnim,
        tileKeyboardContent = tileKeyboardContentAnim,
        tileAutoSyncContainer = tileAutoSyncContainerAnim,
        tileAutoSyncContent = tileAutoSyncContentAnim,
        tileTextOnlyContainer = tileTextOnlyContainerAnim,
        tileTextOnlyContent = tileTextOnlyContentAnim,
        tileClearContainer = tileClearContainerAnim,
        tileClearContent = tileClearContentAnim,
        tileThemeContainer = tileThemeContainerAnim,
        tileThemeContent = tileThemeContentAnim,
        isDark = isDark
    )
}

@Composable
fun Modifier.synqviaCardShadow(
    elevation: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    val colors = SynqviaTheme.colors
    return synqviaCardShadow(
        elevation = elevation,
        shape = shape,
        ambientColor = colors.cardShadowAmbient,
        spotColor = colors.cardShadowSpot
    )
}

fun Modifier.synqviaCardShadow(
    elevation: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    ambientColor: Color,
    spotColor: Color
): Modifier {
    if (ambientColor.alpha <= 0.001f && spotColor.alpha <= 0.001f) return this
    return this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ambientColor,
        spotColor = spotColor
    )
}
