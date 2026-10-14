import re
import unittest
from nightly_version import nightly_version


class NightlyVersionTest(unittest.TestCase):
    source = 'versionName = overrideVersionName ?: "6.0.0"'
    sha = 'a' * 40

    def version(self, run, attempt=1, channel='nightly'):
        return nightly_version(self.source, run, attempt, self.sha, channel)

    def test_shared_counter_upgrades_across_channels_and_retries(self):
        previous = 600002601
        stable = self.version(27, channel='stable')
        nightly = self.version(28)
        self.assertGreater(int(stable['code']), previous)
        self.assertGreater(int(nightly['code']), int(stable['code']))
        self.assertLess(int(self.version(42)['code']), int(self.version(42, 2)['code']))
        self.assertLess(int(self.version(42, 99)['code']), int(self.version(43)['code']))

    def test_clean_names_and_legacy_update_code_in_tag_metadata(self):
        stable = self.version(27, channel='stable')
        self.assertEqual(stable['name'], '6.0.0')
        self.assertEqual(stable['tag'], 'v6.0.0+android-vc600002701')
        self.assertEqual(stable['nightly'], 'false')
        nightly = self.version(28)
        self.assertEqual(nightly['name'], '6.0.0-nightly.2801')
        self.assertEqual(nightly['nightly'], 'true')
        for release in (stable, nightly):
            self.assertEqual(re.search(r'(?:^|-)vc([0-9]+)(?:-|$)', release['tag'])[1], release['code'])
            self.assertNotIn(self.sha[:8], release['name'])
            self.assertNotIn('vc', release['name'])

    def test_invalid_metadata_rejected(self):
        for run, attempt in [(0, 1), (1, 0), (1, 100), (99_999_999, 1)]:
            with self.assertRaises(ValueError): self.version(run, attempt)
        for source in ['garbage', '"5.5.494.1300-bt-profiles"', '"06.0.0"', '"6.0.0-nightly"']:
            with self.assertRaises(ValueError): nightly_version('versionName = overrideVersionName ?: ' + source, 1, 1, self.sha)
        with self.assertRaises(ValueError): self.version(1, channel='anything')
        with self.assertRaises(ValueError): nightly_version(self.source, 1, 1, 'not-a-sha')

if __name__ == '__main__':
    unittest.main()
