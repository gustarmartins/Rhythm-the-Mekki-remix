# Music Assistant in Rhythm Go

Choose **Music Assistant** in Rhythm Go's provider picker (including first-run
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
| USB bit-perfect playback | Not qualified; the current Sendspin receiver advertises 48 kHz, 16-bit stereo |
| Legacy-car one-item queue view | Local playback retains its existing compatibility view; MA queue collapse is disabled to avoid rewriting a shared server queue |

Native audio requires Android 9 or newer and a server compatible with the
bundled encrypted Sendspin protocol. The official mobile implementation requires
API schema 45 or newer; the lab server is 2.10.6 Stable, schema 65. Older servers
and WebRTC-only connections have not been qualified.

## Validation

The official **2.10.6 Stable** container was tested using an existing library
mounted read-only: 1,846 audio-file mappings, unified by MA into 1,827 tracks.
The API adapters passed real-server authentication, catalog, metadata, favorites,
queue occurrences, ordered append, paused transfers, volume and group controls.
Earlier browser Sendspin probes received nonzero decoded audio. Those checks do
not constitute native Rhythm Android or physical speaker acceptance.

Cloud validation compiles the Android app and passes **365 application tests**
and **198 Sendspin tests**. The imported protocol tests include reference crypto
vectors, connection lifecycle, audio scheduling, pairing and redacted logging.
A fresh platform ChaCha cipher per operation fixes a cipher-pooling failure
found by these tests.

A real-server Android instrumentation test exercises the native repository and
actual MediaPlaybackService: FLAC, MP3, pause, seek, FIFO additions, duplicate
occurrences and lyric metadata without a playback restart. It requires privately
supplied credentials and never includes them in source or build artifacts.

**Device qualification is pending.** Munch exposed the missing Android 17 LAN
permission, which is now declared and requested during connection setup. The
corrected APK was installed, but the phone restarted during verification and
became unreachable before the playback test could run. No native listening,
physical Bluetooth/car, group synchronization or Android Auto result is claimed.

The Sendspin module is adapted from the official
[Music Assistant mobile client](https://github.com/music-assistant/mobile-app/tree/d1f3c7fc0a9eb7d8d45beccbe45583d95bd0679e),
under Apache 2.0. Its license, provenance and adaptation notice are included in
`sendspin/` and the APK's license assets. The Stable 6.0 release remains MA-free.
