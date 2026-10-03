package com.github.premtechworks.synqvia.ui.support

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaqDataTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun allFiveFaqEntriesExist_andHaveNonEmptySteps() {
        val items = FaqData.ITEMS
        assertEquals(5, items.size)

        val expectedIds = listOf(
            "how_to_pair",
            "clipboard_sync",
            "battery_optimization",
            "bluetooth_problems",
            "contribute"
        )
        assertEquals(expectedIds, items.map { it.id })

        items.forEach { item ->
            val title = context.getString(item.titleRes)
            assertTrue("Title should not be blank for ${item.id}", title.isNotBlank())

            val steps = context.resources.getStringArray(item.stepsRes)
            assertTrue("Steps should not be empty for ${item.id}", steps.isNotEmpty())
            assertTrue("Should have at least 3 steps for ${item.id}", steps.size >= 3)
            steps.forEach { step ->
                assertTrue("Step should not be blank for ${item.id}", step.isNotBlank())
            }
        }
    }

    @Test
    fun contributeFaq_hasGitHubActionButton() {
        val contributeItem = FaqData.ITEMS.first { it.id == "contribute" }
        assertNotNull(contributeItem.actionButtonTextRes)
        val buttonText = context.getString(contributeItem.actionButtonTextRes!!)
        assertEquals("View on GitHub", buttonText)
        assertEquals(ContactLinks.GITHUB_URL, contributeItem.actionUrl)
    }

    @Test
    fun nonContributeFaqs_haveNoActionButton() {
        val nonContribute = FaqData.ITEMS.filter { it.id != "contribute" }
        nonContribute.forEach { item ->
            assertEquals(null, item.actionButtonTextRes)
            assertEquals(null, item.actionUrl)
        }
    }
}
