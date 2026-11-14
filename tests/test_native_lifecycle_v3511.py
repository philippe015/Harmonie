from pathlib import Path
import re
import unittest
R=Path(__file__).resolve().parents[1]
H=(R/'app/src/main/assets/harmonie.html').read_text()
J=(R/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
S=(R/'app/src/main/java/fr/byphilou/harmonie/PlaybackService.java').read_text()
class NativeLifecycleTest(unittest.TestCase):
    def test_sync_does_not_stop_service(self):
        # Le retour de la notification Media3 dans la WebView n'envoie plus stopNative.
        marker="if(state.mode!=='android-native'){stopOldForLoad(false);"
        self.assertIn(marker,H)
    def test_restoring_last_track_does_not_erase_current_queue(self):
        self.assertIn('stopOldForLoad(autoPlay);state.id=id;',H)
        self.assertIn('if(last&&getTrack(last))await selectTrack(last,false)',H)
    def test_savepoint_is_kept_after_transient_empty_service(self):
        self.assertIn('if(player==null || player.getMediaItemCount()==0)return;',S)
        self.assertIn('if(count<1)return;',S)
    def test_switching_tracks_keeps_resume_playlist(self):
        stop=J.split('@JavascriptInterface public void stopNative()',1)[1].split('@JavascriptInterface',1)[0]
        self.assertIn('c.clearMediaItems();',stop)
        self.assertNotIn('remove("last_queue")',stop)
        clear=J.split('@JavascriptInterface public void clearNative()',1)[1].split('@JavascriptInterface',1)[0]
        self.assertIn('remove("last_queue")',clear)
    def test_controller_connect_queues_pending_commands(self):
        self.assertIn('pendingMedia3Actions.add(action)',J)
        self.assertIn('for (Consumer<MediaController> action : queued) runMedia3Action(action)',J)
        self.assertIn('controllerFuture != null && !controllerFuture.isDone()',J)
    def test_android_bluetooth_432_and_interface_retained(self):
        self.assertIn('432f/440f',J)
        self.assertIn('continuousEnabled()',S)
        self.assertIn('artist-asc',H)
        self.assertIn('appVersionFooter',H)
        self.assertIn('MediaSessionService',S)
        self.assertIn('versionCode 27',(R/'app/build.gradle').read_text())
if __name__=='__main__': unittest.main()
