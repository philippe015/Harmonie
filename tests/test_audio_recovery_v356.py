from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
HTML=(ROOT/'app/src/main/assets/harmonie.html').read_text()
JAVA=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
SERVICE=(ROOT/'app/src/main/java/fr/byphilou/harmonie/PlaybackService.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
class AudioRecoveryV356(unittest.TestCase):
    def test_btn_and_visibility(self):
        for key in ['audioRepairBtn','audioCheckBtn','audioRepairStatus','visibilitychange','async function repairAudioOutput']:
            self.assertIn(key,HTML)
    def test_native_recovery_keeps_queue(self):
        self.assertIn('recoverNativeAudio()',JAVA)
        self.assertIn('c.seekTo(index,position)',JAVA)
        self.assertIn('if(!c.getPlayWhenReady())',JAVA)
        body=JAVA.split('@JavascriptInterface public void recoverNativeAudio()')[1].split('@JavascriptInterface public void pauseNative()')[0]
        self.assertNotIn('clearMediaItems()',body)
        self.assertNotIn('setVolume(',body)
        self.assertNotIn('setPlaybackParameters(',body)
    def test_diagnostics_and_errors(self):
        self.assertIn('getAudioDiagnostics()',JAVA)
        self.assertIn('getStreamVolume(AudioManager.STREAM_MUSIC)',JAVA)
        self.assertIn('onAudioSinkError(',SERVICE)
    def test_four_presets_and_original(self):
        for key in ['Fidélité FLAC','Basses profondes','Concert','Spatial 3D']:
            self.assertIn(key,HTML)
        self.assertIn('432 Hz',HTML)
        self.assertIn('artist-asc',HTML)
    def test_versions(self):
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn("versionName '3.5.20'",GRADLE)
if __name__=='__main__':unittest.main()
