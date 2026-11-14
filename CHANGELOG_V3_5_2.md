# Harmonie 3.5.2 — Lecture continue Android écran éteint

## Problème

Les titres importés dans l'ancien stockage WebView étaient lus par JavaScript, lequel peut être suspendu quand le téléphone se verrouille. De plus, la liste native envoyée à Media3 était parfois limitée à une piste lorsque la recherche filtrait la vue.

## Correctifs

1. Construction d'une file Media3 complète à partir des morceaux Android natifs, sans tenir compte de la recherche textuelle.
2. Respect des dossiers et playlists réellement sélectionnés ; le mode Répéter conserve son sens.
3. Le service Media3 reçoit les titres d'un seul coup et prend en charge les transitions de lecture sans JavaScript.
4. Navigation précédent/suivant via Media3, sans recréer la liste.
5. Journalisation des transitions, de la taille de file et des échecs de décodage, avec saut contrôlé d'un fichier inaccessible.
6. Message dans l'interface lorsqu'un titre utilise l'ancien moteur Web Audio susceptible d'être suspendu écran éteint.
7. Maintien du mode 432 Hz (vitesse et hauteur 432/440), logo Cyberpunk, radios, playlists, interface, Hi-Fi Audio PRO.
8. Identifiant Android inchangé ; versionCode 9 / versionName 3.5.2.

## À tester sur téléphone

- Ajouter au moins 3 titres avec **+ Fichiers** (et non « Sans copie »).
- Choisir **Toutes les pistes**, désactiver la recherche, lancer un titre puis verrouiller l'écran.
- Vérifier passage au titre suivant puis mêmes essais en 432 Hz.
- Si une playlist ne contient qu'une seule piste, c'est normal qu'il n'y ait pas de titre suivant (sauf répétition du même titre).
- Si un morceau ancien est encore dans IndexedDB : le réimporter via **+ Fichiers / + Dossier** pour profiter de Media3 écran éteint.

Il s'agit d'un correctif code vérifié statiquement, **pas d'un APK testé sur appareil réel**.
