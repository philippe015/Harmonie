import unittest
from pathlib import Path
from html.parser import HTMLParser
ROOT=Path(__file__).resolve().parents[1]
H=(ROOT/'app/src/main/assets/harmonie.html').read_text()
class LedModeTests(unittest.TestCase):
    def test_modes_are_available(self):
        for m in ['spectrum','vu','ambient','radial','waveform','particles','matrixrain','pulse','tunnel','off']:
            self.assertIn(f'<option value="{m}">',H)
    def test_palettes_are_available(self):
        for p in ['cyber','rainbow','ice','fire','matrix','plasma','sunset','gold']:
            self.assertIn(f'<option value="{p}">',H)
    def test_native_decorative_fallback_is_not_forced_ambient(self):
        self.assertIn("led.mode==='ambient'",H)
        self.assertNotIn("playing&&(!signal||led.mode==='ambient')",H)
        self.assertIn('animation décorative (Media3',H)
    def test_preview_repaints_canvas(self):
        self.assertIn('drawLED(1000,false,false,true)',H)
        self.assertIn('ledPseudoLevels(t,preview)',H)
    def test_visual_theme_also_updates_led_color(self):
        self.assertIn('led.palette=visuals.theme',H)
        self.assertIn('applyLedPaletteFrame();syncLedState(true)',H)
    def test_background_and_accessibility(self):
        self.assertIn('!document.hidden&&!led.reduced',H)
        self.assertIn('t-led.last>=33',H)
    def test_audio_modes_untouched(self):
        for marker in ('id="btn432"','id="btn440"','id="radioResults"','id="eqBands"','id="playlistSort"','id="appVersionFooter"'):
            self.assertIn(marker,H)
if __name__=='__main__':unittest.main()
