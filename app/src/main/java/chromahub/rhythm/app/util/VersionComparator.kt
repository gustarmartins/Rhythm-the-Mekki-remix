/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.util.Log
import java.util.Locale

/**
 * Channel tier ranking representing release stability.
 * Higher value indicates greater stability: STABLE (3) > BETA (2) > NIGHTLY (1).
 */
enum class ChannelTier(val rank: Int) {
    NIGHTLY(1),
    BETA(2),
    STABLE(3);

    companion object {
        fun fromString(channel: String): ChannelTier =
            when (channel.lowercase(Locale.ROOT).trim()) {
                "nightly", "dev", "snapshot" -> NIGHTLY
                "beta", "alpha", "rc", "preview" -> BETA
                else -> STABLE
            }
    }
}

/**
 * Semantic version representation supporting 4-segment versions (major.minor.patch.subpatch),
 * stability channel tiers, build numbers, and nightly run numbers.
 */
data class SemanticVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val subpatch: Int = 0,
    val tier: ChannelTier = ChannelTier.STABLE,
    val buildNumber: Int = 0,
    val nightlyRunNumber: Int = 0,
    val isCiBuild: Boolean = false,
    val rawVersion: String = "",
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        // 1. Primary rule: Higher numeric version components always win
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        if (subpatch != other.subpatch) return subpatch.compareTo(other.subpatch)

        // 2. Secondary rule: When numeric components are equal, compare stability tiers
        // STABLE (3) > BETA (2) > NIGHTLY (1)
        if (tier.rank != other.tier.rank) {
            return tier.rank.compareTo(other.tier.rank)
        }

        // 3. Tertiary rule: If within the same tier and equal version, compare build / run indices
        when (tier) {
            ChannelTier.NIGHTLY -> {
                if (nightlyRunNumber != other.nightlyRunNumber) {
                    return nightlyRunNumber.compareTo(other.nightlyRunNumber)
                }
            }
            ChannelTier.BETA -> {
                if (buildNumber != other.buildNumber) {
                    return buildNumber.compareTo(other.buildNumber)
                }
            }
            ChannelTier.STABLE -> {
                if (buildNumber != other.buildNumber) {
                    return buildNumber.compareTo(other.buildNumber)
                }
            }
        }

        return 0
    }
}

/**
 * Utility for parsing and comparing app versions across update channels.
 *
 * Precedence Rule:
 * Higher Version Number >> Channel Tier: Stable > Beta > Nightly
 */
object VersionComparator {
    private const val TAG = "VersionComparator"

    private val BUILD_REGEX = Regex("(?:b|build|r)-?(\\d+)", RegexOption.IGNORE_CASE)
    private val NIGHTLY_RUN_REGEX = Regex("nightly-r(\\d+)", RegexOption.IGNORE_CASE)

    /**
     * Parse a version string into a [SemanticVersion].
     *
     * @param versionString The version string to parse.
     * @param isPreRelease Optional explicit flag indicating if the build is a pre-release / beta release (e.g. from GitHub).
     */
    fun parse(versionString: String?, isPreRelease: Boolean? = null): SemanticVersion {
        if (versionString.isNullOrBlank()) {
            return SemanticVersion()
        }

        try {
            val cleaned = versionString.trim().removePrefix("v").removePrefix("V")
            val lower = cleaned.lowercase(Locale.ROOT)

            // Determine channel tier: Nightly > Beta > Stable
            val tier = when {
                lower.contains("nightly") || lower.contains("snapshot") || lower.contains("dev") -> ChannelTier.NIGHTLY
                isPreRelease == true || lower.contains("beta") || lower.contains("alpha") || lower.contains("rc") || lower.contains("preview") -> ChannelTier.BETA
                isPreRelease == false -> ChannelTier.STABLE
                else -> ChannelTier.STABLE
            }

            // Extract nightly run number if present
            val nightlyRunNumber = NIGHTLY_RUN_REGEX.find(cleaned)?.groupValues?.get(1)?.toIntOrNull()
                ?: if (tier == ChannelTier.NIGHTLY) BUILD_REGEX.find(cleaned)?.groupValues?.get(1)?.toIntOrNull() ?: 0 else 0

            // Extract explicit build number (e.g. b-1200)
            val explicitBuildNumber = BUILD_REGEX.find(cleaned)?.groupValues?.get(1)?.toIntOrNull() ?: 0

            // Extract numeric base: split by space, hyphen, underscore
            val baseWithoutSuffix = cleaned
                .split(" ")[0]
                .split("-")[0]
                .split("_")[0]

            val parts = baseWithoutSuffix.split(".")
            val major = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val subpatch = parts.getOrNull(3)?.filter { it.isDigit() }?.toIntOrNull() ?: 0

            val isCi = BUILD_REGEX.containsMatchIn(cleaned) && tier != ChannelTier.NIGHTLY

            val finalBuildNumber = if (explicitBuildNumber > 0) explicitBuildNumber else subpatch

            return SemanticVersion(
                major = major.coerceAtLeast(0),
                minor = minor.coerceAtLeast(0),
                patch = patch.coerceAtLeast(0),
                subpatch = subpatch.coerceAtLeast(0),
                tier = tier,
                buildNumber = finalBuildNumber.coerceAtLeast(0),
                nightlyRunNumber = nightlyRunNumber.coerceAtLeast(0),
                isCiBuild = isCi,
                rawVersion = versionString,
            )
        } catch (e: Exception) {
            try {
                Log.e(TAG, "Error parsing version string: '$versionString'", e)
            } catch (_: Throwable) {}
            return SemanticVersion(rawVersion = versionString)
        }
    }

    /**
     * Compare two version strings.
     */
    fun compare(
        v1: String?,
        v2: String?,
        isPreRelease1: Boolean? = null,
        isPreRelease2: Boolean? = null,
    ): Int {
        val s1 = parse(v1, isPreRelease1)
        val s2 = parse(v2, isPreRelease2)
        return s1.compareTo(s2)
    }

    /**
     * Determine if [candidate] represents a strictly newer update compared to [current].
     */
    fun isNewer(
        candidate: String?,
        current: String?,
        isCandidatePreRelease: Boolean? = null,
        isCurrentPreRelease: Boolean? = null,
    ): Boolean {
        if (candidate.isNullOrBlank() || current.isNullOrBlank()) return false
        return compare(candidate, current, isCandidatePreRelease, isCurrentPreRelease) > 0
    }

    /**
     * Compare base numeric components only (ignoring channel tiers).
     */
    fun compareBaseNumeric(v1: String?, v2: String?): Int {
        val s1 = parse(v1)
        val s2 = parse(v2)
        if (s1.major != s2.major) return s1.major.compareTo(s2.major)
        if (s1.minor != s2.minor) return s1.minor.compareTo(s2.minor)
        if (s1.patch != s2.patch) return s1.patch.compareTo(s2.patch)
        return s1.subpatch.compareTo(s2.subpatch)
    }
}
