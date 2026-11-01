package chromahub.rhythm.app.util

import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** Download into a new preview file, never the audio file or an existing artwork selection. */
internal fun downloadArtworkPreview(client: OkHttpClient, url: String, destination: File) {
    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Artwork download failed (${response.code})")
        val source = response.body.source()
        if (source.request(16L * 1024 * 1024 + 1)) throw IOException("Artwork exceeds 16 MiB")
        val bytes = source.readByteArray()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Downloaded artwork is not an image")
        destination.writeBytes(bytes)
    }
}
