package com.github.premtechworks.synqvia.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

object InlineCodeParser {

    /**
     * Parses a string containing inline backtick markdown code spans (e.g. `code`)
     * into an [AnnotatedString] where text inside backticks is styled with [codeStyle].
     *
     * Handles:
     * - No backtick spans: returns literal text without styles.
     * - Single or multiple backtick spans: styles each paired block.
     * - Unmatched backtick: preserves unmatched backtick as literal text without styling.
     */
    fun parse(text: String, codeStyle: SpanStyle): AnnotatedString {
        if (!text.contains('`')) {
            return AnnotatedString(text)
        }

        val builder = AnnotatedString.Builder()
        val parts = text.split('`')

        if (parts.size % 2 == 1) {
            // Even number of backticks (odd number of split parts) -> all matched
            for (i in parts.indices) {
                if (i % 2 == 1) {
                    builder.pushStyle(codeStyle)
                    builder.append(parts[i])
                    builder.pop()
                } else {
                    builder.append(parts[i])
                }
            }
        } else {
            // Unmatched backtick: the last backtick has no closing pair
            val lastPairedIndex = parts.size - 2
            for (i in 0 until lastPairedIndex) {
                if (i % 2 == 1) {
                    builder.pushStyle(codeStyle)
                    builder.append(parts[i])
                    builder.pop()
                } else {
                    builder.append(parts[i])
                }
            }
            builder.append(parts[lastPairedIndex])
            builder.append('`')
            builder.append(parts.last())
        }

        return builder.toAnnotatedString()
    }
}
