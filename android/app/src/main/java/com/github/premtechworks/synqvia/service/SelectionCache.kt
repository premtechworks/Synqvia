package com.github.premtechworks.synqvia.service

object SelectionCache {
    private const val FRESHNESS_THRESHOLD_MS = 5000L

    @Volatile
    private var cachedText: String? = null

    @Volatile
    private var cachedTimestamp: Long = 0L

    fun updateSelection(text: String) {
        if (text.isNotBlank()) {
            cachedText = text
            cachedTimestamp = System.currentTimeMillis()
        }
    }

    fun getFreshSelection(): String? {
        val now = System.currentTimeMillis()
        return if (now - cachedTimestamp <= FRESHNESS_THRESHOLD_MS) {
            cachedText
        } else {
            null
        }
    }

    fun clear() {
        cachedText = null
        cachedTimestamp = 0L
    }
}
