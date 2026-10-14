#!/usr/bin/env python3
"""SemVer names and one Android build counter shared by Stable and Nightly."""
import os
import re
from pathlib import Path


def nightly_version(source, run_number, attempt, sha, channel='nightly'):
    match = re.search(r'versionName\s*=\s*overrideVersionName\s*\?:\s*"([^"]+)"', source)
    if not match or not re.fullmatch(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)', match[1]):
        raise ValueError('Source version must be MAJOR.MINOR.PATCH without suffixes or leading zeroes')
    if channel not in ('stable', 'nightly'):
        raise ValueError('Unknown release channel')
    if run_number < 1 or not 1 <= attempt <= 99 or not re.fullmatch('[0-9a-f]{40}', sha):
        raise ValueError('Invalid GitHub run identity')
    ordinal = run_number * 100 + attempt
    code = 600_000_000 + ordinal
    if code > 2_100_000_000:
        raise ValueError('Android versionCode exhausted')
    name = match[1] if channel == 'stable' else f'{match[1]}-nightly.{ordinal}'
    # Installed 5.x updaters require -vcNNN to find the true APK code. Keep it in
    # SemVer build metadata on the tag, not in the user-facing name or APK version.
    return {'code': str(code), 'name': name, 'tag': f'v{name}+android-vc{code}',
            'sha': sha, 'channel': channel, 'nightly': str(channel == 'nightly').lower()}


if __name__ == '__main__':
    values = nightly_version(Path('app/build.gradle.kts').read_text(),
                             int(os.environ['GITHUB_RUN_NUMBER']), int(os.environ['GITHUB_RUN_ATTEMPT']),
                             os.environ['GITHUB_SHA'], os.environ.get('RELEASE_CHANNEL', 'nightly'))
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write(''.join(f'{key}={value}\n' for key, value in values.items()))
    print(f"{values['channel'].title()} {values['name']} (Android build {values['code']})")
