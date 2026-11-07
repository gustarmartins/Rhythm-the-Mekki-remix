# Automatic library playback

Selected search results, albums and playlists keep their context order. Play Next
requests retain FIFO order and their individual occurrences, including deliberate
duplicates. Repeat One and Repeat All take priority over library continuation.

With **Continue with device library** enabled, reaching the end appends a random
pass through the available local library, even when shuffle is off. This does not
shuffle the chosen album/list or change the shuffle toggle. Automatic additions
exclude the current song and songs already visited in the current library cycle.
Only playback records a visit; generating a queue does not. The cycle history is
persisted separately from the queue, so replacing a search, restarting the service
or trimming old queue entries does not make visited songs eligible again. Once
the available library is exhausted, a new random cycle starts, avoiding an
immediate repeat when more than one song exists.

**Auto add** builds an initial tail from that same unvisited library pool. The
default **Automatic mix** is random; explicitly saved artist/genre preferences
are retained. Those preferences now mix a related track every four slots while
other candidates are available. They do not restrict the pool or rank by play
count. Explicit album and list choices can still replay tracks intentionally.
Existing queues are preserved through an update: select a new song/list to
replace a previously generated mix.

Music Assistant retains its server-owned queue and continuation. Device-library
continuation does not alter that queue. Paused playback is never started by
autofill. Turning continuation off keeps tracks already queued.

## Optional Last.fm suggestions

Enable **Settings → Queue → Last.fm suggestions** and enter your own API key.
The dialog links to Last.fm's key registration. No Last.fm login/session or
scrobbling is involved. The current canonical artist is sent to the documented
[artist.getSimilar endpoint](https://www.last.fm/api/show/artist.getSimilar).
Returned artist names are matched to local files; external streams are not added.
Matches occupy occasional slots in the random pass, never shrinking the library.

Lookups happen asynchronously when library continuation is needed. Each HTTP
call has a two-second deadline and no automatic retry. Successful lookups are
cached for 24 hours in memory, failed/empty lookups for 15 minutes, with at most
64 cached seeds and one new request per two seconds. Offline, missing-key,
invalid-key and empty-match cases use the local random mix. Changing the queue
cancels obsolete work; repeat, current occurrence and pending requests are checked
again before appending. The key is omitted from Rhythm's settings exports.

Regression coverage includes 2,000-song cycles across queue replacements,
unplayed discarded tails, skipped search prefixes, history compaction, new and
removed files, duplicate IDs, random cycle order, and recommendation diversity.
Last.fm transport/cache/failure tests use a mock server and deterministic clocks;
they do not establish live recommendations with an owner's key or physical-car
playback acceptance.
