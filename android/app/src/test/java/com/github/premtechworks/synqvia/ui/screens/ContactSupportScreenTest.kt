package com.github.premtechworks.synqvia.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.ui.support.ContactLinks
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactSupportScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun contactSupportContent_rendersCardsAndRows() {
        var openedUrl: String? = null
        val links = ContactLinks.getAllQuickLinks(
            telegramUrl = "https://t.me/synqvia",
            githubUrl = "https://github.com/premtechworks/Synqvia",
            kofiUrl = "https://ko-fi.com/synqvia"
        )

        composeTestRule.setContent {
            SynqviaTheme {
                ContactSupportContent(
                    visibleQuickLinks = links,
                    isScrolled = false,
                    scrollState = rememberScrollState(),
                    onBack = {},
                    onOpenUrl = { openedUrl = it }
                )
            }
        }

        // Developer card elements
        composeTestRule.onNodeWithText("DEVELOPER").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prem").assertIsDisplayed()
        composeTestRule.onNodeWithText("Developer · Synqvia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Synqvia is developed and maintained by Prem as an open-source project. Questions, feedback and contributions are welcome.").assertIsDisplayed()

        // Quick links rows
        composeTestRule.onNodeWithText("Join on Telegram").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("View on GitHub").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Support on Ko-fi").performScrollTo().assertIsDisplayed()

        // About card rows
        composeTestRule.onNodeWithText("ABOUT SYNQVIA").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Version").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("v1.0.0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("License").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("GPL-3.0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Repository").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("premtechworks/Synqvia").performScrollTo().assertIsDisplayed()

        // Click repository row
        composeTestRule.onNodeWithText("Repository").performScrollTo().performClick()
        assertEquals(ContactLinks.GITHUB_URL, openedUrl)
    }

    @Test
    fun settingsScreen_navigatesToContactSupportAndBack() {
        var isSupportOpen by mutableStateOf(false)

        composeTestRule.setContent {
            SynqviaTheme {
                if (isSupportOpen) {
                    ContactSupportContent(
                        visibleQuickLinks = ContactLinks.getVisibleQuickLinks(),
                        isScrolled = false,
                        scrollState = rememberScrollState(),
                        onBack = { isSupportOpen = false },
                        onOpenUrl = {}
                    )
                } else {
                    SettingsScreenContent(
                        config = SyncConfig(
                            pcMac = "F8:34:41:53:BE:24",
                            channel = 1,
                            historyCap = 500,
                            deviceName = "prem-pc",
                            deviceId = "device-1"
                        ),
                        logs = emptyList(),
                        autoSync = true,
                        syncTextOnly = true,
                        clearOnDisconnect = false,
                        themeMode = "system",
                        onMacChange = { true },
                        onChannelChange = {},
                        onDeviceNameChange = {},
                        onHistoryCapChange = {},
                        onAutoSyncChange = {},
                        onSyncTextOnlyChange = {},
                        onClearOnDisconnectChange = {},
                        onThemeModeChange = {},
                        onOpenContactSupport = { isSupportOpen = true },
                        onCopyMac = {},
                        onCopyLogs = {},
                        onClearLogs = {}
                    )
                }
            }
        }

        // On Settings root: verify Settings headline and Contact & Support entry card
        composeTestRule.onNodeWithText("Connection & Device").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_contact_support_card").performScrollTo().performClick()

        // After click: Contact & Support is open
        assertTrue(isSupportOpen)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Contact & Support").assertIsDisplayed()
        composeTestRule.onNodeWithText("DEVELOPER").assertIsDisplayed()

        // Click back icon
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()

        // After back: returns to Settings
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Connection & Device").assertIsDisplayed()
    }
}
