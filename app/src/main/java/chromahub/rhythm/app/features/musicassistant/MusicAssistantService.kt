/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.MutableContextWrapper
import android.content.pm.ServiceInfo
import android.net.http.SslError
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Message
import android.os.PowerManager
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import chromahub.rhythm.app.R

/**
 * Owns the server's frontend/player beyond an Activity's lifetime, only on explicit opt-in.
 * Runs in its own process and WebView profile; it never creates a second local Rhythm queue.
 * No JavaScript bridge, injected credentials, SSL bypass, or external intent passthrough.
 */
class MusicAssistantService : Service() {
    inner class LocalBinder : Binder() { val service get() = this@MusicAssistantService }
    private val binder = LocalBinder()
    private val lifetime = MusicAssistantLifetime()
    private var wakeLock: PowerManager.WakeLock? = null
    private var contextWrapper: MutableContextWrapper? = null
    private var pendingPopup: WebView? = null

    var webView by mutableStateOf<WebView?>(null)
        private set
    var server by mutableStateOf("")
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<Int?>(null)
        private set
    var externalUrl by mutableStateOf<String?>(null)
    var backgroundEnabled by mutableStateOf(false)
        private set
    var clearing by mutableStateOf(false)
        private set

    override fun onBind(intent: Intent?): IBinder = binder

    @SuppressLint("SetJavaScriptEnabled")
    fun open(address: String) {
        // Android 8 cannot isolate WebView data directories across processes.
        check(Build.VERSION.SDK_INT >= 28)
        if (clearing) return
        val normalized = MusicAssistantServer.normalize(address) ?: return
        if (server == normalized && webView != null) return
        releasePlayer()
        lifetime.openPlayer()
        server = normalized
        error = null
        loading = true
        val wrapper = MutableContextWrapper(applicationContext)
        contextWrapper = wrapper
        webView = WebView(wrapper).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // Sendspin must be able to receive Play from another MA client after connection.
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = false
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    val target = request.url.toString()
                    if (MusicAssistantServer.sameOrigin(server, target)) return false
                    if (MusicAssistantServer.externalLink(target)) externalUrl = target
                    else error = R.string.music_assistant_blocked_link
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    loading = false
                    CookieManager.getInstance().flush()
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                    if (request.isForMainFrame) {
                        loading = false
                        error = R.string.music_assistant_connection_error
                    }
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame && response.statusCode >= 400) {
                        loading = false
                        error = R.string.music_assistant_connection_error
                    }
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, failure: SslError) {
                    handler.cancel()
                    loading = false
                    error = R.string.music_assistant_certificate_error
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    (view.parent as? ViewGroup)?.removeView(view)
                    webView = null
                    view.destroy()
                    contextWrapper = null
                    disableBackground()
                    loading = false
                    error = R.string.music_assistant_renderer_error
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) = request.deny()

                override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                    if (!isUserGesture) return false
                    // Capture a user-opened help/provider link; never grant it the authenticated view.
                    pendingPopup?.destroy()
                    val popup = WebView(wrapper)
                    pendingPopup = popup
                    popup.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(child: WebView, request: WebResourceRequest): Boolean {
                            val target = request.url.toString()
                            if (MusicAssistantServer.sameOrigin(server, target)) view.loadUrl(target)
                            else if (MusicAssistantServer.externalLink(target)) externalUrl = target
                            pendingPopup = null
                            child.destroy()
                            return true
                        }
                    }
                    (resultMsg.obj as WebView.WebViewTransport).webView = popup
                    resultMsg.sendToTarget()
                    return true
                }

                override fun onCloseWindow(window: WebView) {
                    if (window === pendingPopup) {
                        pendingPopup = null
                        window.destroy()
                    }
                }
            }
            loadUrl(normalized)
        }
    }

    fun attach(context: android.content.Context) { contextWrapper?.baseContext = context }
    fun detach() {
        (webView?.parent as? ViewGroup)?.removeView(webView)
        contextWrapper?.baseContext = applicationContext
        CookieManager.getInstance().flush()
    }

    fun retry() {
        error = null
        if (webView == null) open(server)
        else {
            loading = true
            webView?.reload()
        }
    }

    fun resumeVisibleSession(address: String) {
        if (!lifetime.explicitlyStopped) open(address)
    }

    fun uiHidden(changingConfigurations: Boolean) {
        if (lifetime.shouldReleaseWhenHidden(changingConfigurations)) releasePlayer()
        else detach()
    }

    fun disableBackground() {
        lifetime.stop()
        backgroundEnabled = false
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf() // A visible bound Activity can continue using the service.
    }

    fun releasePlayer() {
        disableBackground()
        pendingPopup?.destroy()
        pendingPopup = null
        detach()
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        contextWrapper = null
        loading = false
    }

    fun forget(onComplete: () -> Unit) {
        if (clearing) return
        clearing = true
        webView?.clearCache(true)
        releasePlayer()
        // This profile belongs solely to Music Assistant, not Rhythm's other WebViews.
        WebStorage.getInstance().deleteAllData()
        android.webkit.WebViewDatabase.getInstance(this).clearHttpAuthUsernamePassword()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            server = ""
            error = null
            clearing = false
            onComplete()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            lifetime.stopPlayer()
            releasePlayer()
        }
        else if (intent?.action == ACTION_BACKGROUND && webView != null) {
            if (backgroundEnabled) return START_NOT_STICKY
            try {
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.music_assistant_name), NotificationManager.IMPORTANCE_LOW))
                val open = PendingIntent.getActivity(this, 0, Intent(this, MusicAssistantActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val stop = PendingIntent.getService(this, 1, Intent(this, MusicAssistantService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notification = NotificationCompat.Builder(this, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.music_assistant_name))
                    .setContentText(getString(R.string.music_assistant_background_notification))
                    .setContentIntent(open)
                    .setOngoing(true)
                    .setSilent(true)
                    .addAction(0, getString(R.string.music_assistant_stop_player), stop)
                    .build()
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "Rhythm:MusicAssistant"
                ).apply { setReferenceCounted(false); acquire() }
                lifetime.enableBackground()
                backgroundEnabled = true
            } catch (_: RuntimeException) {
                disableBackground()
                error = R.string.music_assistant_background_error
            }
        } else stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    companion object {
        const val ACTION_BACKGROUND = "chromahub.rhythm.app.musicassistant.BACKGROUND"
        private const val ACTION_STOP = "chromahub.rhythm.app.musicassistant.STOP"
        private const val CHANNEL = "music_assistant_player"
        private const val NOTIFICATION_ID = 8095
    }
}
