# Harmonie 3.5.17 — LED réellement synchronisées à la musique

- Capture FFT mono de la session audio Media3 par `android.media.audiofx.Visualizer` (morceaux et radios).
- Bouton facultatif **LED synchronisées au son (Android)** ; autorisation RECORD_AUDIO demandée seulement après activation explicite.
- Aucune capture du microphone, aucun enregistrement audio ni transmission du spectre sur Internet.
- LED : trente bandes de fréquence, RMS mono, accentuation des impulsions graves, effet visuel sans impact sur le décodage.
- Capture suspendue lorsque la fenêtre Harmonie est masquée ; lecture en arrière-plan et Bluetooth conservés.
- Si capture refusée ou non prise en charge, animations décoratives conservées ; indication distincte dans le panneau.
- Moteur Web Audio inchangé pour les anciens imports, 432 Hz et huit profils LED conservés.
- Précision : Visualizer est mono, résolution limitée, non garanti sur toutes les sorties audio et certains codecs/flux.

## Confidentialité
Android peut afficher une autorisation **Microphone** car Visualizer requiert `RECORD_AUDIO`, mais cette implémentation analyse exclusivement la session Media3 privée (ID positif) et n'appelle aucune API d'enregistrement du microphone.
