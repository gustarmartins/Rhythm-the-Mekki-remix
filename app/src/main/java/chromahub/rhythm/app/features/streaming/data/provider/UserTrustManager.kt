/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.data.provider

import android.util.Log
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Utility that creates an [OkHttpClient.Builder] pre-configured with an
 * [X509TrustManager] that trusts **both** system pre-installed CAs **and**
 * user-installed CAs (e.g. a custom root CA imported in Android Settings →
 * Security → Trusted credentials → User tab).
 *
 * Without this, OkHttp defaults to a TrustManager backed only by the system
 * KeyStore, which silently ignores user-installed certificates even though
 * Android's own browser and apps would accept them.  This causes:
 *
 *   java.security.cert.CertPathValidatorException:
 *       Trust anchor for certification path not found.
 *
 * when connecting to a Navidrome / Jellyfin server that is secured by a
 * private PKI whose root CA the user has imported into Android.
 *
 * Usage:
 *   val client = buildUserTrustingHttpClientBuilder().connectTimeout(...).build()
 */
internal object UserTrustManager {

    private const val TAG = "UserTrustManager"

    /**
     * Returns an [OkHttpClient.Builder] whose SSL layer trusts both the
     * Android system trust store and the Android user trust store.
     *
     * Falls back to a plain [OkHttpClient.Builder] (default TrustManager) if
     * anything goes wrong so that the app never crashes due to SSL setup.
     */
    fun buildUserTrustingHttpClientBuilder(): OkHttpClient.Builder {
        return try {
            val trustManager = buildUserAwareTrustManager()
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), null)
            }
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build user-trusting TrustManager, falling back to default: ${e.message}", e)
            OkHttpClient.Builder()
        }
    }

    /**
     * Builds a composite [X509TrustManager] that delegates validation to
     * **both** the system CA store and the user CA store.
     *
     * Android exposes the combined trust store (system + user) via the
     * "AndroidCAStore" [KeyStore] type. Feeding that KeyStore into a
     * [TrustManagerFactory] is the simplest and most correct way to obtain
     * a TrustManager that honours user-installed certificates.
     */
    private fun buildUserAwareTrustManager(): X509TrustManager {
        // "AndroidCAStore" contains system CAs + any user-installed CAs.
        val androidCaStore = KeyStore.getInstance("AndroidCAStore").also { it.load(null) }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).also {
            it.init(androidCaStore)
        }

        return tmf.trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()
            ?: throw IllegalStateException("No X509TrustManager found in AndroidCAStore TrustManagerFactory")
    }
}
