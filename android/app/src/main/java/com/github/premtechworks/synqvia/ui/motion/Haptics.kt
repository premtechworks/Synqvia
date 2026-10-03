package com.github.premtechworks.synqvia.ui.motion

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

interface SynqviaHaptics {
    fun tick()
    fun confirm()
    fun reject()
}

/**
 * CompositionLocal providing SynqviaHaptics instance.
 */
val LocalAppHaptics: ProvidableCompositionLocal<SynqviaHaptics> = compositionLocalOf {
    object : SynqviaHaptics {
        override fun tick() {}
        override fun confirm() {}
        override fun reject() {}
    }
}

class SynqviaHapticsImpl(
    private val view: View,
    private val context: Context,
    private val isHapticsEnabledByUser: () -> Boolean
) : SynqviaHaptics {

    private fun isSystemHapticsEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
        } catch (_: Exception) {
            true
        }
    }

    private fun canPerform(): Boolean {
        return isSystemHapticsEnabled() && isHapticsEnabledByUser()
    }

    override fun tick() {
        if (!canPerform()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    override fun confirm() {
        if (!canPerform()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    override fun reject() {
        if (!canPerform()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}

@Composable
fun rememberSynqviaHaptics(
    userEnabled: Boolean = true
): SynqviaHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, userEnabled) {
        com.github.premtechworks.synqvia.ui.haptics.AppHaptics(
            view = view,
            context = context,
            isHapticsEnabledByUser = { userEnabled }
        )
    }
}
