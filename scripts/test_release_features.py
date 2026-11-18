import tempfile
from pathlib import Path
import unittest
from check_release_features import check_sources, music_assistant_allowed


class ReleaseFeaturesTest(unittest.TestCase):
    def test_accepted_61_stable_keeps_the_native_feature_set(self):
        self.assertTrue(music_assistant_allowed('stable', '6.1.0'))
        self.assertTrue(music_assistant_allowed('stable', '6.2.0'))
        self.assertTrue(music_assistant_allowed('nightly', '6.1.0-nightly.8701'))
        self.assertFalse(music_assistant_allowed('stable', '6.0.1'))

    def test_historical_stable_restriction_and_backup_exclusion(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder)
            (p / 'data_extraction_rules.xml').write_text('musicassistant_credentials')
            check_sources(p, 'stable', '6.0.0')
            (p / 'Native.kt').write_text('package musicassistant')
            with self.assertRaises(ValueError): check_sources(p, 'stable', '6.0.1')
            check_sources(p, 'stable', '6.1.0')

    def test_invalid_release_identity_is_not_a_policy_bypass(self):
        for channel, version in [('unknown', '6.1.0'), ('stable', 'garbage'), ('stable', '06.1.0')]:
            with self.assertRaises(ValueError): music_assistant_allowed(channel, version)
