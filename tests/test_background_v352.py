"""Regressions: never rely on a hidden WebView to advance a native playlist."""
import re
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main'
HTML=(SRC/'assets/harmonie.html').read_text()
JAVA=(SRC/'java/fr/byphilou/harmonie/MainActivity.java').read_text()
SERVICE=(SRC/'java/fr/byphilou/harmonie/PlaybackService.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()

class BackgroundPlaylistTests(unittest.TestCase):
    def test_media3_gets_complete_queue_and_native_repeat(self):
        self.assertIn('c.setMediaItems(items,index,0)', JAVA)
        self.assertIn('c.setRepeatMode(repeat==1?',JAVA)
        self.assertIn('c.setShuffleModeEnabled(shuffle)', JAVA)
        self.assertIn('"queueSize"', JAVA)
        self.assertIn('"queueIndex"', JAVA)
        self.assertIn('"hasNext"', JAVA)
        self.assertIn('PlaybackParameters(factor,factor)', JAVA)
    def test_native_next_previous_and_error_recovery(self):
        self.assertIn('nextNative()',JAVA)
        self.assertIn('previousNative()',JAVA)
        self.assertIn('seekToNextMediaItem()',JAVA)
        self.assertIn('seekToPreviousMediaItem()',JAVA)
        self.assertIn('onPlayerError(PlaybackException error)', SERVICE)
        self.assertIn('onMediaItemTransition(', SERVICE)
        self.assertIn('getMediaItemCount() > 1',SERVICE)
    def test_queue_is_unfiltered_by_search_and_preserves_playlist(self):
        func=re.search(r'function nativeQueueIds\(\)\{(.*?)\n  \}', HTML, re.S)
        self.assertIsNotNone(func)
        self.assertIn('state.tracks.filter(t=>t.native)',func.group())
        self.assertIn('isFolderFilter()',func.group())
        self.assertIn('isCustomFilter()',func.group())
        self.assertNotIn('currentPlaylistTracks()',func.group())
        self.assertIn('JSON.stringify(nativeIds',HTML)
        self.assertIn('state.repeat,state.shuffle',HTML)
    def test_legacy_library_has_clear_background_warning(self):
        self.assertIn('function playbackModeHint(',HTML)
        self.assertIn('réimportez',HTML)
        self.assertIn('Sans copie',HTML)
    def test_version_identity_kept_and_incremented(self):
        self.assertIn("applicationId 'fr.byphilou.harmonie'", GRADLE)
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn("versionName '3.5.20'",GRADLE)
        self.assertIn('compileSdk 36', GRADLE)
if __name__ == '__main__': unittest.main()
