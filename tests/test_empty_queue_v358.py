from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
S=(R/'app/src/main/java/fr/byphilou/harmonie/PlaybackService.java').read_text()
J=(R/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
H=(R/'app/src/main/assets/harmonie.html').read_text()
G=(R/'app/build.gradle').read_text()
class EmptyQueueV358(unittest.TestCase):
    def test_0_queue_does_not_erase_backup(self):
        self.assertIn('if(count<1)return;',S)
        self.assertNotIn('if(count<1){getSharedPreferences',S)
    def test_explicit_stop_clears_backup(self):
        self.assertIn('.remove("last_queue").remove("last_position")',J)
    def test_ui_no_false_playing(self):
        self.assertIn('if(nativeLive.queueSize===0)',H)
        self.assertIn('nativeLive.playing=false;',H)
    def test_manual_recovery_rebuilds_empty_queue(self):
        self.assertIn('Number(d.mediaItemCount)===0',H)
        self.assertIn("nativeCall('playNative',state.id,JSON.stringify(ids.length?ids:[state.id])",H)
        self.assertIn("nativeCall('playRadio',radio.active.url",H)
        self.assertIn("nativeCall('restoreSavedQueue',state.volume)",H)
        self.assertIn('restoreSavedQueueOnMain(c,volume)',J)
        self.assertIn('c.setMediaItems(items,index,position)',J)
    def test_diagnostic_has_command_and_timeline(self):
        self.assertIn('last_service_event',S)
        self.assertIn('d.put("lastCommand"',J)
        self.assertIn('d.put("savedQueueCount"',J)
    def test_version_and_features(self):
        self.assertIn('versionCode 27',G)
        self.assertIn("versionName '3.5.20'",G)
        self.assertIn('432f/440f',J)
        self.assertIn('artist-asc',H)
if __name__=='__main__':unittest.main()
