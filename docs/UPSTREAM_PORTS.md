# Upstream ports after the 6.1 Stable baseline

The 6.2 development line selectively ports upstream changes from main at
`85fea209c4d44046192d830faa9181db3ece82ad`. These are reconciled ports, so whole-patch
ancestry or patch equivalence alone is not a complete feature inventory.

| Upstream source | Integrated portions | Retained fork behavior / deliberate exclusions |
|---|---|---|
| `008deb1d` | Stable date-added resolver shared by scanner and repository | Strong recording/path checks, duplicate/relocation guards, scoped fingerprinting, null-provider retention; no weak filename-only identity cache |
| `31ef699d` | Hashed artist image names, conservative legacy ownership, host-folder portrait rules, case-insensitive refresh | Bounded/atomic network cache writes and protected audio artwork saves |
| `669e5851` | Create-with-selected-song path and single click ownership in song pickers | Existing ordered PlaylistWriteQueue and deletion/lifecycle handoff; no parallel targeted writer or larger-list-wins reload heuristic |
| `e1688fc4` | SAF tree resolution, persisted read grants, safe folder suggestions and feedback | Existing central scan scope; no broad fallback for opaque IDs, startup path rewriting or automatic full disk traversal |
| `2a1097e8` | Shared Media3 repeat icon mapping, near-transition end-pause guard | Service-owned occurrence/queue snapshot protocol; no first-matching-song-ID fallback |
| `9fe18999` | Codec classification, badges, filters and regression cases | No optional telemetry dependency or physical bit-perfect/object-rendering claims; no lossless inference for lossy Atmos from bitrate alone |
| `fc7c72a3` | Active Library pager scroll/refresh state, adaptive notification interval editor, onboarding labels | Existing dialog sizing and accurate lyric fallback descriptions |
| `96c2dcca` | Custom/pinned AutoEQ preset UI corrections only | DSP controls and lifecycle, selected-route model and atomic playback rate; no decoder policy, float-output migration or broad inspector |

`4cad8c03` speed/pitch synchronization is already covered by the fork's atomic
PlaybackRateStore/applyPlaybackRate path and is not reapplied. The two large queue
reorder commits (`e4333aa5`, `5b525fbb`) and release/version bumps are not copied.
The experimental metadata branch already overlaps the fork's bounded ID-map
pipeline; incomplete-metadata shortcuts and obsolete Room/tag history are omitted.

The Home/Last.fm/wavy changes from Nightly 8701 are retained. Compilation, JVM
regressions and both signed release builds run in the normal cloud workflow.
Installation, real output-route behavior and physical Bluetooth/car acceptance
remain separate from those automated checks.
