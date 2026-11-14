## v3.5.1 — Correctif SDK Android 36 (28 octobre 2026)
- Correctif Media3/AGP : API 36, AGP 8.10.1, Gradle 8.11.1.
- Conserve targetSdk 35 et les fonctionnalités Audio 432 Hz.
- Mise à jour du script Manjaro et tests de cohérence.

# Harmonie v3.5 — modifications depuis v3.4

- Service Android Media3 ExoPlayer en arrière-plan, avec notification et focus audio.
- Contrôleur `MediaController` conservant le moteur audio indépendant de la WebView.
- Importation `ACTION_OPEN_DOCUMENT` multi-fichiers avec URI persistantes et sans copie du fichier.
- Importation de dossiers `ACTION_OPEN_DOCUMENT_TREE`, parcours des sous-dossiers (1500 références maximum par import).
- Index durable en SharedPreferences, dossiers et pistes visibles dans les filtres/playlists Cyberpunk.
- Radio sélectionnée dans la page transmise au moteur natif Media3 ; HLS `.m3u8` activé.
- Lecture native 432 Hz et Original (vitesse et hauteur), et réglage du volume depuis l’interface.
- Compatibilité ancienne bibliothèque IndexedDB conservée ; aucun effacement ou migration destructive.
- Identifiant Android inchangé, versionCode 7, versionName 3.5.0, logo v3.3 conservé.
- Pas de nouveaux traitements sonores avancés : traitement DSP natif reporté à la v3.6.

## Limitations

Les quatre profils Hi-Fi restent appliqués par Web Audio aux anciens titres HTML, pas aux nouveaux titres Media3. Sans connexion au signal natif, les LED restent décoratives lors de la lecture Media3. La compilation complète et l’essai sur téléphone restent à valider.
