package chromahub.rhythm.app.util

import java.io.File
import java.text.Normalizer
import java.util.Locale

/** A missing cover never authorizes using an arbitrary adjacent personal image. */
internal object ArtworkSourcePolicy {
    private val sharedFolders = setOf("music", "download", "downloads", "pictures", "dcim", "camera", "screenshots", "0")
    private val coverNames = listOf("cover", "folder", "front", "albumart")
    private val extensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "tif", "tiff")

    fun findFolderCover(folder: File): File? {
        if (folder.name.lowercase(Locale.ROOT) in sharedFolders) return null
        return folder.listFiles().orEmpty().filter { file ->
            file.isFile && file.extension.lowercase(Locale.ROOT) in extensions &&
                file.nameWithoutExtension.lowercase(Locale.ROOT) in coverNames
        }.sortedWith(compareBy<File> { coverNames.indexOf(it.nameWithoutExtension.lowercase(Locale.ROOT)) }
            .thenBy { it.name.lowercase(Locale.ROOT) }).firstOrNull()
    }

    private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    fun matches(expectedTitle: String, expectedArtist: String, title: String?, artist: String?): Boolean {
        val wantedArtist = normalized(expectedArtist)
        if (wantedArtist in setOf("", "unknown", "unknown artist", "various artists")) return false
        return normalized(expectedTitle).isNotBlank() && normalized(expectedTitle) == normalized(title.orEmpty()) &&
            wantedArtist == normalized(artist.orEmpty())
    }
}
