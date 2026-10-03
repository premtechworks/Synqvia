package com.github.premtechworks.synqvia.ui.util

import android.os.SystemClock
import android.util.Log
import com.github.premtechworks.synqvia.ui.MainTab

object TabLatencyTracker {
    private const val TAG = "TabLatencyTracker"
    private var pendingTab: MainTab? = null
    private var clickTimeMs: Long = 0L

    fun onTabClicked(tab: MainTab) {
        pendingTab = tab
        clickTimeMs = SystemClock.uptimeMillis()
    }

    fun onTabDrawn(tab: MainTab) {
        if (pendingTab == tab) {
            val latency = SystemClock.uptimeMillis() - clickTimeMs
            Log.d(TAG, "Tab ${tab.name} drawn in ${latency}ms")
            pendingTab = null
        }
    }
}
