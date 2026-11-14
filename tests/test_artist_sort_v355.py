"""Tri par artiste et conservation des playlists Harmonie v3.5.5."""
import unittest
from pathlib import Path
root=Path(__file__).resolve().parents[1]
html=(root/'app/src/main/assets/harmonie.html').read_text()
java=(root/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
gradle=(root/'app/build.gradle').read_text()

class ArtistSortTests(unittest.TestCase):
    def test_sort_control_and_persistence(self):
        for value in ('playlistSort','artist-asc','artist-desc','SORT_KEY','safeSet(SORT_KEY,state.sortMode)'):
            self.assertIn(value,html)
    def test_artist_sort_keeps_saved_order(self):
        self.assertIn('function artistSortTracks(tracks)',html)
        self.assertIn('return tracks.slice()',html)
        self.assertIn('Artiste inconnu',html)
        self.assertIn('return artistSortTracks(tracks)',html)
        self.assertIn('return artistSortTracks(queue).map(t=>t.id)',html)
        self.assertIn('p.items.map(id=>byId.get(id))',html)
    def test_car_queue_reordered_without_restart(self):
        self.assertIn('reorderNativeQueue(String queueJson)',java)
        self.assertIn('c.moveMediaItem(from,i)',java)
        self.assertIn('if(!mediaId.startsWith("native:"))return',java)
        self.assertIn("nativeCall('reorderNativeQueue',JSON.stringify(nativeQueueIds()))",html)
        self.assertIn('c.setMediaItems(items,index,0)',java)
    def test_release_version(self):
        self.assertIn('versionCode 27',gradle)
        self.assertIn("versionName '3.5.20'",gradle)
if __name__=='__main__':unittest.main()
