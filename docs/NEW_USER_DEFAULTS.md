# New-user media defaults

Only an empty `rhythm_preferences` store receives these defaults. Existing users,
including partial old preference stores and per-device lyric profiles, retain their
choices. The one-time artwork prompt is shown after onboarding, not on upgrades.

- Synced lyrics in media displays: on; 20 characters, zero timing offset, zero
  scroll tolerance, and a 600 ms metadata interval. Zero tolerance keeps chunks
  inside the configured width even on a static notification chip.
- Lyrics priority: local sidecar/cache, embedded lyrics, then enabled online
  providers. Missing or unreadable files and provider error messages fall through.
  Missing title/artist tags do not prevent extracting local/embedded lyrics.
  Better Lyrics requests use its current endpoint and include album/duration to
  match the public cache; authentication-required cache misses fall through to the
  other enabled providers. No API credentials are invented or automatically granted.
- Online integrations and supported provider switches: on. Features that require
  credentials still require those credentials. Explicit disabled settings survive.
- Automatic cover discovery: on, with mobile/metered data off. Apple Music motion
  artwork starts off, with Wi-Fi-only networking selected if subsequently enabled.
- Artwork network requests are deferred until the post-onboarding choice is
  dismissed or saved. Users can select cover and motion data permissions separately
  and reopen the choices from Settings → Library → Artwork downloads.
- Wi-Fi-only means an unmetered Wi-Fi or Ethernet route. Cellular, metered Wi-Fi,
  unknown VPN transports and disconnected networks do not qualify. Local and cached
  covers remain available. The process observes network changes without polling;
  discovery retries on eligibility changes, using the existing extraction mutex.
- The image loader, artist-image file downloads, motion lookup requests and video
  data source enforce the policy. Motion playback stops when the route loses its
  permission, including between HLS segments. Audio playback and online lyrics do
  not share the artwork network gate.
- The optional Android Media Manager link explains file embedding separately from
  downloading/displaying artwork. Permissions are requested through Android's UI;
  the app does not grant them automatically.

Regression coverage checks clean vs existing preference stores, repeat startup,
supported API flags, static-display width, cellular/metered/unknown-network denial,
Wi-Fi return, and local → embedded → online fallback including failures/cancellation.
