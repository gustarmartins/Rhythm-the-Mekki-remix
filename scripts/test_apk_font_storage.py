import io
import unittest
import zipfile

from verify_nightly_apks import verify_font_storage


class FontStorageTests(unittest.TestCase):
    def apk(self, fonts):
        result = io.BytesIO()
        with zipfile.ZipFile(result, 'w') as archive:
            archive.writestr('classes.dex', b'dex', compress_type=zipfile.ZIP_DEFLATED)
            for name, compression in fonts:
                archive.writestr(name, b'font bytes', compress_type=compression)
        result.seek(0)
        return result

    def test_accepts_renamed_stored_fonts_and_other_compressed_assets(self):
        verify_font_storage(self.apk([('res/a.ttf', zipfile.ZIP_STORED),
                                      ('res/b.otf', zipfile.ZIP_STORED)]))

    def test_rejects_a_compressed_font_even_when_another_is_stored(self):
        with self.assertRaisesRegex(AssertionError, 'Compressed fonts'):
            verify_font_storage(self.apk([('res/a.ttf', zipfile.ZIP_STORED),
                                          ('res/b.otf', zipfile.ZIP_DEFLATED)]))

    def test_rejects_missing_fonts(self):
        with self.assertRaisesRegex(AssertionError, 'Bundled fonts missing'):
            verify_font_storage(self.apk([]))
