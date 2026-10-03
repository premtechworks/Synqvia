package com.github.premtechworks.synqvia.ime

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.github.premtechworks.synqvia.R

/**
 * Palette representing the keyboard theme tokens (matching L1 SynqviaColors keyboard tokens).
 */
data class KeyboardPalette(
    val isDark: Boolean,
    @StyleRes val themeResId: Int,
    @ColorInt val keyboardBg: Int,
    @ColorInt val keyBg: Int,
    @ColorInt val keyPressed: Int,
    @ColorInt val keyFunction: Int,
    @ColorInt val keyText: Int,
    @ColorInt val keyHint: Int,
    @ColorInt val enterBg: Int,
    @ColorInt val enterIcon: Int,
    @ColorInt val toolbarBg: Int,
    @ColorInt val toolbarIconInactive: Int,
    @ColorInt val toolbarActiveBg: Int,
    @ColorInt val toolbarActiveIcon: Int,
    @ColorInt val divider: Int,
    @ColorInt val cardBg: Int,
    @ColorInt val panelBg: Int,
    @ColorInt val outline: Int,
    @ColorInt val textSecondary: Int,
    @ColorInt val textTertiary: Int,
    @ColorInt val pillFromPcBg: Int,
    @ColorInt val pillFromPcFg: Int,
    @ColorInt val pillToPcBg: Int,
    @ColorInt val pillToPcFg: Int,
    @ColorInt val pinGold: Int,
    @ColorInt val keyShadow: Int
) {
    companion object {
        val Dark = KeyboardPalette(
            isDark = true,
            themeResId = R.style.Theme_Synqvia_Ime_Dark,
            keyboardBg = 0xFF050B18.toInt(),
            keyBg = 0xFF15233B.toInt(),
            keyPressed = 0xFF1E3254.toInt(),
            keyFunction = 0xFF101B2F.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            keyHint = 0xFF5F7191.toInt(),
            enterBg = 0xFF12D4FF.toInt(),
            enterIcon = 0xFF04111F.toInt(),
            toolbarBg = 0xFF081020.toInt(),
            toolbarIconInactive = 0xFF8FA0BC.toInt(),
            toolbarActiveBg = 0x2412D4FF.toInt(),
            toolbarActiveIcon = 0xFF12D4FF.toInt(),
            divider = 0x0FFFFFFF.toInt(),
            cardBg = 0xFF101B2F.toInt(),
            panelBg = 0xFF0B1426.toInt(),
            outline = 0xFF1F2D47.toInt(),
            textSecondary = 0xFF8FA0BC.toInt(),
            textTertiary = 0xFF7A8CAB.toInt(),
            pillFromPcBg = 0x242BD97E.toInt(),
            pillFromPcFg = 0xFF2BD97E.toInt(),
            pillToPcBg = 0x243D8BFF.toInt(),
            pillToPcFg = 0xFF3D8BFF.toInt(),
            pinGold = 0xFFFFC21A.toInt(),
            keyShadow = 0x00000000
        )

        val Light = KeyboardPalette(
            isDark = false,
            themeResId = R.style.Theme_Synqvia_Ime_Light,
            keyboardBg = 0xFFE8EDF5.toInt(),
            keyBg = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFDCE6F5.toInt(),
            keyFunction = 0xFFD5DEEB.toInt(),
            keyText = 0xFF0B1B33.toInt(),
            keyHint = 0xFF7A8AA3.toInt(),
            enterBg = 0xFF1F6FEB.toInt(),
            enterIcon = 0xFFFFFFFF.toInt(),
            toolbarBg = 0xFFFFFFFF.toInt(),
            toolbarIconInactive = 0xFF55657F.toInt(),
            toolbarActiveBg = 0xFFE3EEFF.toInt(),
            toolbarActiveIcon = 0xFF1F6FEB.toInt(),
            divider = 0xFFE1E9F5.toInt(),
            cardBg = 0xFFFFFFFF.toInt(),
            panelBg = 0xFFF4F7FC.toInt(),
            outline = 0xFFE1E9F5.toInt(),
            textSecondary = 0xFF55657F.toInt(),
            textTertiary = 0xFF62738F.toInt(),
            pillFromPcBg = 0xFFE3F6EC.toInt(),
            pillFromPcFg = 0xFF12804A.toInt(),
            pillToPcBg = 0xFFE4EEFF.toInt(),
            pillToPcFg = 0xFF1F6FEB.toInt(),
            pinGold = 0xFFF5B000.toInt(),
            keyShadow = 0x2E1F2F4D.toInt()
        )

        fun forTheme(isDark: Boolean): KeyboardPalette = if (isDark) Dark else Light

        fun resolve(context: Context, themeMode: String): KeyboardPalette {
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> {
                    // System theme: check uiMode night flag
                    val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    nightMode == Configuration.UI_MODE_NIGHT_YES
                }
            }
            return forTheme(isDark)
        }

        fun resolveThemeColor(context: Context, @AttrRes attrResId: Int, defaultColor: Int = Color.BLACK): Int {
            val typedValue = TypedValue()
            return if (context.theme.resolveAttribute(attrResId, typedValue, true)) {
                typedValue.data
            } else {
                defaultColor
            }
        }

        fun ensureThemedContext(context: Context): Context {
            val typedValue = TypedValue()
            return if (context.theme.resolveAttribute(R.attr.synqKbBg, typedValue, true)) {
                context
            } else {
                android.view.ContextThemeWrapper(context, Dark.themeResId)
            }
        }
    }
}
