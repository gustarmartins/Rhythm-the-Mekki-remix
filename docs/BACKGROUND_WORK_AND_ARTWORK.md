# Background work and artwork policy

## Resource scheduling

Application activity-resume events and Android battery broadcasts feed one process-wide
optional-work controller. No lifecycle polling, new service, wakelock, root policy or
second audio engine is introduced. Foreground admission permits three enrichment tasks;
background and battery saver admit one. Already admitted items finish before further
admission; new items recheck policy. Owned worker threads use Android background priority
when no activity is resumed, with a 250 ms interval after each background item. Suspending
network calls release their threads. Cancellation releases admission even on exceptions.
Audio decoding, media buttons, focus, Bluetooth metadata and cached/local lyric display
remain outside this gate.

Covered work: artist/album/song artwork lookup, missing-cover probing, online lyrics and
translation (including streaming-provider lyrics), per-file genre and format probing,
Canvas lookup, and cache maintenance. Background UI progress updates run once per second
instead of ten times. Actual image downloads also drop from four concurrent requests to
one in background/saver; saver blocks uncached remote images when artwork is paused.
Cached and embedded images remain available, with at most two decoders on owned threads.
Invisible Canvas players are disposed. Queue preloading was already
disabled in PreloadController and is not enabled by this change.

Artwork discovery now probes at most 120 songs and finds at most 40 missing covers per
pass, continuing from its cursor next time. Failed track/artist lookups have a six-hour
cooldown; explicit track artwork refresh bypasses it. Existing lyric request coalescing
continues to cancel a shared lookup when its last consumer leaves. Canvas misses are cached.
Normal background transitions preserve decoded covers. Cache directory maintenance is
coalesced and at most daily; Android critical-memory callbacks still clear disposable caches.

## Battery saver

The existing manual switch is preserved. A separate automatic switch defaults on at 15%
while unplugged, with a configurable 1–100% threshold. Charging or recovering more than
2 percentage points above the threshold releases automatic activation. Manual activation
remains independent. Every per-feature switch applies to both activation methods.

Online artwork, online lyrics/translations, library enrichment/maintenance and animated
artwork default to paused during saver. Haptics, marquee and lossless artwork retain their
existing switches. Offload is optional and defaults off when no explicit choice was saved;
existing offload settings remain intact. This avoids enabling a different audio path merely
because the battery dropped. Playback itself is never placed behind optional-work permits.
Saved preferences are not overwritten when automatic saver activates or deactivates.

## Artwork correctness

Only embedded audio artwork, explicitly selected overrides, conventional cover/folder/front/
albumart files in dedicated folders, or title-and-artist-verified remote results qualify.
Shared Music/Download/Pictures/DCIM directories cannot provide a folder cover. Arbitrary
adjacent images and Android's shared MediaStore album-ID fallback are rejected. Missing
covers stay missing or use a generated placeholder. Online matching is deliberately strict;
ambiguous Various Artists compilations do not receive a guessed celebrity cover.

Library, recent-song and playlist read paths replace old unverified automatic database art with the safe per-song provider.
Provider URIs and decode cache namespaces change so old automatic covers are not reused.
Verified remote covers are bound to the song metadata so tag edits invalidate the proof.
Explicitly chosen artwork is retained, and no personal image/audio file is removed.

## Queue behavior

Clear asks for confirmation and retains the current occurrence, position and playback state.
The service clears its real Bluetooth virtual queue or removes at most two ExoPlayer ranges;
it does not send one command per song. Current-context automatic continuation is suppressed
until a new queue context is selected. Duplicate keys are computed once in linear time and
queue sections are memoized. The small queue chip toggles the saved show-played setting.

## Validation boundary

Regression tests cover cover-source admission, strict metadata matches, duplicate queue keys,
10,000-item range clearing, saver threshold/hysteresis, per-feature exceptions, background
concurrency transitions and cancellation. The cloud workflow runs the full unit suite and
builds signed release APKs before publishing a Nightly.

A saved October 13 ANR places the main thread in Media3 collection rebuilding from the
queue-clear UI action, supporting the repeated-removal diagnosis.

A passive October 13 device trace did not contain a usable Rhythm process interval. It does
not establish Rhythm CPU usage or reproduce the reported 1% unplugged stutter. A matched
before/after capture during real playback is still needed for quantitative CPU/battery claims.
These changes target confirmed source-level work; streaming catalog synchronization, DSP
cost and the device's thermal/low-battery clock policy are not measured improvements here.
