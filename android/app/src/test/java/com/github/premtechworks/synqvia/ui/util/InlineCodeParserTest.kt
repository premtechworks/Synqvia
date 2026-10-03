package com.github.premtechworks.synqvia.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineCodeParserTest {

    private val testCodeStyle = SpanStyle(color = Color.Red, background = Color.Yellow)

    @Test
    fun parse_noSpans_returnsUnmodifiedTextWithoutStyles() {
        val text = "This is a regular sentence without backticks."
        val annotated = InlineCodeParser.parse(text, testCodeStyle)

        assertEquals(text, annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }

    @Test
    fun parse_singleSpan_appliesStyleToCodePartOnly() {
        val text = "Run `bluetoothctl` in your terminal."
        val annotated = InlineCodeParser.parse(text, testCodeStyle)

        assertEquals("Run bluetoothctl in your terminal.", annotated.text)
        assertEquals(1, annotated.spanStyles.size)
        val span = annotated.spanStyles.first()
        assertEquals(4, span.start)
        assertEquals(16, span.end)
        assertEquals(testCodeStyle, span.item)
    }

    @Test
    fun parse_multipleSpans_appliesStylesToAllPairedBlocks() {
        val text = "Run `bluetoothctl`, then `power on`, `discoverable on` and `pairable on`."
        val annotated = InlineCodeParser.parse(text, testCodeStyle)

        assertEquals("Run bluetoothctl, then power on, discoverable on and pairable on.", annotated.text)
        assertEquals(4, annotated.spanStyles.size)

        assertEquals("bluetoothctl", annotated.text.substring(annotated.spanStyles[0].start, annotated.spanStyles[0].end))
        assertEquals("power on", annotated.text.substring(annotated.spanStyles[1].start, annotated.spanStyles[1].end))
        assertEquals("discoverable on", annotated.text.substring(annotated.spanStyles[2].start, annotated.spanStyles[2].end))
        assertEquals("pairable on", annotated.text.substring(annotated.spanStyles[3].start, annotated.spanStyles[3].end))
    }

    @Test
    fun parse_unmatchedBacktick_preservesLiteralBacktickWithoutStyling() {
        val text = "This has an `unmatched backtick at the end"
        val annotated = InlineCodeParser.parse(text, testCodeStyle)

        assertEquals("This has an `unmatched backtick at the end", annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }

    @Test
    fun parse_multipleSpansWithTrailingUnmatchedBacktick_stylesPairsAndPreservesUnmatched() {
        val text = "Start with `synqvia --show` and then `unclosed"
        val annotated = InlineCodeParser.parse(text, testCodeStyle)

        assertEquals("Start with synqvia --show and then `unclosed", annotated.text)
        assertEquals(1, annotated.spanStyles.size)
        assertEquals("synqvia --show", annotated.text.substring(annotated.spanStyles[0].start, annotated.spanStyles[0].end))
    }

    @Test
    fun parse_emptyString_returnsEmptyAnnotatedString() {
        val annotated = InlineCodeParser.parse("", testCodeStyle)
        assertEquals("", annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }
}
