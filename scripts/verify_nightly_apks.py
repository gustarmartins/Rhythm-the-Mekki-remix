#!/usr/bin/env python3
"""Publish only installable APKs signed with the fork's existing device certificate."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

EXPECTED_SIGNER = 'e627f9a94ddce07a35d98c3e9a603d15d61c83d3e9283e1513c08dbedc801822'


def verify_font_storage(apk):
    with zipfile.ZipFile(apk) as archive:
        fonts = [entry for entry in archive.infolist()
                 if entry.filename.lower().endswith(('.ttf', '.otf'))]
        assert fonts, 'Bundled fonts missing from APK'
        assert all(entry.compress_type == zipfile.ZIP_STORED for entry in fonts), \
            'Compressed fonts would restore UI-thread decompression during startup'


def verify():
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    build_tools = sorted((sdk / 'build-tools').glob('*/apksigner'), key=lambda p: p.parent.name)[-1].parent
    destination = Path('nightly-dist')
    destination.mkdir(exist_ok=True)
    code, version = int(os.environ['NIGHTLY_CODE']), os.environ['NIGHTLY_NAME']
    manifest = {'versionCode': code, 'versionName': version, 'commit': os.environ['GITHUB_SHA'], 'channel': os.environ.get('RELEASE_CHANNEL', 'nightly'), 'apks': []}
    for flavor, suffix in [('github', 'github'), ('fdroid', 'fdroid')]:
        folder = Path(f'app/build/outputs/apk/{flavor}/release')
        metadata = json.loads((folder / 'output-metadata.json').read_text())
        assert metadata['applicationId'] == 'chromahub.rhythm.app'
        assert any(not entry['filters'] for entry in metadata['elements']), 'Universal APK missing'
        for entry in metadata['elements']:
            apk = folder / entry['outputFile']
            assert entry['versionCode'] == code and entry['versionName'] == version + '+' + suffix
            certificate = subprocess.check_output([str(build_tools / 'apksigner'), 'verify', '--print-certs', str(apk)], text=True)
            fingerprints = re.findall(r'certificate SHA-256 digest: ([0-9a-f]+)', certificate)
            assert fingerprints and set(fingerprints) == {EXPECTED_SIGNER}, 'APK signer differs from installed fork'
            package = subprocess.check_output([str(build_tools / 'aapt'), 'dump', 'badging', str(apk)], text=True).splitlines()[0]
            assert "name='chromahub.rhythm.app'" in package and f"versionCode='{code}'" in package
            assert f"versionName='{version}+{suffix}'" in package
            if manifest['channel'] == 'stable':
                compiled_manifest = subprocess.check_output([str(build_tools / 'aapt'), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'], text=True)
                assert 'musicassistant' not in compiled_manifest.lower(), 'Stable APK contains Music Assistant'
            verify_font_storage(apk)
            digest = hashlib.sha256(apk.read_bytes()).hexdigest()
            shutil.copy2(apk, destination / apk.name)
            manifest['apks'].append({'name': apk.name, 'sha256': digest, 'flavor': flavor,
                                     'size': apk.stat().st_size, 'signerSha256': EXPECTED_SIGNER})
    (destination / ('nightly.json' if manifest['channel'] == 'nightly' else 'release.json')).write_text(json.dumps(manifest, indent=2) + '\n')
    (destination / 'SHA256SUMS').write_text(''.join(f"{a['sha256']}  {a['name']}\n" for a in manifest['apks']))
    print(f"Verified {len(manifest['apks'])} release APKs, versionCode={code}, existing signer matches")


if __name__ == '__main__':
    verify()
