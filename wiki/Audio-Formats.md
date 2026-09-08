# Audio Format Support

Rhythm uses **Media3 ExoPlayer 1.11.0 + FFmpeg Decoder** (Jellyfin media3-ffmpeg-decoder) for professional-grade audio playback. This page details supported formats, technical limitations, and recommendations.

---

## ✅ Built-in Formats (Platform Decoders)

These formats use Android's platform codecs and work on all supported devices:

| Format | Container | Quality | Bit Depth | Sample Rate | Notes |
|:---:|:---:|:---:|:---:|:---:|:---|
| **FLAC** | `.flac` | Lossless | Up to 32-bit | Up to 384kHz | Recommended for lossless audio |
| **ALAC** | `.m4a` | Lossless | Up to 32-bit | Up to 384kHz | Apple Lossless Audio Codec |
| **MP3** | `.mp3` | Lossy | N/A | Up to 48kHz | All bitrates, VBR support |
| **AAC** | `.m4a`, `.aac`, `.mp4`, `.adts`, `.m4b` | Lossy | N/A | Up to 96kHz | AAC-LC, HE-AAC, HE-AACv2; ADTS, audiobook (M4B) |
| **MP4 Audio** | `.mp4`, `.m4a` | Lossy/Lossless | N/A | Up to 96kHz | Audio tracks inside MP4 containers (AAC/ALAC/AC-3) |
| **Matroska Audio** | `.mkv`, `.mka` | Lossy/Lossless | N/A | N/A | Audio streams inside MKV containers (EAC3-JOC, DTS, etc.) |
| **OPA** | `.opa` | Lossless | N/A | N/A | Optimized audio format (OPUS-based) |
| **Vorbis** | `.ogg`, `.oga` | Lossy | N/A | Up to 192kHz | Ogg Vorbis audio |
| **Opus** | `.opus`, `.ogg` | Lossy/Lossless | N/A | Up to 48kHz | Modern, efficient codec |
| **WAV** | `.wav` | Lossless | Up to 32-bit | Up to 192kHz | Uncompressed PCM audio |
| **PCM** | Various | Lossless | Up to 32-bit | Up to 192kHz | Raw audio data |
| **AIFF** | `.aiff`, `.aif` | Lossless | Up to 32-bit | Up to 192kHz | Apple audio interchange format |
| **MIDI** | `.mid`, `.midi` | Instructions | N/A | N/A | Musical Instrument Digital Interface |

---

## 🎬 FFmpeg-Decoded Formats

These formats are decoded by the bundled FFmpeg extension (available in all builds):

| Format | Container | Quality | Bit Depth | Sample Rate | Notes |
|:---:|:---:|:---:|:---:|:---:|:---|
| **APE** | `.ape` | Lossless | Up to 24-bit | Up to 192kHz | Monkey's Audio (high compression) |
| **WavPack** | `.wv` | Lossless | Up to 32-bit | Up to 192kHz | Hybrid lossless/lossy format |
| **TAK** | `.tak` | Lossless | Up to 24-bit | Up to 192kHz | TOM's lossless Audio Kompressor |
| **TTA** | `.tta` | Lossless | Up to 24-bit | Up to 96kHz | True Audio lossless |
| **WMA** | `.wma` | Lossy | N/A | Up to 48kHz | Windows Media Audio |
| **WMA Lossless** | `.wma` | Lossless | Up to 24-bit | Up to 96kHz | Windows Media Audio Lossless |
| **AC-3 (Dolby Digital)** | `.ac3`, `.m4a` | Lossy | N/A | Up to 640kbps | Decoded via FFmpeg; may also use hardware decoder |
| **EAC3-JOC (Dolby Atmos)** | `.eac3`, `.m4a`, `.mkv` | Lossy | N/A | Up to 6Mbps | Decoded via FFmpeg; stereo/surround output |
| **AC-4 (Dolby AC-4)** | `.ac4` | Lossy | N/A | Up to 448kbps | Decoded via FFmpeg extension |

---

## ⚠️ Device-Dependent Formats

These require hardware support and may not work on all devices:

| Format | Container | Notes |
|:---:|:---:|:---|
| **DTS** | `.dts`, `.m4a` | Requires compatible device/hardware decoder |
| **DTS-HD MA** | `.dts`, `.m4a` | High-resolution multichannel DTS |
| **DTS:X** | `.dts`, `.m4a` | Object-based DTS surround |
| **DSD** | `.dsd`, `.dsf`, `.dff` | Super Audio CD; requires compatible DAC |

### Checking Device Compatibility

To check if your device supports Dolby/DTS:
1. Try playing a test file in Rhythm
2. If it plays, your device has the necessary decoder
3. If it fails, convert to a supported format (see below)

---

## 📊 Audio Quality Detection

Rhythm automatically detects and displays quality badges in the player, and provides **quality-based library filters** in the Songs tab:

### Quality Badges

