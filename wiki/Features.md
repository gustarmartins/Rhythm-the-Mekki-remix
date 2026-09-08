# Rhythm Features Overview

Complete guide to all features and capabilities in Rhythm Music Player.

---
## 🎵 Audio Playback

### Professional Media3 ExoPlayer Engine
- **Gapless Playback**: Seamless transitions between tracks
- **High-Resolution Audio**: Support for FLAC, ALAC, WAV, AIFF, APE up to 192kHz/32-bit
- **Format Support**: MP3, AAC, Opus, Vorbis, FLAC, ALAC, WAV, AIFF, APE, WavPack, TAK, TTA, MIDI, AC-3, AC-4, EAC3-JOC (Dolby Atmos) via FFmpeg, WMA, DSD, DTS, DTS-HD MA, DTS:X, and more
- **High-Resolution Audio Mode**: Preserve native sample rates and bit depths, bypassing Android processing
- **Smart Buffering**: Optimized for smooth playback

### Advanced Playback Controls
- Play, Pause, Skip, Previous
- Seek with precision timeline
- Shuffle and Repeat modes (All, One, Off)
- Queue management with drag-and-drop reordering
- Playback speed control
- Sleep timer with fade-out
- Restore queue and playback position after restart (Persist Queue)

---
## 🎨 Material You Design

### Dynamic Theming
- **Material 3 Expressive**: Updated adaptive shapes, fluid motion, and refined components
- **Material You**: Automatic color extraction from wallpaper
- **Light/Dark Modes**: System, light, or dark theme
- **Custom Color Schemes**: Create personalized palettes
- **Festive Themes**: Seasonal decorations (Christmas, Halloween, etc.)
- **Font Customization**: Choose from system fonts or import custom fonts

### Modern UI Components
- Edge-to-edge design with gesture navigation
- Smooth animations and transitions
- **Responsive layouts for phones, tablets, and foldables** — tablet-optimized multi-pane layout
- **Posture-Aware Player**: The player adapts its layout to the device posture (tabletop, book, separated) on foldables
- **Material Symbols**: Variable-weight icon font for crisp, scalable icons
- Split-screen and multi-window support
- Adaptive icons and dynamic shortcuts

---
## 🎤 Synchronized Lyrics

### LRCLib Integration
- **Real-time Lyrics**: Word-by-word synchronized highlighting
- **Full-Screen View**: Immersive lyrics experience — tap to expand
- **Lyrics Widget**: Home screen widget showing current track lyrics
- **Community Database**: Access to thousands of synced lyrics
- **Karaoke Mode**: Follow along with precise timing
- **Seek by Tapping**: Jump to specific lyrics timestamp
- **Auto-Scroll**: Automatic scrolling during playback
- **Manual Reload**: Refresh lyrics on demand

### Lyrics Management
- Sync adjustment controls (±offset)
- Support for embedded lyrics (ID3, Vorbis comments)
- Multiple language support
- Save custom sync offsets per track
- Offline embedded lyrics fallback

---
## 🎛️ Audio Enhancement

### 10-Band Professional Equalizer
- **Frequency Bands**: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
- **Preset Profiles**: Flat, Rock, Pop, Jazz, Classical, Electronic, Hip Hop, Vocal Boost
- **Custom EQ**: Create and save personalized profiles
- **Real-time Processing**: Instant audio adjustments
- **Import/Export**: Share EQ profiles

### AutoEQ Integration
- **6032+ Device Presets**: Optimized profiles for headphones and speakers
- **Automatic Detection**: Recognize connected audio devices
- **Smart Suggestions**: Recommend profiles for detected devices
- **Profile Search**: Find presets by brand and model
- **Device Management**: Save and organize your audio devices

### Replay Gain
- **Album Gain**: Maintains dynamic range within albums
- **Track Gain**: Normalizes perceived loudness per song
- **Configured in**: Settings → Queue & Playback → Playback → Replay Gain

### Crossfade
- **Smooth Transitions**: Blends the end of one track into the start of the next
- **Adjustable Duration**: 1–12 seconds
- **Configured in**: Settings → Queue & Playback → Playback → Crossfade

### Audio Effects
- **Bass Boost**: Enhance low frequencies (0-1000)
- **Virtualizer/Spatial Audio**: 3D sound effect
- **Mono Audio**: Downmix stereo channels to a center mono channel for single-earpiece listening, with a dedicated device toggle
- **Volume Normalization**: Consistent playback levels
- **Audio Focus**: Automatic pause for calls and notifications

