import pathlib, unittest
R = pathlib.Path(__file__).resolve().parents[1]
HTML = (R/'app/src/main/assets/harmonie.html').read_text()
BRIDGE = (R/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
SERVICE = (R/'app/src/main/java/fr/byphilou/harmonie/PlaybackService.java').read_text()
AMBIENCE = (R/'app/src/main/java/fr/byphilou/harmonie/NativeAmbienceEffects.java').read_text()
GRADLE = (R/'app/build.gradle').read_text()
class TestNativeHifi(unittest.TestCase):
 def test_version(self):
  self.assertIn("versionName '3.5.20'", GRADLE)
  self.assertIn('versionCode 27', GRADLE)
 def test_tone_activates_radio(self):
  self.assertIn('function enableRadioEqForPro()',HTML)
  self.assertIn('els.radioEQ.checked=true',HTML)
  self.assertIn('enableRadioEqForPro();routeDSP()',HTML)
 def test_ambience_sent_to_native(self):
  for p in ('ambienceOn','ambienceAmount','virtualizerOn','virtualizerAmount'):
   self.assertIn(p,HTML)
   self.assertIn(p,BRIDGE)
   self.assertIn(p,AMBIENCE)
 def test_session_and_refresh(self):
  self.assertIn('nativeAmbience.onSessionChanged(audioSessionId)',SERVICE)
  self.assertIn('nativeAmbience.apply()',SERVICE)
  self.assertIn('nativeAmbience.release()',SERVICE)
 def test_safe_audio(self):
  self.assertIn('player.setAuxEffectInfo(',AMBIENCE)
  self.assertIn('new Virtualizer(0,sessionId)',AMBIENCE)
  self.assertIn('catch(Exception | LinkageError',AMBIENCE)
 def test_status_truthful(self):
  self.assertIn('nativeAmbienceStatus',HTML)
  self.assertIn('nativeAmbienceStatus',BRIDGE)
  self.assertIn('pas écho à répétitions',AMBIENCE)
if __name__=='__main__': unittest.main()
