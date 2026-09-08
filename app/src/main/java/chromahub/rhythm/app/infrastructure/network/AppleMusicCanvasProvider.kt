/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import okhttp3.HttpUrl.Companion.toHttpUrl

private object AppleCanvasLogger {
    private const val TAG = "AppleMusicCanvas"
    fun d(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (e: RuntimeException) {
            println("[$TAG] D: $msg")
        }
    }
    fun w(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (e: RuntimeException) {
            println("[$TAG] W: $msg")
        }
    }
    fun e(msg: String, t: Throwable) {
        try {
            Log.e(TAG, msg, t)
        } catch (e: RuntimeException) {
            System.err.println("[$TAG] E: $msg")
            t.printStackTrace()
        }
    }
}

object AppleMusicCanvasProvider {


    // Fallback/Default public JWT token — loaded from local.properties via BuildConfig (not hardcoded)
    private val APPLE_MUSIC_TOKEN: String
        get() = chromahub.rhythm.app.BuildConfig.APPLE_MUSIC_FALLBACK_TOKEN

    private var cachedToken: String? = null
    private var tokenExpiryMs: Long = 0L


    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    private suspend fun getOrFetchToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryMs - 60_000) {
            return@withContext cachedToken!!
        }

        AppleCanvasLogger.d("Fetching fresh developer token dynamically...")
        try {
            val request = Request.Builder()
                .url("https://music.apple.com/us/browse")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppleCanvasLogger.w("Failed to get Apple Music browse page: ${response.code}")
                    return@use null
                }
                val html = response.body.string()

                val scriptRegex = Pattern.compile("/assets/index(?:-legacy)?[~-][a-zA-Z0-9_-]+\\.js")
                val matcher = scriptRegex.matcher(html)
                val scripts = mutableListOf<String>()
                while (matcher.find()) {
                    val script = matcher.group()
                    if (!scripts.contains(script)) {
                        scripts.add(script)
                    }
                }
                AppleCanvasLogger.d("Found script candidates in HTML: $scripts")

                var fetchedToken: String? = null
                for (scriptPath in scripts) {
                    val scriptUrl = "https://music.apple.com$scriptPath"
                    AppleCanvasLogger.d("Fetching script: $scriptUrl")

                    val scriptReq = Request.Builder()
                        .url(scriptUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()

                    client.newCall(scriptReq).execute().use { scriptRes ->
                        if (!scriptRes.isSuccessful) return@use

                        val scriptText = scriptRes.body.string()
                        val tokenRegex = Pattern.compile("ey[a-zA-Z0-9_-]+\\.ey[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
                        val tokenMatcher = tokenRegex.matcher(scriptText)
                        
                        while (tokenMatcher.find()) {
                            val token = tokenMatcher.group()
                            try {
                                val parts = token.split(".")
                                if (parts.size >= 2) {
                                    val body = parts[1]
                                    val decodedBytes = Base64.getUrlDecoder().decode(body)
                                    val decoded = String(decodedBytes, Charsets.UTF_8)
                                    
                                    if (decoded.contains("iss") && decoded.contains("exp")) {
                                        val expIndex = decoded.indexOf("\"exp\":")
                                        if (expIndex != -1) {
                                            val endIdx = decoded.indexOf("}", expIndex)
                                            val segment = if (endIdx != -1) decoded.substring(expIndex + 6, endIdx) else decoded.substring(expIndex + 6)
                                            val expValStr = segment.takeWhile { it.isDigit() }
                                            val expSeconds = expValStr.toLongOrNull() ?: 0L
                                            
                                            if (expSeconds * 1000 > now) {
                                                fetchedToken = token
                                                tokenExpiryMs = expSeconds * 1000
                                                AppleCanvasLogger.d("Successfully decoded and verified new token. Expires at: ${Date(tokenExpiryMs)}")
                                                break
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore decoding exceptions for invalid matches
                            }
                        }
                    }
                    if (fetchedToken != null) break
                }

                if (fetchedToken != null) {
                    cachedToken = fetchedToken
                    fetchedToken
                } else {
                    AppleCanvasLogger.w("Could not locate any valid fresh token in script files. Using fallback token.")
                    APPLE_MUSIC_TOKEN
                }
            } ?: APPLE_MUSIC_TOKEN
        } catch (e: Exception) {
            AppleCanvasLogger.e("Error occurred during dynamic token fetch. Using fallback token.", e)
            APPLE_MUSIC_TOKEN
        }
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        storefront: String = "us",
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = cacheKey("song", song, artist, album ?: "", storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.value }

        val result = searchAndFetchMotion(song, artist, album, storefront, "songs")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
            return@withContext result
        }

        // Fallback: if song search returned null, try searching for the album of the song
        if (!album.isNullOrBlank()) {
            AppleCanvasLogger.d("Song search returned null for '$song' by '$artist'. Falling back to album search for '$album'...")
            val albumResult = searchAndFetchMotion(term = album, artist = artist, album = null, storefront = storefront, type = "albums")
            if (albumResult != null) {
                val songResult = albumResult.copy(name = song)
                cache[key] = CacheEntry(songResult, System.currentTimeMillis() + CACHE_TTL_MS)
                return@withContext songResult
            }
        }

        return@withContext null
    }

    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        val key = cacheKey("album", album, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return@withContext it.value }

        val result = searchAndFetchMotion(term = album, artist = artist, album = null, storefront = storefront, type = "albums")
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return@withContext result
    }

    suspend fun getAlbumDescription(
        album: String,
        artist: String,
        storefront: String = "us",
    ): String? = withContext(Dispatchers.IO) {
        try {
            val query = if (album.contains(artist, ignoreCase = true)) album else "$artist $album"
            val token = getOrFetchToken()
            val urlBuilder = "https://amp-api.music.apple.com/v1/catalog/$storefront/search".toHttpUrl().newBuilder()
            urlBuilder.addQueryParameter("term", query)
            urlBuilder.addQueryParameter("types", "albums")
            urlBuilder.addQueryParameter("limit", "5")
            urlBuilder.addQueryParameter("extend", "editorialNotes")

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Authorization", "Bearer $token")
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code != 200) return@use null
                val bodyStr = response.body.string()
                val root = JsonParser.parseString(bodyStr).asJsonObject
                val resultsObj = root.getAsJsonObject("results") ?: return@use null
                val typeObj = resultsObj.getAsJsonObject("albums") ?: return@use null
                val dataArray = typeObj.getAsJsonArray("data") ?: return@use null

                for (itemElement in dataArray) {
                    val obj = itemElement.asJsonObject
                    val attributes = obj.getAsJsonObject("attributes") ?: continue
                    val resultArtistName = attributes.get("artistName")?.asString ?: ""
                    val resultName = attributes.get("name")?.asString ?: ""

                    if (artistMatches(artist, resultArtistName) &&
                        (resultName.equals(album, ignoreCase = true) ||
                         resultName.contains(album, ignoreCase = true) ||
                         album.contains(resultName, ignoreCase = true))
                    ) {
                        val editorialNotes = attributes.getAsJsonObject("editorialNotes")
                        if (editorialNotes != null) {
                            val description = editorialNotes.get("standard")?.asString
                                ?: editorialNotes.get("short")?.asString
                            if (!description.isNullOrBlank()) {
                                return@use description.replace(Regex("<[^>]*>"), "").trim()
                            }
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            AppleCanvasLogger.e("Error fetching album description", e)
            null
        }
    }


    private suspend fun searchAndFetchMotion(
        term: String,
        artist: String,
        album: String?,
        storefront: String,
        type: String,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        try {
            AppleCanvasLogger.d("searching for $type: $term (album: $album) in $storefront")
            var query = if (term.contains(artist, ignoreCase = true)) term else "$artist $term"
            if (!album.isNullOrBlank() && !query.contains(album, ignoreCase = true)) {
                query = "$query $album"
            }

            val token = getOrFetchToken()
            val urlBuilder = "https://amp-api.music.apple.com/v1/catalog/$storefront/search".toHttpUrl().newBuilder()
            
            urlBuilder.addQueryParameter("term", query)
            urlBuilder.addQueryParameter("types", type)
            urlBuilder.addQueryParameter("limit", "10")
            urlBuilder.addQueryParameter("extend", "editorialVideo")
            urlBuilder.addQueryParameter("include", "albums")

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Authorization", "Bearer $token")
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code != 200) {
                    AppleCanvasLogger.w("Search failed with status ${response.code}")
                    return@use null
                }

                val bodyStr = response.body.string()
                val root = JsonParser.parseString(bodyStr).asJsonObject
                val resultsObj = root.getAsJsonObject("results") ?: return@use null
                val typeObj = resultsObj.getAsJsonObject(type) ?: return@use null
                val dataArray = typeObj.getAsJsonArray("data") ?: return@use null

                val scoredResults = mutableListOf<Pair<Int, JsonObject>>()

                for (itemElement in dataArray) {
                    val obj = itemElement.asJsonObject
                    val attributes = obj.getAsJsonObject("attributes") ?: continue
                    val resultArtistName = attributes.get("artistName")?.asString ?: ""
                    val resultName = attributes.get("name")?.asString ?: ""
                    val resultCollectionName = attributes.get("albumName")?.asString 
                        ?: attributes.get("collectionName")?.asString 
                        ?: ""

                    val nameLower = resultName.lowercase(Locale.ROOT)
                    val collectionLower = resultCollectionName.lowercase(Locale.ROOT)
                    
                    val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                            collectionLower.contains("playlist") || collectionLower.contains("set list") ||
                            nameLower.contains("essentials") || collectionLower.contains("essentials") ||
                            collectionLower.contains("dj mix") || collectionLower.contains("mixed") ||
                            collectionLower.contains("apple music") || collectionLower.contains("today's hits") ||
                            nameLower.contains("session") || collectionLower.contains("session")

                    if (isBlacklisted) {
                        AppleCanvasLogger.d("Skipping blacklisted result: '$resultName' (Album: '$resultCollectionName')")
                        continue
                    }

                    val artistMatch = artistMatches(artist, resultArtistName)
                    if (!artistMatch) continue

                    var score = 10 // Base matching score since artist matched
                    
                    val nameMatch = resultName.equals(term, ignoreCase = true)
                    val nameFuzzy = resultName.contains(term, ignoreCase = true) || term.contains(resultName, ignoreCase = true)

                    if (nameMatch) {
                        score += 15
                    } else if (nameFuzzy) {
                        score += 7
                    } else {
                        score -= 10
                    }

                    val editionWords = listOf("deluxe", "expanded", "remastered", "remix", "version", "edit", "mix", "bonus")
                    for (word in editionWords) {
                        val inTerm = term.contains(word, ignoreCase = true)
                        val inResult = resultName.contains(word, ignoreCase = true)
                        if (inTerm && inResult) score += 5
                        else if (inTerm != inResult && inResult) score -= 3
                    }

                    if (!album.isNullOrBlank() && resultCollectionName.isNotEmpty()) {
                        val albumMatch = resultCollectionName.equals(album, ignoreCase = true)
                        val albumFuzzy = resultCollectionName.contains(album, ignoreCase = true) || album.contains(resultCollectionName, ignoreCase = true)
                        
                        if (albumMatch) score += 20
                        else if (albumFuzzy) score += 10
                    }

                    AppleCanvasLogger.d("Result candidate: '$resultName' by '$resultArtistName' (Album: '$resultCollectionName') -> Score: $score")
                    scoredResults.add(score to obj)
                }

                scoredResults.sortByDescending { it.first }

                for ((score, obj) in scoredResults) {
                    if (score < 12) {
                        AppleCanvasLogger.d("skipping result with low score: $score")
                        continue
                    }
                    val attributes = obj.getAsJsonObject("attributes") ?: continue
                    val resultName = attributes.get("name")?.asString ?: ""
                    val resultArtistName = attributes.get("artistName")?.asString ?: ""

                    var targetAlbumId: String? = null
                    val objType = obj.get("type")?.asString

                    if (objType == "songs") {
                        val relationships = obj.getAsJsonObject("relationships")
                        if (relationships != null) {
                            val albumsRel = relationships.getAsJsonObject("albums")
                            if (albumsRel != null) {
                                val albumsData = albumsRel.getAsJsonArray("data")
                                if (albumsData != null && albumsData.size() > 0) {
                                    targetAlbumId = albumsData.get(0).asJsonObject.get("id")?.asString
                                }
                            }
                        }
                        if (targetAlbumId == null) {
                            targetAlbumId = attributes.get("collectionId")?.asString
                        }
                        if (targetAlbumId == null) {
                            val url = attributes.get("url")?.asString
                            if (url != null) {
                                val albumPart = url.substringAfter("/album/", "").substringBefore("?")
                                val id = albumPart.substringAfterLast("/", "")
                                if (id.isNotEmpty() && id.all { it.isDigit() }) {
                                    targetAlbumId = id
                                }
                            }
                        }
                    } else if (objType == "albums") {
                        targetAlbumId = obj.get("id")?.asString
                    }

                    if (targetAlbumId == null || targetAlbumId.startsWith("pl.")) {
                        AppleCanvasLogger.d("skipping null or playlist albumId ($targetAlbumId) for $resultName")
                        continue
                    }

                    AppleCanvasLogger.d("trying resolve for $targetAlbumId (from $objType)")

                    // Check editorialVideo directly in the search result
                    val ev = attributes.getAsJsonObject("editorialVideo")
                    if (ev != null) {
                        val hlsUrl = extractEditorialVideoUrl(ev)
                        if (!hlsUrl.isNullOrBlank()) {
                            val collName = attributes.get("collectionName")?.asString
                            val resolvedAlbumName = if (objType == "songs") collName else resultName
                            AppleCanvasLogger.d("Found direct editorialVideo for $resultName (ID: $targetAlbumId)")
                            return@use CanvasArtwork(
                                name = resultName,
                                artist = resultArtistName,
                                albumId = targetAlbumId,
                                albumName = resolvedAlbumName,
                                animated = hlsUrl
                            )
                        }
                    }

                    // Fallback to full lookup
                    val fetched = fetchMotionArtwork(
                        albumId = targetAlbumId,
                        storefront = storefront,
                        fallbackArtist = resultArtistName,
                        titleOverride = if (objType == "songs") resultName else null,
                        artistOverride = if (objType == "songs") resultArtistName else null
                    )
                    if (fetched != null) return@use fetched
                }
                null
            }
        } catch (e: Exception) {
            AppleCanvasLogger.e("Error searching and fetching motion", e)
            null
        }
    }

    private suspend fun fetchMotionArtwork(
        albumId: String,
        storefront: String,
        fallbackArtist: String?,
        titleOverride: String? = null,
        artistOverride: String? = null,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (albumId.startsWith("pl.")) return@withContext null
        try {
            AppleCanvasLogger.d("fetching album $albumId")
            val token = getOrFetchToken()
            val url = "https://amp-api.music.apple.com/v1/catalog/$storefront/albums/$albumId?extend=editorialVideo&include=tracks"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code != 200) {
                    AppleCanvasLogger.w("album fetch failed for $albumId: ${response.code}")
                    return@use null
                }

                val bodyStr = response.body.string()
                val root = JsonParser.parseString(bodyStr).asJsonObject
                val dataArray = root.getAsJsonArray("data") ?: return@use null
                if (dataArray.size() == 0) return@use null

                val albumObj = dataArray.get(0).asJsonObject
                val attributes = albumObj.getAsJsonObject("attributes") ?: return@use null
                val albumName = attributes.get("name")?.asString ?: ""
                val artistName = attributes.get("artistName")?.asString ?: fallbackArtist

                val nameLower = albumName.lowercase(Locale.ROOT)
                val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                        nameLower.contains("essentials") || nameLower.contains("dj mix") ||
                        nameLower.contains("mixed") || nameLower.contains("apple music") ||
                        nameLower.contains("today's hits") || nameLower.contains("session")

                if (isBlacklisted) {
                    AppleCanvasLogger.d("fetchMotionArtwork: ignoring blacklisted album '$albumName' ($albumId)")
                    return@use null
                }

                val finalTitle = titleOverride ?: albumName
                val finalArtist = artistOverride ?: artistName

                val ev = attributes.getAsJsonObject("editorialVideo")
                if (ev != null) {
                    val videoUrl = extractEditorialVideoUrl(ev)
                    if (!videoUrl.isNullOrBlank()) {
                        AppleCanvasLogger.d("found editorialVideo for $finalTitle (album: $albumName, id: $albumId)")
                        return@use CanvasArtwork(
                            name = finalTitle,
                            artist = finalArtist,
                            albumId = albumId,
                            albumName = albumName,
                            animated = videoUrl
                        )
                    }
                }
                AppleCanvasLogger.d("no editorialVideo for $albumId")
                null
            }
        } catch (e: Exception) {
            AppleCanvasLogger.e("error in fetchMotionArtwork for $albumId", e)
            null
        }
    }

    private fun extractEditorialVideoUrl(ev: JsonObject): String? {
        val candidates = listOf("motionDetailRaw", "motionDetailSquare", "motionDetailTall", "motionDetailStatic")
        for (key in candidates) {
            val asset = ev.getAsJsonObject(key) ?: continue
            val video = asset.get("video")?.asString
                ?: asset.get("videoUrl")?.asString
                ?: asset.get("hlsUrl")?.asString
                ?: asset.get("url")?.asString
            
            if (!video.isNullOrBlank()) return video
        }
        return null
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\(|\\u005b"), " ")
            .replace(Regex("\\)|\\u005d"), " ")
            .replace(Regex("\\b(deluxe|special|expanded|remastered|remaster|explicit|clean|live|studio|mono|stereo|single|ep|lp|remix|bonus|track|version|edition|anniversary|recordings|collection|greatest hits|best of)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }

    private fun artistMatches(requested: String, returned: String): Boolean {
        val delimiters = "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)"
        val requestedList = requested.split(Regex(delimiters, RegexOption.IGNORE_CASE))
            .map { it.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
        
        val returnedList = returned.split(Regex(delimiters, RegexOption.IGNORE_CASE))
            .map { it.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
        
        if (requestedList.isEmpty() || returnedList.isEmpty()) return false
        return requestedList.all { req -> returnedList.any { res -> res == req } }
    }
}
