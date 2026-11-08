# Queue row actions

Queue rows identify a service-owned occurrence, including when a song appears more
than once. Material and Expressive player sheets pass that token through the same
app-only session command. Display indexes and song IDs are never edit targets.

- Normal selection plays the chosen occurrence and continues from it.
- With Play Next selected, a queue tap moves the existing occurrence after earlier
  unplayed manual requests and before context tracks. It preserves the current
  song, position and pause state. Repeated taps are idempotent.
- Removing a row cannot remove the current occurrence, including if playback has
  advanced while a swipe animation was running. Dragging only edits upcoming rows.
- Queue edits and clear share the reservation lock with asynchronous Play Next
  additions, retaining the order in which requests arrived.
- Native shuffle uses playback traversal order. Bluetooth compatibility edits the
  full virtual queue while retaining the one-item client view. The service persists
  the resulting queue. Repeat All Previous wraps without duplicating occurrences.
- Music Assistant edits use server occurrence IDs, refresh before mutation and
  verify the resulting order. Server restrictions on played/buffered rows surface
  as errors; failed edits never replace playback or add a duplicate song.
- Paged service snapshots include matching ID/token lists under one revision and
  recheck that revision after asynchronous metadata reads. Refresh requests received
  during a read are retained. Rejected row actions refresh the queue and display a
  dismissible error.

## Shuffle and saved requests

Shuffle is a service-owned operation for app controls, media controllers and
notification actions. It keeps the played prefix and current occurrence, then
the unplayed FIFO requests, and randomizes only the remaining context. Disabling
shuffle restores the context's relative order without demoting pending requests.
New requests append after that manual segment. Consumed, skipped or deleted
requests are not resurrected by a later shuffle or Previous action.

The manual engine reorders the physical playlist with at most two local range
edits; the native engine sets a traversal permutation. Both expose their actual
shuffle state to media controllers. The manual engine uses an identity native
permutation so the platform cannot shuffle its physical queue a second time.
Neither path replaces the current MediaSource, seeks, prepares or starts playback.
Bluetooth compatibility applies the same policy to its full virtual queue.

The service saves song IDs, occurrence tokens, pending requests, the original
context order, cursor and position together. UI persistence requests ask the
service for that snapshot. Restoring it retains the saved traversal and tokens,
including duplicate songs; missing files drop only their own occurrences. Older
ID-only saves remain readable, although they cannot recover manual reservations
that were never saved. Explicit new contexts and queue clearing discard the old
reservations. Delayed streaming additions retain the existing reservation lock
and insert into the service's current order after resolution.

Music Assistant retains server ownership: after its shuffle command, Rhythm
re-pins known pending occurrences in FIFO order and verifies the resulting order.
If playback advances or the server refuses a buffered-item move, that failure is
reported instead of claiming the requests were preserved.

## Regression coverage

`QueueEntryActionTest` covers FIFO promotions, duplicates, idempotence, consumed
reservations, stale tokens, delayed removal, current-item guards, manual dragging
and arbitrary shuffle traversal. `LegacyQueueNavigationTest` covers Previous and
repeated Repeat All wraparound. Existing Play Next, legacy-index and continuation
regressions remain part of the suite.

`QueueShufflePolicyTest` covers repeated shuffle/unshuffle, later additions,
consumption, deletion, dragging, fresh selections and large queues.
`QueueShufflePlaybackTest` exercises real Media3 players, checks that the current
window UID survives, and verifies position, play intent, repeat modes, duplicate
requests and restoration metadata. `QueueServiceAndroidTest` runs real controller
IPC against the playback service with a 2,077-song queue, both shuffle engines,
later requests, restoration and Next. `ServerPlayNextOrderTest` verifies server
reordering, concurrent advancement and rejected edits.

Android compilation and the complete JVM suite run in the normal release workflow.
Those checks do not establish physical Bluetooth-car behavior or live Music
Assistant server acceptance; those require separate playback validation.
