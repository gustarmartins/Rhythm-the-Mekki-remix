#!/usr/bin/env python3
"""Keep the historical 6.0 restriction; native Music Assistant ships in Stable 6.1+."""
import argparse
from pathlib import Path
import re


def music_assistant_allowed(channel, version):
    if channel not in ('stable', 'nightly'):
        raise ValueError('Unknown release channel')
    match = re.fullmatch(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:[-+].*)?', version)
    if not match:
        raise ValueError('Invalid release version')
    return channel == 'nightly' or tuple(map(int, match.groups())) >= (6, 1, 0)


def check_sources(root, channel, version):
    if music_assistant_allowed(channel, version):
        return
    bad = [str(p) for p in Path(root).rglob('*') if p.is_file()
           and p.suffix in ('.kt', '.java', '.xml') and p.name != 'data_extraction_rules.xml'
           and ('musicassistant' in p.read_text().lower() or 'music_assistant' in p.read_text().lower())]
    if bad:
        raise ValueError('This Stable release line excludes Music Assistant: ' + ', '.join(bad))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--channel', required=True)
    parser.add_argument('--version', required=True)
    args = parser.parse_args()
    check_sources('app/src', args.channel, args.version)
