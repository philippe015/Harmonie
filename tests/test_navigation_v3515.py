import re
import unittest
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HTML = (ROOT/'app/src/main/assets/harmonie.html').read_text()
GRADLE = (ROOT/'app/build.gradle').read_text()
JAVA = (ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()

class NodeIds(HTMLParser):
    def __init__(self):
        super().__init__(); self.ids = []
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if 'id' in attrs: self.ids.append(attrs['id'])

class NavigationTests(unittest.TestCase):
    def test_new_tabs_are_complete_and_unique(self):
        parser=NodeIds(); parser.feed(HTML)
        self.assertEqual(len(parser.ids),len(set(parser.ids)), 'Duplicate element id in HTML')
        for identifier in ('musicSubTracks','musicSubPlaylists','musicSubTools','radioSubExplore','radioSubFavorites',
                           'musicTracksPane','musicPlaylistsPane','musicToolsPane','radioExplorePane','radioFavoritesPane',
                           'musicListArea','radioPages'):
            self.assertIn(identifier,parser.ids)
    def test_music_subnavigation_does_not_change_player(self):
        m=re.search(r'function showMusicSubtab\(name\)\{(.*?)\n  \}',HTML,re.S)
        self.assertIsNotNone(m)
        for required in ("$('musicListArea').hidden",'safeSet(MUSIC_SUBTAB_KEY','renderLibrary()'):
            self.assertIn(required,m.group(1))
        for forbidden in ('stopNative','playNative','stopRadio(','pauseNative','playRadio('):
            self.assertNotIn(forbidden,m.group(1))
    def test_radio_favorites_are_separate_from_search(self):
        self.assertIn("$('radioSubFavorites').addEventListener('click',()=>showRadioSubtab('favorites'))",HTML)
        self.assertIn("$('radioPages').hidden=radio.onlyFavorites",HTML)
        self.assertIn("radio.onlyFavorites=name==='favorites'",HTML)
    def test_existing_audio_features_and_ids_kept(self):
        for marker in ['id="btn432"','id="btn440"','id="eqBands"','id="playBtn"',
                       'id="radioResults"','id="playlistSort"','id="radioEQ"','data-pro-profile="fidelity"',
                       'function syncNativeAudioEffects','function playRadio(st)','id="appVersionFooter"']:
            self.assertIn(marker,HTML)
    def test_version_and_android_identity(self):
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn("versionName '3.5.20'",GRADLE)
        self.assertIn('VERSION 3.5.20 · BUILD 27',HTML)
        self.assertIn('VERSION 3.5.20 · BUILD 27',JAVA)
    def test_settings_persist_and_first_tab_open(self):
        self.assertIn('MUSIC_SUBTAB_KEY',HTML)
        self.assertIn('RADIO_SUBTAB_KEY',HTML)
        self.assertIn("showMusicSubtab(safeGet(MUSIC_SUBTAB_KEY)||'tracks')",HTML)
        self.assertIn("showRadioSubtab(safeGet(RADIO_SUBTAB_KEY)||'explore')",HTML)

if __name__=='__main__':unittest.main()
