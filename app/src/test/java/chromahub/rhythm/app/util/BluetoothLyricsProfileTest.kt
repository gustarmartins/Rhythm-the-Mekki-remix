package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.BluetoothLyricsTextMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

class BluetoothLyricsProfileTest {
    private val qcy = BluetoothDisplayDevice.from("10:20:30:40:50:60", "QCY H3")
    private val car = BluetoothDisplayDevice.from("10:20:30:40:50:61", "Citroën")
    private val headphones = BluetoothLyricsProfile(500, 16, 2, 2000, 1000)
    private val driving = BluetoothLyricsProfile(-1200, 26, 0, 1250, 600, BluetoothLyricsTextMode.ORIGINAL)

    @Test fun qcyAndCarRetainEveryParameterThroughSerializedReload() {
        val type = object : TypeToken<Map<String, BluetoothLyricsProfile>>() {}.type
        val persisted = Gson().toJson(mapOf(qcy.key to headphones, car.key to driving))
        val loaded = Gson().fromJson<Map<String, BluetoothLyricsProfile>>(persisted, type)
        assertEquals(headphones, resolveBluetoothLyricsProfile(qcy, loaded, driving, emptyMap()))
        assertEquals(driving, resolveBluetoothLyricsProfile(car, loaded, headphones, emptyMap()))
    }

    @Test fun connectedButUnselectedHeadphonesDoNotOverrideCar() {
        val profiles = mapOf(qcy.key to headphones, car.key to driving)
        assertEquals(driving, resolveBluetoothLyricsProfile(car, profiles, headphones, emptyMap()))
    }

    @Test fun oldTimingAndUnsavedGlobalParametersSurviveUpgrade() {
        val defaults = headphones.copy(offsetMs = 0)
        assertEquals(defaults.copy(offsetMs = -1500),
            resolveBluetoothLyricsProfile(car, emptyMap(), defaults, mapOf(car.name to -1500)))
    }

    @Test fun fullProfileWinsOverOldTimingAndDeviceRename() {
        val renamed = car.copy(name = "Renamed")
        assertEquals(driving, resolveBluetoothLyricsProfile(renamed, mapOf(car.key to driving),
            headphones, mapOf(car.key to 900)))
    }

    @Test fun nonBluetoothDisplayUsesDefaultsWithoutCarOffset() {
        assertEquals(headphones.copy(offsetMs = 0), resolveBluetoothLyricsProfile(null,
            mapOf(car.key to driving), headphones, emptyMap()))
    }

    @Test fun editingCarDoesNotModifyUnsavedHeadphoneDefaults() {
        assertEquals(headphones, resolveBluetoothLyricsProfile(qcy,
            mapOf(car.key to driving), headphones, mapOf(qcy.key to 500)))
    }
}
