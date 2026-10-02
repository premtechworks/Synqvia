package com.github.premtechworks.synqvia.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.github.premtechworks.synqvia.R

enum class ShiftState {
    OFF,
    ON,
    CAPS_LOCKED
}

enum class KeyboardLayer {
    LETTERS,
    SYMBOLS_1,
    SYMBOLS_2
}

enum class EnterAction(val iconRes: Int, val imeAction: Int) {
    NEWLINE(R.drawable.ic_ime_enter, EditorInfo.IME_ACTION_NONE),
    ACTION_DONE(R.drawable.ic_ime_action_done, EditorInfo.IME_ACTION_DONE),
    ACTION_GO(R.drawable.ic_ime_action_next, EditorInfo.IME_ACTION_GO),
    ACTION_NEXT(R.drawable.ic_ime_action_next, EditorInfo.IME_ACTION_NEXT),
    ACTION_SEARCH(R.drawable.ic_ime_action_search, EditorInfo.IME_ACTION_SEARCH),
    ACTION_SEND(R.drawable.ic_ime_action_send, EditorInfo.IME_ACTION_SEND)
}

/**
 * Pure, unit-testable state machine encapsulating keyboard state transitions,
 * shift/caps lock behavior, auto-capitalization, double-space period, surrogate pairs,
 * and input type / action mappings.
 */
class KeyboardState(
    initialShift: ShiftState = ShiftState.OFF,
    initialLayer: KeyboardLayer = KeyboardLayer.LETTERS
) {
    var shiftState: ShiftState = initialShift
        private set

    var layer: KeyboardLayer = initialLayer
        private set

    private var lastShiftTapTime: Long = 0L
    private var lastSpaceTapTime: Long = 0L

    fun setLayer(newLayer: KeyboardLayer) {
        layer = newLayer
    }

    /**
     * Single tap = capitalize next letter (ON).
     * Double tap within 300ms = caps lock (CAPS_LOCKED).
     * Tapping while CAPS_LOCKED or ON (after delay) reverts to OFF.
     */
    fun onShiftTap(now: Long = System.currentTimeMillis()): ShiftState {
        shiftState = when (shiftState) {
            ShiftState.CAPS_LOCKED -> ShiftState.OFF
            ShiftState.ON -> {
                if (now - lastShiftTapTime <= 300L) {
                    ShiftState.CAPS_LOCKED
                } else {
                    ShiftState.OFF
                }
            }
            ShiftState.OFF -> ShiftState.ON
        }
        lastShiftTapTime = now
        return shiftState
    }

    /**
     * Typing a character consumes single-tap shift (ON -> OFF).
     * CAPS_LOCKED stays active.
     */
    fun onCharacterTyped(): ShiftState {
        if (shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }
        return shiftState
    }

    /**
     * Applies auto-capitalization from cursor mode unless locked in caps lock.
     */
    fun applyAutoCaps(capsMode: Int): ShiftState {
        if (shiftState != ShiftState.CAPS_LOCKED) {
            shiftState = if (capsMode != 0) ShiftState.ON else ShiftState.OFF
        }
        return shiftState
    }

    /**
     * Handles space insertion. Double space within 400ms after alphanumeric
     * triggers period insertion (". ").
     * Returns Pair<String to insert, Boolean whether to delete 1 previous space first>.
     */
    fun onSpaceTap(textBeforeCursor: CharSequence?, now: Long = System.currentTimeMillis()): Pair<String, Boolean> {
        if (now - lastSpaceTapTime <= 400L && !textBeforeCursor.isNullOrEmpty() && textBeforeCursor.endsWith(" ")) {
            val preceding = textBeforeCursor.dropLast(1)
            val lastChar = preceding.lastOrNull()
            if (lastChar != null && lastChar.isLetterOrDigit()) {
                lastSpaceTapTime = 0L
                return Pair(". ", true)
            }
        }
        lastSpaceTapTime = now
        return Pair(" ", false)
    }

    companion object {
        /**
         * Calculates number of characters to delete for a single backspace press,
         * accounting for surrogate pairs (e.g. emoji).
         */
        fun calculateBackspaceDeleteCount(textBeforeCursor: CharSequence?): Int {
            if (textBeforeCursor.isNullOrEmpty()) return 0
            if (textBeforeCursor.length >= 2) {
                val c1 = textBeforeCursor[textBeforeCursor.length - 2]
                val c2 = textBeforeCursor[textBeforeCursor.length - 1]
                if (Character.isSurrogatePair(c1, c2)) {
                    return 2
                }
            }
            return 1
        }

        fun isNumericInput(inputType: Int): Boolean {
            val classType = inputType and InputType.TYPE_MASK_CLASS
            return classType == InputType.TYPE_CLASS_NUMBER ||
                    classType == InputType.TYPE_CLASS_PHONE ||
                    classType == InputType.TYPE_CLASS_DATETIME
        }

        fun isPasswordInput(inputType: Int): Boolean {
            val classType = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            return if (classType == InputType.TYPE_CLASS_TEXT) {
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            } else if (classType == InputType.TYPE_CLASS_NUMBER) {
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            } else {
                false
            }
        }

        fun resolveEnterAction(imeOptions: Int): EnterAction {
            val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            if (noEnterAction) return EnterAction.NEWLINE

            return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_DONE -> EnterAction.ACTION_DONE
                EditorInfo.IME_ACTION_GO -> EnterAction.ACTION_GO
                EditorInfo.IME_ACTION_NEXT -> EnterAction.ACTION_NEXT
                EditorInfo.IME_ACTION_SEARCH -> EnterAction.ACTION_SEARCH
                EditorInfo.IME_ACTION_SEND -> EnterAction.ACTION_SEND
                else -> EnterAction.NEWLINE
            }
        }
    }
}
