package com.github.premtechworks.synqvia.ui.haptics

import android.content.Context
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.github.premtechworks.synqvia.ui.motion.SynqviaHaptics

/**
 * Single owner for haptics throughout the app.
 * Wraps View + user's "Haptic Feedback" setting, exposing tick(), confirm(), reject().
 * Implements a safety-net de-bounce ignoring any call within 80ms of the previous one.
 */
class AppHaptics(
    private val view: View? = null,
    private val context: Context? = null,
    private val isHapticsEnabledByUser: () -> Boolean = { true },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val feedbackPerformer: ((Int) -> Boolean)? = null
) : SynqviaHaptics {

    @Volatile
    private var hasEverFired: Boolean = false

    @Volatile
    private var lastHapticTimestamp: Long = 0L

    companion object {
        const val DEBOUNCE_MS = 80L
    }

    private fun isSystemHapticsEnabled(): Boolean {
        val ctx = context ?: view?.context ?: return true
        return try {
            Settings.System.getInt(
                ctx.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
        } catch (_: Exception) {
            true
        }
    }

    private fun canPerform(): Boolean {
        val now = clock()
        // First call always passes; subsequent calls debounced by DEBOUNCE_MS.
        if (hasEverFired && now - lastHapticTimestamp < DEBOUNCE_MS) {
            return false
        }
        if (!isHapticsEnabledByUser() || !isSystemHapticsEnabled()) {
            return false
        }
        lastHapticTimestamp = now
        hasEverFired = true
        return true
    }

    private fun perform(feedbackConstant: Int) {
        if (!canPerform()) return

        if (feedbackPerformer != null) {
            feedbackPerformer.invoke(feedbackConstant)
            return
        }

        val v = view ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            v.performHapticFeedback(feedbackConstant)
        } else {
            v.post { v.performHapticFeedback(feedbackConstant) }
        }
    }

    override fun tick() {
        val constant = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                HapticFeedbackConstants.SEGMENT_TICK
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                HapticFeedbackConstants.CONTEXT_CLICK
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 ->
                HapticFeedbackConstants.TEXT_HANDLE_MOVE
            else ->
                HapticFeedbackConstants.KEYBOARD_TAP
        }
        perform(constant)
    }

    override fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        perform(constant)
    }

    override fun reject() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        perform(constant)
    }
}

val LocalAppHaptics: ProvidableCompositionLocal<SynqviaHaptics> = compositionLocalOf {
    AppHaptics()
}

@Composable
fun rememberAppHaptics(
    userEnabled: Boolean = true
): AppHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, userEnabled) {
        AppHaptics(view = view, context = context, isHapticsEnabledByUser = { userEnabled })
    }
}
