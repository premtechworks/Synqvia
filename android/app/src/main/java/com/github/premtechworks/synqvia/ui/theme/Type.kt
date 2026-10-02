@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.github.premtechworks.synqvia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.github.premtechworks.synqvia.R

// ==========================================
// Synqvia Apple-like Type System (SF Pro DNA via Inter & JetBrains Mono)
// ==========================================

/**
 * Builds an Inter variable font family with explicit weight and optical sizing (opsz) axes.
 * Clamps opsz to the font's supported [14f, 32f] range.
 */
private fun interFontFamily(
    weight: Int,
    sizeSp: Float,
    fontWeight: FontWeight = FontWeight(weight)
): FontFamily {
    val clampedOpsz = sizeSp.coerceIn(14f, 32f)
    return FontFamily(
        Font(
            resId = R.font.inter_variable,
            weight = fontWeight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.opticalSizing(clampedOpsz.sp)
            )
        )
    )
}

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

/**
 * Canonical typography scale tokens with optical sizing, tracking in em, and weight discipline.
 */
object SynqviaType {
    // LargeTitle 30/36 SemiBold(650) -0.022em (screen titles)
    val LargeTitle = TextStyle(
        fontFamily = interFontFamily(650, 30f),
        fontWeight = FontWeight(650),
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.022).em,
        color = TextPrimary
    )

    // Title 22/28 SemiBold -0.018em
    val Title = TextStyle(
        fontFamily = interFontFamily(600, 22f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.018).em,
        color = TextPrimary
    )

    // Headline 17/22 SemiBold -0.014em (card titles, "Connected")
    val Headline = TextStyle(
        fontFamily = interFontFamily(600, 17f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.014).em,
        color = TextPrimary
    )

    // Headline with tabular numerals (e.g. RFCOMM channel, numeric status)
    val HeadlineTnum = Headline.copy(fontFeatureSettings = "tnum")

    // Body 15/21 Regular -0.010em
    val Body = TextStyle(
        fontFamily = interFontFamily(400, 15f),
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.010).em,
        color = TextPrimary
    )

    // Body with tabular numerals
    val BodyTnum = Body.copy(fontFeatureSettings = "tnum")

    // Callout 14/20 Regular -0.008em
    val Callout = TextStyle(
        fontFamily = interFontFamily(400, 14f),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.008).em,
        color = TextPrimary
    )

    // Callout with tabular numerals
    val CalloutTnum = Callout.copy(fontFeatureSettings = "tnum")

    // Footnote 13/18 Regular -0.005em
    val Footnote = TextStyle(
        fontFamily = interFontFamily(400, 13f),
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.005).em,
        color = TextSecondary
    )

    // Footnote with tabular numerals
    val FootnoteTnum = Footnote.copy(fontFeatureSettings = "tnum")

    // Caption 12/16 Medium 0
    val Caption = TextStyle(
        fontFamily = interFontFamily(500, 12f),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.em,
        color = TextSecondary
    )

    // Caption with tabular numerals (for relative times, counts, "N chars")
    val CaptionTnum = Caption.copy(fontFeatureSettings = "tnum")

    // CaptionSemiBold 12/16 SemiBold (for URL hosts, active permission labels)
    val CaptionSemiBold = TextStyle(
        fontFamily = interFontFamily(600, 12f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.em,
        color = TextSecondary
    )

    // SubheadlineSemiBold 14/19 SemiBold (for checklist headers, secondary titles)
    val SubheadlineSemiBold = TextStyle(
        fontFamily = interFontFamily(600, 14f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.008).em,
        color = TextPrimary
    )

    // Overline 11/14 SemiBold +0.04em (section labels, pills, nav labels)
    val Overline = TextStyle(
        fontFamily = interFontFamily(600, 11f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em,
        color = TextSecondary
    )

    // Button 15/20 SemiBold -0.010em
    val Button = TextStyle(
        fontFamily = interFontFamily(600, 15f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.010).em
    )

    // ButtonSmall 12/16 SemiBold -0.005em (pill buttons, quick actions)
    val ButtonSmall = TextStyle(
        fontFamily = interFontFamily(600, 12f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.005).em
    )

    // StatNumber 26/30 weight 650 -0.020em + tnum
    val StatNumber = TextStyle(
        fontFamily = interFontFamily(650, 26f),
        fontWeight = FontWeight(650),
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.020).em,
        fontFeatureSettings = "tnum",
        color = TextPrimary
    )

    // Mono 13/18 JetBrains Mono Regular, letterSpacing 0 (MAC, device ID, log)
    val Mono = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.em,
        fontFeatureSettings = "tnum",
        color = TextPrimary
    )

    // Footnote SemiBold (for date group headers)
    val FootnoteSemiBold = TextStyle(
        fontFamily = interFontFamily(600, 13f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.005).em,
        color = TextSecondary
    )

    // MonoSmall 11/15 JetBrains Mono Regular, letterSpacing 0, tnum
    val MonoSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.em,
        fontFeatureSettings = "tnum",
        color = TextSecondary
    )

    // MonoSmall Medium 11/15 JetBrains Mono Medium, letterSpacing 0, tnum
    val MonoSmallMedium = MonoSmall.copy(fontWeight = FontWeight.Medium)

    // MonoMedium 13/18 JetBrains Mono Medium, letterSpacing 0, tnum
    val MonoMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.em,
        fontFeatureSettings = "tnum",
        color = TextPrimary
    )

    // Chips (Medium unselected, SemiBold selected with tabular numbers)
    val Chip = Caption.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val ChipSelected = Caption.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
}

/**
 * Backward compatibility aliases for existing references.
 */
object SynqviaTypography {
    val LargeTitle = SynqviaType.LargeTitle
    val Title = SynqviaType.Title
    val ScreenTitle = SynqviaType.LargeTitle
    val SectionTitle = SynqviaType.Headline
    val CardTitle = SynqviaType.Headline
    val CardTitleSmall = SynqviaType.Headline
    val Body = SynqviaType.Body
    val Callout = SynqviaType.Callout
    val Footnote = SynqviaType.Footnote
    val Caption = SynqviaType.Caption
    val CaptionTnum = SynqviaType.CaptionTnum
    val Overline = SynqviaType.Overline
    val StatNumber = SynqviaType.StatNumber
    val ButtonLabel = SynqviaType.Button
    val ChipLabel = SynqviaType.Chip
    val NavLabel = SynqviaType.Overline
    val MonospaceCode = SynqviaType.Mono
    val MonospaceCodeSmall = SynqviaType.MonoSmall
    val CodeMono = SynqviaType.Mono
    val headlineSmall = SynqviaType.Headline
    val bodySmall = SynqviaType.Caption
}

// Material 3 Typography integration
val Typography = Typography(
    displayLarge = SynqviaType.LargeTitle,
    displayMedium = SynqviaType.LargeTitle,
    displaySmall = SynqviaType.Title,
    headlineLarge = SynqviaType.StatNumber,
    headlineMedium = SynqviaType.LargeTitle,
    headlineSmall = SynqviaType.Title,
    titleLarge = SynqviaType.Title,
    titleMedium = SynqviaType.Headline,
    titleSmall = SynqviaType.Headline,
    bodyLarge = SynqviaType.Body,
    bodyMedium = SynqviaType.Callout,
    bodySmall = SynqviaType.Footnote,
    labelLarge = SynqviaType.Button,
    labelMedium = SynqviaType.Caption,
    labelSmall = SynqviaType.Overline
)
