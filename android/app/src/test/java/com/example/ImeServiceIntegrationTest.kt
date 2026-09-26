package com.example

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.test.core.app.ApplicationProvider
import com.example.data.ClipEntity
import com.example.ime.ClipImeAdapter
import com.example.ime.MyClipSyncImeService
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
import org.robolectric.shadows.ShadowInputMethodManager

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
        val controller = Robolectric.buildService(MyClipSyncImeService::class.java)
        val service = controller.create().get()

        assertNotNull(service)

        val inputView = service.onCreateInputView()
        assertNotNull(inputView)

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
        val editable = android.text.SpannableStringBuilder("Hello ")
        android.text.Selection.setSelection(editable, 6)
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            initialSelStart = 6
            initialSelEnd = 6
        }

        val testInputConnection = object : android.view.inputmethod.BaseInputConnection(
            android.view.View(context), true
        ) {
            override fun getEditable(): android.text.Editable = editable
        }

        // Commit text simulation (analogous to pasteClip)
        val textToPaste = "World from MyClipSync!"
        testInputConnection.commitText(textToPaste, 1)

        assertEquals("Hello World from MyClipSync!", editable.toString())
    }
}
