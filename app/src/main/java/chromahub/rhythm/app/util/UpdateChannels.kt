package chromahub.rhythm.app.util

/** Channels published by this fork. Retired/unknown choices resolve to stable. */
object UpdateChannels {
    val available = listOf("stable", "nightly")

    fun normalize(saved: String?, isNightlyBuild: Boolean = false): String = when (saved) {
        null -> if (isNightlyBuild) "nightly" else "stable"
        "nightly" -> "nightly"
        else -> "stable"
    }
}
