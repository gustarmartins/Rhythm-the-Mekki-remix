package chromahub.rhythm.app.util

import android.graphics.Bitmap
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtworkPreviewDownloadTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun validImageBytesReachOnlyThePreviewFile() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray()
        bitmap.recycle()
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().body(Buffer().write(bytes)).build())
            server.start()
            val destination = temporary.newFile()
            downloadArtworkPreview(OkHttpClient(), server.url("/art").toString(), destination)
            assertArrayEquals(bytes, destination.readBytes())
        }
    }

    @Test fun htmlAndHttpErrorsNeverBecomeASuccessfulArtworkPreview() {
        for (response in listOf(
            MockResponse.Builder().body("<html>Unavailable</html>").build(),
            MockResponse.Builder().code(404).body("missing").build(),
            MockResponse.Builder().body("").build()
        )) {
            MockWebServer().use { server ->
                server.enqueue(response)
                server.start()
                val destination = temporary.newFile().apply { writeText("existing selection") }
                assertThrows(IOException::class.java) {
                    downloadArtworkPreview(OkHttpClient(), server.url("/art").toString(), destination)
                }
                assertEquals("existing selection", destination.readText())
            }
        }
    }

    @Test fun oversizedResponseIsRejectedBeforeWriting() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().body(Buffer().write(ByteArray(16 * 1024 * 1024 + 1))).build())
            server.start()
            val destination = temporary.newFile()
            assertThrows(IOException::class.java) {
                downloadArtworkPreview(OkHttpClient(), server.url("/art").toString(), destination)
            }
            assertEquals(0L, destination.length())
        }
    }
}
