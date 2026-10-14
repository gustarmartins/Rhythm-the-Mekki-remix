# Stable and Nightly delivery

`.github/workflows/nightly.yml` is the single **Build and Publish Rhythm**
workflow for both channels; its shared run counter keeps Android versions ordered.
See [VERSIONING.md](VERSIONING.md) for the version naming and allocation contract.

- Push `main`: build and publish a Nightly for the development version.
- Dispatch `nightly.yml` on a `release/*` branch with `channel=stable`: build and
  publish Stable. Music Assistant source and manifest entries are prohibited.
- Dispatch on `main` with `channel=nightly`: build a fresh Nightly.

Each source commit/channel gets its own concurrency group; a duplicate run of the
same source/channel replaces its predecessor. Different commits may build in
parallel. The updater compares versions and codes, never publication time alone.

Both channels run the complete JVM suite, build GitHub and F-Droid releases with
R8/lint-vital, and verify APK package/version/signature against the fork's existing
certificate. Missing signing secrets fail the build. Secrets never enter artifacts
or release notes. APKs and their manifests are retained even if publication fails,
so a permission/transport failure can be recovered with the exact verified bytes.

All assets are uploaded to a draft and their hashes checked before publication.
Stable is a normal GitHub release and becomes Latest; Nightly is a prerelease.
Never overwrite a published tag or reuse a version code for changed bytes.

Use **Settings → Updates → Stable** or **Nightly**. Android can require installation
confirmation. Existing explicit channel preferences are retained. Fresh Nightly
installs default to Nightly, and fresh Stable installs default to Stable.

The historical `Rhythm-Nightly-Artifacts` artifact name remains available for
older clients; modern clients use published APK attachments. Stable artifacts
have a distinct name. Manifests contain the source SHA, Android code, clean
version, distribution, sizes, hashes and certificate fingerprints.

For requested changes: implement/review, add meaningful regressions, push, follow
the appropriate cloud run, inspect reports and published assets, and provide the
release URL. Source, builds, publication, installation and physical playback/car
acceptance are separate claims. Never interrupt active playback to install.
