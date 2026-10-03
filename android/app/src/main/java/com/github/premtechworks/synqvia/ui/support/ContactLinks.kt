package com.github.premtechworks.synqvia.ui.support

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.github.premtechworks.synqvia.R

enum class QuickLinkType {
    TELEGRAM,
    GITHUB,
    KOFI
}

data class QuickLinkItem(
    val type: QuickLinkType,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
    val lightContainerColor: Color,
    val darkContainerColor: Color,
    val lightContentColor: Color,
    val darkContentColor: Color,
    val url: String
)

object ContactLinks {
    const val TELEGRAM_URL = "https://t.me/imprem1122"
    const val GITHUB_URL = "https://github.com/premtechworks/Synqvia"
    const val GITHUB_ISSUES_URL = "https://github.com/premtechworks/Synqvia/issues"
    const val KOFI_URL = "https://ko-fi.com/premkumargara"

    // Tile accents
    val TELEGRAM_LIGHT_CONTAINER = Color.Transparent
    val TELEGRAM_DARK_CONTAINER = Color.Transparent
    val TELEGRAM_GLYPH = Color.Unspecified

    val KOFI_LIGHT_CONTAINER = Color(0xFFFDECE6)
    val KOFI_DARK_CONTAINER = Color(0xFFFF5E5B).copy(alpha = 0.14f)
    val KOFI_GLYPH = Color(0xFFFF5E5B)

    /**
     * Normalizes a URL: removes template braces {{...}}, trims whitespace,
     * and ensures an http:// or https:// scheme prefix.
     */
    fun normalizeUrl(rawUrl: String): String {
        var trimmed = rawUrl.trim()
        if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) {
            trimmed = trimmed.substring(2, trimmed.length - 2).trim()
        }
        if (trimmed.isEmpty() || trimmed.contains("{{") || trimmed.contains("}}") ||
            trimmed == "TELEGRAM_URL" || trimmed == "KOFI_URL" || trimmed == "PLACEHOLDER"
        ) {
            return ""
        }
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    /**
     * Checks if a URL is valid and does not contain unresolved placeholders.
     */
    fun isValidUrl(url: String): Boolean {
        return normalizeUrl(url).isNotBlank()
    }

    fun getAllQuickLinks(
        telegramUrl: String = TELEGRAM_URL,
        githubUrl: String = GITHUB_URL,
        kofiUrl: String = KOFI_URL
    ): List<QuickLinkItem> = listOf(
        QuickLinkItem(
            type = QuickLinkType.TELEGRAM,
            titleRes = R.string.link_telegram_title,
            subtitleRes = R.string.link_telegram_subtitle,
            iconRes = R.drawable.ic_brand_telegram,
            lightContainerColor = TELEGRAM_LIGHT_CONTAINER,
            darkContainerColor = TELEGRAM_DARK_CONTAINER,
            lightContentColor = TELEGRAM_GLYPH,
            darkContentColor = TELEGRAM_GLYPH,
            url = normalizeUrl(telegramUrl).ifEmpty { telegramUrl }
        ),
        QuickLinkItem(
            type = QuickLinkType.GITHUB,
            titleRes = R.string.link_github_title,
            subtitleRes = R.string.link_github_subtitle,
            iconRes = R.drawable.ic_brand_github,
            lightContainerColor = Color(0xFFF0F4F8),
            darkContainerColor = Color(0xFF131D2E),
            lightContentColor = Color(0xFF0D1117),
            darkContentColor = Color(0xFFFFFFFF),
            url = normalizeUrl(githubUrl).ifEmpty { githubUrl }
        ),
        QuickLinkItem(
            type = QuickLinkType.KOFI,
            titleRes = R.string.link_kofi_title,
            subtitleRes = R.string.link_kofi_subtitle,
            iconRes = R.drawable.ic_brand_kofi,
            lightContainerColor = KOFI_LIGHT_CONTAINER,
            darkContainerColor = KOFI_DARK_CONTAINER,
            lightContentColor = KOFI_GLYPH,
            darkContentColor = KOFI_GLYPH,
            url = normalizeUrl(kofiUrl).ifEmpty { kofiUrl }
        )
    )

    fun getVisibleQuickLinks(
        telegramUrl: String = TELEGRAM_URL,
        githubUrl: String = GITHUB_URL,
        kofiUrl: String = KOFI_URL
    ): List<QuickLinkItem> {
        return getAllQuickLinks(telegramUrl, githubUrl, kofiUrl).filter { isValidUrl(it.url) }
    }
}
