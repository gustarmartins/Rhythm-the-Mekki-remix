package chromahub.rhythm.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStartPolicyTest {
    @Test
    fun nullActionIsOwnedByMedia3WithoutPlaceholderPromotion() {
        assertFalse(ServiceStartPolicy.requiresManualForegroundPromotion(null))
    }

    @Test
    fun explicitAppCommandRequiresImmediateForegroundPromotion() {
        assertTrue(
            ServiceStartPolicy.requiresManualForegroundPromotion(
                "chromahub.rhythm.app.action.PLAY_PAUSE",
            ),
        )
    }
}
