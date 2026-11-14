# Harmonie 3.5.4 — Bluetooth voiture & commandes volant

- MediaSession Media3 : play/pause, suivant/précédent via touches AVRCP, notification Android et casque sans intercept manuel.
- Manifest : MediaButtonReceiver officiel pour une reprise après arrêt du service.
- Session.Callback.onPlaybackResumption : restauration des titres, de la file native, de la position et des paramètres 432/440 Hz.
- Sauvegarde périodique de la file, ainsi qu'aux transitions et changements d'état.
- Métadonnées titre, artiste et album lues à l'import de fichiers via MediaMetadataRetriever, et transférées aux MediaItem.
- Flux radio : nom de la station et « Radio Internet » dans la MediaSession.
- Dépendance AndroidX Core explicite pour MediaButtonReceiver.
- Identifiant fr.byphilou.harmonie et UI Cyberpunk inchangés ; v3.5.4 (code 11).

Limites : lecture Bluetooth et reprise concernent le moteur natif Media3. Un autoradio doit prendre en charge A2DP et AVRCP, et afficher ce que son firmware permet. Ce projet n'est pas une interface Android Auto. Les effets Web Audio ne sont pas actifs dans Media3 ; prévue pour v3.6.
