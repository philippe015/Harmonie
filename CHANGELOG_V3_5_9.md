# Harmonie Android v3.5.9 — Lecture continue en arrière-plan

- Correction de la fin de file native : Media3 relance le début de la sélection lorsque « Lecture continue » est activé, même quand l'écran est verrouillé et la WebView suspendue.
- Correction de la reprise de session Media3 : la file restaurée contient maintenant TOUS les morceaux, y compris lors de l'aperçu de notification Bluetooth (plus de reprise limitée à un seul titre).
- La lecture normale du titre suivant au milieu d'une file reste gérée par ExoPlayer. Si la file contient un seul morceau, la lecture continue répète ce seul morceau (il n'existe pas de titre suivant dans cette sélection).
- Nouveau interrupteur discret « Lecture continue (écran éteint) », actif par défaut et mémorisé ; désactivable, distinct du bouton de répétition.
- Aucune relance après pause volontaire, ni pour les stations radio, ni après une fin si la préférence est désactivée.
- Diagnostic : dernière fin de file, taille, index, statut et activation de la lecture continue.
- Ne plus publier la position du lecteur Media3 à la MediaSession WebView concurrente.
- Contrôles Bluetooth, 432 Hz, tri artiste, Hi-Fi et identité Cyberpunk conservés.

**Limites :** les titres enregistrés uniquement dans l'ancienne bibliothèque HTML ne bénéficient pas de ce mode natif ; importer depuis « + Fichiers » ou « + Dossier » Android. Ce projet est fourni pour compilation sur Manjaro ; aucun essai APK réel sur téléphone n'est effectué ici.
