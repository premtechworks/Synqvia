package com.github.premtechworks.synqvia

import android.content.Context
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.ime.ClipImeAdapter
import com.github.premtechworks.synqvia.ime.SynqviaImeService
import com.github.premtechworks.synqvia.ime.SynqviaKeyboardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImeServiceIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testImeServiceLifecycle() {
        val controller = Robolectric.buildService(SynqviaImeService::class.java)
        val service = controller.create().get()

        assertNotNull(service)

        val inputView = service.onCreateInputView()
        assertNotNull(inputView)
        assertTrue(inputView is SynqviaKeyboardView)

        controller.destroy()
    }

    @Test
    fun testImeService_createsKeyboardAndTogglesClipboard() {
        val controller = Robolectric.buildService(SynqviaImeService::class.java)
        val service = controller.create().get()

        val inputView = service.onCreateInputView() as SynqviaKeyboardView
        assertNotNull(inputView)

        val layoutKeys = inputView.findViewById<View>(R.id.layout_keys_container)
        val layoutClipboard = inputView.findViewById<View>(R.id.layout_clipboard_container)

        // Initially keys are visible and clipboard is hidden
        assertFalse(inputView.isClipboardActive)
        assertEquals(View.VISIBLE, layoutKeys.visibility)
        assertEquals(View.GONE, layoutClipboard.visibility)

        // Toggle clipboard panel via toolbar button
        val btnClipboard = inputView.findViewById<View>(R.id.btn_ime_clipboard)
        btnClipboard.performClick()

        assertTrue(inputView.isClipboardActive)
        assertEquals(View.GONE, layoutKeys.visibility)
        assertEquals(View.VISIBLE, layoutClipboard.visibility)

        // Toggle again to return to keys
        btnClipboard.performClick()
        assertFalse(inputView.isClipboardActive)
        assertEquals(View.VISIBLE, layoutKeys.visibility)
        assertEquals(View.GONE, layoutClipboard.visibility)

        controller.destroy()
    }

    @Test
    fun testImeService_windowNavBarConfigured() {
        val controller = Robolectric.buildService(SynqviaImeService::class.java)
        val service = controller.create().get()
        val win = service.window?.window
        if (win != null) {
            service.applyWindowNavBar(win)
            val expectedColor = androidx.core.content.ContextCompat.getColor(service, R.color.ime_background)
            assertEquals(expectedColor, win.navigationBarColor)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                assertFalse(win.isNavigationBarContrastEnforced)
            }
        }
        controller.destroy()
    }

    @Test
    fun testSynqviaKeyboardView_computedHeightMatchesContent() {
        val keyboardView = SynqviaKeyboardView(context)
        val computedHeight = keyboardView.computeContentHeight()
        val keyHeight = keyboardView.getKeyHeight()
        val density = context.resources.displayMetrics.density
        val expectedHeight = (4 * keyHeight) + (3 * (6 * density).toInt()) + (8 * density).toInt() + (8 * density).toInt()

        assertEquals(expectedHeight, computedHeight)
        val contentLayout = keyboardView.findViewById<View>(R.id.layout_ime_content)
        assertEquals(computedHeight, contentLayout.layoutParams.height)
        assertEquals(computedHeight, keyboardView.layoutClipboardContainer.layoutParams.height)
    }

    @Test
    fun testSynqviaKeyboardView_toolbarActiveStates() {
        val keyboardView = SynqviaKeyboardView(context)
        val btnClipboard = keyboardView.findViewById<android.widget.ImageButton>(R.id.btn_ime_clipboard)
        val btnPinned = keyboardView.findViewById<android.widget.ImageButton>(R.id.btn_ime_pinned)
        val btnPicker = keyboardView.findViewById<android.widget.ImageButton>(R.id.btn_ime_picker)

        // Initial state: panel closed, all inactive
        assertFalse(keyboardView.isClipboardActive)
        assertFalse(keyboardView.isPinnedFilterActive)

        // Open clipboard
        btnClipboard.performClick()
        assertTrue(keyboardView.isClipboardActive)
        assertFalse(keyboardView.isPinnedFilterActive)

        // Open pinned filter
        btnPinned.performClick()
        assertTrue(keyboardView.isClipboardActive)
        assertTrue(keyboardView.isPinnedFilterActive)

        // Close panel
        btnClipboard.performClick()
        assertFalse(keyboardView.isClipboardActive)
        assertFalse(keyboardView.isPinnedFilterActive)
    }

    @Test
    fun testImeService_onComputeInsets() {
        val controller = Robolectric.buildService(SynqviaImeService::class.java)
        val service = controller.create().get()
        service.onCreateInputView()

        val insets = android.inputmethodservice.InputMethodService.Insets()
        service.onComputeInsets(insets)

        assertEquals(insets.contentTopInsets, insets.visibleTopInsets)
        controller.destroy()
    }

    @Test
    fun testClipImeAdapter_bindingAndFormatting() {
        var clickedClip: ClipEntity? = null
        var pinnedClip: ClipEntity? = null
        var deletedClip: ClipEntity? = null

        val adapter = ClipImeAdapter(
            onItemClick = { clickedClip = it },
            onItemLongClick = { _, _ -> },
            onPinClick = { pinnedClip = it },
            onDeleteClick = { deletedClip = it }
        )

        val testClip = ClipEntity(
            id = "test-1",
            text = "Clipboard snippet to paste",
            ts = System.currentTimeMillis() - 120_000L, // 2 minutes ago
            src = "android",
            direction = "local",
            pinned = true,
            sensitive = false
        )

        val holder = adapter.createViewHolder(android.widget.FrameLayout(context), 0)
        adapter.submitList(listOf(testClip))

        // Directly bind
        adapter.onBindViewHolder(holder, 0)

        assertEquals("Clipboard snippet to paste", holder.tvText.text.toString())
        assertEquals("2m ago", holder.tvTime.text.toString())
        assertEquals("Local", holder.tvSource.text.toString())

        // Test clicks
        holder.itemView.performClick()
        assertEquals(testClip.id, clickedClip?.id)

        holder.btnPin.performClick()
        assertEquals(testClip.id, pinnedClip?.id)

        holder.btnDelete.performClick()
        assertEquals(testClip.id, deletedClip?.id)
    }

    @Test
    fun testClipImeAdapter_sensitiveContentMasked() {
        val adapter = ClipImeAdapter(
            onItemClick = {},
            onItemLongClick = { _, _ -> },
            onPinClick = {},
            onDeleteClick = {}
        )

        val sensitiveClip = ClipEntity(
            id = "test-sensitive",
            text = "SensitivePassword123!",
            ts = System.currentTimeMillis(),
            src = "android",
            direction = "local",
            pinned = false,
            sensitive = true
        )

        val holder = adapter.createViewHolder(android.widget.FrameLayout(context), 0)
        adapter.submitList(listOf(sensitiveClip))
        adapter.onBindViewHolder(holder, 0)

        // Must display masked placeholder, NOT plaintext password
        assertEquals(context.getString(R.string.ime_sensitive_masked), holder.tvText.text.toString())
    }

    @Test
    fun testInputConnectionPasteSimulation() {
        // Create an editable target simulating focused text field
        val editable = SpannableStringBuilder("Hello ")
        Selection.setSelection(editable, 6)

        val testInputConnection = object : BaseInputConnection(
            View(context), true
        ) {
            override fun getEditable(): android.text.Editable = editable
        }

        // Commit text simulation (analogous to pasteClip)
        val textToPaste = "World from Synqvia!"
        testInputConnection.commitText(textToPaste, 1)

        assertEquals("Hello World from Synqvia!", editable.toString())
    }
}