---
## 📚 Library Management

### Smart Organization
- **Songs**: All tracks with sorting and quality-based filtering
- **Liked**: Your favorite (♥) tracks
- **Playlists**: Custom and auto-generated playlists
- **Albums**: Album-based browsing with artwork
- **Artists**: Artist-based organization with album grouping
- **Album Artists**: Grouping by album artist rather than track artist
- **Dates**: Tracks organized by release date
- **Explorer**: File system browser

### Quality-Based Filter Chips
The Songs tab includes dynamic quality filter chips that appear only when matching tracks exist:
- **Lossless tiers**: Studio Master → Hi-Res Lossless → CD Quality → Lossless
- **Surround formats**: Dolby Atmos, Dolby Digital Plus, Dolby Digital, DTS, Dolby / Surround
- **Special**: DSD, Lossy, Mono
- **Bitrate**: High Quality (≥320kbps), Standard (128–319kbps)
- **Duration**: Short (<3m), Medium (3–5m), Long (>5m)

### A–Z Scroll Bar
- **Alphabetical Navigation**: Drag along letter strip to jump to songs, albums, or artists
- **Auto-appears**: When library exceeds threshold
- **Configured in**: Settings → Library & Media → Library Settings

### Search & Library Exploration
- **Instant Search**: Real-time results across all categories
- **Multi-Category Search**: Search songs, albums, artists simultaneously
- **Recent Searches**: Quick access to previous searches
- **Search Filters**: Filter by artist, album, or genre

### Metadata Editing
- Edit song title, artist, album, genre
- Modify track number and year
- Album artwork editing (embed or fetch online)
- Batch metadata operations
- Automatic metadata detection

---
## 📋 Playlist Features

### Playlist Management
- **Create/Edit/Delete**: Full playlist control
- **Drag & Drop Reordering**: Visual song arrangement
- **Multi-Select**: Batch add/remove operations
- **Default Auto-Playlists**: Recently Added and Most Played
- **Playlist Artwork**: Auto-generated from songs

### Import/Export
- **Formats**: JSON, M3U, M3U8, PLS
- **Bulk Export**: Export all playlists at once
- **Custom Locations**: Choose export directory
- **Import from Files**: Load external playlists
- **Cross-Platform**: Compatible with other players

---
## 🛡️ Hearing Safety (Rhythm Guard)

Rhythm includes a dedicated safety system to protect your hearing:
- **Age-Aware Defaults**: Automatic volume capping based on user age.
- **Manual Protection**: User-configurable volume limits to prevent hearing damage.
- **Safety Alerts**: Notifications when volume levels exceed safe thresholds.
---
## 🌐 Go Mode (Streaming)

Beyond local playback, Rhythm supports a complete streaming ecosystem through **Go Mode**:
- **Dual-Mode Architecture**: Seamlessly switch between Local and Go (streaming) modes from onboarding or Settings.
- **Server Integration**: Connect to external streaming servers (Subsonic, Navidrome, Jellyfin).
- **Shared UI**: Streaming browsing and playback reuse the same library and player screens as local mode.
- **Nearby Server Discovery**: Automatically discover Subsonic-compatible servers on your local network — no manual address entry needed.
- **Rhythm Go Downloads**: Download tracks from your streaming server for offline playback. Perfect for commuting, flights, or areas with limited connectivity. Configure in Settings → Advanced → Experimental Features → Go Mode.
---
## 📊 Playback Statistics


### Rhythm Stats 2.0
- **Overview**: Total play count, listening time, top genres
- **Songs**: Most played tracks
- **Albums**: Most played albums
- **Artists**: Favorite artists ranking
- **History**: Recent plays with timestamps
- **Play Count**: Track listening frequency
- **Total Playtime**: Accumulated listening hours

### Listening Insights
- Daily, weekly, monthly breakdowns
- Genre distribution analysis
- Peak listening times
- Favorite artists ranking

---

## 📱 Home Screen Widgets

### Glance Widgets (Modern)
- **4 Widgets**: Music, Cookie, Stats, and Lyrics widgets with adaptive responsive sizes
- **Material 3 Design**: Dynamic colors and theming
- **Real-time Updates**: Instant playback state sync
- **Playback Controls**: Play/pause, skip controls
- **Album Art**: Dynamic artwork display
- **Customizable**: Corner radius and transparency settings

