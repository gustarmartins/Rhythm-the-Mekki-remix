package chromahub.rhythm.app.util

import chromahub.rhythm.app.network.RhythmLyricsLine
import chromahub.rhythm.app.shared.data.model.LyricsData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Rejects "synced" provider/cache payloads whose timestamps cannot drive playback,
 * and rescales lyrics fetched from normal-speed catalog tracks to match tempo-edited
 * (e.g. Nightcore / sped-up / slowed) local audio durations.
 */
object LyricsTimingPolicy {
    private val gson = Gson()
    private val tempoEditRegex = Regex(
        "(?i)(?:\\b(?:nightcore|nightstep|daycore|zcore|bakaricore|s3rl|nc)\\b|spe(?:e)?d\\s*up|slowed)"
    )
    private val lrcLengthHeaderRegex = Regex(
        "(?i)\\[length\\s*:\\s*(\\d+)\\s*[:.]\\s*(\\d+(?:[.:]\\d+)?)\\s*]"
    )
    private val lrcLineTimestampRegex = Regex(
        "\\[(\\d+)[.:](\\d+)([.:]\\d+)?(?:-(\\d+)[.:](\\d+)([.:]\\d+)?)?]"
    )
    private val lrcWordTimestampRegex = Regex(
        "<(\\d+)[.:](\\d+)([.:]\\d+)?>"
    )

    fun hasUsableTimeline(timestamps: List<Long>): Boolean {
        if (timestamps.isEmpty()) return false
        if (timestamps.size == 1) return timestamps.single() >= 0L

        val normalized = timestamps.map { it.coerceAtLeast(0L) }
        return normalized.distinct().size > 1 && normalized.maxOrNull()!! > 0L
    }

    fun isTempoEditedTrack(title: String?, album: String?, pathOrUri: String? = null): Boolean {
        return listOfNotNull(title, album, pathOrUri).any { tempoEditRegex.containsMatchIn(it) }
    }

