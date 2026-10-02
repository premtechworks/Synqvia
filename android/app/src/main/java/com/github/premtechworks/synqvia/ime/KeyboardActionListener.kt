package com.github.premtechworks.synqvia.ime

/**
 * Action listener driving IME InputConnection or in-app preview text manipulation.
 */
interface KeyboardActionListener {
    fun onText(text: String)
    fun onBackspace()
    fun onBackspaceRepeat()
    fun onEnter()
    fun onShiftChanged(state: ShiftState)
    fun onLayerChanged(layer: KeyboardLayer)
    fun onToggleClipboard()
    fun onToolbar2()
    fun onOpenKeyboardPicker()
}
