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

## Regression coverage

`QueueEntryActionTest` covers FIFO promotions, duplicates, idempotence, consumed
reservations, stale tokens, delayed removal, current-item guards, manual dragging
and arbitrary shuffle traversal. `LegacyQueueNavigationTest` covers Previous and
repeated Repeat All wraparound. Existing Play Next, legacy-index and continuation
regressions remain part of the suite.

Android compilation and the complete JVM suite run in the normal release workflow.
Those checks do not establish physical Bluetooth-car behavior or live Music
Assistant server acceptance; those require separate playback validation.