    fun extractLrcLengthHeaderMs(lrc: String?): Long? {
        if (lrc.isNullOrBlank()) return null
        val match = lrcLengthHeaderRegex.find(lrc) ?: return null
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].replace(':', '.').toDoubleOrNull() ?: return null
        return (minutes * 60_000L + (seconds * 1000.0).roundToLong()).takeIf { it > 0L }
    }

    private data class TimelineStats(
        val maxTimelineMs: Long,
        val trailingEmptyMarkerMs: Long?
    )

    private fun parseTimePartsMs(minStr: String, secStr: String, fracStr: String): Long {
        val minutes = minStr.toLongOrNull() ?: 0L
        val seconds = (secStr + fracStr.replace(':', '.')).toDoubleOrNull() ?: 0.0
        return minutes * 60_000L + (seconds * 1000.0).roundToLong()
    }

    private fun formatTimeMs(ms: Long): String {
        val clamped = ms.coerceAtLeast(0L)
        val minutes = clamped / 60_000L
        val remMs = clamped % 60_000L
        val wholeSeconds = remMs / 1000L
        val centiseconds = ((remMs % 1000L) + 5L) / 10L
        val adjustedMinutes: Long
        val adjustedSeconds: Long
        val adjustedCentis: Long
        if (centiseconds >= 100L) {
            val totalSec = wholeSeconds + 1L
            adjustedMinutes = minutes + (totalSec / 60L)
            adjustedSeconds = totalSec % 60L
            adjustedCentis = 0L
        } else {
            adjustedMinutes = minutes
            adjustedSeconds = wholeSeconds
            adjustedCentis = centiseconds
        }
        return String.format(Locale.US, "%02d:%02d.%02d", adjustedMinutes, adjustedSeconds, adjustedCentis)
    }

    private fun inspectTimeline(lyrics: LyricsData): TimelineStats? {
        var maxMs = 0L
        var trailingMarkerMs: Long? = null

        val synced = lyrics.syncedLyrics
        if (!synced.isNullOrBlank()) {
            var lastLineTimeMs: Long? = null
            var lastLineWasEmpty = false
            for (rawLine in synced.lineSequence()) {
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) continue
                val matches = lrcLineTimestampRegex.findAll(trimmed).toList()
                if (matches.isEmpty()) continue
                var lineMaxMs = 0L
                for (m in matches) {
                    val startMs = parseTimePartsMs(m.groupValues[1], m.groupValues[2], m.groupValues[3])
                    val endMs = if (m.groupValues[4].isNotEmpty()) {
                        parseTimePartsMs(m.groupValues[4], m.groupValues[5], m.groupValues[6])
                    } else 0L
                    lineMaxMs = maxOf(lineMaxMs, startMs, endMs)
                }
                for (wm in lrcWordTimestampRegex.findAll(trimmed)) {
                    val wordMs = parseTimePartsMs(wm.groupValues[1], wm.groupValues[2], wm.groupValues[3])
                    lineMaxMs = maxOf(lineMaxMs, wordMs)
                }
                maxMs = maxOf(maxMs, lineMaxMs)
                val textAfterTags = trimmed
                    .replace(lrcLineTimestampRegex, "")
                    .replace(lrcWordTimestampRegex, "")
                    .trim()
                lastLineTimeMs = lineMaxMs
                lastLineWasEmpty = textAfterTags.isEmpty()
            }
            if (lastLineWasEmpty && lastLineTimeMs != null && lastLineTimeMs > 0L) {
                trailingMarkerMs = lastLineTimeMs
            }
        }

        val wordJson = lyrics.wordByWordLyrics
        if (!wordJson.isNullOrBlank()) {
            val parsedWords = RhythmLyricsParser.parseWordByWordLyrics(wordJson)
            for (line in parsedWords) {
                maxMs = maxOf(maxMs, line.lineTimestamp, line.lineEndtime)
                for (w in line.words) {
                    maxMs = maxOf(maxMs, w.timestamp, w.endtime)
                }
            }
        }

        return if (maxMs > 0L) TimelineStats(maxMs, trailingMarkerMs) else null
    }

    fun resolveSourceDurationMs(
        lyrics: LyricsData,
        catalogDurationsMs: List<Long> = emptyList()
    ): Long? {
        val stats = inspectTimeline(lyrics) ?: return lyrics.sourceDurationMs
        val maxTimelineMs = stats.maxTimelineMs

        lyrics.sourceDurationMs?.takeIf { it >= maxTimelineMs }?.let { return it }
        extractLrcLengthHeaderMs(lyrics.syncedLyrics)?.takeIf { it >= maxTimelineMs }?.let { return it }

        val validCatalog = catalogDurationsMs
            .filter { it >= maxTimelineMs && maxTimelineMs.toDouble() / it.toDouble() >= 0.65 }
            .sorted()
        if (validCatalog.isNotEmpty()) {
            return validCatalog.first()
        }

        return stats.trailingEmptyMarkerMs?.let { it + 2_000L } ?: (maxTimelineMs + 3_500L)
    }

    fun scaleLrcTimestamps(lrcContent: String, scale: Double): String {
        if (lrcContent.isBlank() || !scale.isFinite() || scale <= 0.0) return lrcContent
        val withScaledLines = lrcLineTimestampRegex.replace(lrcContent) { m ->
            val startMs = (parseTimePartsMs(m.groupValues[1], m.groupValues[2], m.groupValues[3]) * scale).roundToLong()
            val startFormatted = formatTimeMs(startMs)
            if (m.groupValues[4].isNotEmpty()) {
                val endMs = (parseTimePartsMs(m.groupValues[4], m.groupValues[5], m.groupValues[6]) * scale).roundToLong()
                "[$startFormatted-${formatTimeMs(endMs)}]"
            } else {
                "[$startFormatted]"
            }
        }
        return lrcWordTimestampRegex.replace(withScaledLines) { wm ->
            val wordMs = (parseTimePartsMs(wm.groupValues[1], wm.groupValues[2], wm.groupValues[3]) * scale).roundToLong()
            "<${formatTimeMs(wordMs)}>"
        }
    }

    fun scaleWordByWordJson(wordByWordJson: String, scale: Double): String {
        if (wordByWordJson.isBlank() || !scale.isFinite() || scale <= 0.0) return wordByWordJson
        return try {
            val listType = object : TypeToken<List<RhythmLyricsLine>>() {}.type
            val lines: List<RhythmLyricsLine> = gson.fromJson(wordByWordJson, listType) ?: return wordByWordJson
            val scaled = lines.map { line ->
                val scaledStart = line.timestamp?.let { (it * scale).roundToLong().coerceAtLeast(0L) }
                val scaledEnd = line.endtime?.let { rawEnd ->
                    if (rawEnd > 86_400_000L) rawEnd else (rawEnd * scale).roundToLong().coerceAtLeast(scaledStart ?: 0L)
                }
                val scaledWords = line.text?.map { word ->
                    val wStart = (word.timestamp * scale).roundToLong().coerceAtLeast(0L)
                    val wEnd = maxOf(wStart, (word.endtime * scale).roundToLong())
                    word.copy(timestamp = wStart, endtime = wEnd)
                }
                line.copy(
                    timestamp = scaledStart,
                    endtime = scaledEnd,
                    text = scaledWords
                )
            }
            gson.toJson(scaled)
        } catch (_: Exception) {
            wordByWordJson
        }
    }

    fun adjustToTrackDuration(
        lyrics: LyricsData,
        targetDurationMs: Long?,
        isTempoEdited: Boolean = false,
        catalogDurationsMs: List<Long> = emptyList()
    ): LyricsData {
        if (targetDurationMs == null || targetDurationMs < 15_000L || !lyrics.hasSyncedLyrics()) {
            return lyrics
        }
        val stats = inspectTimeline(lyrics) ?: return lyrics
        val maxTimelineMs = stats.maxTimelineMs
        val sourceDurationMs = resolveSourceDurationMs(lyrics, catalogDurationsMs) ?: return lyrics

        val timelineOverflowsTrack = maxTimelineMs > targetDurationMs + 1_500L
        val tempoEditedMismatch = isTempoEdited && abs(targetDurationMs - sourceDurationMs) >= 4_000L
        if (!timelineOverflowsTrack && !tempoEditedMismatch) {
            return lyrics
        }

        val scale = targetDurationMs.toDouble() / sourceDurationMs.toDouble()
        if (!scale.isFinite() || scale !in 0.50..1.65 || abs(1.0 - scale) < 0.03) {
            return lyrics
        }

        val scaledSynced = lyrics.syncedLyrics?.let { scaleLrcTimestamps(it, scale) }
        val scaledWordByWord = lyrics.wordByWordLyrics?.let { scaleWordByWordJson(it, scale) }
        return lyrics.copy(
            syncedLyrics = scaledSynced,
            wordByWordLyrics = scaledWordByWord,
            sourceDurationMs = targetDurationMs
        )
    }
}

fun LyricsData.hasUsableSyncedTimeline(): Boolean {
    val parsed = syncedLyrics?.let(LyricsParser::parseLyrics).orEmpty()
        .filter { line -> line.text.any(Character::isLetterOrDigit) }
    return LyricsTimingPolicy.hasUsableTimeline(parsed.map { it.timestamp })
}