| Badge | Criteria | Formats |
|:---:|:---|:---|
| **STUDIO MASTER** | ≥24-bit/192kHz lossless | FLAC, ALAC, WAV, AIFF |
| **HI-RES LOSSLESS** | ≥48kHz lossless (not Studio Master) | FLAC ≥48kHz, ALAC ≥48kHz |
| **CD QUALITY** | 16-bit/44.1kHz lossless | FLAC, ALAC, WAV, PCM, AIFF |
| **LOSSLESS** | Standard lossless compression | FLAC, ALAC, WAV, PCM, APE, WV, TAK, TTA, AIFF |
| **DSD** | Direct Stream Digital | DSD, DSF, DFF |
| **DOLBY ATMOS** | Dolby Atmos / TrueHD | EAC3-JOC, TrueHD |
| **DOLBY DIGITAL PLUS** | Enhanced Dolby | E-AC-3 |
| **DOLBY DIGITAL** | Standard Dolby | AC-3 |
| **DTS** | DTS surround | DTS, DTS-HD MA, DTS:X |
| **LOSSY** | Any lossy compression | MP3, AAC, OGG, Opus, WMA |

### Quality-Based Library Filters

The Songs tab in the Library screen provides dynamic filter chips that appear only when matching tracks are present:

| Filter Category | What It Shows |
|:---|:---|
| **Studio Master** | 24-bit/192kHz+ lossless tracks |
| **Hi-Res Lossless** | ≥48kHz lossless (excludes Studio Master, DSD, Dolby) |
| **CD Quality** | 16-bit/44.1kHz lossless |
| **Lossless** | Standard lossless (excludes hi-res, CD quality, DSD, Dolby) |
| **Lossy** | All lossy-compressed tracks |
| **DSD** | DSD/DSF/DFF tracks |
| **Dolby Atmos** | Dolby Atmos or TrueHD tracks |
| **Dolby Digital Plus** | E-AC-3 tracks |
| **Dolby Digital** | AC-3 tracks |
| **DTS** | DTS, DTS-HD MA, DTS:X tracks |
| **Dolby / Surround** | Any surround (Dolby, DTS, multichannel) |
| **High Quality** | Lossy ≥320kbps (not Dolby/surround) |
| **Standard** | Lossy 128–319kbps (not Dolby/surround) |
| **Mono** | Single-channel tracks |
| **Short / Medium / Long** | Duration-based filters |

### Quality Detection Details

- **Lossless Detection**: Based on codec type + file extension fallback
- **Hi-Res Detection**: Sample rate ≥48kHz or calculated bit depth ≥18-bit
- **Studio Master Detection**: Sample rate ≥192kHz or calculated bit depth ≥22-bit at ≥96kHz
- **CD Quality Detection**: Lossless, sample rate ≤48kHz, calculated bit depth <20-bit
- **Dolby/DTS Detection**: Codec identification from metadata
- **Container Detection**: Parsed from file headers

---

## 🔧 Technical Limitations

### Container Format Ambiguity
Some containers can hold multiple codecs:
- `.m4a` can contain: AAC, ALAC, AC-3, E-AC-3
- `.ogg` can contain: Vorbis, Opus, FLAC
- `.mp4` can contain: AAC, AC-3, video tracks

Rhythm identifies the **actual codec** inside, not just the container.

### MP4 / Matroska Audio (.mp4/.mkv/.mka)
Rhythm scans and plays audio streams embedded in MP4 and Matroska containers (including `.mka` audio-only files). During media scan, `video/mp4`, `video/x-matroska`, and `application/x-matroska` MIME types are indexed alongside regular audio files. Files extracted from these containers are identified by their **actual codec** (AAC, ALAC, AC-3, E-AC-3, DTS, etc.), not just the container extension.

> ⚠️ **Note:** MP4/MKV containers may be excluded from the default library if the **Allowed Formats** setting (`Settings → Library & Media → Media Scan → Allowed Formats`) is configured to filter them out — toggle the `mp4`/`mkv` formats there to include or exclude them.

### Hardware Dependencies
- **Dolby/DTS**: Requires device-specific hardware decoders
- **Sample Rate**: Limited by Android AudioTrack (typically 192kHz max)
- **Bit Depth**: Most Android devices support 16-bit or 24-bit
- **Channel Configuration**: Stereo universally supported, multi-channel varies

### ExoPlayer Capabilities
- **ExoPlayer 1.11.0 + FFmpeg**: Adds EAC3-JOC, AC-3, AC-4, WMA decoding beyond ExoPlayer defaults
- **Gapless Playback**: Supported for MP3, AAC, FLAC, Opus
- **Seeking**: Accurate for most formats, approximate for some streaming codecs
- **Metadata**: Depends on container format (ID3 for MP3, Vorbis comments for FLAC)
- **Album Art**: Embedded images in FLAC, MP3, M4A

---

## 💡 Format Recommendations

