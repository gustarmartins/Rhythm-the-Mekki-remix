package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class BluetoothDisplayDeviceTest {
    private val car = BluetoothDisplayDevice.from("10:20:30:40:50:60", "Stereo")
    private val headphones = BluetoothDisplayDevice.from("10:20:30:40:50:61", "Stereo")

    @Test fun sameNamedDevicesKeepIndependentTiming() {
        val saved = mapOf(car.key to 1_000, headphones.key to -350)
        assertEquals(1_000, bluetoothDisplayProfileValue(car, saved, 0))
        assertEquals(-350, bluetoothDisplayProfileValue(headphones, saved, 0))
    }
    @Test fun renamingADeviceKeepsItsTimingProfile() {
        val renamed = BluetoothDisplayDevice.from("10:20:30:40:50:60", "Renamed stereo")
        assertEquals(car.key, renamed.key)
        assertEquals(900, bluetoothDisplayProfileValue(renamed, mapOf(car.key to 900), 0))
    }
    @Test fun existingNameBasedTimingIsStillReadable() {
        assertEquals(800, bluetoothDisplayProfileValue(car, mapOf("Stereo" to 800), 0))
    }
    @Test fun addressBasedTimingTakesPriorityOverLegacyName() {
        assertEquals(700, bluetoothDisplayProfileValue(car, mapOf(car.key to 700, "Stereo" to 800), 0))
    }
    @Test fun profileReloadDoesNotChangeDeviceIdentity() {
        val recreated = BluetoothDisplayDevice.from("10:20:30:40:50:60", "Stereo")
        assertEquals(car, recreated)
        assertTrue(bluetoothDisplayCompatibilityEnabled(recreated, mapOf(car.key to true), false))
    }
    @Test fun compatibilityIsScopedToOneDevice() {
        val settings = mapOf(car.key to true, headphones.key to false)
        assertTrue(bluetoothDisplayCompatibilityEnabled(car, settings, false))
        assertFalse(bluetoothDisplayCompatibilityEnabled(headphones, settings, true))
    }
    @Test fun speakersNeverUseBluetoothCompatibility() {
        assertFalse(bluetoothDisplayCompatibilityEnabled(null, mapOf(car.key to true), true))
    }
    @Test fun missingAddressUsesNormalizedName() {
        assertEquals(BluetoothDisplayDevice.from(null, " Stereo ").key,
            BluetoothDisplayDevice.from("02:00:00:00:00:00", "Stereo").key)
    }
}
