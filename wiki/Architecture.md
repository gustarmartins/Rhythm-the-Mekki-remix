# Rhythm Architecture Guide

Technical documentation of Rhythm's app structure, design patterns, and architectural decisions.

## 🔄 Dual-Mode Architecture

Rhythm employs a unique dual-mode architecture to support both local and streaming playback experiences while sharing core infrastructure.

### Local Mode (`features/local`)
Focuses on device-based media using the Android `MediaStore` API. It handles local file indexing, metadata extraction from files, and local playback state.

### Streaming Mode (`features/streaming`)
Provides a completely separate pipeline for streaming servers. It includes its own data repositories and presentation layer, allowing the app to function as a streaming client without interfering with the local library. In recent releases the streaming UI has been progressively merged into the shared local UI while keeping its dedicated repositories and ViewModels.

### Shared Core
Both modes leverage the `shared` and `infrastructure` layers:
- **Shared Data**: Common domain models (Song, Album, Artist) ensure consistency.
- **Playback Service**: A unified `MediaPlaybackService` handles the actual audio output via ExoPlayer, regardless of whether the source is local or streaming.
- **Audio Processors**: The audio pipeline (`RhythmAudioProcessor`) chains effects such as Replay Gain, Bass Boost, Virtualizer/Spatialization, and Mono downmix (`RhythmMonoAudioProcessor`) before output.
- **Infrastructure**: Common utilities for networking, permissions, and background workers are used by both modes.


### State Observation

Playback state is observed via `Player.Listener` attached to the ExoPlayer `Player` interface.

```kotlin
// Observe ExoPlayer state via MediaController
mediaController.addListener(object : Player.Listener {
    override fun onPlaybackStateChanged(state: Int) { ... }
    override fun onIsPlayingChanged(isPlaying: Boolean) { ... }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { ... }
})

// In Compose, use collectAsState() on StateFlow from ViewModel
@Composable
fun PlayerScreen() {
    val playbackState by viewModel.playbackState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
}
```

---

## 🌐 Network Layer

### API Integration

```kotlin
// LRCLib API for synchronized lyrics
interface LRCLibApiService {
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<LyricsData>>
}

// Deezer API for artwork
interface DeezerApiService {
    @GET("search/track")
    suspend fun searchTrack(
        @Query("q") query: String
    ): Response<DeezerSearchResponse>
}

// YouTube Music API for artwork
interface YouTubeMusicApiService {
    @GET("api/music/song")
    suspend fun searchSong(
        @Query("q") query: String
    ): Response<YouTubeMusicResponse>
}
```

---

## 🧪 Testing Architecture

### Unit Tests

```kotlin
class MusicViewModelTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `player displays current track title`() {
        composeTestRule
            .onNodeWithTag("track_title")
            .assertIsDisplayed()
    }
}
```

---

## 🔐 Security & Privacy

### Data Privacy

- No analytics or tracking code
- All data stored locally
- No server communication except optional features

### Permissions

```kotlin
// Runtime permission requests are handled via Accompanist Permissions API
// and AndroidX Activity Result API at the composable level
@Composable
fun RequestAudioPermission() {
    val permissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            android.Manifest.permission.READ_MEDIA_AUDIO
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    // LaunchedEffect to trigger request
}
```

---

## ⚡ Performance Optimization

### Lazy Loading

```kotlin
@Composable
fun SongList(songs: List<Song>) {
    LazyColumn {
        items(songs, key = { it.id }) { song ->
            SongItem(song)
        }
    }
}
```

### Image Caching

```kotlin
// Coil for efficient image loading
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(song.albumArtUri)
        .crossfade(true)
        .build(),
    contentDescription = "Album Art"
)
```

### Background Processing

```kotlin
// Media scanning happens via ContentResolver + MediaStore queries
// in the MusicRepository layer, not in a dedicated worker
suspend fun scanMedia(context: Context): List<PlayableItem> {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATA
    )
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection, null, null, null
    )
    // Parse cursor into PlayableItem list
}
```

---

## 📊 Dependency Injection

Currently using manual DI (manual constructor injection + service locator pattern in feature modules).

```kotlin
// MusicViewModel receives a MediaController from MediaPlaybackService
// and interacts with it directly for playback control
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private var mediaController: MediaController? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun onConnected(controller: MediaController) {
        mediaController = controller
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }
}
```

---

## 🔄 Build System

### Gradle Kotlin DSL

```kotlin
// build.gradle.kts
android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "chromahub.rhythm.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 544561196
        versionName = "5.4.456.1196 Beta"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### Version Catalog

```toml
[versions]
kotlin = "2.4.10"
composeBom = "2026.06.01"
media3 = "1.11.0"

[libraries]
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
```

---

## 🎯 Design Patterns

### Repository Pattern
- Abstraction over data sources
- Testable business logic
- Single source of truth

### Observer Pattern
- StateFlow for reactive updates
- LiveData alternative
- Lifecycle-aware

### Factory Pattern
- ViewModel creation
- Widget instantiation

### Singleton Pattern
- AppSettings
- Repository instances

---

## 📚 Further Reading

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Media3 Documentation](https://developer.android.com/guide/topics/media/media3)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Material Design 3](https://m3.material.io/)

---

**Questions?** Check [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing) or ask in [Telegram](https://t.me/RhythmSupport) or [Discord](https://discord.gg/XjPyUYPQYc)!
