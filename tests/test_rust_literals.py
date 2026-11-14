"""Vérifications précompilation des littéraux numériques Rust."""
from pathlib import Path
import re
import unittest

SOURCE = Path(__file__).resolve().parents[1] / 'rust-core' / 'src' / 'lib.rs'

class RustSourceTests(unittest.TestCase):
    def test_no_float_without_integer_part(self):
        text = SOURCE.read_text(encoding='utf-8')
        # Ignore les plages `0..=999`, mais pas les nombres `.85`, `.3`, `.001`.
        self.assertEqual(re.findall(r'(?<![\w.\d])\.\d+\b', text), [])

    def test_echo_feedback_and_smoke_test_use_valid_literals(self):
        source = SOURCE.read_text(encoding='utf-8')
        self.assertIn('feedback.clamp(0.0,0.85)', source)
        self.assertIn('harmonie_echo_new(48000,100,0.3)', source)
if __name__ == '__main__':
    unittest.main()
