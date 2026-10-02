package com.github.premtechworks.synqvia.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilTest {

    @Test
    fun testIsUrl_validUrls() {
        assertTrue(UrlUtil.isUrl("https://github.com"))
        assertTrue(UrlUtil.isUrl("https://github.com/premtechworks/synqvia"))
        assertTrue(UrlUtil.isUrl("http://example.org:8080/path?q=1&b=2#section"))
        assertTrue(UrlUtil.isUrl("https://www.google.com/search?q=android"))
        assertTrue(UrlUtil.isUrl("www.google.com"))
    }

    @Test
    fun testIsUrl_invalidUrls() {
        assertFalse(UrlUtil.isUrl("Hello world"))
        assertFalse(UrlUtil.isUrl(""))
        assertFalse(UrlUtil.isUrl("   "))
        assertFalse(UrlUtil.isUrl("not_a_url"))
        assertFalse(UrlUtil.isUrl("https://"))
        assertFalse(UrlUtil.isUrl("http://"))
        assertFalse(UrlUtil.isUrl("user@example.com"))
        assertFalse(UrlUtil.isUrl("multi\nline\ntext"))
    }

    @Test
    fun testExtractHost() {
        assertEquals("github.com", UrlUtil.extractHost("https://github.com/premtechworks/synqvia"))
        assertEquals("google.com", UrlUtil.extractHost("https://www.google.com/search?q=android"))
        assertEquals("google.com", UrlUtil.extractHost("www.google.com"))
        assertEquals("developer.android.com", UrlUtil.extractHost("https://developer.android.com/jetpack/compose"))
        assertEquals("sub.domain.co.uk", UrlUtil.extractHost("https://www.sub.domain.co.uk/test"))
        assertNull(UrlUtil.extractHost("Just some random text"))
    }
}
