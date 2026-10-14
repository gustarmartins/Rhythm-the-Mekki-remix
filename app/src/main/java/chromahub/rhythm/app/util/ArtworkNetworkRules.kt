package chromahub.rhythm.app.util

internal data class ArtworkConnection(
    val internet: Boolean = false,
    val wifiOrEthernet: Boolean = false,
    val metered: Boolean = true,
    val cellular: Boolean = false
)

/** Unknown, cellular and metered routes require consent; saved/local images need no network. */
internal fun allowsArtworkDownload(connection: ArtworkConnection, allowMobileData: Boolean, setupCompleted: Boolean): Boolean =
    setupCompleted && connection.internet && (allowMobileData ||
        (connection.wifiOrEthernet && !connection.metered && !connection.cellular))
