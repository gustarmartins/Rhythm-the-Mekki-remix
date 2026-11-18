/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.io.File

internal object ArtistArtworkFiles {
    fun key(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFKC).trim().lowercase(Locale.ROOT)
    fun fileName(name: String): String {
        val canonical = key(name)
        val hash = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(16)
        val slug = canonical.map { if (it.isLetterOrDigit() || it in "._-") it else '_' }
            .joinToString("").take(40).trim('.', '_').ifBlank { "artist" }
        return "${slug}_$hash.jpg"
    }
    fun legacyName(name: String): String = (name + ".jpg").map {
        if (it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "._-") it else '_'
    }.joinToString("")
    fun legacyOwners(names: List<String>): Map<String, Set<String>> =
        names.groupBy(::legacyName).mapValues { (_, values) -> values.map(::key).toSet() }
    fun canMigrate(name: String, owners: Set<String>): Boolean =
        owners == setOf(key(name)) && legacyName(name).removeSuffix(".jpg").any { it != '_' }
    fun isLegacyFile(name: String, file: File, cacheDirectory: File): Boolean =
        file.parentFile?.absolutePath == cacheDirectory.absolutePath && file.name == legacyName(name)
}
