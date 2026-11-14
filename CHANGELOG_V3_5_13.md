# Harmonie Android 3.5.13 — Pochettes après réimportation

- Extraction et miniaturisation des pochettes embarquées FLAC/MP3/M4A via Android MediaMetadataRetriever.
- Cache JPG dans les fichiers privés Android (`files/harmonie_covers`), non stocké dans la bibliothèque JSON.
- Images proposées à la WebView uniquement sur son origine locale et servies à la demande.
- L'importation native par dossiers relance la lecture des métadonnées et des pochettes en arrière-plan.
- Le lecteur et les pistes affichent à nouveau les pochettes après suppression/réimportation.
- Bouton « Réanalyser les pochettes » : rescan natif en arrière-plan + relecture des imports HTML dépourvus de pochette.
- Lecture HTML : un CDN de métadonnées indisponible n'empêche plus indéfiniment les nouvelles tentatives.
- Pas de téléchargement externe arbitraire de pochettes : les images sont celles intégrées dans les fichiers.
- Id Android, audio Media3, Bluetooth, 432 Hz et interface Cyberpunk conservés.

**Limites** : les fichiers sans image intégrée conservent l'illustration par défaut. Pour les dossiers volumineux, la réanalyse peut durer ; laisser Harmonie ouverte. Compilation APK réelle à valider sur Manjaro.
