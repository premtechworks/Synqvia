package com.github.premtechworks.synqvia.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupStateTest {

    @Test
    fun resolveSetupCompletionState_allRequiredGranted_returnsAllSet() {
        val state = resolveSetupCompletionState(
            btGranted = true,
            notificationGranted = true,
            batteryGranted = true
        )

        assertTrue(state.isAllRequiredGranted)
        assertEquals(0, state.missingRequiredCount)
        assertEquals("All set! You're ready to sync", state.title)
        assertEquals("All required permissions are configured.", state.subtitle)
    }

    @Test
    fun resolveSetupCompletionState_oneMissing_returnsOneStepRemaining() {
        val state = resolveSetupCompletionState(
            btGranted = true,
            notificationGranted = false,
            batteryGranted = true
        )

        assertFalse(state.isAllRequiredGranted)
        assertEquals(1, state.missingRequiredCount)
        assertEquals("1 required step remaining", state.title)
        assertEquals("Configure required permissions to enable sync.", state.subtitle)
    }

    @Test
    fun resolveSetupCompletionState_threeMissing_returnsThreeStepsRemaining() {
        val state = resolveSetupCompletionState(
            btGranted = false,
            notificationGranted = false,
            batteryGranted = false
        )

        assertFalse(state.isAllRequiredGranted)
        assertEquals(3, state.missingRequiredCount)
        assertEquals("3 required steps remaining", state.title)
        assertEquals("Configure required permissions to enable sync.", state.subtitle)
    }

    @Test
    fun resolveImeSetupState_whenDefault_returnsActive() {
        val state = resolveImeSetupState(isDefaultIme = true, isImeEnabled = true)

        assertTrue(state.isGranted)
        assertEquals("Synqvia is your default keyboard. Automatic capture is running.", state.description)
        assertEquals("", state.actionLabel)
    }

    @Test
    fun resolveImeSetupState_whenEnabledNotDefault_returnsSetDefault() {
        val state = resolveImeSetupState(isDefaultIme = false, isImeEnabled = true)

        assertFalse(state.isGranted)
        assertEquals("Synqvia is enabled, but not set as the default keyboard. Automatic capture requires Synqvia to be the default.", state.description)
        assertEquals("Set Default", state.actionLabel)
    }

    @Test
    fun resolveImeSetupState_whenNotEnabled_returnsEnable() {
        val state = resolveImeSetupState(isDefaultIme = false, isImeEnabled = false)

        assertFalse(state.isGranted)
        assertEquals("Enable the Synqvia keyboard in system settings to use the clipboard panel.", state.description)
        assertEquals("Enable", state.actionLabel)
    }
}
