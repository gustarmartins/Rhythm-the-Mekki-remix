# Music Assistant in Rhythm Go

Use **Open in Rhythm Go** in Go settings, or choose **Music Assistant** in
Rhythm Go's provider picker (including first-run
setup), enter the server URL, and sign in with a Music Assistant account. The
server's songs, albums, artists, playlists and search use Rhythm's existing UI.
Select a song to connect this phone's native audio receiver. Bluetooth audio
then follows the phone's normal output route.

The connection accepts LAN, Tailscale and HTTPS reverse-proxy URLs. The VPN must
already connect the phone to the server. A reverse proxy must forward WebSocket
connections. Use the server's direct URL, not Home Assistant ingress. Android 17
requires nearby/local-network access; Connect requests that permission.

## Native playback

Music Assistant delivers phone audio through Sendspin. A Media3 adapter exposes
the server's queue and playback state to Rhythm's existing player, notification,
lyrics and Bluetooth metadata code. The server owns track resolution and queue
occurrences; server URIs are never interpreted as phone filesystem paths or
preview downloads.

- Library, search, album/artist browsing, playlists, favorites and provider lyrics
  are connected to the native streaming repository.
- The account token and Sendspin identity are encrypted using an Android
  Keystore key and excluded from backup. Passwords are not persisted. Native
  authentication is separate from the optional server-administration WebView.
- Phone audio uses native decoders and AudioTrack, respects audio focus and
  headphone interruptions, and balances audio-effect OPEN/CLOSE notifications.
  Rhythm's bass, spatial and mono PCM processors are reused; session-based EQ
  and JamesDSP use the active native audio session.
- Media controls use the active server queue. Duplicate tracks retain separate
  occurrence IDs. Play Next requests are serialized, appended, moved in caller
  order and read back before acknowledgement. A failed mutation never replaces
  playback or blindly retries an ambiguous request.
- Server changes are refreshed while the native player is connected. Output
  controls are available in the player's output sheet and Go settings. Selecting
  an output observes its queue; **Move playback here** explicitly transfers the
  current queue while preserving pause state. Compatible players can be grouped
  and their volumes adjusted; the server decides compatibility.

The **Server administration** entry remains available for provider setup and
other MA administration. Normal Rhythm playback does not require its web player.

## Current boundaries

| Feature | Current behavior |
| --- | --- |
| Native phone playback and ordinary Rhythm UI | Implemented; device qualification described below |
| Rhythm lyrics and Bluetooth metadata | Uses the existing native formatter, timing and canonical track identity; physical car acceptance is separate |
| Group queues and remote outputs | Native control adapters; synchronization depends on server/player compatibility |
| Local phone files mixed into an MA queue | Not supported; the server cannot read Android content URIs |
| Offline downloads and embedded artwork/tag edits | Not exposed by this MA adapter; preview streams are never substituted for full originals |
| WebRTC Remote ID | Not implemented; use a reachable direct URL, including Tailscale |
| Crossfade, normalization and queue continuation | Server-owned; the phone's local dual-player engine does not rewrite shared queues |
| Phone-side skip-silence, ReplayGain and speed/pitch | Not connected to Sendspin; server-side audio processing is separate |
| USB bit-perfect playback | Not qualified; the current Sendspin receiver advertises 48 kHz, 16-bit stereo |
| Legacy-car one-item queue view | Local playback retains its existing compatibility view; MA queue collapse is disabled to avoid rewriting a shared server queue |

Native audio requires Android 9 or newer and a server compatible with the
bundled encrypted Sendspin protocol. The official mobile implementation requires
API schema 45 or newer; the lab server is 2.10.6 Stable, schema 65. Older servers
and WebRTC-only connections have not been qualified.

## Spotify, YouTube Music and other services

The native adapter consumes the music sources configured on the MA server; it
does not need a separate Rhythm screen or decoder for each source. Account
setup and provider availability remain server-specific. Only the filesystem
provider has been exercised in this lab.

- MA supports Spotify listing and playback with Premium. Its current
  [Spotify documentation](https://www.music-assistant.io/music-providers/spotify/)
  describes the available playback engines and their account requirements.
- MA's [YouTube Music provider](https://www.music-assistant.io/music-providers/youtube-music/)
  requires a paid account and cookie/token setup. It is an unofficial, best-effort
  integration, so it cannot be promised the same reliability as local files.
- For direct integrations, [Spotify App Remote](https://developer.spotify.com/documentation/android/tutorials/getting-started)
  controls the installed Spotify app instead of supplying audio to Rhythm's
  engine. [MusicKit for Android](https://developer.apple.com/musickit/) supports
  in-app Apple Music playback through Apple's SDK. Either needs a separate
  playback adapter and capability review; neither is implemented by this change.

These are feasibility findings from October 14, 2026, not account or playback
qualification for those services.

## Validation

The official **2.10.6 Stable** container was tested using an existing library
mounted read-only: 1,846 audio-file mappings, unified by MA into 1,827 tracks.
The API adapters passed real-server authentication, catalog, metadata, favorites,
queue occurrences, ordered append, paused transfers, volume and group controls.
Earlier browser Sendspin probes received nonzero decoded audio. Those checks do
not constitute native Rhythm Android or physical speaker acceptance.

Cloud validation compiles the Android app and passes **374 application tests**
and **198 Sendspin tests**. The imported protocol tests include reference crypto
vectors, connection lifecycle, audio scheduling, pairing and redacted logging.
A fresh platform ChaCha cipher per operation fixes a cipher-pooling failure
found by these tests.

A real-server Android instrumentation test exercises the native repository and
actual MediaPlaybackService: FLAC, MP3, pause, seek, FIFO additions, duplicate
occurrences and lyric metadata without a playback restart. It requires privately
supplied credentials and never includes them in source or build artifacts.

**Native Android regression passed on Munch (Poco F4, Android API 37).**
The real-server instrumentation test passed with 1,827 tracks: authentication,
protected token storage, catalog and search, FLAC and MP3 playback through
AudioTrack, advancing position, stable pause, seek, FIFO order, duplicate
occurrences and lyric metadata without queue replacement or rewind. The native
Go library UI also displayed server tracks and covers on a separate display.

This run caught and fixed a lyric/pause clock discontinuity: positions now use
the server timestamp instead of restarting elapsed time on every poll. UI search
also exposed a wait behind the library's artist-image downloads; search now uses
available artwork without waiting for that background pass.

Cloud run [38538978091](https://github.com/gustarmartins/Rhythm-the-Mekki-remix/actions/runs/38538978091)
compiled debug, instrumentation and signed minified release APKs. The device test
passed on its debug build from source `f1b1ddec` (one test, zero failures or skips).
The lyric assertion uses a fixture; physical listening, Bluetooth/car displays,
group synchronization and Android Auto still require separate acceptance.

The Sendspin module is adapted from the official
[Music Assistant mobile client](https://github.com/music-assistant/mobile-app/tree/d1f3c7fc0a9eb7d8d45beccbe45583d95bd0679e),
under Apache 2.0. Its license, provenance and adaptation notice are included in
`sendspin/` and the APK's license assets. The Stable 6.0 release remains MA-free.
