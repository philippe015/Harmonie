"""Régression : MediaController est confiné au thread Android, jamais au thread JS WebView."""
from pathlib import Path
import re
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
HTML=(ROOT/'app/src/main/assets/harmonie.html').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()

class Media3ThreadV357(unittest.TestCase):
    def test_js_bridge_is_read_only_cached_snapshot(self):
        m=re.search(r'@JavascriptInterface public String getAudioDiagnostics\(\)\s*\{([^}]*)\}',JAVA)
        self.assertIsNotNone(m)
        self.assertEqual('return audioDiagnosticSnapshot;',m.group(1).strip())
        self.assertNotIn('controller.',m.group(1))
    def test_snapshot_main_thread_only(self):
        self.assertIn('private volatile String audioDiagnosticSnapshot',JAVA)
        self.assertIn('Looper.myLooper() != Looper.getMainLooper()',JAVA)
        self.assertIn('audioDiagnosticSnapshot = collectAudioDiagnosticsOnMainThread()',JAVA)
        self.assertIn('mainHandler.postDelayed(this, 750)',JAVA)
        self.assertIn('refreshAudioDiagnosticSnapshot();',JAVA)
    def test_controller_calls_safe(self):
        self.assertIn('runOnUiThread(() -> {',JAVA)
        self.assertIn('controllerFuture.addListener',JAVA)
        self.assertIn('mainHandler.post(command)',JAVA)
        self.assertIn('refreshAudioDiagnosticSnapshot();',JAVA)
        self.assertIn('runMedia3Action(action)',JAVA)
    def test_diagnostic_is_useful(self):
        for field in ('playbackState','suppressionReason','playerVolume','musicVolume','positionMs','playerError','mediaItemCount'):
            self.assertIn('d.put("'+field+'"',JAVA)
        self.assertIn("Périphériques disponibles (pas forcément actifs)",HTML)
        self.assertIn("Erreur Media3",HTML)
    def test_identity_and_432_preserved(self):
        self.assertIn("versionName '3.5.20'",GRADLE)
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn('432f/440f',JAVA)
        for text in ('Fidélité FLAC','Basses profondes','Concert','Spatial 3D','artist-asc'):
            self.assertIn(text,HTML)

if __name__=='__main__':unittest.main()
