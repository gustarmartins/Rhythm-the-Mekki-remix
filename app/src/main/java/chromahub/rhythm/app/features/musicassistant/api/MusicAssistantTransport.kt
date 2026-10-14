/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant.api

import chromahub.rhythm.app.features.musicassistant.MusicAssistantServer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Control plane only. A media URI identifies a server item; it is never an audio URL. */
internal fun interface MusicAssistantTransport {
    suspend fun command(command: String, args: JsonObject): JsonElement
}

internal enum class MusicAssistantFailure {
    AUTHENTICATION, PERMISSION, NOT_FOUND, UNSUPPORTED_REQUEST, SERVER, NETWORK,
    REDIRECT, INVALID_RESPONSE, RESPONSE_TOO_LARGE
}

/** Deliberately excludes response bodies, URLs, tokens, and credential-bearing causes. */
internal class MusicAssistantApiException(val failure: MusicAssistantFailure) :
    IOException("Music Assistant request failed: ${failure.name}")

/**
 * Direct LAN/Tailscale/reverse-proxy transport for the native integration.
 * No automatic replay: an ambiguous timeout after a queue mutation must be reconciled by
 * reading server state, never by submitting the mutation again. WebRTC can implement the
 * same interface once its authenticated signaling/data-channel transport is connected.
 */
internal class MusicAssistantHttpTransport(
    server: String,
    private val accessToken: () -> String,
    client: OkHttpClient = OkHttpClient(),
) : MusicAssistantTransport {
    private val base = requireNotNull(MusicAssistantServer.normalize(server)) {
        "A Music Assistant HTTP or HTTPS server address is required"
    }.toHttpUrl()
    private val http = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        // Queue preparation can legitimately take longer than OkHttp's default 10s read
        // timeout on a cold server. The per-call deadline below remains authoritative.
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun command(command: String, args: JsonObject): JsonElement {
        require(command.matches(Regex("[a-z_]+(?:/[a-z_]+)+"))) { "Invalid API command" }
        val token = accessToken()
        if (token.isBlank() || token.any { it <= ' ' || it >= '\u007f' }) {
            throw MusicAssistantApiException(MusicAssistantFailure.AUTHENTICATION)
        }
        val payload = JsonObject().apply {
            addProperty("message_id", UUID.randomUUID().toString())
            addProperty("command", command)
            add("args", args.deepCopy())
        }
        val timeoutSeconds = when (command) {
            "player_queues/play_media", "player_queues/transfer" -> 90L
            else -> 30L
        }
        return post("api", payload, token, timeoutSeconds)
    }

    /** Passwords are sent once to the selected server and are not persisted by this adapter. */
    suspend fun login(username: String, password: String): String {
        require(username.isNotBlank() && password.isNotEmpty()) { "Credentials are required" }
        val payload = JsonObject().apply {
            addProperty("provider_id", "builtin")
            addProperty("device_name", "Rhythm")
            add("credentials", JsonObject().apply {
                addProperty("username", username)
                addProperty("password", password)
            })
        }
        val result = post("auth/login", payload, null).objectOrFail()
        if (result.boolean("success") != true) {
            throw MusicAssistantApiException(MusicAssistantFailure.AUTHENTICATION)
        }
        return result.text("token")?.takeIf { it.isNotBlank() }
            ?: throw MusicAssistantApiException(MusicAssistantFailure.INVALID_RESPONSE)
    }

    private suspend fun post(path: String, payload: JsonObject, token: String?, timeoutSeconds: Long = 30): JsonElement =
        suspendCancellableCoroutine { continuation ->
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                // Append rather than resolving /api, preserving an explicit proxy prefix.
                .url(base.newBuilder().addPathSegments(path).build())
                .header("Accept", "application/json")
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .post(object : RequestBody() {
                    override fun contentType() = body.contentType()
                    override fun contentLength() = body.contentLength()
                    override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
                    // Also prevents follow-up replay on Retry-After: 0 responses (e.g. HTTP 503).
                    override fun isOneShot() = true
                })
                .build()
            val call = http.newCall(request)
            call.timeout().timeout(timeoutSeconds, TimeUnit.SECONDS)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(
                        MusicAssistantApiException(MusicAssistantFailure.NETWORK)
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            val failure = when (it.code) {
                                in 200..299 -> null
                                401 -> MusicAssistantFailure.AUTHENTICATION
                                403 -> MusicAssistantFailure.PERMISSION
                                404 -> MusicAssistantFailure.NOT_FOUND
                                400, 405, 422 -> MusicAssistantFailure.UNSUPPORTED_REQUEST
                                in 300..399 -> MusicAssistantFailure.REDIRECT
                                else -> MusicAssistantFailure.SERVER
                            }
                            if (failure != null) throw MusicAssistantApiException(failure)
                            val body = it.body
                            if (body.contentLength() > MAX_RESPONSE_BYTES) {
                                throw MusicAssistantApiException(MusicAssistantFailure.RESPONSE_TOO_LARGE)
                            }
                            val buffer = Buffer()
                            val source = body.source()
                            while (true) {
                                val read = source.read(buffer, minOf(8192L, MAX_RESPONSE_BYTES + 1 - buffer.size))
                                if (read == -1L) break
                                if (buffer.size > MAX_RESPONSE_BYTES) {
                                    throw MusicAssistantApiException(MusicAssistantFailure.RESPONSE_TOO_LARGE)
                                }
                            }
                            val json = buffer.readUtf8()
                            if (json.isBlank()) {
                                if (it.code == 204) JsonNull.INSTANCE else invalidResponse()
                            } else {
                                val parsed = try { JsonParser.parseString(json) } catch (_: RuntimeException) {
                                    invalidResponse()
                                }
                                // HTTP returns the bare command result, not a websocket envelope.
                                if (parsed.isJsonPrimitive && parsed.asJsonPrimitive.isString) invalidResponse()
                                parsed
                            }
                        }
                    }
                    if (continuation.isActive) result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(
                            it as? MusicAssistantApiException
                                ?: MusicAssistantApiException(MusicAssistantFailure.NETWORK)
                        ) }
                    )
                }
            })
        }

    companion object {
        internal const val MAX_RESPONSE_BYTES = 8L * 1024 * 1024
    }
}

internal fun invalidResponse(): Nothing =
    throw MusicAssistantApiException(MusicAssistantFailure.INVALID_RESPONSE)

internal fun JsonElement.objectOrFail(): JsonObject = if (isJsonObject) asJsonObject else invalidResponse()
internal fun JsonObject.text(key: String): String? = get(key)?.takeIf {
    it.isJsonPrimitive && it.asJsonPrimitive.isString
}?.asString
internal fun JsonObject.boolean(key: String): Boolean? = get(key)?.takeIf {
    it.isJsonPrimitive && it.asJsonPrimitive.isBoolean
}?.asBoolean
internal fun JsonObject.number(key: String): Double? = get(key)?.takeIf {
    it.isJsonPrimitive && it.asJsonPrimitive.isNumber
}?.asDouble?.takeIf { it.isFinite() }
internal fun JsonObject.requiredText(key: String): String = text(key)?.takeIf { it.isNotBlank() } ?: invalidResponse()

/** Supports bare arrays and the older paged-list response without treating malformed data as empty. */
internal fun JsonElement.itemsOrFail(): List<JsonObject> {
    val array = when {
        isJsonArray -> asJsonArray
        isJsonObject -> asJsonObject.get("items")?.takeIf { it.isJsonArray }?.asJsonArray ?: invalidResponse()
        else -> invalidResponse()
    }
    return array.map { it.objectOrFail() }
}
