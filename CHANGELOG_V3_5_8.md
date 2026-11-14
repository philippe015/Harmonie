# Harmonie Android 3.5.8 — Diagnostic de file vide et récupération

## Diagnostic du relevé utilisateur
Media3 indiquait STATE_IDLE, 0 piste, position 0 et `isPlaying=false` avec volume Android non nul.
Le diagnostic prouve une file vide sur le contrôleur, mais ne détermine pas encore pourquoi elle s'est vidée.

## Correctifs
- L'interface ne conserve plus un ancien état « lecture » quand le moteur Android ne possède plus de morceaux.
- Le bouton « Rétablir le son » reconstruit manuellement une file native vide depuis les pistes indexées ou relance la radio active.
- Le service préserve la dernière file valide en cas de timeline vide transitoire.
- Le journal Media3 note les événements de timeline, la dernière commande, et la taille de la file de reprise.
- Aucune reprise forcée à la suite d'une pause ou d'une interruption de priorité audio.
- Bluetooth, playlists, tri artistes, interface Cyberpunk, 432 Hz conservés.

## Vérification indispensable
La compilation et les essais sur Android sont nécessaires. Quand la coupure se reproduit, relever Diagnostic / son coupé (notamment dernière commande, dernier événement du service).
