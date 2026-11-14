# Harmonie Android v3.5.12 — Égaliseur et 432 Hz sur les radios Media3

- Radio native : possibilité d'activer l'égaliseur depuis la case radio existante.
- L'égaliseur 8 bandes est relié à la session audio du MediaSessionService via DynamicsProcessing (Android 9+), si pris en charge par la sortie audio.
- Repli Android 8/8.1 ou si effet 8 bandes absent : égaliseur système avec bandes matérielles ; si aucun effet, la radio continue sans traitement.
- Un changement de bande met à jour la session sans redémarrer le flux et sans toucher à la playlist.
- Radio native : option 432 Hz / Original. En 432 Hz, seul le pitch varie de 432/440 (environ -31,8 cents), le tempo reste à 100 %. C'est une transformation relative ; l'accordage de la station reste inconnu.
- Local : comportement 432 Hz précédent conservé.
- Le diagnostic signale le type d'égaliseur réellement attaché ou son indisponibilité.
- Les effets Web Audio d'écho/spatialisation ne sont pas connectés à Media3 et ne sont donc pas revendiqués pour les radios natives.
- L'interface Cyberpunk, les boutons Bluetooth voiture et la lecture en arrière-plan sont conservés.
- Version : 3.5.12, build 19.
