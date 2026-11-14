import unittest
from pathlib import Path
from xml.etree import ElementTree
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main'
HTML=(SRC/'assets/harmonie.html').read_text()
JAVA=(SRC/'java/fr/byphilou/harmonie/MainActivity.java').read_text()
SERVICE=(SRC/'java/fr/byphilou/harmonie/PlaybackService.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()

class NativePlayback35(unittest.TestCase):
    def test_media3_foreground_service(self):
        root=ElementTree.parse(SRC/'AndroidManifest.xml').getroot()
        ns='{http://schemas.android.com/apk/res/android}'
        app=root.find('application')
        services=app.findall('service')
        self.assertEqual(len(services),1)
        self.assertEqual(services[0].get(ns+'foregroundServiceType'),'mediaPlayback')
        self.assertIn('FOREGROUND_SERVICE_MEDIA_PLAYBACK',(SRC/'AndroidManifest.xml').read_text())
        self.assertIn('extends MediaSessionService',SERVICE)
        self.assertIn('setHandleAudioBecomingNoisy(true)',SERVICE)
        self.assertIn('new ExoPlayer.Builder',SERVICE)
    def test_permissions_and_library(self):
        for key in ('ACTION_OPEN_DOCUMENT','ACTION_OPEN_DOCUMENT_TREE',
                    'takePersistableUriPermission','native_library_v35','chooseMusicFiles',
                    'getLibrary','getState'):
            self.assertIn(key,JAVA)
        self.assertIn('if(meta.native)',HTML)
        self.assertIn('HARMONIE_NATIVE_LIBRARY',HTML)
        self.assertIn('sans duplication',HTML)
    def test_original_432_and_440_supported(self):
        self.assertIn('new PlaybackParameters(factor,factor)',JAVA)
        self.assertIn('432f/440f',JAVA)
        self.assertIn('setFrequency',JAVA)
        for item in ('btn432','btn440','ledCanvas','radioTab','fxEchoOn','eqBands','stageOverlay'):
            self.assertIn('id="'+item+'"',HTML)
    def test_build_settings_unchanged_package_and_new_version(self):
        self.assertIn("applicationId 'fr.byphilou.harmonie'",GRADLE)
        self.assertIn("versionCode 27",GRADLE)
        self.assertIn("versionName '3.5.20'",GRADLE)
        self.assertIn('media3-session',GRADLE)
        self.assertIn('media3-exoplayer-hls',GRADLE)

if __name__=='__main__':unittest.main()
