from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
HTML=(ROOT/'app/src/main/assets/harmonie.html').read_text()

class CoverBuildFixTests(unittest.TestCase):
    def test_cover_counter_not_shadowed(self):
        method=JAVA.split('private void reindexCoversAsync() {',1)[1].split('private MediaMetadata carMetadata(',1)[0]
        self.assertIn('int updated = 0, without = 0;',method)
        self.assertNotIn('JSONObject updated=',method)
        self.assertIn('JSONObject refreshedTrack=refreshed.get(current.optString("id"));',method)
        self.assertIn('merged.put(refreshedTrack==null?current:refreshedTrack);',method)
        self.assertIn('final int a=updated,b=without;',method)
    def test_version_badge_matches_android_package(self):
        self.assertIn("versionName '3.5.20'",GRADLE)
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn('VERSION 3.5.20 · BUILD 27',HTML)
        self.assertIn('return "VERSION 3.5.20 · BUILD 27"',JAVA)

if __name__ == '__main__': unittest.main()
