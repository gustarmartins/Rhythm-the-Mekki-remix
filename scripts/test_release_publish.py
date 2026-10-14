import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import publish_release


class PublishReleaseTest(unittest.TestCase):
    def exercise(self, channel='stable', existing=False, bad_digest=False):
        with tempfile.TemporaryDirectory() as tmp:
            old = os.getcwd()
            try:
                os.chdir(tmp)
                folder = Path('nightly-dist'); folder.mkdir()
                apks = [{'name': f'app-{i}.apk', 'size': 3, 'sha256': 'a' * 64} for i in range(10)]
                for a in apks: (folder / a['name']).write_bytes(b'apk')
                filename = 'release.json' if channel == 'stable' else 'nightly.json'
                name = '6.0.0' if channel == 'stable' else '6.1.0-nightly.2801'
                manifest = {'commit': 'b' * 40, 'versionCode': 600002801, 'versionName': name, 'channel': channel, 'apks': apks}
                (folder / filename).write_text(json.dumps(manifest))
                (folder / 'SHA256SUMS').write_text('checksums')
                assets = [{'name': a['name'], 'size': a['size'], 'digest': 'sha256:' + ('c' * 64 if bad_digest else a['sha256'])} for a in apks]
                assets += [{'name': filename}, {'name': 'SHA256SUMS'}]
                previous = [{'draft': False, 'tag_name': 'v6.0.0+android-vc600002701'}] if existing else []
                env = {'RELEASE_CHANNEL': channel, 'RELEASE_TAG': 'v' + name + '+android-vc600002801', 'RELEASE_NAME': name,
                       'RELEASE_CODE': '600002801', 'GITHUB_SHA': 'b' * 40, 'GITHUB_REPOSITORY': 'owner/repo'}
                draft = {'id': 123, 'draft': True, 'tag_name': env['RELEASE_TAG'], 'assets': assets}
                def github_read(command, **kwargs):
                    endpoint = command[-1]
                    if '/releases/tags/' in endpoint:
                        raise AssertionError('Published-tag endpoint cannot read drafts')
                    if endpoint.endswith('/releases/123'):
                        return json.dumps(draft)
                    github_read.count += 1
                    return json.dumps(previous if github_read.count == 1 else [draft])
                github_read.count = 0
                with patch.dict(os.environ, env), patch('publish_release.subprocess.check_output', side_effect=github_read), patch('publish_release.subprocess.run') as run:
                    run.return_value.returncode = 1  # No previous release at the exact tag.
                    if existing or bad_digest:
                        with self.assertRaises(AssertionError): publish_release.main()
                        self.assertFalse(any(call.args[0][:3] == ['gh', 'release', 'edit'] for call in run.call_args_list))
                    else:
                        publish_release.main()
                        commands = [call.args[0] for call in run.call_args_list]
                        create = next(cmd for cmd in commands if cmd[:3] == ['gh', 'release', 'create'])
                        self.assertIn('--draft', create)
                        self.assertEqual('--prerelease' in create, channel == 'nightly')
                        self.assertIn('--latest=' + str(channel == 'stable').lower(), commands[-1])
            finally: os.chdir(old)

    def test_stable_is_verified_then_published_latest(self): self.exercise()
    def test_nightly_is_prerelease_and_not_latest(self): self.exercise('nightly')
    def test_existing_stable_cannot_be_republished_under_new_code(self): self.exercise(existing=True)
    def test_mismatched_upload_never_leaves_draft(self): self.exercise(bad_digest=True)

if __name__ == '__main__': unittest.main()
