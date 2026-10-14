# Native Music Assistant integration — 6.1 Nightly development

The target is to browse a Music Assistant library in Rhythm's existing screens
and listen through Rhythm's native player. A server filesystem track is a
provider-backed media item, not a file path or HTTP download on the phone.

The first native implementation is the catalog and control API layer under
`features/musicassistant/api`. It does not yet connect Music Assistant to the
Rhythm GO provider picker or replace the phone audio engine. The existing
experimental web workspace remains available on Nightly. Stable 6.0.0 excludes
both integrations.

## Implemented adapters

- Direct HTTP/HTTPS connection, including Tailscale addresses and reverse-proxy
  path prefixes; built-in account login and bearer-token API requests.
- Paged library tracks, provider search, individual track details and album
  tracks, mapped into Rhythm's shared provider-song model. Provider-qualified
  identity, FLAC/MP3 format metadata and embedded plain/timed lyrics are retained.
- Player discovery, active group queue resolution and paged queue occurrences.
  Repeated songs keep separate `queue_item_id` identities.
- Explicit Play, append, pause/resume/stop/next/previous, volume, seek, queue
  transfer, favorite state and group membership commands. Transfers preserve
  the source's pause state by omitting `auto_play`.
- Cancellable requests, bounded response size, no redirect forwarding, no
  automatic mutation retries and sanitized failures. An invalid response raises
  an error instead of reporting a successful empty library.

Credentials are supplied in memory to this layer. It does not persist them or
borrow the WebView login. The future native account UI must use private secure
storage excluded from backup, independently of the existing WebView profile.

These adapters do not automatically execute against any server. The caller must
select a player, resolve its active queue, and reconcile server state after
mutations. `append` means append to the queue tail; it must not be presented as
Rhythm's FIFO Play Next. That integration needs reservation ordering, server
occurrence reconciliation and cancellation handling before it can be exposed.

## What your friend would have today

| Capability | Existing Nightly web workspace | Native Rhythm integration |
| --- | --- | --- |
| Server FLAC/MP3 library and configured music providers | Supplied by the server frontend | Catalog adapters implemented; existing Rhythm screens not connected yet |
| Playback on this phone | Server web player | Sendspin audio/clock/pairing integration pending |
| Shared server queue and remote players | Server frontend controls | API adapters implemented; native controls pending |
| Compatible synchronized player groups and member volumes | Server frontend controls | Group/volume API adapters implemented; native UI and synchronized phone output pending |
| Direct LAN/Tailscale server URL | Available | HTTP transport implemented |
| Remote ID over WebRTC | Not implemented by Rhythm's connection screen | Signaling, authentication and data-channel transport pending |
| Rhythm Bluetooth lyrics, DSP and Android Auto behavior for server audio | Web audio has a separate playback path | Media3 service/Sendspin integration and device validation pending |
| Offline original files and embedding edited artwork/tags | Not provided by this integration | Needs provider-specific capability; a preview URL is not a full-track download |

## Next integration steps

1. Native account storage and catalog provider wiring, including authenticated
   artwork and paging for albums, artists and playlists.
2. Native Sendspin player with a Media3 adapter: server-owned queue/state,
   synchronized clock/output, audio focus, background lifecycle and balanced
   JamesDSP session broadcasts. Keep local queue continuation and crossfade out
   of server-owned queues. Preserve FIFO requests and Bluetooth compatibility.
3. Native player/group picker, shared queue events and transfer controls.
4. WebRTC Remote ID signaling and transports for API and Sendspin audio.

The official mobile client's encrypted Sendspin implementation requires server
API schema 45 or newer. The friend's server version is not yet known. That must
be checked before choosing a compatible native playback path; older versions
must receive an explicit unsupported-version result, not a silent preview-file
or web-player substitution.

## Evidence and validation

The adapters were checked against the official
[server API implementation](https://github.com/music-assistant/server/tree/bb8a6fe866712ec98f83111d90daeebc4ecf9457/music_assistant/controllers)
and [mobile client](https://github.com/music-assistant/mobile-app/tree/d1f3c7fc0a9eb7d8d45beccbe45583d95bd0679e).
No upstream implementation has been copied into this API layer. Protocol/audio
module reuse, if added later, must preserve upstream licensing and attribution.

`MusicAssistantApiTest` uses mock HTTP responses to cover request/auth contracts,
proxy paths, metadata, queue occurrences, paused transfers, ordered appends,
group controls, favorites, errors, redirects, retry prevention, cancellation and
response limits. This is contract coverage, not a real-server compatibility or
listening result. Cloud CI additionally runs the full existing JVM suite and
builds/verifies both signed release flavors before publication.

### Real-server validation — October 14, 2026

The adapters were also exercised against the official **2.10.6 Stable** server
image (API schema **65**) using an existing music library mounted read-only.
Native Kotlin calls passed login, paging, search, FLAC/MP3 details, album lookup,
embedded lyrics, favorite write/read/removal, duplicate queue occurrences,
ordered append, transfer without autoplay, member volume and group membership.
Two muted, instrumented browser Sendspin players received nonzero decoded audio.
This verifies the server and API layer; it does not qualify Rhythm's unfinished
native audio engine, physical speaker synchronization or Android Auto.

The test exposed and corrected three assumptions:

- Stable does not provide `music/favorites/set_item`. Use the portable add/remove
  commands, resolving a provider item to its library identity before removal.
- Cold queue preparation can exceed the HTTP client's default 10-second read
  timeout. Queue play/transfer get a bounded 90-second deadline; ordinary calls
  remain bounded at 30 seconds, cancellation works, and mutations are not retried.
- Sendspin may implement pause as a stop with retained `resume_pos`. Keep that
  position independently of `elapsed_time`, which may be zero after transfer.

An exact title containing ` - ` returned no matches through the server's own
search API while a shorter query found it; this server search behavior remains.
The test server also restarted once during setup/testing; the cause was not
established. Subsequent completed checks passed. Long-run server stability and
physical playback are not claimed.
