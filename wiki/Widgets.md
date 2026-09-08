# Rhythm Home Screen Widgets

Complete guide to setting up and customizing Rhythm's home screen widgets.

---

## 📱 Widget Types

Rhythm offers three types of widgets to suit different needs:

### Lyrics Widget
- **Purpose**: Display current track lyrics on your home screen
- **Real-time**: Synced with playback position
- **Compact**: Minimal footprint with scrolling text

### Glance Widgets
- **Technology**: Built with Jetpack Glance framework
- **Design**: Material 3 with dynamic theming
- **Variants**: Music, Cookie, Stats, and Lyrics widgets
- **Layouts**: Adaptive responsive sizes
- **Requirements**: Android 8.0+
- **Features**: Real-time updates, customizable appearance

---

## 🎨 Glance Widgets (Recommended)

### Available Layouts

#### 1. Extra Small (2x1)
- **Size**: 110dp × 48dp (2×1 cells)
- **Features**:
  - Song title and artist (scrolling text)
  - Play/pause button
  - Compact horizontal layout
- **Best for**: Quick glance at current track

#### 2. Small (2x2)
- **Size**: 120dp × 120dp (2×2 cells)
- **Features**:
  - Album artwork
  - Song title and artist
  - Play/pause and skip controls
  - Compact square layout
- **Best for**: Minimal home screen footprint

#### 3. Medium (3x2)
- **Size**: 250dp × 120dp (3×2 cells)
- **Features**:
  - Album artwork
  - Song title, artist, album
  - Full playback controls
  - Horizontal layout
- **Best for**: Standard widget size

#### 4. Wide (4x2)
- **Size**: 320dp × 120dp (4×2 cells)
- **Features**:
  - Large album artwork
  - Complete metadata
  - Full playback controls
  - Extended horizontal layout
- **Best for**: Wide home screens

#### 5. Large (3x3)
- **Size**: 250dp × 250dp (3×3 cells)
- **Features**:
  - Large album artwork
  - Complete track information
  - Full playback controls
  - Vertical layout with maximum info
- **Best for**: Dedicated music widget space

#### 6. Extra Large (4x4)
- **Size**: 320dp × 320dp (4×4 cells)
- **Features**:
  - Maximum album artwork display
  - All metadata visible
  - Large playback controls
  - Premium visual experience
- **Best for**: Music-focused home screen

---

## 🔧 Adding a Widget

### Android 12+

1. **Long-press** on your home screen
2. Tap **Widgets**
3. Scroll to find **Rhythm** widgets
4. Choose between:
   - **Music Widget** (Glance)
   - **Cookie Widget**
   - **Stats Widget**
   - **Lyrics Widget**
5. **Drag** to home screen
6. **Resize** to desired size using corner handles
7. Widget auto-updates to optimal layout

### Android 8-11

1. **Long-press** on empty home screen area
2. Tap **Widgets** in popup menu
3. Find **Rhythm** in widget list
4. **Long-press and drag** widget to home screen
5. **Drop** in desired location
6. **Resize** using corner/edge handles

---

## ⚙️ Widget Customization

### Glance Widget Settings

Access widget settings: **Rhythm → Settings → Home & Widgets → Widget Settings**

#### Corner Radius
- **Range**: 0-32dp
- **Default**: 24dp
- **Effect**: Rounded corners for modern look
- **Note**: Glance widgets only

#### Transparency
- **Range**: 0-100%
- **Default**: 85%
- **Effect**: Widget background opacity
- **Note**: Adapts to wallpaper

#### Album Art Display
- **Toggle**: Show/hide album artwork
- **Default**: Enabled
- **Effect**: Display current track artwork

#### Information Display
- Show/hide song title
- Show/hide artist name
- Show/hide album name
- Customizable metadata visibility

### Auto-Update Behavior

Widgets update automatically when:
- Track changes
- Playback state changes (play/pause)
- Metadata is edited
- Artwork is updated

**Update Method**: WorkManager background tasks
**Frequency**: Real-time for playback events
**Battery Impact**: Minimal, optimized scheduling

---

## 🎯 Widget Controls

### Available Actions

