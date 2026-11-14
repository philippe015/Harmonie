from pathlib import Path
import re, unittest

ROOT=Path(__file__).resolve().parents[1]
BEAT=(ROOT/'app/src/main/java/fr/byphilou/harmonie/NativeBeatVisualizer.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
HTML=(ROOT/'app/src/main/assets/harmonie.html').read_text()
JAVA=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()

class LedJavaCompilationTests(unittest.TestCase):
    def test_json_put_uses_boxed_double(self):
        self.assertIn('levels.put(Double.valueOf(Math.round(level * 1000.0) / 1000.0));',BEAT)
        self.assertNotIn('levels.put(Math.round(level * 1000.0) / 1000.0);',BEAT)
    def test_fft_30_bands_unchanged(self):
        self.assertIn('i < 30',BEAT)
        self.assertIn('new Visualizer(sessionId)',BEAT)
        self.assertIn('frame.put("levels", levels)',BEAT)
    def test_android_version(self):
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn("versionName '3.5.20'",GRADLE)
        self.assertIn('VERSION 3.5.20 · BUILD 27',HTML)
        self.assertIn('VERSION 3.5.20 · BUILD 27',JAVA)
    def test_bluetooth_radio_and_432_still_present(self):
        for marker in ('id="btn432"','id="btn440"','id="radioResults"','id="ledNativeSync"'):
            self.assertIn(marker,HTML)
