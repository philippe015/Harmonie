from pathlib import Path
import re,unittest
ROOT=Path(__file__).resolve().parents[1]
H=(ROOT/'app/src/main/assets/harmonie.html').read_text()
G=(ROOT/'app/build.gradle').read_text()
class HiFiAdvancedTests(unittest.TestCase):
    def test_no_separate_fx_panel(self):
        self.assertNotIn('EFFETS AUDIO · ÉCHO & SPATIAL',H)
        self.assertEqual(H.count('id="fxExtra"'),1)
        self.assertTrue(H.index('id="hifiPro"')<H.index('id="fxExtra"')<H.index('id="proAdvanced"')+4000)
    def test_eight_profiles_and_tone_sliders(self):
        self.assertEqual(len(re.findall(r'data-pro-profile="',H)),8)
        for id in ['proBass','proVoice','proTreble','proSave','proLoad','proBypass','proEngineNote','fxEchoOn','fxSpaceOn']:
            self.assertIn('id="'+id+'"',H)
    def test_audio_effect_wiring(self):
        for code in ['function applyProToneControls()','function saveProPersonal()','function loadProPersonal()','function bypassProEffects()','syncNativeAudioEffects()','routeDSP();syncEqUI();persistEqualizer()']:
            self.assertIn(code,H)
        for x in ['id="btn432"','id="btn440"','id="radioTab"','id="ledNativeSync"']:
            self.assertIn(x,H)
    def test_version(self):
        self.assertIn('versionCode 27',G)
        self.assertIn("versionName '3.5.20'",G)
        self.assertIn('VERSION 3.5.20 · BUILD 27',H)
