/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.core.domain.scan

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import chromahub.rhythm.app.features.local.data.database.entity.SongEntity
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/** Preserve import history without treating a reused row ID or a common filename as identity. */
class StableDateAddedResolver(
    private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
    private val pathExists: (String) -> Boolean = { File(it).exists() }
) {
    constructor(context: Context) : this(context.getSharedPreferences("song_date_added_cache", Context.MODE_PRIVATE))
    private var byId = emptyMap<String, SongEntity>()
    private var byPath = emptyMap<String, SongEntity>()
    private var byMetadata = emptyMap<String, List<SongEntity>>()
    private val claimed = hashSetOf<String>()
    private val pending = linkedMapOf<String, Long>()

    @Synchronized
    fun prepareForScan(songs: List<SongEntity>) {
        byId = songs.associateBy { it.id }
        byPath = songs.filter { !it.path.isNullOrBlank() }.associateBy { normalizePath(it.path) }
        byMetadata = songs.groupBy { metadataKey(it.title, it.artist, it.album, it.duration) }
        claimed.clear()
    }

    @Synchronized
    fun claimExistingSong(id: String) { claimed.add(id) }

    @Synchronized
    fun resolveDateAdded(
        filePath: String?, observedDateAddedMs: Long, songId: String? = null,
        title: String = "", artist: String = "", album: String = "", durationMs: Long = 0
    ): Long {
        val path = normalizePath(filePath)
        val meta = metadataKey(title, artist, album, durationMs)
        fun sameMetadata(song: SongEntity) = meta.isNotEmpty() &&
            metadataKey(song.title, song.artist, song.album, song.duration) == meta && abs(song.duration - durationMs) <= 1000
        val samePath = byPath[path]
        val row = byId[songId]?.takeIf {
            (path.isNotEmpty() && normalizePath(it.path) == path) || sameMetadata(it)
        }
        val atPath = samePath?.takeIf { it.id == songId || sameMetadata(it) }
        // A new copy is a new addition. Only transfer metadata history when exactly one
        // previous owner exists and its original file is actually gone.
        val relocated = if (row == null && atPath == null && meta.isNotEmpty()) {
            byMetadata[meta].orEmpty().filter { it.id !in claimed && sameMetadata(it) }
                .singleOrNull()?.takeIf { old -> old.path?.let { !pathExists(it) } == true }
        } else null
        val matched = row ?: atPath ?: relocated
        matched?.let { claimed.add(it.id) }
        val observed = normalizeTimestamp(observedDateAddedMs).takeIf { it > 0 } ?: now()
        val historical = matched?.dateAdded?.let(::normalizeTimestamp)?.takeIf { it > 0 }
        val key = path.takeIf { it.isNotEmpty() }?.let(::pathKey)
        // If an existing path belongs to a different recording, retire its cached date.
        val cached = if (samePath == null || matched != null) {
            key?.let { pending[it] ?: prefs.getLong(it, -1) }?.let(::normalizeTimestamp)?.takeIf { it > 0 }
        } else null
        val date = listOfNotNull(historical, cached, observed).minOrNull() ?: observed
        if (key != null) pending[key] = date
        return date
    }

    @Synchronized
    fun flush() {
        if (pending.isEmpty()) return
        val writes = pending.toMap()
        prefs.edit {
            writes.forEach { (key, value) -> putLong(key, value) }
            // Bound stale path history; current Room records remain the authoritative dates.
            val old = prefs.all.filterKeys { it.startsWith("date_added_") && it !in writes }
            val excess = (old.size + writes.size - MAX_PATH_HISTORY).coerceAtLeast(0)
            old.entries.sortedBy { (it.value as? Long) ?: 0L }.take(excess).forEach { remove(it.key) }
        }
        pending.keys.removeAll(writes.keys)
    }

    companion object {
        private const val MAX_PATH_HISTORY = 50000
        fun normalizePath(path: String?): String {
            var value = path.orEmpty().trim().replace('\\', '/').trimEnd('/').lowercase(Locale.ROOT)
            for (alias in listOf("/sdcard", "/storage/self/primary")) {
                if (value == alias || value.startsWith("$alias/")) value = "/storage/emulated/0" + value.removePrefix(alias)
            }
            return value
        }
        fun normalizeTimestamp(value: Long): Long = when {
            value <= 0 -> 0
            value < 100_000_000_000L -> value * 1000
            value < 100_000_000_000_000L -> value
            value < 100_000_000_000_000_000L -> value / 1000
            else -> value / 1_000_000
        }
        fun pathKey(path: String): String = "date_added_" + MessageDigest.getInstance("SHA-256")
            .digest(normalizePath(path).toByteArray()).joinToString("") { "%02x".format(it) }
        private fun metadataKey(title: String, artist: String, album: String, duration: Long): String {
            fun norm(s: String) = Normalizer.normalize(s, Normalizer.Form.NFKC).trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
            val t = norm(title); val a = norm(artist)
            if (t in setOf("", "unknown", "unknown title", "<unknown>") || a in setOf("", "unknown", "unknown artist", "<unknown>") || duration <= 0) return ""
            return "$t\u0000$a\u0000${norm(album)}\u0000${duration / 1000}"
        }
    }
}
