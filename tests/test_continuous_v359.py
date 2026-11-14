from pathlib import Path
import re,unittest
ROOT=Path(__file__).resolve().parents[1]
J=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
S=(ROOT/'app/src/main/java/fr/byphilou/harmonie/PlaybackService.java').read_text()
H=(ROOT/'app/src/main/assets/harmonie.html').read_text()
G=(ROOT/'app/build.gradle').read_text()
class ContinuousV359(unittest.TestCase):
    def test_natively_restarted_in_service_not_webview(self):
        self.assertIn('onNativePlaylistEnded()',S)
        self.assertIn('if (playbackState == Player.STATE_ENDED)',S)
        self.assertIn('player.seekTo(next,0L);',S)
        self.assertIn('player.prepare();',S)
        self.assertIn('player.play();',S)
    def test_not_restarted_after_pause_or_radio(self):
        self.assertIn('!player.getPlayWhenReady()',S)
        self.assertIn('id.startsWith("native:")',S)
        self.assertIn('continuousEnabled()',S)
        self.assertIn('player.getRepeatMode()!=Player.REPEAT_MODE_OFF',S)
    def test_native_session_not_overridden_by_browser_position(self):
        self.assertIn("state.mode!=='android-native'&&!(radio.active&&radio.native)&&'mediaSession'",H)
    def test_persistent_user_toggle_and_diagnostics(self):
        self.assertIn('continuousPlayback',H)
        self.assertIn('setContinuousPlayback(boolean enabled)',J)
        self.assertIn('getBoolean(CONTINUOUS_KEY,true)',S)
        self.assertIn('return new MediaSession.MediaItemsWithStartPosition(items,index,position)',S)
        self.assertNotIn('Collections.singletonList(items.get(index))',S)
        self.assertIn('d.put("lastEndDiagnostic"',J)
        self.assertIn('Dernière fin de file',H)
    def test_preserved_432_artist_bluetooth_identity(self):
        self.assertIn('432f/440f',J)
        self.assertIn('artist-asc',H)
        self.assertIn('MediaSessionService',S)
        self.assertIn("versionName '3.5.20'",G)
        self.assertIn('versionCode 27',G)
if __name__=='__main__':unittest.main()
