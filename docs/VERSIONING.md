# Release versioning

Rhythm's fork starts an independent three-component release line at **6.0.0**.
The old upstream-derived four-number versions are retained only as historical
versions. Package identity, signing key and user data remain compatible.

Names follow SemVer syntax:

- Stable: `6.0.0`. Fix-only Stable releases increment PATCH (`6.0.1`).
- New backward-compatible features increment MINOR (`6.1.0`).
- Breaking compatibility changes increment MAJOR.
- Nightly: the next development version with `-nightly.N`, such as
  `6.1.0-nightly.2801`. N is a numeric build ordinal, not a feature label.
- Distribution is build metadata in the APK versionName: `6.0.0+github`,
  `6.0.0+fdroid`, or `6.1.0-nightly.2801+github`.
- Git SHAs, dates and validation details belong in the release manifest and notes,
  not in the user-facing version name.

Android `versionCode` is an independent integer used for installation ordering.
Both channels MUST use the same `nightly.yml` workflow ID and its existing counter:

```
ordinal = GITHUB_RUN_NUMBER * 100 + GITHUB_RUN_ATTEMPT
versionCode = 600000000 + ordinal
```

Attempts are 1–99; a new run outranks all attempts of an earlier run. The shared
counter already exceeds all legacy fork APKs. Do not derive Android codes by
concatenating SemVer digits, use a second workflow counter, reset the counter,
or reuse a published version/code with different bytes. Out-of-order completion
is safe: updaters compare versions and exact codes rather than publication time.
The source's default code 600000000 is a development floor; distributable builds
must use the CI override and should not be built/delivered with that default.

To bootstrap installed 5.x updaters, release tags include the exact Android code
in valid SemVer build metadata: `v6.0.0+android-vc600002701`. Existing clients
recognize `-vcNNN`; new clients hide this tag metadata in their displayed release
name. Build metadata is ignored for semantic precedence. Release titles start
with the clean version, for example **v6.0.0 - Rhythm Stable**.

Stable is manually dispatched with `channel=stable` on `release/*`. Main push
builds use `channel=nightly`. Stable must exclude Music Assistant source and
manifest entries until that integration has been explicitly approved for Stable.
The legacy `release.yml` publisher is retired to prevent duplicate counters,
unverified publication and unsolicited channel broadcasts.

A publisher may need a pre-created tag when the workflow token cannot create a
ref on a non-default branch. After the run allocates its version, an authorized
maintainer can push that exact immutable tag at the run's SHA. This does not
publish the APK: tests and APK verification must still pass before the workflow
publishes the draft. Never move an existing release tag.

The release is uploaded as a draft and all asset hashes are read back before it
becomes public. Both flavors use the existing signing identity. `release.json`
(Stable) or `nightly.json` (Nightly), plus `SHA256SUMS`, records the exact artifacts.
