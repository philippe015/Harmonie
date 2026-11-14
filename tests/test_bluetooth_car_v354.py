"""Tests ciblés Bluetooth/MediaSession (contrôles statiques du projet Android)."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'app/src/main'
ACT=(SOURCE/'java/fr/byphilou/harmonie/MainActivity.java').read_text()
SERVICE=(SOURCE/'java/fr/byphilou/harmonie/PlaybackService.java').read_text()
HTML=(SOURCE/'assets/harmonie.html').read_bytes()
MANIFEST=(SOURCE/'AndroidManifest.xml').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
ANDROID='{http://schemas.android.com/apk/res/android}'

class BluetoothCarTests(unittest.TestCase):
    def test_official_media_button_receiver_and_running_service(self):
        root=ET.fromstring(MANIFEST)
        app=root.find('application')
        services=app.findall('service')
        self.assertTrue(any(s.attrib.get(ANDROID+'name')=='.PlaybackService' for s in services))
        receivers=app.findall('receiver')
        self.assertTrue(any(r.attrib.get(ANDROID+'name') == 'androidx.media3.session.MediaButtonReceiver' and
            any(a.attrib.get(ANDROID+'name') == 'android.intent.action.MEDIA_BUTTON' for a in r.iter('action'))
            for r in receivers))
        self.assertIn('FOREGROUND_SERVICE_MEDIA_PLAYBACK',MANIFEST)

    def test_session_receives_standard_keys_not_custom_bluetooth_intents(self):
        self.assertIn('new MediaSession.Builder(this, player)',SERVICE)
        self.assertIn('.setCallback(new MediaSession.Callback()',SERVICE)
        self.assertIn('onPlaybackResumption(', SERVICE)
        self.assertNotIn('KEYCODE_MEDIA_NEXT',SERVICE)
        self.assertIn('player.setHandleAudioBecomingNoisy(true)',SERVICE)
        self.assertIn('player.setAudioAttributes(attributes, true)',SERVICE)

    def test_resume_persists_playlist_and_position_without_webview(self):
        for token in ('savePlaybackState()', 'savePlaybackProgress()', 'restoreCarQueue(isForPlayback)',
                      'getSharedPreferences(CAR_PREFS,MODE_PRIVATE)',
                      'new MediaSession.MediaItemsWithStartPosition(items,index,position)',
                      'player.setRepeatMode(', 'player.setShuffleModeEnabled(',
                      'player.setPlaybackParameters(new PlaybackParameters(speed,pitch))'):
            self.assertIn(token,SERVICE)
        self.assertIn('Uri.parse(src)', SERVICE)
        self.assertNotIn('import android.webkit.WebView;',SERVICE)

    def test_car_metadata_uses_real_tags_and_radio_name(self):
        for token in ('MediaMetadataRetriever','METADATA_KEY_TITLE', 'METADATA_KEY_ARTIST',
                      'METADATA_KEY_ALBUM','carMetadata(t)',
                      '.setArtist(artist)', '.setAlbumTitle(album)'):
            self.assertIn(token,ACT)
        self.assertIn('setArtist("Radio Internet")', ACT)
        self.assertIn('setMediaMetadata(meta)',ACT)

    def test_432_is_unchanged_and_identity_is_stable(self):
        self.assertIn('432f/440f',ACT)
        self.assertIn('new PlaybackParameters(factor,factor)',ACT)
        self.assertIn('new PlaybackParameters(1f,relativePitch)',ACT)
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn("versionName '3.5.20'",GRADLE)
        self.assertIn("applicationId 'fr.byphilou.harmonie'",GRADLE)
        self.assertIn('androidx.core:core:1.16.0',GRADLE)

    def test_cyberpunk_player_and_radio_controls_retained(self):
        for marker in (b'id="visualTheme"', b'id="musicTab"', b'id="radioTab"',
                       b'id="radioResults"', b'id="btn432"', b'id="btn440"',
                       b'id="playlistSelect"', b'BY PHILOU/CYBERPUNK', b'class="hifi-pro"'):
            self.assertIn(marker,HTML)

if __name__=='__main__':unittest.main()
