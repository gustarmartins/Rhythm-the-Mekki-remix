package chromahub.rhythm.app.features.local.presentation.screens.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingPermissionRecoveryTest {
    @Test fun configuredUserStaysInAppThroughPermissionRevocationAndRestoration() {
        var step = OnboardingStep.COMPLETE
        for (access in listOf(true, false, false, true)) {
            step = onboardingStepAfterPermissionCheck(true, step, access)
            assertEquals(OnboardingStep.COMPLETE, step)
        }
    }

    @Test fun switchingFromGoToLocalWithoutPermissionDoesNotRestartSetup() {
        val goStep = onboardingStepAfterPermissionCheck(true, OnboardingStep.COMPLETE, true)
        val localStep = onboardingStepAfterPermissionCheck(true, goStep, false)
        assertEquals(OnboardingStep.COMPLETE, localStep)
    }

    @Test fun newUserStillMustGrantPermissionAndExplicitlyContinueSetup() {
        val denied = onboardingStepAfterPermissionCheck(false, OnboardingStep.PERMISSIONS, false)
        val granted = onboardingStepAfterPermissionCheck(false, denied, true)
        assertEquals(OnboardingStep.PERMISSIONS, denied)
        assertEquals(OnboardingStep.PERMISSIONS, granted)
        assertEquals(OnboardingStep.MEDIA_SCAN,
            onboardingStepAfterPermissionCheck(false, OnboardingStep.MEDIA_SCAN, true))
    }
}
