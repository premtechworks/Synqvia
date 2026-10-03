package com.github.premtechworks.synqvia.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.github.premtechworks.synqvia.data.SyncConfig
import com.github.premtechworks.synqvia.service.SyncConnectionState
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingAndPairScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingContent_rendersHeroElementsAndChips() {
        composeTestRule.setContent {
            SynqviaTheme {
                OnboardingContent(onGetStarted = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Synqvia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clipboard sync between Android and Linux.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline & Private").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text + History").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Internet Needed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_get_started_btn").assertIsDisplayed()
    }

    @Test
    fun pairScreenContent_rendersTargetDeviceAndBeforeYouConnectCard() {
        composeTestRule.setContent {
            SynqviaTheme {
                PairScreenContent(
                    config = SyncConfig(
                        pcMac = "F8:34:41:53:BE:24",
                        channel = 1,
                        historyCap = 500,
                        deviceName = "prem-pc",
                        deviceId = "android-device"
                    ),
                    connectionState = SyncConnectionState.Connected(
                        peerName = "prem-pc",
                        mac = "F8:34:41:53:BE:24"
                    ),
                    onBack = {},
                    onPairAndConnect = { _, _ -> },
                    previewPairedDevices = listOf(
                        PairedDeviceItem("prem-pc", "F8:34:41:53:BE:24")
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Pair with your Linux PC").assertIsDisplayed()
        composeTestRule.onNodeWithText("prem-pc").assertIsDisplayed()
        composeTestRule.onNodeWithText("F8:34:41:53:BE:24").assertIsDisplayed()

        // Verify "Before you connect" card and its 3 status rows
        composeTestRule.onNodeWithText("Before you connect").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bluetooth on").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Synqvia running on PC").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("PC paired with phone").performScrollTo().assertIsDisplayed()
    }
}
