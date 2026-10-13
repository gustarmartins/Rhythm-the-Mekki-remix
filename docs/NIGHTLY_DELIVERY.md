# Nightly delivery

Every change pushed to `main` runs
`.github/workflows/nightly.yml`. Manual dispatch rebuilds the current main branch.
PR checks remain in `android.yml`; main builds are not duplicated there.

The cloud job runs the JVM regression suite, builds both release flavors with
R8/lint-vital, and verifies actual APK package/version/signature before publishing.
Missing signing secrets fail the build. The expected certificate matches the
existing fork installation. Credentials never appear in artifacts or release notes.

`600000000 + run_number * 100 + run_attempt` is the Android version code.
Attempts must be 1–99. The same ordinal appears after `nightly-r` in the version
name; `-vc` records the exact APK version code for update checks. Source version
numbers remain human-facing. Any future stable release must use a version code
higher than the latest Nightly if it is intended to replace Nightly installs.
Do not overwrite a published tag or reuse a version code for changed bytes.

Releases are first uploaded as drafts and only published after every asset is
present. The in-app Nightly channel selects published APKs, not workflow status
or a moving nightly.link ZIP. `nightly.json` and `SHA256SUMS` describe the exact
commit, APKs, signing fingerprint and hashes. Failed/cancelled jobs publish nothing.

Choose **Settings → Updates → Nightly**, then check/download the update.
Android can require confirmation to install. An existing explicit Stable/Beta
preference is preserved; fresh Nightly installs default to the Nightly channel.

For requested changes: implement and review the scoped diff, add meaningful
regressions, push, watch the cloud run, inspect test results and release assets,
and provide the resulting release URL. Building or publishing does not prove
physical Bluetooth-car behavior; keep that acceptance separate.
