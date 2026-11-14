"""Prevent regression of Android compileSdk/AGP/Gradle versions for Media3 1.10.1."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class BuildCompatibility351(unittest.TestCase):
    def test_gradle_plugin_compiles_api_36(self):
        top=(ROOT/'build.gradle').read_text()
        app=(ROOT/'app/build.gradle').read_text()
        self.assertIn("version '8.10.1'",top)
        self.assertIn('compileSdk 36',app)
        self.assertIn('targetSdk 35',app)
        self.assertIn('media3-session:1.10.1',app)
        self.assertIn('media3-exoplayer-hls:1.10.1',app)
    def test_manjaro_script_is_in_sync(self):
        text=(ROOT/'scripts/compiler_manjaro.sh').read_text()
        for fragment in ("'platforms;android-36'",'gradle-8.11.1-bin.zip','gradle-8.11.1', 'android-36/android.jar'):
            self.assertIn(fragment,text)
        self.assertNotIn('gradle-8.9-bin.zip',text)
    def test_original_identity_and_432(self):
        app=(ROOT/'app/build.gradle').read_text()
        java=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
        self.assertIn("applicationId 'fr.byphilou.harmonie'",app)
        self.assertIn("versionName '3.5.20'",app)
        self.assertIn('432f/440f',java)
if __name__=='__main__':unittest.main()