### For Maximum Compatibility
✅ **Use these formats**:
1. **FLAC** - Universal lossless support
2. **MP3** - Universal lossy support (320kbps or V0 for quality)
3. **AAC** - Modern lossy format (256kbps recommended)

### For Lossless Audio Libraries
✅ **FLAC is the gold standard**:
- Widely supported across all platforms
- Open-source and royalty-free
- Excellent compression (50-60% of WAV size)
- Preserves all audio data perfectly
- Supports embedded artwork and metadata

### For Hi-Res Audio
✅ **FLAC 24-bit/96kHz or higher**:
- Check your device's DAC capabilities first
- Most modern smartphones support up to 192kHz
- Diminishing returns beyond 96kHz for most listeners
- Use high-quality headphones/speakers to appreciate the difference

### For Space Efficiency
✅ **Modern lossy formats**:
- **Opus** @ 128-160kbps: Best quality-per-bitrate
- **AAC** @ 256kbps: Excellent quality, wide compatibility
- **MP3** @ 320kbps or V0: Still excellent for most listeners

---

## 🛠️ Converting Unsupported Formats

### Recommended Conversion Tools

#### Desktop (Windows/Mac/Linux)
1. **[FFmpeg](https://ffmpeg.org/)** (Command-line)
   ```bash
   # APE to FLAC
   ffmpeg -i input.ape output.flac
   
   # DSD to FLAC
   ffmpeg -i input.dsf output.flac
   
   # AIFF to WAV
   ffmpeg -i input.aiff output.wav
   ```

2. **[dBpoweramp](https://www.dbpoweramp.com/)** (Windows/Mac - Paid)
   - User-friendly GUI
   - Batch conversion
   - Preserves metadata

3. **[foobar2000](https://www.foobar2000.org/)** (Windows - Free)
   - Excellent converter
   - Extensive format support
   - Free and lightweight

#### Online (Web-based)
- **[CloudConvert](https://cloudconvert.com/)**: Supports 200+ formats
- **[Online-Convert](https://www.online-convert.com/)**: Audio conversion
- **[Zamzar](https://www.zamzar.com/)**: Various audio formats

⚠️ **Privacy Warning**: Online converters upload your files. Use desktop tools for sensitive/personal audio.

### Batch Conversion Scripts

For Linux/Mac users with many files:

```bash
#!/bin/bash
# Convert all APE files to FLAC in current directory
for file in *.ape; do
    ffmpeg -i "$file" "${file%.ape}.flac"
done
```

```bash
# Convert all DSD files to FLAC (24-bit/88.2kHz)
for file in *.dsf; do
    ffmpeg -i "$file" -sample_fmt s32 -ar 88200 "${file%.dsf}.flac"
done
```

---

## 🎯 Troubleshooting Playback Issues

### File Won't Play
1. **Check format**: Verify it's a supported format
2. **Inspect codec**: Use MediaInfo or FFprobe to identify actual codec
3. **Test on device**: Try playing in other apps (VLC, YouTube Music)
4. **Convert format**: Use FFmpeg to convert to FLAC or MP3

### Dolby/DTS Files Won't Play
- **Not supported by device**: Convert to FLAC or AAC
- **Check device specs**: Look for Dolby Atmos/DTS support in manufacturer specs
- **Try other apps**: Test if VLC or other players work

### Poor Audio Quality
- **Check bitrate**: Low bitrate = poor quality
- **Verify source**: Ensure original file is high quality
- **Use equalizer**: Apply AutoEQ preset for your device
- **Check output device**: Quality limited by headphones/speakers

### Metadata Not Showing
- **Re-tag files**: Use MP3Tag or Picard to fix metadata
- **Rescan library**: Settings → Library & Media → Media Scan
- **Check file permissions**: Ensure Rhythm has read access

---

## 📚 Additional Resources

### Format Information
- [Wikipedia: Audio Coding Format](https://en.wikipedia.org/wiki/Audio_coding_format)
- [Hydrogen Audio Wiki](https://wiki.hydrogenaud.io/)
- [ExoPlayer Supported Formats](https://exoplayer.dev/supported-formats.html)

### Metadata Tools
- **[Mp3tag](https://www.mp3tag.de/)** - Windows/Mac metadata editor
- **[MusicBrainz Picard](https://picard.musicbrainz.org/)** - Auto-tagging tool
- **[Kid3](https://kid3.kde.org/)** - Cross-platform tag editor

### Audio Analysis
- **[MediaInfo](https://mediaarea.net/MediaInfo)** - Technical file information
- **[FFprobe](https://ffmpeg.org/ffprobe.html)** - Command-line media analyzer
- **[Spek](https://www.spek.cc/)** - Acoustic spectrum analyzer

---

**Questions?** Ask in our [Telegram Community](https://t.me/RhythmSupport), [Discord Server](https://discord.gg/XjPyUYPQYc), or check the [FAQ](https://github.com/cromaguy/Rhythm/wiki/FAQ).
