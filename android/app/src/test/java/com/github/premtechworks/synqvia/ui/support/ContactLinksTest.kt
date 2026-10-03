package com.github.premtechworks.synqvia.ui.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactLinksTest {

    @Test
    fun isValidUrl_rejectsPlaceholderUrls() {
        assertFalse(ContactLinks.isValidUrl("{{TELEGRAM_URL}}"))
        assertFalse(ContactLinks.isValidUrl("{{KOFI_URL}}"))
        assertFalse(ContactLinks.isValidUrl("https://example.com/{{PLACEHOLDER}}"))
        assertFalse(ContactLinks.isValidUrl(""))
        assertFalse(ContactLinks.isValidUrl("   "))
    }

    @Test
    fun isValidUrl_acceptsValidUrls() {
        assertTrue(ContactLinks.isValidUrl("https://github.com/premtechworks/Synqvia"))
        assertTrue(ContactLinks.isValidUrl("https://t.me/imprem1122"))
        assertTrue(ContactLinks.isValidUrl("t.me/imprem1122"))
        assertTrue(ContactLinks.isValidUrl("{{t.me/imprem1122}}"))
        assertTrue(ContactLinks.isValidUrl("https://ko-fi.com/premkumargara"))
        assertTrue(ContactLinks.isValidUrl("{{https://ko-fi.com/premkumargara}}"))
    }

    @Test
    fun getVisibleQuickLinks_includesAllThreeByDefault() {
        val visibleLinks = ContactLinks.getVisibleQuickLinks()
        // Default TELEGRAM_URL, GITHUB_URL, and KOFI_URL are all valid now
        assertEquals(3, visibleLinks.size)
        assertEquals(QuickLinkType.TELEGRAM, visibleLinks[0].type)
        assertEquals("https://t.me/imprem1122", visibleLinks[0].url)
        assertEquals(QuickLinkType.GITHUB, visibleLinks[1].type)
        assertEquals("https://github.com/premtechworks/Synqvia", visibleLinks[1].url)
        assertEquals(QuickLinkType.KOFI, visibleLinks[2].type)
        assertEquals("https://ko-fi.com/premkumargara", visibleLinks[2].url)
    }

    @Test
    fun getVisibleQuickLinks_hidesPlaceholderRowsWhenConfiguredWithPlaceholders() {
        val visibleLinks = ContactLinks.getVisibleQuickLinks(
            telegramUrl = "{{TELEGRAM_URL}}",
            githubUrl = "https://github.com/premtechworks/Synqvia",
            kofiUrl = "{{KOFI_URL}}"
        )
        assertEquals(1, visibleLinks.size)
        assertEquals(QuickLinkType.GITHUB, visibleLinks.first().type)
    }
}
