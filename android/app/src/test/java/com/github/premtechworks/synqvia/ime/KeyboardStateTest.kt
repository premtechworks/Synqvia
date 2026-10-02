package com.github.premtechworks.synqvia.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardStateTest {

    @Test
    fun testSingleShiftTapAutoRevertsOnCharacterTyped() {
        val state = KeyboardState()
        assertEquals(ShiftState.OFF, state.shiftState)

        val shifted = state.onShiftTap(1000L)
        assertEquals(ShiftState.ON, shifted)

        val reverted = state.onCharacterTyped()
        assertEquals(ShiftState.OFF, reverted)
        assertEquals(ShiftState.OFF, state.shiftState)
    }

    @Test
    fun testDoubleShiftTapWithin300MsLocksCaps() {
        val state = KeyboardState()
        state.onShiftTap(1000L)
        val locked = state.onShiftTap(1200L) // 200ms delta <= 300ms
        assertEquals(ShiftState.CAPS_LOCKED, locked)

        // Typing a character does not turn off CAPS_LOCKED
        val afterTyping = state.onCharacterTyped()
        assertEquals(ShiftState.CAPS_LOCKED, afterTyping)

        // Tapping shift while CAPS_LOCKED unlocks to OFF
        val unlocked = state.onShiftTap(2000L)
        assertEquals(ShiftState.OFF, unlocked)
    }

    @Test
    fun testDoubleShiftTapAfter300MsRevertsToOff() {
        val state = KeyboardState()
        state.onShiftTap(1000L)
        val toggledOff = state.onShiftTap(1400L) // 400ms delta > 300ms
        assertEquals(ShiftState.OFF, toggledOff)
    }

    @Test
    fun testAutoCapsAppliesUnlessCapsLock() {
        val state = KeyboardState()
        assertEquals(ShiftState.ON, state.applyAutoCaps(1))
        assertEquals(ShiftState.OFF, state.applyAutoCaps(0))

        // Lock caps
        state.onShiftTap(1000L)
        state.onShiftTap(1150L)
        assertEquals(ShiftState.CAPS_LOCKED, state.shiftState)

        // auto caps should not downgrade CAPS_LOCKED
        assertEquals(ShiftState.CAPS_LOCKED, state.applyAutoCaps(0))
    }

    @Test
    fun testDoubleSpacePeriodInsertion() {
        val state = KeyboardState()
        val firstSpace = state.onSpaceTap("Hello", 1000L)
        assertEquals(" ", firstSpace.first)
        assertFalse(firstSpace.second)

        val secondSpace = state.onSpaceTap("Hello ", 1200L) // 200ms delta
        assertEquals(". ", secondSpace.first)
        assertTrue(secondSpace.second) // Replace 1 space
    }

    @Test
    fun testDoubleSpaceWithoutPrecedingAlphanumericDoesNotInsertPeriod() {
        val state = KeyboardState()
        val firstSpace = state.onSpaceTap("!!", 1000L)
        assertEquals(" ", firstSpace.first)

        val secondSpace = state.onSpaceTap("!! ", 1200L)
        assertEquals(" ", secondSpace.first)
        assertFalse(secondSpace.second)
    }

    @Test
    fun testBackspaceDeleteCountWithSurrogatePair() {
        // Empty or null
        assertEquals(0, KeyboardState.calculateBackspaceDeleteCount(null))
        assertEquals(0, KeyboardState.calculateBackspaceDeleteCount(""))

        // Single standard character
        assertEquals(1, KeyboardState.calculateBackspaceDeleteCount("a"))
        assertEquals(1, KeyboardState.calculateBackspaceDeleteCount("abc"))

        // Surrogate pair (emoji \uD83D\uDE00)
        val emojiText = "Hello \uD83D\uDE00"
        assertEquals(2, KeyboardState.calculateBackspaceDeleteCount(emojiText))
    }

    @Test
    fun testResolveEnterAction() {
        assertEquals(EnterAction.ACTION_DONE, KeyboardState.resolveEnterAction(EditorInfo.IME_ACTION_DONE))
        assertEquals(EnterAction.ACTION_SEARCH, KeyboardState.resolveEnterAction(EditorInfo.IME_ACTION_SEARCH))
        assertEquals(EnterAction.ACTION_SEND, KeyboardState.resolveEnterAction(EditorInfo.IME_ACTION_SEND))
        assertEquals(EnterAction.ACTION_GO, KeyboardState.resolveEnterAction(EditorInfo.IME_ACTION_GO))
        assertEquals(EnterAction.ACTION_NEXT, KeyboardState.resolveEnterAction(EditorInfo.IME_ACTION_NEXT))

        // Flag no enter action overrides
        val noAction = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        assertEquals(EnterAction.NEWLINE, KeyboardState.resolveEnterAction(noAction))
        assertEquals(EnterAction.NEWLINE, KeyboardState.resolveEnterAction(EditorInfo.IME_ACTION_NONE))
    }

    @Test
    fun testInputTypeDetection() {
        assertTrue(KeyboardState.isNumericInput(InputType.TYPE_CLASS_NUMBER))
        assertTrue(KeyboardState.isNumericInput(InputType.TYPE_CLASS_PHONE))
        assertFalse(KeyboardState.isNumericInput(InputType.TYPE_CLASS_TEXT))

        assertTrue(KeyboardState.isPasswordInput(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertTrue(KeyboardState.isPasswordInput(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
        assertFalse(KeyboardState.isPasswordInput(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL))
    }
}
