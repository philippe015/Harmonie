from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
JS = (ROOT/'app/src/main/assets/harmonie.html').read_text()
JAVA = (ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
GRADLE = (ROOT/'app/build.gradle').read_text()
class ArtworkReloadTests(unittest.TestCase):
    def test_cached_artwork_endpoints(self):
        self.assertIn('/__native_cover__/',JAVA)
        self.assertIn('nativeCoverResponse(',JAVA)
        self.assertIn('artForTrack(t)',JS)
        self.assertIn('artForTrack(tr)',JS)
    def test_embedded_art_extraction_and_persistence(self):
        self.assertIn('getEmbeddedPicture()',JAVA)
        self.assertIn('harmonie_covers',JAVA)
        self.assertIn('hasCover',JAVA)
        self.assertIn('BitmapFactory.Options',JAVA)
    def test_reanalyze_existing_tracks(self):
        self.assertIn('refreshNativeCovers()',JAVA)
        self.assertIn('readCarTags(t, Uri.parse(uri))',JAVA)
        self.assertIn('refreshAllCovers()',JS)
        self.assertIn('Réanalyser les pochettes',JS)
        self.assertIn('await enrichTrackMeta(ids[i],{force:true})',JS)
    def test_folder_import_reindexes_artwork(self):
        self.assertIn('if(added>0)reindexCoversAsync();',JAVA)
        self.assertIn('coversReindexing.compareAndSet(false,true)',JAVA)
        self.assertIn('JSONArray latest = loadNativeLibrary()',JAVA)
    def test_cache_invalidation(self):
        self.assertIn('coverRevision',JAVA)
        self.assertIn('t.coverRevision||0',JS)
    def test_invalid_cover_paths_rejected(self):
        self.assertIn('id.matches("native:[0-9a-fA-F-]{36}")',JAVA)
    def test_updates_version(self):
        self.assertIn('versionCode 27',GRADLE)
        self.assertIn("versionName '3.5.20'",GRADLE)
if __name__=='__main__':unittest.main()
