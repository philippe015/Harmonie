# Feuille de route communautaire

Ce sont des objectifs, **pas des fonctionnalités déjà garanties**.

## Priorité 1 — stabilité Android

- Reproduire et corriger les interruptions de lecture / pertes de file Media3 écran verrouillé.
- Tester les boutons AVRCP/Bluetooth de différents véhicules et périphériques.
- Renforcer le cycle de vie MediaSessionService et la restauration de file.
- Mettre en place un test CI Android réel (Gradle et SDK 36), en plus des contrôles statiques.

## Priorité 2 — bibliothèque et interface

- Préserver les URI Android et leurs autorisations de manière résiliente.
- Améliorer extraction et cache des pochettes, grands catalogues FLAC et suppression/importation.
- Améliorer navigation sous-onglets, accessibilité et affichage sur petits écrans.

## Priorité 3 — audio avancé (v3.6)

- Harmoniser les traitements Hi-Fi PRO pour Web Audio et Media3 sans suggérer un effet sonore non réellement appliqué.
- Ajouter des diagnostics clairs de compatibilité des effets Android selon appareil/route audio.
- Étudier un traitement DSP natif plus complet, avec contrôle des performances, latence et clipping.
- Valider précisément le comportement du mode 432 Hz sur fichiers et radios.

## Priorité 4 — rendu LED

- Tester l'API Visualizer sur divers téléphones et casques Bluetooth.
- Optimiser la fréquence de rafraîchissement, consommation et animations inactives.

**Contribuer :** ouvrez une Issue, proposez un correctif documenté et soumettez une Pull Request.
