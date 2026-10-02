package com.github.premtechworks.synqvia.ui.util

import java.net.URI
import java.util.Locale

object UrlUtil {

    fun normalizeUrl(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.contains('\n') || trimmed.contains(' ')) return null
        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
            else -> return null
        }
        return try {
            val uri = URI(withScheme)
            val host = uri.host ?: return null
            if (host.contains('.') && !host.endsWith('.')) {
                withScheme
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isUrl(text: String): Boolean {
        return normalizeUrl(text) != null
    }

    fun extractHost(text: String): String? {
        val normalized = normalizeUrl(text) ?: return null
        return try {
            val uri = URI(normalized)
            val host = uri.host ?: return null
            val lowerHost = host.lowercase(Locale.ROOT)
            if (lowerHost.startsWith("www.")) {
                lowerHost.substring(4)
            } else {
                lowerHost
            }
        } catch (_: Exception) {
            null
        }
    }
}
