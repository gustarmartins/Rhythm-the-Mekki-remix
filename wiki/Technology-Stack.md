# Technology Stack

This document details the technical architecture and libraries used in Rhythm Music Player.

## 🏗️ Core Technologies

### UI & Design

| Technology | Purpose |
|:---|:---|
| **Jetpack Compose** | Modern declarative UI toolkit for Android |
| **Material 3** | Material Design components and theming system |
| **Material Symbols Variable Font** | Custom static font asset replacing deprecated material-icons-extended |
| **AndroidX Palette** | Dynamic color extraction from images |

### Audio & Media

| Technology | Purpose |
|:---|:---|
| **Media3 ExoPlayer** | Professional-grade media playback engine |
| **FFmpeg Decoder** | Extended codec support (EAC3-JOC, AC-3, AC-4, WMA) via the Jellyfin media3-ffmpeg-decoder extension |
| **Audio Processors** | Replay Gain, Bass Boost, Virtualizer, and Mono downmix DSP chain |
| **MediaStore API** | Android media content provider |
| **AudioFocus** | Audio focus management for calls/notifications |

### Widgets

| Technology | Purpose |
|:---|:---|
| **Glance** | Modern reactive widgets with Material 3 design |
| **WorkManager** | Background widget updates |

### Programming Language

| Technology | Purpose |
|:---|:---|
| **Kotlin** | 100% Kotlin codebase |
| **Kotlin Coroutines** | Asynchronous programming |
| **Kotlin Flow** | Reactive streams and state management |

## 🎨 Architecture

### Design Pattern

**MVVM (Model-View-ViewModel) + Clean Architecture**

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  ┌──────────────────────────────────────────┐   │
│  │   Composables (Screens & Components)     │   │
│  └──────────────────────────────────────────┘   │
│                      ↕                           │
│  ┌──────────────────────────────────────────┐   │
│  │         ViewModels (State)               │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│                 Domain Layer                     │
│  ┌──────────────────────────────────────────┐   │
│  │      Use Cases (Business Logic)          │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │     Repository Interfaces                │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │      Models (Data Entities)              │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│                  Data Layer                      │
│  ┌──────────────────────────────────────────┐   │
│  │    Repository Implementations            │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │  Data Sources (Local & Remote)           │   │
│  │  • MediaStore                            │   │
│  │  • LRCLib API                            │   │
│  │  • Deezer API                            │   │
│  │  • Local Storage                         │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### Project Structure

```
app/src/main/java/chromahub/rhythm/app/
├── activities/                  # Main Android activities (e.g. MainActivity.kt)
├── core/                        # Core Shared Business/Domain Logic
│   └── domain/
│       ├── model/               # Core domain entities (Song, Album, Artist, Playlist)
│       ├── repository/          # Core repository interfaces
│       └── usecase/             # Core business use cases
├── features/                    # Feature Modules
│   ├── local/                   # Local media playback feature (Clean Architecture)
│   │   ├── data/                # Local repositories, room database, MediaStore integration
│   │   ├── di/                  # Local dependency injection configs
│   │   ├── domain/              # Local use cases and business logic
│   │   └── presentation/        # Local screens, viewmodels, themes, and views
│   └── streaming/               # Streaming server client feature (Clean Architecture)
│       ├── data/                # Remote repositories and networking clients
│       ├── di/                  # Streaming dependency injection configs
│       ├── domain/              # Streaming use cases
│       └── presentation/        # Streaming screens and viewmodels
├── infrastructure/              # Base Infrastructure layer
│   ├── audio/                   # ExoPlayer setup, controller, and FFmpeg configuration
│   ├── network/                 # Retrofit, OkHttp, and REST API definitions
│   ├── service/                 # MusicService.kt & background media session handlers
│   ├── widget/                  # Glance app widget definitions
│   └── worker/                  # WorkManager background sync/scan workers
├── shared/                      # Shared Cross-Cutting Presentation/Domain/Data components
│   ├── data/
│   ├── domain/
│   └── presentation/            # Shared UI elements, color themes, styling, and custom icons
└── util/ & utils/               # General utility files and extension functions
```

## 📦 Libraries & Dependencies

### AndroidX & Jetpack

```kotlin
// Core
androidx.core:core-ktx
androidx.core:core-splashscreen
androidx.lifecycle:lifecycle-runtime-ktx
androidx.lifecycle:lifecycle-viewmodel-compose
androidx.fragment:fragment-ktx

// Compose
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.compose.ui:ui-tooling

// Navigation
androidx.navigation:navigation-compose

// Media
androidx.media3:media3-exoplayer
androidx.media3:media3-session
androidx.media3:media3-ui

// Widgets
androidx.glance:glance-appwidget
androidx.work:work-runtime-ktx

// Room Database
androidx.room:room-runtime
androidx.room:room-ktx

// Other
androidx.palette:palette-ktx
```

### Networking

```kotlin
// HTTP Client
com.squareup.retrofit2:retrofit
com.squareup.retrofit2:converter-gson
com.squareup.okhttp3:okhttp
com.squareup.okhttp3:logging-interceptor

// JSON
com.google.code.gson:gson
```

### Image Loading

```kotlin
// Coil for Compose
io.coil-kt:coil-compose
```

### Utilities

```kotlin
// Permissions
com.google.accompanist:accompanist-permissions

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android
```

