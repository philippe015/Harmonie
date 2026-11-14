from pathlib import Path
import re
import unittest
R=Path(__file__).resolve().parents[1]
HTML=(R/'app/src/main/assets/harmonie.html').read_text()
BEAT=(R/'app/src/main/java/fr/byphilou/harmonie/NativeBeatVisualizer.java').read_text()
SERVICE=(R/'app/src/main/java/fr/byphilou/harmonie/PlaybackService.java').read_text()
ACTIVITY=(R/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
MANIFEST=(R/'app/src/main/AndroidManifest.xml').read_text()

class NativeLedTests(unittest.TestCase):
    def test_opt_in_and_permission(self):
        self.assertIn('android.permission.RECORD_AUDIO',MANIFEST)
        self.assertIn('id="ledNativeSync"',HTML)
        self.assertIn("setNativeLedCapture",HTML)
        self.assertIn('requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}',ACTIVITY)
    def test_capture_own_session_only_and_never_microphone(self):
        self.assertIn('new Visualizer(sessionId)',BEAT)
        self.assertIn('sessionId <= 0',BEAT)
        self.assertNotIn('new Visualizer(0)',BEAT)
        self.assertNotIn('AudioRecord',BEAT)
        self.assertNotIn('MediaRecorder',BEAT)
    def test_callback_modes_release_and_background(self):
        for marker in ('onFftDataCapture','onWaveFormDataCapture','getCaptureSizeRange','getMaxCaptureRate','old.release()', 'if (!visible)', 'if (!playing)'):
            self.assertIn(marker,BEAT)
        self.assertIn('onAudioSessionIdChanged',SERVICE)
        self.assertIn('EVENT_IS_PLAYING_CHANGED',SERVICE)
        self.assertIn('beatVisualizer.release()',SERVICE)
        self.assertIn('onPause()',ACTIVITY)
    def test_bridge_safe_thread_and_state(self):
        self.assertIn('return NativeBeatVisualizer.getFrame()',ACTIVITY)
        self.assertIn('private static final int PERMISSION_NATIVE_LED',ACTIVITY)
        self.assertIn('getNativeLedFrame',HTML)
        self.assertIn("ledHasNativeAudio()",HTML)
        self.assertIn('ledPseudoLevels(t,preview)',HTML)
    def test_live_frames_display_only_when_fresh(self):
        self.assertIn('Date.now()-Number(frame.timestamp)<1500',HTML)
        self.assertIn('frame.levels.length===30',HTML)
        self.assertIn('spectre mono',HTML)
        self.assertIn('lecture en arrière-plan', (R/'CHANGELOG_V3_5_17.md').read_text())
    def test_existing_features_and_version(self):
        self.assertIn('versionCode 27',(R/'app/build.gradle').read_text())
        self.assertIn("versionName '3.5.20'",(R/'app/build.gradle').read_text())
        for marker in ('id="btn432"','id="btn440"','id="radioResults"','id="playlistSort"','id="ledMode"'):
            self.assertIn(marker,HTML)
