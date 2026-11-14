import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HTML = (ROOT / "app/src/main/assets/harmonie.html").read_text(encoding="utf-8")
GRADLE = (ROOT / "app/build.gradle").read_text(encoding="utf-8")

class HiFiProTests(unittest.TestCase):
    def test_eight_buttons_and_unchanged_core_controls(self):
        for label in ("fidelity", "bass", "concert", "spatial", "vocal", "rock", "classical", "night"):
            self.assertIn(f'data-pro-profile="{label}"', HTML)
        self.assertEqual(len(re.findall(r'data-pro-profile="', HTML)), 8)
        for original_id in ('eqBands','fxEchoOn','fxSpaceOn','radioTab','musicTab','stageOverlay','ledCanvas','btn432'):
            self.assertIn(f'id="{original_id}"', HTML)

    def test_original_fidelity_has_no_effects_and_original_speed(self):
        self.assertIn('gains:[0,0,0,0,0,0,0,0],eqEnabled:false,hifi:false,echoOn:false', HTML)
        self.assertIn('if(p.original&&state.frequency!==440)setFrequency(440)', HTML)

    def test_profile_persistence_and_custom_state(self):
        self.assertIn("const PRO_KEY='harmonie-v3.4-hifi-audio-pro'", HTML)
        self.assertIn('function markProCustom()', HTML)
        self.assertIn('function applyProProfile(', HTML)
        self.assertIn('initProProfileControls();', HTML)

    def test_android_package_and_update_code(self):
        self.assertIn("applicationId 'fr.byphilou.harmonie'", GRADLE)
        self.assertIn('versionCode 27', GRADLE)
        self.assertIn("versionName '3.5.20'", GRADLE)

if __name__ == '__main__': unittest.main()