## 🔄 State Management

### StateFlow & Compose State

Rhythm uses Kotlin Flow and Compose state for reactive UI updates:

```kotlin
// ViewModel observes ExoPlayer state via MediaController
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private var mediaController: MediaController? = null

    val playbackState: StateFlow<@Player.State Int> = 
        MutableStateFlow(Player.STATE_IDLE)

    fun connect(controller: MediaController) {
        mediaController = controller
        controller.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                (playbackState as MutableStateFlow).value = state
            }
        })
    }
}
```

### Repository Pattern

Data access abstracted through repositories:

```kotlin
interface MusicRepository {
    fun getSongs(): Flow<List<PlayableItem>>
    fun getAlbums(): Flow<List<AlbumItem>>
    fun getArtists(): Flow<List<ArtistItem>>
    fun getPlaylists(): Flow<List<PlaylistItem>>
    suspend fun getSongById(id: String): PlayableItem?
    suspend fun getAlbumById(id: String): AlbumItem?
    suspend fun searchSongs(query: String): List<PlayableItem>
}
```

## 🎵 Audio Playback Architecture

### ExoPlayer Integration

```
┌─────────────────────────────────────┐
│         MusicService                │
│   (Foreground Service)              │
│                                     │
│  ┌──────────────────────────────┐  │
│  │      ExoPlayer               │  │
│  │  • Media3 ExoPlayer 1.11.0   │  │
│  │  • FFmpeg decoder extension  │  │
│  │  • Gapless playback          │  │
│  │  • Audio focus handling      │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaSession               │  │
│  │  • Playback state            │  │
│  │  • Queue management          │  │
│  │  • Media buttons             │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaNotification          │  │
│  │  • Playback controls         │  │
│  │  • Album art                 │  │
│  │  • Metadata display          │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
            ↕
┌─────────────────────────────────────┐
│     UI (Player Composables)         │
│  • Observe playback state           │
│  • Send playback commands           │
│  • Display metadata                 │
└─────────────────────────────────────┘
```

## 📱 Audio Processing Architecture

Rhythm chains audio effects through the `RhythmAudioProcessor` pipeline inside `RhythmPlayerEngine`:

```
┌────────────────────────────────────────────┐
│            RhythmPlayerEngine             │
│   (infrastructure/service/player/)        │
│                                            │
│  ┌────────────────────────────────────┐   │
│  │      RhythmAudioProcessor         │   │
│  │  • Replay Gain (album/track)      │   │
│  │  • Bass Boost                     │   │
│  │  • Virtualizer / Spatialization   │   │
│  │  • Mono downmix (RhythmMono       │   │
│  │    AudioProcessor)                │   │
│  └────────────────────────────────────┘   │
│                                            │
│  • Posture-aware player layouts (#529)     │
│  • Device-specific audio routing           │
└────────────────────────────────────────────┘
```

## 📱 Widget Architecture

### Glance Widgets (Modern)

```kotlin
class RhythmMusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            RhythmMusicWidgetContent()
        }
    }
}

@Composable
fun RhythmMusicWidgetContent() {
    // Observe playback data via GlanceState
    // Material 3 widget UI
    MaterialTheme {
        // Widget content with play/pause, skip, track info, album art
    }
}
```

### Background Updates

```kotlin
class RhythmWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Update widget data from current playback state
        GlanceAppWidgetManager(context)
            .getGlanceIds(RhythmMusicWidget::class.java)
            .forEach { glanceId ->
                RhythmMusicWidget().update(context, glanceId)
            }
        return Result.success()
    }
}
```

## 🔧 Build System

### Gradle Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

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
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
}
```

### Version Catalog

```toml
# gradle/libs.versions.toml
[versions]
agp = "9.3.1"
kotlin = "2.4.10"
ksp = "2.3.6"
composeBom = "2026.06.01"
material3 = "1.5.0-alpha25"
media3 = "1.11.0"

[libraries]
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-material3-android = { group = "androidx.compose.material3", name = "material3-android", version.ref = "material3" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
```

## 🧪 Testing

### Unit Tests
- ViewModel logic testing (JUnit 4)
- Repository testing
- Use case testing

### UI Tests
- Compose UI testing (Compose Test)
- Navigation testing
- Integration testing
- Macrobenchmark for baseline profiles

### Build & Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## 🔐 Security & Privacy

- **No Analytics**: Zero tracking code
- **Local Storage**: All data stored on device
- **Minimal Permissions**: Only essential permissions
- **FOSS Compliance**: Fully open source
- **Reproducible Builds**: Consistent APK generation

## 📊 Performance Optimizations

- **Lazy Loading**: Load music library on demand
- **Image Caching**: Coil caches album art efficiently
- **Background Processing**: WorkManager for non-urgent tasks
- **Compose Optimization**: Remember, derivedStateOf, keys
- **ExoPlayer Buffering**: Optimized buffer sizes

## 🔄 CI/CD

- GitHub Actions for automated builds (android.yml, beta.yml, release.yml)
- Automated testing on push
- Release automation with signing
- Code quality checks and linting
- Reproducible build support

---

**Want to contribute?** Check the [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing)! Questions? Ask in [Telegram](https://t.me/RhythmSupport) or [Discord](https://discord.gg/XjPyUYPQYc).
