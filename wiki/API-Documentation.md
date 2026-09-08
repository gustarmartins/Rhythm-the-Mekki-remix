# API Documentation

Internal API reference for developers working with Rhythm's codebase. This covers the actual interfaces, classes, and patterns used in the project.

> **Note:** For the exact implementation details, always refer to the source code. This document provides a high-level overview of key architecture components.

---

## 📚 Overview

### Key Packages

| Package | Purpose |
|:---|:---|
| `chromahub.rhythm.app.core.domain` | Domain interfaces and models |
| `chromahub.rhythm.app.features.local` | Local playback feature |
| `chromahub.rhythm.app.features.streaming` | Streaming playback feature |
| `chromahub.rhythm.app.shared` | Shared utilities, themes, navigation |
| `chromahub.rhythm.app.infrastructure` | Player engine, audio processors, service, widgets, workers, network |

---

## 🎵 Playback Service

### MediaPlaybackService

**File:** `infrastructure/service/player/RhythmPlayerEngine.kt`

The core playback engine using Media3 ExoPlayer with a custom `DefaultRenderersFactory` that enables the FFmpeg decoder extension and chains the app's audio processors (Replay Gain, Bass Boost, Virtualizer, Mono downmix):

```kotlin
val renderersFactory = object : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
        enableOffload: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setAudioProcessorChain(audioProcessorChain)
            .build()
    }
}.apply {
    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
}
```

**Service:** `infrastructure/service/MediaPlaybackService.kt` extends `MediaLibraryService` and manages:
- `MediaLibrarySession` for external control
- `Player` instance via `RhythmPlayerEngine`
- Media notification with playback controls
- Audio focus handling
- Mono audio downmix state (`RhythmMonoAudioProcessor`) toggled via the device configuration sheet

---

## 🗂️ Data Layer

### MusicRepository

**File:** `core/domain/repository/MusicRepository.kt`

```kotlin
interface MusicRepository {
    fun getSongs(): Flow<List<PlayableItem>>
    fun getAlbums(): Flow<List<AlbumItem>>
    fun getArtists(): Flow<List<ArtistItem>>
    fun getPlaylists(): Flow<List<PlaylistItem>>
    suspend fun getSongById(id: String): PlayableItem?
    suspend fun getAlbumById(id: String): AlbumItem?
    suspend fun getArtistById(id: String): ArtistItem?
    suspend fun getPlaylistById(id: String): PlaylistItem?
    suspend fun getSongsForAlbum(albumId: String): List<PlayableItem>
    suspend fun searchSongs(query: String): List<PlayableItem>
    suspend fun searchAlbums(query: String): List<AlbumItem>
    suspend fun searchArtists(query: String): List<ArtistItem>
    suspend fun searchPlaylists(query: String): List<PlaylistItem>
}
```

### Domain Models

All identifiers are `String` type. Core models are in `core/domain/model/`:

| Model | Key Fields |
|:---|:---|
| `PlayableItem` | `id: String`, `title: String`, `artist: String?`, `album: String?`, `albumId: String?`, `duration: Long`, `uri: String?` |
| `AlbumItem` | `id: String`, `title: String`, `artist: String?`, `year: Int?`, `artworkUri: Uri?` |
| `ArtistItem` | `id: String`, `name: String`, `albumCount: Int`, `trackCount: Int` |
| `PlaylistItem` | `id: String`, `name: String`, `dateCreated: Long`, `dateModified: Long` |

---

## 🌐 Network APIs

### LRCLib (Synchronized Lyrics)

```kotlin
interface LRCLibApiService {
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<LyricsData>>
}
```

### Deezer (Artwork)

```kotlin
interface DeezerApiService {
    @GET("search/track")
    suspend fun searchTrack(
        @Query("q") query: String
    ): Response<DeezerSearchResponse>
}
```

### YouTube Music (Artwork)

```kotlin
interface YouTubeMusicApiService {
    @GET("api/music/song")
    suspend fun searchSong(
        @Query("q") query: String
    ): Response<YouTubeMusicResponse>
}
```

---

## 📱 Widgets

### Glance Widgets

| Widget | Class |
|:---|:---|
| Music Player Widget | `RhythmMusicWidget : GlanceAppWidget()` |
| Lyrics Widget | `RhythmLyricsWidget : GlanceAppWidget()` |

Widget updates are managed by `RhythmWidgetWorker` (a `CoroutineWorker`) and `GlanceWidgetUpdater`.

---

## 🎨 State Management

- **Playback State**: Observed via `Player.Listener` on ExoPlayer's `Player` interface
- **UI State**: `StateFlow` in ViewModels, collected as Compose state via `collectAsState()`
- **Settings**: `AppSettings` class backed by `DataStore` / `SharedPreferences`
- **Navigation**: Compose Navigation with a sealed `Route` class hierarchy

---

## 📦 Build Configuration

### BuildConfig Flags

All flags are `true` for both `fdroid` and `github` flavors:

| Flag | Purpose |
|:---|:---|
| `ENABLE_YOUTUBE_MUSIC` | YouTube Music artwork search |
| `ENABLE_SPOTIFY_SEARCH` | Spotify metadata search |
| `ENABLE_LYRICALLY_API` | Lyricall API for lyrics |
| `ENABLE_DEEZER` | Deezer artwork search |
| `ENABLE_LRCLIB` | LRCLib synchronized lyrics |
| `ENABLE_BETTERLYRICS` | BetterLyrics multi-source lyrics fallback |
| `ENABLE_WIKIPEDIA` | Wikipedia artist bio lookup |
| `FLAVOR` | Distribution channel (`fdroid` / `github`) |
| `IS_NIGHTLY` | Nightly vs stable channel |
| `APPLE_MUSIC_FALLBACK_TOKEN` | Apple Music artwork token (env/`local.properties`) |

### Dependency Versions

| Dependency | Version |
|:---|:---|
| AGP | `9.3.1` |
| Kotlin | `2.4.10` |
| Compose BOM | `2026.06.01` |
| Material3 | `1.5.0-alpha25` |
| Media3 | `1.11.0` |
| FFmpeg Decoder | `1.9.0+1` (Jellyfin fork) |
| Gradle | `9.6.1` |

---

## 📚 Further Reading

- [Architecture Guide](https://github.com/cromaguy/Rhythm/wiki/Architecture)
- [Technology Stack](https://github.com/cromaguy/Rhythm/wiki/Technology-Stack)
- [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing)

---

**Questions?** Check the [FAQ](https://github.com/cromaguy/Rhythm/wiki/FAQ) or ask in [Telegram](https://t.me/RhythmSupport) or [Discord](https://discord.gg/XjPyUYPQYc)!
