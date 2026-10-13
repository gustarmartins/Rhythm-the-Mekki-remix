#!/usr/bin/env python3
"""A unique, increasing Android version for each Nightly run/attempt."""
import os
import re
from pathlib import Path


def nightly_version(source, run_number, attempt, sha):
    match = re.search(r'versionName\s*=\s*overrideVersionName\s*\?:\s*"([^"]+)"', source)
    if not match or not re.fullmatch(r'[0-9]+(?:\.[0-9]+){3}(?:-[a-zA-Z0-9.-]+)?', match[1]):
        raise ValueError('Missing or invalid source versionName')
    if run_number < 1 or not 1 <= attempt <= 99 or not re.fullmatch('[0-9a-f]{40}', sha):
        raise ValueError('Invalid GitHub run identity')
    ordinal = run_number * 100 + attempt
    code = 600_000_000 + ordinal
    if code > 2_100_000_000:
        raise ValueError('Android versionCode exhausted')
    base = match[1].split('-')[0]
    name = f'{base}-nightly-r{ordinal}-{sha[:8]}-vc{code}'
    return {'code': str(code), 'name': name, 'tag': f'v{name}', 'sha': sha}


if __name__ == '__main__':
    values = nightly_version(Path('app/build.gradle.kts').read_text(),
                             int(os.environ['GITHUB_RUN_NUMBER']), int(os.environ['GITHUB_RUN_ATTEMPT']),
                             os.environ['GITHUB_SHA'])
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write(''.join(f'{key}={value}\n' for key, value in values.items()))
    print(f"Nightly {values['name']} (Android {values['code']})")
