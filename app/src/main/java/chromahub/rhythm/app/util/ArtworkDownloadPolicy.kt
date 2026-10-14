package chromahub.rhythm.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.CanvasNetworkMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One process-owned Android network observer; audio streaming is outside this policy. */
object ArtworkDownloadPolicy {
    private var connectivity: ConnectivityManager? = null
    @Volatile private var settings: AppSettings? = null
    private val _connection = MutableStateFlow(ArtworkConnection())
    // Expose an opaque change counter so UI can dispose motion playback after a route change.
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()

    fun initialize(context: Context, appSettings: AppSettings) {
        if (settings != null) return
        settings = appSettings
        val manager = context.getSystemService(ConnectivityManager::class.java)
        connectivity = manager
        refresh()
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refresh()
        })
    }

    private fun refresh() {
        val manager = connectivity ?: return
        val caps = manager.activeNetwork?.let(manager::getNetworkCapabilities)
        val next = ArtworkConnection(
            internet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            wifiOrEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true,
            metered = manager.isActiveNetworkMetered,
            cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        )
        if (next != _connection.value) {
            _connection.value = next
            _revision.value += 1
        }
    }

    fun artworkAllowed(): Boolean {
        val prefs = settings ?: return false
        return allowsArtworkDownload(_connection.value, prefs.artworkAllowMobileData.value,
            prefs.artworkSetupCompleted.value)
    }

    fun canvasAllowed(): Boolean {
        val prefs = settings ?: return false
        return prefs.appleCanvasEnabled.value && prefs.integrationsEnabled.value &&
            allowsArtworkDownload(_connection.value, prefs.appleCanvasNetworkMode.value == CanvasNetworkMode.BOTH,
                prefs.artworkSetupCompleted.value)
    }
}
