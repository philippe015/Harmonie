# Harmonie Android v3.5.6 — Diagnostic des coupures de son

## Ajouts
- Bouton « Rétablir le son » dans le panneau déroulant Diagnostic / son coupé, sous les commandes de lecture.
- Pour Media3 : relance manuelle de la sortie/décodeur au même titre et même position, sans modifier la file, le mode 432 Hz ou le volume.
- Pour Web Audio : rebranchement du graphe et tentative de reprise du contexte et du média.
- Informations de diagnostic : sortie Android, volume système, gain Media3, état du lecteur et état Web Audio.
- Journal Android `HarmoniePlayback` : erreurs du pipeline AudioTrack, sans redémarrage automatique en boucle.
- Reprise d'un contexte Web Audio suspendu lors du retour au premier plan si une lecture est signalée en cours.

## Limites
- La progression de la piste et le vumètre NE PROUVENT PAS la sortie sonore réelle. Aucun algorithme ne peut garantir la détection d'une sortie muette à partir de la seule position.
- Android peut volontairement réduire/couper le son pendant un appel, une notification prioritaire ou un changement Bluetooth. La récupération ne doit pas contourner cet arbitrage.
- Media3 et Web Audio restent des moteurs séparés ; les effets de l'égaliseur ne modifient pas le pipeline natif Media3.
- Le bouton nécessite que l'interface soit ouverte ; le diagnostic des coupures écran éteint peut aussi se faire via ADB/logcat.

## Tests
- Tests statiques Python, JS, Bash ; compilation Java/Gradle à exécuter sur Manjaro.
