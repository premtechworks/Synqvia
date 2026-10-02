package com.github.premtechworks.synqvia.ui

import androidx.test.core.app.ApplicationProvider
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.data.SyncPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationStartDestinationTest {

    private lateinit var app: SynqviaApp
    private lateinit var syncPreferences: SyncPreferences

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        syncPreferences = app.container.syncPreferences
    }

    @Test
    fun resolveStartDestination_whenOnboardingNotDone_alwaysReturnsOnboarding() {
        assertEquals(AppRoutes.ONBOARDING, AppRoutes.resolveStartDestination(onboardingDone = false, pcMac = null))
        assertEquals(AppRoutes.ONBOARDING, AppRoutes.resolveStartDestination(onboardingDone = false, pcMac = ""))
        assertEquals(AppRoutes.ONBOARDING, AppRoutes.resolveStartDestination(onboardingDone = false, pcMac = "   "))
        assertEquals(
            AppRoutes.ONBOARDING,
            AppRoutes.resolveStartDestination(onboardingDone = false, pcMac = "F8:34:41:53:BE:24")
        )
    }

    @Test
    fun resolveStartDestination_whenOnboardingDoneAndNoMac_returnsPair() {
        assertEquals(AppRoutes.PAIR, AppRoutes.resolveStartDestination(onboardingDone = true, pcMac = null))
        assertEquals(AppRoutes.PAIR, AppRoutes.resolveStartDestination(onboardingDone = true, pcMac = ""))
        assertEquals(AppRoutes.PAIR, AppRoutes.resolveStartDestination(onboardingDone = true, pcMac = "   "))
    }

    @Test
    fun resolveStartDestination_whenOnboardingDoneAndHasMac_returnsMain() {
        assertEquals(
            AppRoutes.MAIN,
            AppRoutes.resolveStartDestination(onboardingDone = true, pcMac = "F8:34:41:53:BE:24")
        )
    }

    @Test
    fun viewModel_initialRoute_matchesPreferencesMatrix() {
        // Scenario 1: First launch (onboarding not done)
        syncPreferences.onboardingDone = false
        syncPreferences.updateConfig(syncPreferences.getConfig().copy(pcMac = "F8:34:41:53:BE:24"))
        val vm1 = MainViewModel(
            application = app,
            clipRepository = app.container.clipRepository,
            syncPreferences = syncPreferences,
            defaultImeDetector = app.container.defaultImeDetector
        )
        assertEquals(AppRoutes.ONBOARDING, vm1.currentRoute.value)

        // Scenario 2: Onboarding done, but no PC MAC
        syncPreferences.onboardingDone = true
        syncPreferences.updateConfig(syncPreferences.getConfig().copy(pcMac = ""))
        val vm2 = MainViewModel(
            application = app,
            clipRepository = app.container.clipRepository,
            syncPreferences = syncPreferences,
            defaultImeDetector = app.container.defaultImeDetector
        )
        assertEquals(AppRoutes.PAIR, vm2.currentRoute.value)

        // Scenario 3: Onboarding done with PC MAC configured
        syncPreferences.onboardingDone = true
        syncPreferences.updateConfig(syncPreferences.getConfig().copy(pcMac = "F8:34:41:53:BE:24"))
        val vm3 = MainViewModel(
            application = app,
            clipRepository = app.container.clipRepository,
            syncPreferences = syncPreferences,
            defaultImeDetector = app.container.defaultImeDetector
        )
        assertEquals(AppRoutes.MAIN, vm3.currentRoute.value)
    }

    @Test
    fun viewModel_navigationFlowAndBackHandler() {
        syncPreferences.onboardingDone = true
        syncPreferences.updateConfig(syncPreferences.getConfig().copy(pcMac = "F8:34:41:53:BE:24"))
        val vm = MainViewModel(
            application = app,
            clipRepository = app.container.clipRepository,
            syncPreferences = syncPreferences,
            defaultImeDetector = app.container.defaultImeDetector
        )

        // Start at MAIN
        assertEquals(AppRoutes.MAIN, vm.currentRoute.value)

        // Navigate to PAIR (e.g., from Sync chevron click)
        vm.navigateTo(AppRoutes.PAIR)
        assertEquals(AppRoutes.PAIR, vm.currentRoute.value)

        // Navigate back should return to MAIN
        vm.navigateBack()
        assertEquals(AppRoutes.MAIN, vm.currentRoute.value)

        // Set onboarding done helper updates preferences
        syncPreferences.onboardingDone = false
        assertFalse(syncPreferences.onboardingDone)
        vm.setOnboardingDone(true)
        assertTrue(syncPreferences.onboardingDone)
    }
}
