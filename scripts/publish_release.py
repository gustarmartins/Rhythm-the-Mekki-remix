#!/usr/bin/env python3
"""Publish only a complete verified release; never overwrite a published version."""
import json
import os
from pathlib import Path
import subprocess


def main():
    channel = os.environ['RELEASE_CHANNEL']
    assert channel in ('stable', 'nightly')
    tag, name, code, sha = (os.environ[k] for k in ('RELEASE_TAG', 'RELEASE_NAME', 'RELEASE_CODE', 'GITHUB_SHA'))
    repo = os.environ['GITHUB_REPOSITORY']
    destination = Path('nightly-dist')
    manifest_name = 'release.json' if channel == 'stable' else 'nightly.json'
    manifest = json.loads((destination / manifest_name).read_text())
    assert manifest['commit'] == sha and manifest['versionCode'] == int(code)
    assert manifest['versionName'] == name and manifest['channel'] == channel
    assert len(manifest['apks']) == 10
    # A semantic Stable version is immutable even if a later CI retry has a new Android code.
    releases = json.loads(subprocess.check_output(['gh', 'api', f'repos/{repo}/releases?per_page=100'], text=True))
    if channel == 'stable':
        assert not any(not r['draft'] and r['tag_name'].removeprefix('v').split('+')[0] == name for r in releases), 'Stable version already published; increment PATCH'
    source_notes = Path(f'docs/releases/{name}.md')
    body = source_notes.read_text() if channel == 'stable' and source_notes.exists() else f'Rhythm {name} Nightly. See the commit history for changes.\n'
    body += (f'\nSource: `{sha}`. Android build: **{code}**.\n\n'
             f'Update using **Settings → Updates → {channel.title()}**. Android may ask to confirm installation.\n\n'
             'Both release flavors passed the complete JVM suite, compilation, R8/lint-vital, '
             'and APK package/version/signature verification. All APKs use the existing fork signing key. '
             f'`{manifest_name}` and `SHA256SUMS` identify these exact files.\n')
    Path('release-notes.md').write_text(body)
    existing = subprocess.run(['gh', 'release', 'view', tag, '--repo', repo], capture_output=True)
    assert existing.returncode != 0, 'Release tag already exists; inspect it instead of overwriting assets'
    command = ['gh', 'release', 'create', tag, *map(str, sorted(destination.iterdir())), '--repo', repo,
               '--target', sha, '--title', f'v{name} - Rhythm {channel.title()}', '--notes-file', 'release-notes.md', '--draft']
    if channel == 'nightly': command.append('--prerelease')
    subprocess.run(command, check=True)
    release = json.loads(subprocess.check_output(['gh', 'api', f'repos/{repo}/releases/tags/{tag}'], text=True))
    assets = {a['name']: a for a in release['assets']}
    for apk in manifest['apks']:
        assert assets[apk['name']]['size'] == apk['size']
        assert assets[apk['name']]['digest'] == 'sha256:' + apk['sha256']
    assert manifest_name in assets and 'SHA256SUMS' in assets
    subprocess.run(['gh', 'release', 'edit', tag, '--repo', repo, '--draft=false', '--latest=' + str(channel == 'stable').lower()], check=True)

if __name__ == '__main__': main()
