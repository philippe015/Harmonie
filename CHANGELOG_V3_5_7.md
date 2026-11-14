# Harmonie Android v3.5.7 — Correctif diagnostic MediaController / thread

## Défaut constaté sur téléphone

`java.lang.IllegalStateException: MediaController method is called from a wrong thread.`

`@JavascriptInterface` exécute ses méthodes sur un thread JavaScript WebView différent du thread principal. La v3.5.6 lisait `MediaController` directement depuis `getAudioDiagnostics`, ce qui déclenchait cette exception et empêchait tout diagnostic fiable de l'audio.

## Correctif

- Tous les getters MediaController du diagnostic sont lus depuis le **main Looper**, sur lequel le MediaController a été créé.
- Les données sont publiées dans un instantané `volatile` et le bridge WebView ne fait que lire cet instantané.
- Actualisation environ toutes les 1,5 secondes, ainsi qu'après connexion et commandes du lecteur.
- Diagnostic enrichi (volume, état Media3, position, taille de file, erreurs, suspension audio, heure de mesure, sorties disponibles).
- Pas de blocage du thread de la WebView ni de demande de permissions supplémentaires.
- Aucun redémarrage automatique en boucle.
- L'exception relevée était **dans le diagnostic** ; la cause de la coupure sonore initiale reste à établir avec les mesures maintenant fonctionnelles.

## Conservé

Interface Cyberpunk, logo, lecture native Media3 en veille, Bluetooth voiture, playlists et tri par artiste, radios, 432 Hz / Original et profils Hi-Fi.

Version Android `3.5.7` / code `14` ; applicationId inchangé.
