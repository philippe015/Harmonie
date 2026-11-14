# Contribuer à Harmonie

Merci de contribuer au projet **BY PHILOU/CYBERPUNK**.

## Avant une Pull Request

- Ouvrez une issue si le changement est important pour discuter de son objectif.
- Évitez les modifications visuelles non demandées : l'interface Cyberpunk est une caractéristique à conserver.
- N'introduisez pas d'accès réseau à la bibliothèque locale ni de collecte de données sans justification explicite.
- Préservez la compatibilité des préférences, playlists et fichiers Android autorisés.
- Exécutez `python -m unittest discover -s tests -p 'test_*.py' -v`.
- Pour le code Android, indiquez si `bash compiler_manjaro.sh` a réellement créé l'APK ; ne présentez pas les tests statiques comme une compilation complète.
- Joignez un scénario de validation : fichier FLAC, radio, enchaînement écran éteint, Bluetooth, 432 Hz, égaliseur, pochettes et LED selon la zone modifiée.

## Format de la contribution

Une PR doit expliquer : le problème, la cause, les fichiers modifiés, les tests réellement effectués et les limitations restantes.

Ne fournissez ni APK signé avec une clé personnelle ni fichiers audio sous droits. Les contributions de code sont proposées selon la licence MIT du projet.
