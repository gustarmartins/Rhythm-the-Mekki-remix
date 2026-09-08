/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.data.repository

import android.content.Context
import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

data class StreamingServiceSession(
    val serviceId: String,
    val isConnected: Boolean,
    val serverUrl: String = "",
    val username: String = "",
    val lastConnectedAtEpochMillis: Long = 0L
)

class StreamingServiceSessionRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessions = MutableStateFlow(loadSessions())
    val sessions: StateFlow<Map<String, StreamingServiceSession>> = _sessions.asStateFlow()

    fun getSession(serviceId: String): StreamingServiceSession {
        return _sessions.value[serviceId] ?: defaultSession(serviceId)
    }

    fun isConnected(serviceId: String): Boolean {
        return getSession(serviceId).isConnected
    }

    fun connect(serviceId: String, serverUrl: String, username: String) {
        val now = System.currentTimeMillis()
        prefs.edit {
    putBoolean(keyConnected(serviceId), true)
    putString(keyServerUrl(serviceId), serverUrl)
    putString(keyUsername(serviceId), username)
    putLong(keyConnectedAt(serviceId), now)
}
        _sessions.value = loadSessions()
    }

    fun disconnect(serviceId: String) {
        prefs.edit {
    putBoolean(keyConnected(serviceId), false)
    remove(keyServerUrl(serviceId))
    remove(keyUsername(serviceId))
    remove(keyConnectedAt(serviceId))
}
        _sessions.value = loadSessions()
    }

    private fun loadSessions(): Map<String, StreamingServiceSession> {
        return StreamingServiceId.all.associateWith { serviceId ->
            StreamingServiceSession(
                serviceId = serviceId,
                isConnected = prefs.getBoolean(keyConnected(serviceId), false),
                serverUrl = prefs.getString(keyServerUrl(serviceId), "") ?: "",
                username = prefs.getString(keyUsername(serviceId), "") ?: "",
                lastConnectedAtEpochMillis = prefs.getLong(keyConnectedAt(serviceId), 0L)
            )
        }
    }

    private fun defaultSession(serviceId: String): StreamingServiceSession {
        return StreamingServiceSession(serviceId = serviceId, isConnected = false)
    }

    private fun keyConnected(serviceId: String) = "${serviceId}_connected"
    private fun keyServerUrl(serviceId: String) = "${serviceId}_server_url"
    private fun keyUsername(serviceId: String) = "${serviceId}_username"
    private fun keyConnectedAt(serviceId: String) = "${serviceId}_connected_at"

    companion object {
        private const val PREFS_NAME = "streaming_service_sessions"
    }
}
