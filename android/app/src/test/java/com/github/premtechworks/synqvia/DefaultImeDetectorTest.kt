package com.github.premtechworks.synqvia

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.ime.AndroidDefaultImeDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultImeDetectorTest {

    private lateinit var context: Context
    private lateinit var detector: AndroidDefaultImeDetector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        detector = AndroidDefaultImeDetector(context)
    }

    @Test
    fun testWhenSynqviaIsDefaultWithShortClassName_returnsTrue() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            "${context.packageName}/.ime.SynqviaImeService"
        )
        assertTrue(detector.isSynqviaDefaultIme())
    }

    @Test
    fun testWhenSynqviaIsDefaultWithFullClassName_returnsTrue() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            "${context.packageName}/com.github.premtechworks.synqvia.ime.SynqviaImeService"
        )
        assertTrue(detector.isSynqviaDefaultIme())
    }

    @Test
    fun testWhenGboardIsDefault_returnsFalse() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME"
        )
        assertFalse(detector.isSynqviaDefaultIme())
    }

    @Test
    fun testWhenThirdPartyKeyboardIsDefault_returnsFalse() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            "com.touchtype.swiftkey/com.touchtype.KeyboardService"
        )
        assertFalse(detector.isSynqviaDefaultIme())
    }

    @Test
    fun testWhenSettingIsNull_returnsFalse() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            null
        )
        assertFalse(detector.isSynqviaDefaultIme())
    }

    @Test
    fun testWhenSettingIsEmpty_returnsFalse() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            ""
        )
        assertFalse(detector.isSynqviaDefaultIme())
    }

    @Test
    fun testCustomTargetPackageName_matchesSpecifiedPackage() {
        val customDetector = AndroidDefaultImeDetector(context, "com.custom.ime")
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            "com.custom.ime/.CustomService"
        )
        assertTrue(customDetector.isSynqviaDefaultIme())

        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            "${context.packageName}/.ime.SynqviaImeService"
        )
        assertFalse(customDetector.isSynqviaDefaultIme())
    }
}