### Lyrics Widget
- **Home Screen**: Display current track lyrics at a glance
- **Real-time Updates**: Synced with playback position
- **Compact Layout**: Minimal footprint while showing full text

---

## 🌐 Online Integration

### LRCLib
- Community-driven lyrics database
- Automatic lyrics fetching
- Configurable API endpoints
- Offline fallback support

### Deezer API
- High-quality album artwork
- Automatic artwork downloading
- Smart fallback images
- Configurable fetch settings

### GitHub Updates
- Automatic update checking
- In-app update notifications
- Direct APK downloads
- Smart update polling
- Background update checks

### Optional Integrations
- **Deezer** — high-quality album artwork fetching
- **YouTube Music** — additional artwork lookup
- **LRCLib** — community synced lyrics database

---

## ⚙️ Settings & Customization

### Playback Settings
- Audio focus behavior
- Resume on device reconnect (headphones / Bluetooth)
- Gapless playback toggle
- Crossfade options

### Library Settings
- Blacklist/whitelist folder modes
- **Allowed Formats**: Include/exclude specific audio formats from the library (e.g., MP3, FLAC, OGG, M4A, Opus, MP4/MKV audio, Dolby, DSD)
- Block or allow individual songs, clear all songs
- Library tab order and Combine Discs options
- Artwork source preferences

### Appearance Settings
- Theme selection (Light/Dark/System)
- Material You dynamic colors
- Festive theme toggles
- Font selection
- Home screen customization

### Advanced Features
- Settings search
- Backup & Restore with auto-backup scheduling
- Crash log history

---

## 🔔 Notifications

### Media Notification
- **Rich Controls**: Play/pause, skip, previous, favorite
- **Album Artwork**: Dynamic cover art
- **Seek Bar**: Timeline scrubbing
- **Metadata**: Song, artist, album info
- **Persistent**: Continues in background
- **Android Style**: Modern notification design

### Update Notifications
- New version alerts
- Automatic check scheduling
- Smart polling intervals
- Silent background checks
- Customizable notification preferences

---

## 🔒 Privacy & Security

### Privacy First
- ✅ No analytics or tracking
- ✅ No personal data collection
- ✅ All data stored locally
- ✅ No background data transmission
- ✅ Optional internet features only

### Permissions
- Granular permission controls
- Transparent permission usage
- Android media permissions
- Scoped storage
- Minimal permission requirements

---

## 💾 Backup & Restore

### Backup Features
- Full settings backup (file or clipboard)
- Playlist export
- Custom EQ profiles
- Auto-backup scheduling

### Restore Options
- Settings restoration
- Playlist import
- Cross-device sync
- Version migration support

---

## 🎯 Gestures & Controls

### Playback Gestures
- Swipe up/down: Volume control
- Swipe left/right: Skip tracks (configurable)
- Double-tap: Play/pause
- Long-press: Context menu
- Drag album art: Seek
- Tap lyrics timestamp: Seek to that point in the song

### Library Gestures
- Pull-to-refresh: Rescan library
- Swipe actions: Quick playlist add
- Pinch-to-zoom: Grid density (planned)

---

## 🔄 Updates & Maintenance

### Auto-Update System
- GitHub integration
- Background update checks
- Smart polling (battery-aware)
- In-app installation
- Update notifications
- Version tracking

### Maintenance Features
- Cache size limits with auto-trim
- Clear all cache or lyrics cache
- Storage usage monitoring

---

## 📖 Additional Features

### File Explorer
- Browse music by folder structure
- File-based organization
- Quick folder access
- External storage support

### Queue Management
- Current queue viewing
- Queue history
- Save queue as playlist
- Clear queue option
- Queue shuffle

### Accessibility
- Screen reader support
- High contrast modes
- Large text options
- Haptic feedback controls
- Keyboard navigation

---

---

**Want more details?** Check out:
- [Getting Started Guide](https://github.com/cromaguy/Rhythm/wiki/Getting-Started)
- [Audio Formats](https://github.com/cromaguy/Rhythm/wiki/Audio-Formats)
- [Technology Stack](https://github.com/cromaguy/Rhythm/wiki/Technology-Stack)
- [FAQ](https://github.com/cromaguy/Rhythm/wiki/FAQ)

**Have questions?** Join our [Telegram Community](https://t.me/RhythmSupport)!
