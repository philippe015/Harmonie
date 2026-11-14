"""Tests réalisables sur Manjaro sans émulateur Android."""
import importlib.util
import json
from pathlib import Path
import urllib.request
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1]
PYTHON=ROOT/'app/src/main/python/harmonie_server.py'
spec=importlib.util.spec_from_file_location('harmonie_server',PYTHON)
server=importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)

class HarmonieTests(unittest.TestCase):
    def test_html_original_present(self):
        text=(ROOT/'app/src/main/assets/harmonie.html').read_text(encoding='utf-8')
        for text_to_find in ['BY PHILOU/CYBERPUNK','Ma musique','Radios Internet','eqBands','stageOverlay','fxEchoOn','fxSpaceOn','HARMONIE_ANDROID_FILES']:
            with self.subTest(text_to_find=text_to_find): self.assertIn(text_to_find,text)

    def test_url_gate(self):
        self.assertTrue(server.media_safe_url('https://de1.api.radio-browser.info/json/stations/search?limit=1'))
        self.assertFalse(server.media_safe_url('http://127.0.0.1/secret'))
        self.assertFalse(server.media_safe_url('https://evil.example/json/x'))

    def test_radio_fallback(self):
        class Fake:
            def __enter__(self):return self
            def __exit__(self,*args):pass
            def read(self,n):return b'[{"name":"Station test"}]'
        with patch.object(server.urllib.request,'urlopen',return_value=Fake()):
            data=server.remote_radio_data('/json/stations/search?limit=1')
            self.assertEqual(json.loads(data)[0]['name'],'Station test')

    def test_invalid_endpoint(self):
        with self.assertRaises(ValueError):server.remote_radio_data('/evil/endpoint')

    def test_http_local(self):
        url=server.start(str(ROOT/'app/src/main/assets'))
        self.assertIn('127.0.0.1',url)
        with urllib.request.urlopen(url+'healthz', timeout=5) as response:
            self.assertEqual(json.loads(response.read()),{'ready':True})
        with urllib.request.urlopen(url, timeout=5) as response:
            self.assertIn('Harmonie',response.read().decode('utf-8'))
        with self.assertRaises(Exception):
            urllib.request.urlopen(url+'evil',timeout=5)

    def test_rust_source_exports(self):
        rust=(ROOT/'rust-core/src/lib.rs').read_text()
        for symbol in ['harmonie_version','harmonie_stereo_width','harmonie_echo_new','harmonie_echo_process','harmonie_lrc_ms']:
            self.assertIn(f'fn {symbol}',rust)

if __name__=='__main__':unittest.main()
