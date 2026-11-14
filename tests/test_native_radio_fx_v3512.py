from pathlib import Path
import re
import unittest
R=Path(__file__).resolve().parents[1]
H=(R/'app/src/main/assets/harmonie.html').read_text()
J=(R/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
S=(R/'app/src/main/java/fr/byphilou/harmonie/PlaybackService.java').read_text()
E=(R/'app/src/main/java/fr/byphilou/harmonie/NativeAudioEffects.java').read_text()
G=(R/'app/build.gradle').read_text()

class RadioFxTests(unittest.TestCase):
    def test_effect_is_attached_to_native_audio_session(self):
        self.assertIn('onAudioSessionIdChanged(int audioSessionId)',S)
        self.assertIn('nativeEffects.onSessionChanged(audioSessionId)',S)
        self.assertIn('new DynamicsProcessing(0,id,cfg)',E)
        self.assertIn('new Equalizer(0,id)',E)
        self.assertIn('setPreEqBandAllChannelsTo(i,',E)
    def test_no_effect_on_global_session_and_release(self):
        self.assertIn('if(id<=0 || id==0)',E)
        self.assertIn('nativeEffects.release()',S)
        self.assertIn('closeEffect()',E)
        self.assertIn('catch(Exception | LinkageError failure)',E)
    def test_radio_432_change_pitch_not_stream_speed(self):
        radio=J.split('public void playRadio(',1)[1].split('@JavascriptInterface',1)[0]
        self.assertIn('frequency==432?432f/440f:1f',radio)
        self.assertIn('new PlaybackParameters(1f,relativePitch)',radio)
        self.assertIn('new PlaybackParameters(1f,hz==432?432f/440f:1f)',J)
        self.assertIn('new PlaybackParameters(factor,factor)',J) # local remains old mode
    def test_js_bridge_and_radio_switch_no_restart(self):
        self.assertIn("nativeCall('playRadio',st.url,st.name,state.volume,state.frequency)",H)
        self.assertIn("nativeCall('playRadio',radio.active.url,radio.active.name||'Radio Internet',state.volume,state.frequency)",H)
        self.assertIn('void setNativeAudioEffects(String json)',J)
        self.assertIn("radioEnabled:els.radioEQ.checked",H)
        self.assertIn('syncNativeAudioEffects();if(radio.active&&radio.native)',H)
    def test_only_existing_eq_sliders(self):
        self.assertEqual(len(re.findall(r'const EQ_FREQS=',H)),1)
        self.assertEqual(len(re.findall(r'id="eqBands"',H)),1)
        self.assertIn('inputGainAllChannelsTo', E.replace('setInputGainAllChannelsTo','inputGainAllChannelsTo'))
    def test_settings_persist_and_audio_state_exposed(self):
        self.assertIn('getSharedPreferences(NativeAudioEffects.PREFS,MODE_PRIVATE)',J)
        self.assertIn('d.put("nativeEqStatus"',J)
        self.assertIn('d.put("playbackPitch"',J)
        self.assertIn('getSharedPreferences(PREFS,Context.MODE_PRIVATE)',E)
        self.assertIn('super.onStartCommand(intent,flags,startId)',S)
        self.assertIn('nativeEffects.onMediaItemChanged(item!=null && item.mediaId.startsWith("radio:"))',S)
    def test_version_and_appearance(self):
        self.assertIn('versionCode 27',G)
        self.assertIn("versionName '3.5.20'",G)
        self.assertIn('VERSION 3.5.20 · BUILD 27',H)
        for marker in ['appVersionFooter','artist-asc','continuousPlayback','data-pro-profile','logo']:
            self.assertIn(marker,H)

if __name__=='__main__':unittest.main()
