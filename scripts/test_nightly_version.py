import unittest
from nightly_version import nightly_version


class NightlyVersionTest(unittest.TestCase):
    source = 'versionName = overrideVersionName ?: "5.5.494.1300-bt-profiles"'
    sha = 'a' * 40

    def version(self, run, attempt=1):
        return nightly_version(self.source, run, attempt, self.sha)

    def test_every_build_and_retry_can_upgrade(self):
        self.assertLess(int(self.version(42)['code']), int(self.version(42, 2)['code']))
        self.assertLess(int(self.version(42, 99)['code']), int(self.version(43)['code']))
        self.assertGreater(int(self.version(1)['code']), 554941294)

    def test_tag_matches_built_version_and_carries_real_code(self):
        v = self.version(42)
        self.assertEqual(v['tag'], 'v' + v['name'])
        self.assertTrue(v['name'].endswith('-vc' + v['code']))
        self.assertTrue(v['name'].startswith('5.5.494.1300-nightly-r4201-'))

    def test_unbounded_or_invalid_metadata_is_rejected(self):
        for run, attempt in [(0, 1), (1, 0), (1, 100), (99_999_999, 1)]:
            with self.assertRaises(ValueError):
                self.version(run, attempt)
        with self.assertRaises(ValueError):
            nightly_version('garbage', 1, 1, self.sha)


if __name__ == '__main__':
    unittest.main()
