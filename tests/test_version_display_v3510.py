from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
HTML=(ROOT/'app/src/main/assets/harmonie.html').read_text()
JAVA=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()

class TestVersionDisplay(unittest.TestCase):
    def test_visible_footer_and_style(self):
        self.assertIn('id="appVersionFooter"',HTML)
        self.assertIn('VERSION 3.5.20 · BUILD 27',HTML)
        self.assertIn('.app-version-badge{',HTML)
        self.assertRegex(HTML,r'<footer class="footer">.*?id="appVersionFooter".*?</footer>')
    def test_android_version_dynamic_and_thread_safe(self):
        self.assertIn('getAppVersionLabel()',JAVA)
        self.assertIn('getPackageInfo(getPackageName(), 0)',JAVA)
        self.assertIn('native.getAppVersionLabel()',HTML)
        self.assertIn('syncAppVersionFooter();',HTML)
        method=JAVA.split('@JavascriptInterface public String getAppVersionLabel()',1)[1].split('@JavascriptInterface',1)[0]
        self.assertNotIn('controller.',method)
        self.assertNotIn('withController(',method)
    def test_version_consistency(self):
        v=re.search(r"versionName '([^']+)'",GRADLE).group(1)
        n=re.search(r'versionCode (\d+)',GRADLE).group(1)
        self.assertEqual((v,n),('3.5.20','27'))
        self.assertIn(f'VERSION {v} · BUILD {n}',HTML)
        self.assertIn(f'VERSION {v} · BUILD {n}',JAVA)
    def test_audio_features_preserved(self):
        self.assertIn('continuousPlayback',HTML)
        self.assertIn('432',HTML)
        self.assertIn('artist-asc',HTML)
        self.assertIn('getAudioDiagnostics()',JAVA)
        self.assertIn('setContinuousPlayback(boolean enabled)',JAVA)

if __name__ == '__main__': unittest.main()