#### Playback Controls
- **Play/Pause**: Toggle playback
- **Skip Next**: Jump to next track
- **Skip Previous**: Return to previous track
- **Tap Album Art**: Open Rhythm app

#### Limitations
- No seek bar (due to widget size constraints)
- No volume control (use system controls)
- No queue management (open app for full features)

---

## 🔄 Widget Updates

### Real-Time Updates

Widgets update instantly for:
- ✅ Track changes
- ✅ Play/pause state
- ✅ Album artwork changes
- ✅ Metadata edits

### Background Updates

- **Technology**: WorkManager periodic tasks
- **Frequency**: Event-driven
- **Battery**: Optimized with job scheduling
- **Restrictions**: Respects Android Doze mode

---

## 🐛 Troubleshooting

### Widget Not Updating

**Solutions:**
1. **Disable Battery Optimization**:
   - Settings → Apps → Rhythm → Battery
   - Select "Unrestricted"

2. **Grant Notification Permission**:
   - Required for foreground service
   - Settings → Apps → Rhythm → Permissions → Notifications

3. **Remove and Re-add Widget**:
   - Long-press widget → Remove
   - Add widget again from widget menu

4. **Update Rhythm**:
   - Check for latest version
   - Install updates from GitHub/F-Droid

### Widget Shows "Loading" or Blank

**Causes:**
- App not running in background
- Permissions not granted
- Widget cache corruption

**Solutions:**
1. Open Rhythm app once
2. Play a song
3. Minimize app (don't force close)
4. Widget should populate

### Widget Controls Not Working

**Checks:**
- Ensure Rhythm has required permissions
- Verify app is not force-stopped
- Check notification access is enabled
- Restart device if issue persists

### Widget Appears Stretched or Cropped

**Launcher Compatibility:**
- Some launchers have widget sizing issues
- Try different widget layout size
- Use stock launcher for best compatibility
- Report launcher-specific issues on GitHub

---

## 💡 Best Practices

### Optimal Widget Sizes

- **Phone Portrait**: 3×2 Medium or 4×2 Wide
- **Phone Landscape**: 4×2 Wide or 5×2 Extended
- **Tablet**: 4×4 Extra Large or 5×5 Maximum
- **Foldable (Closed)**: 2×2 Small or 3×2 Medium
- **Foldable (Open)**: 4×4 or 5×5

### Battery Optimization

- Widgets are highly optimized
- Updates only when necessary
- No polling for changes
- Event-driven architecture
- Minimal battery impact (<1%)

### Home Screen Design Tips

1. **Placement**: Put widget where you frequently glance
2. **Size**: Match widget to your usage (controls vs. artwork)
3. **Theme**: Glance widgets auto-adapt to Material You colors
4. **Wallpaper**: Choose wallpaper that complements widget transparency
5. **Density**: Don't overcrowd home screen with multiple widgets

---

## 🔮 Upcoming Widget Features

### Planned
- Custom widget themes
- Multiple widget instances with different settings
- Lock screen widgets (Android 13+)
- Queue preview
- Playback progress bar

---

## 📊 Widget Features

| Feature | Glance Widgets |
|:--------|:---------------|
| **Material 3 Design** | ✅ Yes |
| **Dynamic Theming** | ✅ Full |
| **Customizable Corners** | ✅ Yes |
| **Transparency Control** | ✅ Yes |
| **Widgets** | Music, Cookie, Stats, Lyrics |
| **Sizes** | Adaptive responsive |
| **Battery Efficiency** | ✅ Optimized |
| **Update Speed** | ⚡ Instant |

---

## 🔗 Related Documentation

- [Getting Started](https://github.com/cromaguy/Rhythm/wiki/Getting-Started)
- [Features Overview](https://github.com/cromaguy/Rhythm/wiki/Features)
- [Getting Started](https://github.com/cromaguy/Rhythm/wiki/Getting-Started)
- [Troubleshooting](https://github.com/cromaguy/Rhythm/wiki/Troubleshooting)

---

**Need help?** Join our [Telegram Community](https://t.me/RhythmSupport) or [Discord Server](https://discord.gg/XjPyUYPQYc)!
