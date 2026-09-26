package com.github.premtechworks.synqvia.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClipAccessService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                    // Extract selected text from event if present
                    val eventText = event.text?.joinToString("") ?: ""
                    if (eventText.isNotBlank()) {
                        SelectionCache.updateSelection(eventText)
                    } else {
                        // Attempt to extract selected text from source node
                        event.source?.let { node ->
                            extractSelectionFromNode(node)
                        }
                    }
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    val text = event.text?.joinToString("") ?: ""
                    if (text.isNotBlank()) {
                        SelectionCache.updateSelection(text)
                    }
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    event.source?.let { node ->
                        extractSelectionFromNode(node)
                    }
                }
                else -> Unit
            }
        } catch (_: Exception) {
            // Defensive against potential recycling exceptions
        }
    }

    private fun extractSelectionFromNode(node: AccessibilityNodeInfo) {
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        val text = node.text?.toString() ?: ""

        if (start in 0 until end && end <= text.length) {
            val selected = text.substring(start, end)
            if (selected.isNotBlank()) {
                SelectionCache.updateSelection(selected)
            }
        }
    }

    override fun onInterrupt() {
        SelectionCache.clear()
    }
}
